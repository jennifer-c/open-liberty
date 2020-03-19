/*******************************************************************************
 * Copyright (c) 2015, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http.logging.source;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.http.channel.internal.values.AccessLogCurrentTime;
import com.ibm.ws.http.channel.internal.values.AccessLogElapsedRequestTime;
import com.ibm.ws.http.channel.internal.values.AccessLogElapsedTime;
import com.ibm.ws.http.channel.internal.values.AccessLogFirstLine;
import com.ibm.ws.http.channel.internal.values.AccessLogLocalIP;
import com.ibm.ws.http.channel.internal.values.AccessLogLocalPort;
import com.ibm.ws.http.channel.internal.values.AccessLogRemoteHost;
import com.ibm.ws.http.channel.internal.values.AccessLogRemoteIP;
import com.ibm.ws.http.channel.internal.values.AccessLogRemoteUser;
import com.ibm.ws.http.channel.internal.values.AccessLogRequestCookie;
import com.ibm.ws.http.channel.internal.values.AccessLogRequestHeaderValue;
import com.ibm.ws.http.channel.internal.values.AccessLogResponseHeaderValue;
import com.ibm.ws.http.channel.internal.values.AccessLogResponseSize;
import com.ibm.ws.http.channel.internal.values.AccessLogResponseSizeB;
import com.ibm.ws.http.channel.internal.values.AccessLogStartTime;
import com.ibm.ws.http.logging.internal.AccessLogger.FormatSegment;
import com.ibm.ws.logging.data.AccessLogData;
import com.ibm.ws.logging.data.KeyValuePairList;
import com.ibm.wsspi.collector.manager.BufferManager;
import com.ibm.wsspi.collector.manager.Source;
import com.ibm.wsspi.http.HttpCookie;
import com.ibm.wsspi.http.channel.HttpRequestMessage;
import com.ibm.wsspi.http.channel.HttpResponseMessage;
import com.ibm.wsspi.http.logging.AccessLogForwarder;
import com.ibm.wsspi.http.logging.AccessLogRecordData;
import com.ibm.wsspi.http.logging.LogForwarderManager;

/**
 *
 */
public class AccessLogSource implements Source {

    private static final TraceComponent tc = Tr.register(AccessLogSource.class);

    private final String sourceName = "com.ibm.ws.http.logging.source.accesslog";
    private final String location = "memory";
    private static String USER_AGENT_HEADER = "User-Agent";

    private BufferManager bufferMgr = null;
    private AccessLogHandler accessLogHandler;

    protected synchronized void activate(Map<String, Object> configuration) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Activating " + this);
        }
    }

    protected void deactivate(int reason) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, " Deactivating " + this, " reason = " + reason);
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getSourceName() {
        return sourceName;
    }

    /** {@inheritDoc} */
    @Override
    public String getLocation() {
        return location;
    }

    /** {@inheritDoc} */
    @Override
    public void setBufferManager(BufferManager bufferMgr) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Setting buffer manager " + this);
        }
        this.bufferMgr = bufferMgr;
        startSource();
    }

    /** {@inheritDoc} */
    @Override
    public void unsetBufferManager(BufferManager bufferMgr) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Un-setting buffer manager " + this);
        }
        //Indication that the buffer will no longer be available
        stopSource();
        this.bufferMgr = null;
    }

    /**
     *
     */
    private void startSource() {
        accessLogHandler = new AccessLogHandler();
        LogForwarderManager.registerAccessLogForwarder(accessLogHandler);
    }

    /**
     *
     */
    private void stopSource() {
        LogForwarderManager.deregisterAccessLogForwarder(accessLogHandler);
        accessLogHandler = null;
    }

    private class AccessLogHandler implements AccessLogForwarder {

        private final AtomicLong seq = new AtomicLong();

        /** {@inheritDoc} */
        @Override
        public void process(AccessLogRecordData recordData, FormatSegment[] parsedFormat) {
            HttpRequestMessage request = recordData.getRequest();
            HttpResponseMessage response = recordData.getResponse();
            KeyValuePairList kvplCookies = new KeyValuePairList("cookies");
            KeyValuePairList kvplRequestHeaders = new KeyValuePairList("requestHeaders");
            KeyValuePairList kvplResponseHeaders = new KeyValuePairList("responseHeaders");
            ArrayList<String> formatSpecifiers = new ArrayList<String>();

            if (request != null) {

                AccessLogData accessLogData = new AccessLogData();

                long requestStartTimeVal = recordData.getStartTime(); // needed for sequence
                // LG 265
                if (AccessLogData.isCustomAccessLogToJSONEnabled.equals("logFormat") || AccessLogData.isCustomAccessLogToJSONEnabledCollector.equals("logFormat")) {
                    for (FormatSegment s : parsedFormat) {
                        if (s.log != null) {
                            String formatSpecifier = s.log.getName();
                            formatSpecifiers.add(formatSpecifier);
                            switch (formatSpecifier) {
                                case "%C":
                                    if (s.data != null) {
                                        HttpCookie c = AccessLogRequestCookie.getCookie(response, request, s.data);
                                        if (c != null)
                                            kvplCookies.addKeyValuePair(c.getName(), c.getValue());
                                    } else {
                                        // If data is null, they specified %C without a cookie name, which returns all cookies
                                        List<HttpCookie> cookies = AccessLogRequestCookie.getAllCookies(response, request, null);
                                        for (HttpCookie c : cookies) {
                                            if (c != null)
                                                kvplCookies.addKeyValuePair(c.getName(), c.getValue());
                                        }
                                    }
                                    break;
                                case "%i":
                                    if (s.data.equals(USER_AGENT_HEADER))
                                        accessLogData.setUserAgent(request.getHeader(USER_AGENT_HEADER).asString(), formatSpecifiers);
                                    else if (s.data != null)
                                        kvplRequestHeaders.addKeyValuePair((String) s.data, AccessLogRequestHeaderValue.getHeaderValue(response, request, s.data));
                                    break;
                                case "%o":
                                    if (s.data != null)
                                        kvplResponseHeaders.addKeyValuePair((String) s.data, AccessLogResponseHeaderValue.getHeaderValue(response, request, s.data));
                                    break;
                            }
                        }
                    }
                    accessLogData.setRemoteIP(AccessLogRemoteIP.getRemoteIP(response, request, null), formatSpecifiers);
                    accessLogData.setRequestHost(AccessLogLocalIP.getLocalIP(response, request, null), formatSpecifiers);
                    accessLogData.setBytesSent(AccessLogResponseSize.getResponseSizeAsString(response, request, null), formatSpecifiers);
                    accessLogData.setBytesReceived(AccessLogResponseSizeB.getBytesReceived(response, request, null), formatSpecifiers);
                    accessLogData.setRequestElapsedTime(AccessLogElapsedTime.getElapsedTime(response, request, null), formatSpecifiers);
                    accessLogData.setRemoteHost(AccessLogRemoteHost.getRemoteHostAddress(response, request, null), formatSpecifiers);
                    accessLogData.setRequestProtocol(request.getVersion(), formatSpecifiers);
                    accessLogData.setRequestMethod(request.getMethod(), formatSpecifiers);
                    accessLogData.setRequestPort(AccessLogLocalPort.getLocalPort(response, request, null), formatSpecifiers);
                    accessLogData.setQueryString(request.getQueryString(), formatSpecifiers);
                    accessLogData.setRequestFirstLine(AccessLogFirstLine.getFirstLineAsString(response, request, null), formatSpecifiers);
                    accessLogData.setElapsedTime(AccessLogElapsedRequestTime.getElapsedRequestTime(response, request, null), formatSpecifiers);
                    accessLogData.setResponseCode(response.getStatusCodeAsInt(), formatSpecifiers);
                    accessLogData.setRequestStartTime(AccessLogStartTime.getStartTimeAsString(response, request, null), formatSpecifiers);
                    accessLogData.setAccessLogDatetime(AccessLogCurrentTime.getAccessLogCurrentTimeAsString(response, request, null), formatSpecifiers);
                    accessLogData.setRemoteUser(AccessLogRemoteUser.getRemoteUser(response, request, null), formatSpecifiers);
                    accessLogData.setUriPath(request.getRequestURI(), formatSpecifiers);

                    if (kvplCookies.getList().size() > 0)
                        accessLogData.setCookies(kvplCookies);
                    if (kvplRequestHeaders.getList().size() > 0)
                        accessLogData.setRequestHeader(kvplRequestHeaders);
                    if (kvplResponseHeaders.getList().size() > 0)
                        accessLogData.setResponseHeader(kvplResponseHeaders);
                    // LG 265 end
                }
                if (AccessLogData.isCustomAccessLogToJSONEnabled.equals("default") || AccessLogData.isCustomAccessLogToJSONEnabledCollector.equals("default")) {
                    accessLogData.setRequestStartTime(Long.toString(requestStartTimeVal));
                    accessLogData.setUriPath(request.getRequestURI());
                    accessLogData.setRequestMethod(request.getMethod());
                    accessLogData.setQueryString(request.getQueryString());
                    accessLogData.setRequestHost(recordData.getLocalIP());
                    accessLogData.setRequestPort(recordData.getLocalPort());
                    accessLogData.setRemoteHost(recordData.getRemoteAddress());
                    accessLogData.setRequestProtocol(request.getVersion());
                    accessLogData.setBytesReceived(recordData.getBytesWritten());
                    accessLogData.setResponseCode(response.getStatusCodeAsInt());
                    accessLogData.setElapsedTime(recordData.getElapsedTime());
                    accessLogData.setUserAgent(request.getHeader(USER_AGENT_HEADER).asString());
                }

                // These fields aren't in logFormat, so they'll always show up
                accessLogData.setDatetime(recordData.getTimestamp());

                String sequenceVal = requestStartTimeVal + "_" + String.format("%013X", seq.incrementAndGet());
                accessLogData.setSequence(sequenceVal);

                accessLogData.setSourceName(sourceName);

                bufferMgr.add(accessLogData);

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Added a event to buffer " + accessLogData);
                }
            }
        }
    }
}
