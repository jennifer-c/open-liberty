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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.http.channel.internal.values.AccessLogRemoteIP;
import com.ibm.ws.http.channel.internal.values.AccessLogRequestCookie;
import com.ibm.ws.http.channel.internal.values.AccessLogResponseSize;
import com.ibm.ws.http.logging.internal.AccessLogger.FormatSegment;
import com.ibm.ws.logging.data.AccessLogData;
import com.ibm.ws.logging.data.AccessLogDataFormatter;
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
    ///// NEW

    private static Map<FormatSegment[], SetterFormatter> setterFormatterMap = new HashMap<FormatSegment[], SetterFormatter>();

    private static class SetterFormatter {
        List<AccessLogDataFieldSetter> setters;
        AccessLogDataFormatter[] formatters = { null, null, null };

        void addSetters(List<AccessLogDataFieldSetter> setters) {
            this.setters = setters;
        }

        void addFormatters(AccessLogDataFormatter[] formatters) {
            this.formatters = formatters;
        }

        List<AccessLogDataFieldSetter> getSetters() {
            return this.setters;
        }

        AccessLogDataFormatter[] getFormatters() {
            return this.formatters;
        }
    }
    //// END

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

            // Should we make the Map as <FormatSegment[], SetterFormatter>? Is it even possible to check if a key exists when comparing arrays?
            // TODO implement the caching
            SetterFormatter currentSF;
            if (setterFormatterMap.containsKey("TODO GOES STRAIGHT TO ELSE CURRENTLY")) {
                currentSF = setterFormatterMap.get(parsedFormat); // get the formatter and setter
            } else {
                SetterFormatter newSF = new SetterFormatter();
                List<AccessLogDataFieldSetter> fieldSetters = new ArrayList<AccessLogDataFieldSetter>();
                AccessLogDataFormatter[] formatters = { new AccessLogDataFormatter(), new AccessLogDataFormatter(), new AccessLogDataFormatter() };
                for (FormatSegment s : parsedFormat) {
                    if (s.log != null) {
                        switch (s.log.getName()) {
                            case "%a":
                                fieldSetters.add((a, d) -> a.setRemoteIP(AccessLogRemoteIP.getRemoteIP(d.getResponse(), d.getRequest(), s.data)));
                                formatters[1].add((j, a) -> {
                                    j.addField(AccessLogData.getRemoteIPKeyJSON(), a.getRemoteIP(), false, false);
                                    return j;
                                });
                                break;
                            case "%b":
                                fieldSetters.add((a, d) -> a.setBytesSent(AccessLogResponseSize.getResponseSizeAsString(d.getResponse(), d.getRequest(), s.data)));
                                formatters[1].add((j, a) -> {
                                    j.addField(AccessLogData.getBytesSentKeyJSON(), a.getBytesSent(), false, false);
                                    return j;
                                });
                                break;

                            // For this case, should we check that getCookie is not null *before* putting it into the setter/formatter list?
                            case "%C":
                                if (s.data != null) {
                                    fieldSetters.add((a, d) -> {
                                        HttpCookie c = AccessLogRequestCookie.getCookie(d.getResponse(), d.getRequest(), s.data);
                                        if (c != null)
                                            a.setCookies(c.getName(), c.getValue());
                                    });
                                } else {
                                    fieldSetters.add((a, d) -> {
                                        AccessLogRequestCookie.getAllCookies(d.getResponse(), d.getRequest(), null).forEach(c -> a.setCookies(c.getName(), c.getValue()));
                                    });
                                }
                                formatters[1].add((j, a) -> {
                                    if (a.getCookies() != null)
                                        a.getCookies().getList().forEach(c -> j.addField(AccessLogData.getCookieKeyJSON(c), c.getStringValue(), false, false));
                                    return j;
                                });
                        }
                    }
                }
                // TODO default formatters
                newSF.addSetters(fieldSetters);
                newSF.addFormatters(formatters);
                // make new formatter and setter, then
                // add key into map
                currentSF = newSF;
            }

            AccessLogData accessLogData = new AccessLogData();
            for (AccessLogDataFieldSetter s : currentSF.setters) {
                s.add(accessLogData, recordData);
            }

            accessLogData.addFormatters(currentSF.formatters);
            // collectorJSONUtils does the rest of the work from here

            // * --------------------------------original-------------------------------- * //

            HttpRequestMessage request = recordData.getRequest();
            HttpResponseMessage response = recordData.getResponse();

            if (request != null) {

                long requestStartTimeVal = recordData.getStartTime(); // needed for sequence

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
