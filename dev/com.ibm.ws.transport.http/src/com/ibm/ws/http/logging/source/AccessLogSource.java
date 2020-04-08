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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.http.channel.internal.values.AccessLogCurrentTime;
import com.ibm.ws.http.channel.internal.values.AccessLogElapsedTime;
import com.ibm.ws.http.channel.internal.values.AccessLogFirstLine;
import com.ibm.ws.http.channel.internal.values.AccessLogRemoteIP;
import com.ibm.ws.http.channel.internal.values.AccessLogRemoteUser;
import com.ibm.ws.http.channel.internal.values.AccessLogRequestCookie;
import com.ibm.ws.http.channel.internal.values.AccessLogRequestHeaderValue;
import com.ibm.ws.http.channel.internal.values.AccessLogResponseHeaderValue;
import com.ibm.ws.http.channel.internal.values.AccessLogResponseSize;
import com.ibm.ws.http.channel.internal.values.AccessLogStartTime;
import com.ibm.ws.http.logging.internal.AccessLogger.FormatSegment;
import com.ibm.ws.logging.collector.LogFieldConstants;
import com.ibm.ws.logging.data.AccessLogConfig;
import com.ibm.ws.logging.data.AccessLogData;
import com.ibm.ws.logging.data.AccessLogDataFormatter;
import com.ibm.ws.logging.data.FormatSpecifier;
import com.ibm.ws.logging.data.JsonFieldAdder;
import com.ibm.wsspi.collector.manager.BufferManager;
import com.ibm.wsspi.collector.manager.Source;
import com.ibm.wsspi.http.HttpCookie;
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
    public static final int MAX_USER_AGENT_LENGTH = 2048;
    private SetterFormatter currentSF = new SetterFormatter();
    public String jsonAccessLogFields = "";
    public static String jsonAccessLogFieldsLogstash = "";
    public Map<String, Object> configuration;

    private static class SetterFormatter {
        String logFormat;
        String loggingConfig;
        String logstashConfig;
        List<AccessLogDataFieldSetter> setters;
        AccessLogDataFormatter[] formatters = { null, null, null, null };

        private SetterFormatter() {
            this.logFormat = "";
            this.loggingConfig = "";
            this.logstashConfig = "";
        }

        void setSettersAndFormatters(List<AccessLogDataFieldSetter> setters, AccessLogDataFormatter[] formatters) {
            this.setters = setters;
            this.formatters = formatters;
        }

        List<AccessLogDataFieldSetter> getSetters() {
            return this.setters;
        }

        AccessLogDataFormatter[] getFormatters() {
            return this.formatters;
        }

        boolean checkConfigChange(String logFormat, String loggingConfig, String logstashConfig) {
            if (!logFormat.equals(this.logFormat) || !loggingConfig.equals(this.loggingConfig) || !logstashConfig.equals(this.logstashConfig))
                return true;
            return false;
        }

        void setFullConfig(String logFormat, String loggingConfig, String logstashConfig) {
            this.logFormat = logFormat;
            this.loggingConfig = loggingConfig;
            this.logstashConfig = logstashConfig;
        }
    }

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

    void addDefaultFields(Map<String, HashSet<Object>> map) {
        String[] defaultFields = { "%h", "%H", "%A", "%B", "%m", "%p", "%q", "%{R}W", "%s", "%U" };
        for (String s : defaultFields) {
            map.put(s, null);
        }
    }

    void initializeFieldMap(Map<String, HashSet<Object>> map, FormatSegment[] parsedFormat) {
        if (jsonAccessLogFields.equals("default") || jsonAccessLogFieldsLogstash.equals("default")) {
            addDefaultFields(map);
        }
        if (jsonAccessLogFields.equals("logFormat") || jsonAccessLogFieldsLogstash.equals("logFormat")) {
            for (FormatSegment s : parsedFormat) {
                if (s.log != null) {
                    // cookies and headers will require data
                    if (s.data != null) {
                        HashSet<Object> data = new HashSet<Object>();
                        if (map.containsKey(s.log.getName())) {
                            data = map.get(s.log.getName());
                        }
                        data.add(s.data);
                        map.put(s.log.getName(), data);
                    } else {
                        map.put(s.log.getName(), null);
                    }
                }
            }
        }
    }

    ArrayList<AccessLogDataFieldSetter> populateSetters(Map<String, HashSet<Object>> fields) {

        ArrayList<AccessLogDataFieldSetter> fieldSetters = new ArrayList<AccessLogDataFieldSetter>();
        for (String f : fields.keySet()) {
            switch (f) {
                //@formatter:off
                case "%h": fieldSetters.add((ald, alrd) -> ald.setRemoteHost(alrd.getRemoteAddress())); break;
                case "%H": fieldSetters.add((ald, alrd) -> ald.setRequestProtocol(alrd.getVersion())); break;
                case "%A": fieldSetters.add((ald, alrd) -> ald.setRequestHost(alrd.getLocalIP())); break;
                case "%B": fieldSetters.add((ald, alrd) -> ald.setBytesReceived(alrd.getBytesWritten())); break;
                case "%m": fieldSetters.add((ald, alrd) -> ald.setRequestMethod(alrd.getRequest().getMethod())); break;
                case "%p": fieldSetters.add((ald, alrd) -> ald.setRequestPort(alrd.getLocalPort())); break;
                case "%q": fieldSetters.add((ald, alrd) -> ald.setQueryString(alrd.getRequest().getQueryString())); break;
                case "%{R}W": fieldSetters.add((ald, alrd) -> ald.setElapsedTime(alrd.getElapsedTime())); break;
                case "%s": fieldSetters.add((ald, alrd) -> ald.setResponseCode(alrd.getResponse().getStatusCodeAsInt())); break;
                case "%U": fieldSetters.add((ald, alrd) -> ald.setUriPath(alrd.getRequest().getRequestURI())); break;
                // New - access log only fields
                case "%a": fieldSetters.add((ald, alrd) -> ald.setRemoteIP(AccessLogRemoteIP.getRemoteIP(alrd.getResponse(), alrd.getRequest(), null))); break;
                case "%b": fieldSetters.add((ald, alrd) -> ald.setBytesSent(AccessLogResponseSize.getResponseSizeAsString(alrd.getResponse(), alrd.getRequest(), null))); break;
                case "%C":
                    if (fields.get("%C") == null) {
                        fieldSetters.add((ald, alrd) -> AccessLogRequestCookie.getAllCookies(alrd.getResponse(), alrd.getRequest(), null)
                                                        .forEach(c -> ald.setCookies(c.getName(), c.getValue())));
                    } else {
                        for (Object data : fields.get("%C")) {
                            fieldSetters.add((ald, alrd) -> {
                                HttpCookie c = AccessLogRequestCookie.getCookie(alrd.getResponse(), alrd.getRequest(), data);
                                if (c != null)
                                    ald.setCookies(c.getName(), c.getValue());
                            });
                        }
                    } break;
                case "%D": fieldSetters.add((ald, alrd) -> ald.setRequestElapsedTime(AccessLogElapsedTime.getElapsedTime(alrd.getResponse(), alrd.getRequest(), null))); break;
                case "%i":
                    for (Object data : fields.get("%i")) {
                        if (data.equals(USER_AGENT_HEADER))
                            fieldSetters.add((ald, alrd) -> ald.setUserAgent(alrd.getRequest().getHeader(USER_AGENT_HEADER).asString()));
                        else if (data != null)
                            fieldSetters.add((ald, alrd) -> ald.setRequestHeader((String) data, AccessLogRequestHeaderValue.getHeaderValue(alrd.getResponse(), alrd.getRequest(), data)));
                    } break;
                case "%o":
                    for (Object data : fields.get("%o")) {
                        if (data != null)
                            fieldSetters.add((ald, alrd) -> ald.setResponseHeader((String) data, AccessLogResponseHeaderValue.getHeaderValue(alrd.getResponse(), alrd.getRequest(), data)));
                    } break;
                case "%r": fieldSetters.add((ald, alrd) -> ald.setRequestFirstLine(AccessLogFirstLine.getFirstLineAsString(alrd.getResponse(), alrd.getRequest(), null))); break;
                case "%t": fieldSetters.add((ald, alrd) -> ald.setRequestStartTime(AccessLogStartTime.getStartTimeAsString(alrd.getResponse(), alrd.getRequest(), null))); break;
                case "%{t}W": fieldSetters.add((ald, alrd) -> ald.setAccessLogDatetime(AccessLogCurrentTime.getAccessLogCurrentTimeAsString(alrd.getResponse(), alrd.getRequest(), null))); break;
                case "%u": fieldSetters.add((ald, alrd) -> ald.setRemoteUser(AccessLogRemoteUser.getRemoteUser(alrd.getResponse(), alrd.getRequest(), null))); break;
                //@formatter:on
            }
        }
        return fieldSetters;
    }

    AccessLogDataFormatter populateCustomFormatters(FormatSegment[] parsedFormat, FormatSpecifier format) {
        AccessLogDataFormatter formatter = new AccessLogDataFormatter();
        boolean isFirstCookie = true;
        boolean isFirstRequestHeader = true;
        boolean isFirstResponseHeader = true;
        for (FormatSegment s : parsedFormat) {
            if (s.log != null) {
                switch (s.log.getName()) {
                    // Original - default fields
                    //@formatter:off
                    case "%h": formatter.add(addRemoteHostField     (format)); break;
                    case "%H": formatter.add(addRequestProtocolField(format)); break;
                    case "%A": formatter.add(addRequestHostField    (format)); break;
                    case "%B": formatter.add(addBytesReceivedField  (format)); break;
                    case "%m": formatter.add(addRequestMethodField  (format)); break;
                    case "%p": formatter.add(addRequestPortField    (format)); break;
                    case "%q": formatter.add(addQueryStringField    (format)); break;
                    case "%{R}W": formatter.add(addElapsedTimeField (format)); break;
                    case "%s": formatter.add(addResponseCodeField   (format)); break;
                    case "%U": formatter.add(addUriPathField        (format)); break;
                    // New - access log only fields
                    case "%a": formatter.add(addRemoteIPField       (format)); break;
                    case "%b": formatter.add(addBytesSentField      (format)); break;
                    case "%C":
                        if (isFirstCookie) {
                            formatter.add(addCookiesField(format));
                            isFirstCookie = false;
                        } break;
                    case "%D": formatter.add(addRequestElapsedTimeField(format)); break;
                    case "%i":
                        if (s.data.equals(USER_AGENT_HEADER)) {
                            formatter.add(addUserAgentField(format));
                        } else if (isFirstRequestHeader) {
                            formatter.add(addRequestHeaderField(format));
                            isFirstRequestHeader = false;
                        } break;
                    case "%o":
                        if (isFirstResponseHeader) {
                            formatter.add(addResponseHeaderField(format));
                            isFirstResponseHeader = false;
                        } break;
                    case "%r": formatter.add(addRequestFirstLineField    (format)); break;
                    case "%t": formatter.add(addRequestStartTimeField    (format)); break;
                    case "%{t}W": formatter.add(addAccessLogDatetimeField(format)); break;
                    case "%u": formatter.add(addRemoteUserField          (format)); break;
                    //@formatter:on
                }
            }
        }
        return formatter;

    }

    private AccessLogDataFormatter populateDefaultFormatters(FormatSpecifier format) {

        AccessLogDataFormatter formatter = new AccessLogDataFormatter();

        formatter = new AccessLogDataFormatter();
        // %h
        formatter.add(addRemoteHostField(format));
        // %H
        formatter.add(addRequestProtocolField(format));
        // %A
        formatter.add(addRequestHostField(format));
        // %B
        formatter.add(addBytesReceivedField(format));
        // %m
        formatter.add(addRequestMethodField(format));
        // %p
        formatter.add(addRequestPortField(format));
        // %q
        formatter.add(addQueryStringField(format));
        // %{R}W
        formatter.add(addElapsedTimeField(format));
        // %s
        formatter.add(addResponseCodeField(format));
        // %U
        formatter.add(addUriPathField(format));
        // User agent
        formatter.add(addUserAgentField(format));
        return formatter;
    }

    private class AccessLogHandler implements AccessLogForwarder {

        private final AtomicLong seq = new AtomicLong();

        /** {@inheritDoc} */
        @Override
        public void process(AccessLogRecordData recordData, FormatSegment[] parsedFormat, String formatString) {
            jsonAccessLogFields = AccessLogConfig.jsonAccessLogFields;
            if (currentSF.checkConfigChange(formatString, jsonAccessLogFields, jsonAccessLogFieldsLogstash)) {
                System.out.println("config change");
                SetterFormatter newSF = new SetterFormatter();
                List<AccessLogDataFieldSetter> fieldSetters = new ArrayList<AccessLogDataFieldSetter>();
                AccessLogDataFormatter[] formatters = { null, null, null, null };
                Map<String, HashSet<Object>> fieldsToAdd = new HashMap<String, HashSet<Object>>();

                System.out.println(jsonAccessLogFieldsLogstash);
                System.out.println(jsonAccessLogFields);

                // Create the mapping of fields to add:{<format key> : <data value/null>}
                // Prevents duplicates
                initializeFieldMap(fieldsToAdd, parsedFormat);

                newSF.setFullConfig(formatString, jsonAccessLogFields, jsonAccessLogFieldsLogstash);

                // Create setter list
                fieldSetters = populateSetters(fieldsToAdd);
                // These fields are always added
                fieldSetters.add((ald, alrd) -> ald.setSequence(alrd.getStartTime() + "_" + String.format("%013X", seq.incrementAndGet())));
                fieldSetters.add((ald, alrd) -> ald.setDatetime(alrd.getTimestamp()));

                if (jsonAccessLogFields.equals("default")) {
                    formatters[0] = populateDefaultFormatters(FormatSpecifier.JSON);
                }
                if (jsonAccessLogFields.equals("logFormat")) {
                    formatters[1] = populateCustomFormatters(parsedFormat, FormatSpecifier.JSON);
                }
                if (jsonAccessLogFieldsLogstash.equals("default")) {
                    formatters[2] = populateDefaultFormatters(FormatSpecifier.LOGSTASH);
                }
                if (jsonAccessLogFieldsLogstash.equals("logFormat")) {
                    formatters[3] = populateCustomFormatters(parsedFormat, FormatSpecifier.LOGSTASH);
                }

                newSF.setSettersAndFormatters(fieldSetters, formatters);

                currentSF = newSF;

            }
            SetterFormatter temp = currentSF;
            AccessLogData accessLogData = new AccessLogData();
            for (AccessLogDataFieldSetter s : temp.getSetters()) {
                s.add(accessLogData, recordData);
            }

            accessLogData.addFormatters(temp.getFormatters());
            // collectorJSONUtils does the rest of the work from here

            accessLogData.setSourceName(sourceName);

            bufferMgr.add(accessLogData);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Added a event to buffer " + accessLogData);
            }
        }
    }

    private static JsonFieldAdder addRemoteHostField(FormatSpecifier format) {
        return (jsonBuilder, ald) -> {
            return jsonBuilder.addField(AccessLogData.getRemoteHostKey(format), ald.getRemoteHost(), false, true);
        };
    }

    private static JsonFieldAdder addRequestProtocolField(FormatSpecifier format) {
        return (jsonBuilder, ald) -> {
            return jsonBuilder.addField(AccessLogData.getRequestProtocolKey(format), ald.getRequestProtocol(), false, true);
        };
    }

    private static JsonFieldAdder addRequestHostField(FormatSpecifier format) {
        return (jsonBuilder, ald) -> {
            return jsonBuilder.addField(AccessLogData.getRequestHostKey(format), ald.getRequestHost(), false, true);
        };
    }

    private static JsonFieldAdder addBytesReceivedField(FormatSpecifier format) {
        return (jsonBuilder, ald) -> {
            return jsonBuilder.addField(AccessLogData.getBytesReceivedKey(format), ald.getBytesReceived(), false);
        };
    }

    private static JsonFieldAdder addRequestMethodField(FormatSpecifier format) {
        return (jsonBuilder, ald) -> {
            return jsonBuilder.addField(AccessLogData.getRequestMethodKey(format), ald.getRequestMethod(), false, true);
        };
    }

    private static JsonFieldAdder addRequestPortField(FormatSpecifier format) {
        return (jsonBuilder, ald) -> {
            return jsonBuilder.addField(AccessLogData.getRequestPortKey(format), ald.getRequestPort(), false, true);
        };
    }

    private static JsonFieldAdder addQueryStringField(FormatSpecifier format) {
        return ((jsonBuilder, ald) -> {
            String jsonQueryString = ald.getQueryString();
            if (jsonQueryString != null) {
                try {
                    jsonQueryString = URLDecoder.decode(jsonQueryString, LogFieldConstants.UTF_8);
                } catch (UnsupportedEncodingException e) {
                    // ignore, use the original value;
                }
            }
            return jsonBuilder.addField(AccessLogData.getQueryStringKey(format), jsonQueryString, false, true);
        });
    }

    private static JsonFieldAdder addElapsedTimeField(FormatSpecifier format) {
        return (jsonBuilder, ald) -> {
            return jsonBuilder.addField(AccessLogData.getElapsedTimeKey(format), ald.getElapsedTime(), false);
        };
    }

    private static JsonFieldAdder addResponseCodeField(FormatSpecifier format) {
        return (jsonBuilder, ald) -> {
            return jsonBuilder.addField(AccessLogData.getResponseCodeKey(format), ald.getResponseCode(), false);
        };
    }

    private static JsonFieldAdder addUriPathField(FormatSpecifier format) {
        return (jsonBuilder, ald) -> {
            return jsonBuilder.addField(AccessLogData.getUriPathKey(format), ald.getUriPath(), false, true);
        };
    }

    private static JsonFieldAdder addRemoteIPField(FormatSpecifier format) {
        return (jsonBuilder, ald) -> {
            return jsonBuilder.addField(AccessLogData.getRemoteIPKey(format), ald.getRemoteIP(), false, true);
        };
    }

    private static JsonFieldAdder addBytesSentField(FormatSpecifier format) {
        return ((jsonBuilder, ald) -> {
            return jsonBuilder.addField(AccessLogData.getBytesSentKey(format), ald.getBytesSent(), false, true);
        });
    }

    private static JsonFieldAdder addCookiesField(FormatSpecifier format) {
        return (jsonBuilder, ald) -> {
            if (ald.getCookies() != null)
                ald.getCookies().getList().forEach(c -> jsonBuilder.addField(AccessLogData.getCookieKey(format, c), c.getStringValue(), true, true));
            return jsonBuilder;
        };
    }

    private static JsonFieldAdder addRequestElapsedTimeField(FormatSpecifier format) {
        return (jsonBuilder, ald) -> {
            return jsonBuilder.addField(AccessLogData.getRequestElapsedTimeKey(format), ald.getRequestElapsedTime(), false);
        };
    }

    private static JsonFieldAdder addUserAgentField(FormatSpecifier format) {
        return (jsonBuilder, ald) -> {
            String userAgent = ald.getUserAgent();
            if (userAgent != null && userAgent.length() > MAX_USER_AGENT_LENGTH)
                userAgent = userAgent.substring(0, MAX_USER_AGENT_LENGTH);
            return jsonBuilder.addField(AccessLogData.getUserAgentKey(format), userAgent, true, true);
        };
    }

    private static JsonFieldAdder addRequestHeaderField(FormatSpecifier format) {
        return (jsonBuilder, ald) -> {
            if (ald.getRequestHeaders() != null)
                ald.getRequestHeaders().getList().forEach(h -> jsonBuilder.addField(AccessLogData.getRequestHeaderKey(format, h), h.getStringValue(), false, true));
            return jsonBuilder;
        };
    }

    private static JsonFieldAdder addResponseHeaderField(FormatSpecifier format) {
        return (jsonBuilder, ald) -> {
            if (ald.getResponseHeaders() != null)
                ald.getResponseHeaders().getList().forEach(h -> jsonBuilder.addField(AccessLogData.getResponseHeaderKey(format, h), h.getStringValue(), true, true));
            return jsonBuilder;
        };
    }

    private static JsonFieldAdder addRequestFirstLineField(FormatSpecifier format) {
        return (jsonBuilder, ald) -> {
            return jsonBuilder.addField(AccessLogData.getRequestFirstLineKey(format), ald.getRequestFirstLine(), false, true);
        };
    }

    private static JsonFieldAdder addRequestStartTimeField(FormatSpecifier format) {
        return (jsonBuilder, ald) -> {
            return jsonBuilder.addField(AccessLogData.getRequestStartTimeKey(format), ald.getRequestStartTime(), false, true);
        };
    }

    private static JsonFieldAdder addAccessLogDatetimeField(FormatSpecifier format) {
        return (jsonBuilder, ald) -> {
            return jsonBuilder.addField(AccessLogData.getAccessLogDatetimeKey(format), ald.getAccessLogDatetime(), false, true);
        };
    }

    private static JsonFieldAdder addRemoteUserField(FormatSpecifier format) {
        return (jsonBuilder, ald) -> {
            if (ald.getRemoteUser() != null && !ald.getRemoteUser().isEmpty())
                return jsonBuilder.addField(AccessLogData.getRemoteUserKey(format), ald.getRemoteUser(), false, true);
            return jsonBuilder;
        };
    }
}
