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
import com.ibm.ws.logging.data.AccessLogData;
import com.ibm.ws.logging.data.AccessLogDataFormatter;
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

    //private static Map<String, SetterFormatter> setterFormatterMap = new HashMap<String, SetterFormatter>();

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

        void setLogFormat(String logFormat) {
            this.logFormat = logFormat;
        }

        void setLoggingConfig(String loggingConfig) {
            this.loggingConfig = loggingConfig;
        }

        void setLogstashConfig(String logstashConfig) {
            this.logstashConfig = logstashConfig;
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
            if (logFormat.equals(this.logFormat) && loggingConfig.equals(this.loggingConfig) && logstashConfig.equals(this.logstashConfig))
                // Config did NOT change
                return false;
            return true;
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

    // --------------------

    void addDefaultFields(Map<String, ArrayList<Object>> map) {
        String[] defaultFields = { "%h", "%H", "%A", "%B", "%m", "%p", "%q", "%{R}W", "%s", "%U" };
        for (String s : defaultFields) {
            map.put(s, null);
        }
    }

    void initializeFieldMap(Map<String, ArrayList<Object>> map, FormatSegment[] parsedFormat) {
        if (AccessLogData.isCustomAccessLogToJSONEnabled.equals("default") || AccessLogData.isCustomAccessLogToJSONEnabledCollector.equals("default")) {
            addDefaultFields(map);
        }
        if (AccessLogData.isCustomAccessLogToJSONEnabled.equals("logFormat") || AccessLogData.isCustomAccessLogToJSONEnabledCollector.equals("logFormat")) {
            for (FormatSegment s : parsedFormat) {
                if (s.log != null) {
                    // cookies and headers will require data
                    if (s.data != null) {
                        ArrayList<Object> data = new ArrayList<Object>();
                        if (map.containsKey(s.log.getName())) {
                            data = map.get(s.log.getName());
                            data.add(s.data);
                            map.put(s.log.getName(), data);
                        } else {
                            data.add(s.data);
                            map.put(s.log.getName(), data);
                        }
                    } else {
                        map.put(s.log.getName(), null);
                    }
                }
            }
        }
    }

    ArrayList<AccessLogDataFieldSetter> populateSetters(Map<String, ArrayList<Object>> fields) {

        ArrayList<AccessLogDataFieldSetter> fieldSetters = new ArrayList<AccessLogDataFieldSetter>();
        for (String f : fields.keySet()) {
            switch (f) {
                case "%h":
                    fieldSetters.add((ald, alrd) -> ald.setRemoteHost(alrd.getRemoteAddress()));
                    break;
                case "%H":
                    fieldSetters.add((ald, alrd) -> ald.setRequestProtocol(alrd.getVersion()));
                    break;
                case "%A":
                    fieldSetters.add((ald, alrd) -> ald.setRequestHost(alrd.getLocalIP()));
                    break;
                case "%B":
                    fieldSetters.add((ald, alrd) -> ald.setBytesReceived(alrd.getBytesWritten()));
                    break;
                case "%m":
                    fieldSetters.add((ald, alrd) -> ald.setRequestMethod(alrd.getRequest().getMethod()));
                    break;
                case "%p":
                    fieldSetters.add((ald, alrd) -> ald.setRequestPort(alrd.getLocalPort()));
                    break;
                case "%q":
                    fieldSetters.add((ald, alrd) -> ald.setQueryString(alrd.getRequest().getQueryString()));
                    break;
                case "%{R}W":
                    fieldSetters.add((ald, alrd) -> ald.setElapsedTime(alrd.getElapsedTime()));
                    break;
                case "%s":
                    fieldSetters.add((ald, alrd) -> ald.setResponseCode(alrd.getResponse().getStatusCodeAsInt()));
                    break;
                case "%U":
                    fieldSetters.add((ald, alrd) -> ald.setUriPath(alrd.getRequest().getRequestURI()));
                    break;

                // New - access log only fields
                case "%a":
                    fieldSetters.add((ald, alrd) -> ald.setRemoteIP(AccessLogRemoteIP.getRemoteIP(alrd.getResponse(), alrd.getRequest(), null)));
                    break;
                case "%b":
                    fieldSetters.add((ald, alrd) -> ald.setBytesSent(AccessLogResponseSize.getResponseSizeAsString(alrd.getResponse(), alrd.getRequest(), null)));
                    break;
                case "%C":
                    if (fields.get("%C") == null) {
                        fieldSetters.add((ald, alrd) -> {
                            AccessLogRequestCookie.getAllCookies(alrd.getResponse(), alrd.getRequest(), null).forEach(c -> ald.setCookies(c.getName(), c.getValue()));
                        });
                    } else {
                        for (Object data : fields.get("%C")) {
                            fieldSetters.add((ald, alrd) -> {
                                HttpCookie c = AccessLogRequestCookie.getCookie(alrd.getResponse(), alrd.getRequest(), data);
                                if (c != null)
                                    ald.setCookies(c.getName(), c.getValue());
                            });
                        }
                    }
                    break;
                case "%D":
                    fieldSetters.add((ald, alrd) -> ald.setRequestElapsedTime(AccessLogElapsedTime.getElapsedTime(alrd.getResponse(), alrd.getRequest(), null)));
                    break;
                case "%i":
                    for (Object data : fields.get("%i")) {
                        if (data.equals(USER_AGENT_HEADER)) {
                            fieldSetters.add((ald, alrd) -> ald.setUserAgent(alrd.getRequest().getHeader(USER_AGENT_HEADER).asString()));
                        } else if (data != null) {
                            fieldSetters.add((ald, alrd) -> {
                                ald.setRequestHeader((String) data, AccessLogRequestHeaderValue.getHeaderValue(alrd.getResponse(), alrd.getRequest(), data));
                            });
                        }
                    }
                    break;
                case "%o":
                    for (Object data : fields.get("%o")) {
                        if (data != null) {
                            fieldSetters.add((ald, alrd) -> {
                                ald.setResponseHeader((String) data, AccessLogResponseHeaderValue.getHeaderValue(alrd.getResponse(), alrd.getRequest(), data));
                            });
                        }
                    }
                    break;
                case "%r":
                    fieldSetters.add((ald, alrd) -> ald.setRequestFirstLine(AccessLogFirstLine.getFirstLineAsString(alrd.getResponse(), alrd.getRequest(), null)));
                    break;
                case "%t":
                    fieldSetters.add((ald, alrd) -> ald.setRequestStartTime(AccessLogStartTime.getStartTimeAsString(alrd.getResponse(), alrd.getRequest(), null)));
                    break;
                case "%{t}W":
                    fieldSetters.add((ald, alrd) -> ald.setAccessLogDatetime(AccessLogCurrentTime.getAccessLogCurrentTimeAsString(alrd.getResponse(), alrd.getRequest(), null)));
                    break;
                case "%u":
                    fieldSetters.add((ald, alrd) -> ald.setRemoteUser(AccessLogRemoteUser.getRemoteUser(alrd.getResponse(), alrd.getRequest(), null)));
                    break;
            }
        }
        return fieldSetters;
    }

    AccessLogDataFormatter populateCustomFormatters(FormatSegment[] parsedFormat) {
        AccessLogDataFormatter formatter = new AccessLogDataFormatter();
        boolean isFirstCookie = true;
        boolean isFirstRequestHeader = true;
        boolean isFirstResponseHeader = true;
        for (FormatSegment s : parsedFormat) {
            if (s.log != null) {
                switch (s.log.getName()) {
                    // Original - default fields
                    case "%h":
                        formatter.add((jsonBuilder, ald) -> {
                            return jsonBuilder.addField(AccessLogData.getRemoteHostKeyJSON(), ald.getRemoteHost(), false, true);
                        });
                        break;
                    case "%H":

                        formatter.add((jsonBuilder, ald) -> {
                            return jsonBuilder.addField(AccessLogData.getRequestProtocolKeyJSON(), ald.getRequestProtocol(), false, true);
                        });
                        break;
                    case "%A":

                        formatter.add((jsonBuilder, ald) -> {
                            return jsonBuilder.addField(AccessLogData.getRequestHostKeyJSON(), ald.getRequestHost(), false, true);
                        });
                        break;
                    case "%B":

                        formatter.add((jsonBuilder, ald) -> {
                            return jsonBuilder.addField(AccessLogData.getBytesReceivedKeyJSON(), ald.getBytesReceived(), false);
                        });
                        break;
                    case "%m":

                        formatter.add((jsonBuilder, ald) -> {
                            return jsonBuilder.addField(AccessLogData.getRequestMethodKeyJSON(), ald.getRequestMethod(), false, true);
                        });
                        break;
                    case "%p":

                        formatter.add((jsonBuilder, ald) -> {
                            return jsonBuilder.addField(AccessLogData.getRequestPortKeyJSON(), ald.getRequestPort(), false, true);
                        });
                        break;
                    case "%q":

                        formatter.add((jsonBuilder, ald) -> {
                            String jsonQueryString = ald.getQueryString();
                            if (jsonQueryString != null) {
                                try {
                                    jsonQueryString = URLDecoder.decode(jsonQueryString, LogFieldConstants.UTF_8);
                                } catch (UnsupportedEncodingException e) {
                                    // ignore, use the original value;
                                }
                            }
                            return jsonBuilder.addField(AccessLogData.getQueryStringKeyJSON(), jsonQueryString, false, true);
                        });
                        break;
                    case "%{R}W":

                        formatter.add((jsonBuilder, ald) -> {
                            return jsonBuilder.addField(AccessLogData.getElapsedTimeKeyJSON(), ald.getElapsedTime(), false);
                        });
                        break;
                    case "%s":

                        formatter.add((jsonBuilder, ald) -> {
                            return jsonBuilder.addField(AccessLogData.getResponseCodeKeyJSON(), ald.getResponseCode(), false);
                        });
                        break;
                    case "%U":

                        formatter.add((jsonBuilder, ald) -> {
                            return jsonBuilder.addField(AccessLogData.getUriPathKeyJSON(), ald.getUriPath(), false, true);
                        });
                        break;

                    // New - access log only fields
                    case "%a":

                        formatter.add((jsonBuilder, ald) -> {
                            return jsonBuilder.addField(AccessLogData.getRemoteIPKeyJSON(), ald.getRemoteIP(), false, true);
                        });
                        break;
                    case "%b":
                        formatter.add((jsonBuilder, ald) -> {
                            return jsonBuilder.addField(AccessLogData.getBytesSentKeyJSON(), ald.getBytesSent(), false, true);
                        });
                        break;

                    case "%C":

                        if (isFirstCookie) {
                            formatter.add((jsonBuilder, ald) -> {
                                if (ald.getCookies() != null)
                                    ald.getCookies().getList().forEach(c -> jsonBuilder.addField(AccessLogData.getCookieKeyJSON(c), c.getStringValue(), true,
                                                                                                 true));
                                return jsonBuilder;
                            });
                            isFirstCookie = false;
                        }
                        break;
                    case "%D":

                        formatter.add((jsonBuilder, ald) -> {
                            return jsonBuilder.addField(AccessLogData.getRequestElapsedTimeKeyJSON(), ald.getRequestElapsedTime(), false);
                        });
                        break;
                    case "%i":
                        if (s.data.equals(USER_AGENT_HEADER)) {
                            formatter.add((jsonBuilder, ald) -> {
                                String userAgent = ald.getUserAgent();
                                if (userAgent != null && userAgent.length() > MAX_USER_AGENT_LENGTH)
                                    userAgent = userAgent.substring(0, MAX_USER_AGENT_LENGTH);
                                return jsonBuilder.addField(AccessLogData.getUserAgentKeyJSON(), userAgent, true, true);
                            });
                        } else if (isFirstRequestHeader) {
                            formatter.add((jsonBuilder, ald) -> {
                                if (ald.getRequestHeaders() != null)
                                    ald.getRequestHeaders().getList().forEach(h -> jsonBuilder.addField(AccessLogData.getRequestHeaderKeyJSON(h),
                                                                                                        h.getStringValue(),
                                                                                                        false,
                                                                                                        true));
                                return jsonBuilder;
                            });
                            isFirstRequestHeader = false;
                        }

                        break;
                    case "%o":
                        if (isFirstResponseHeader) {
                            formatter.add((jsonBuilder, ald) -> {
                                if (ald.getResponseHeaders() != null)
                                    ald.getResponseHeaders().getList().forEach(h -> jsonBuilder.addField(AccessLogData.getResponseHeaderKeyJSON(h),
                                                                                                         h.getStringValue(),
                                                                                                         true,
                                                                                                         true));
                                return jsonBuilder;
                            });
                            isFirstResponseHeader = false;
                        }
                        break;
                    case "%r":
                        formatter.add((jsonBuilder, ald) -> {
                            return jsonBuilder.addField(AccessLogData.getRequestFirstLineKeyJSON(), ald.getRequestFirstLine(), false, true);
                        });
                        break;
                    case "%t":
                        formatter.add((jsonBuilder, ald) -> {
                            return jsonBuilder.addField(AccessLogData.getRequestStartTimeKeyJSON(), ald.getRequestStartTime(), false, true);
                        });
                        break;
                    case "%{t}W":
                        formatter.add((jsonBuilder, ald) -> {
                            return jsonBuilder.addField(AccessLogData.getAccessLogDatetimeKeyJSON(), ald.getAccessLogDatetime(), false, true);
                        });
                        break;
                    case "%u":
                        formatter.add((jsonBuilder, ald) -> {
                            if (ald.getRemoteUser() != null && !ald.getRemoteUser().isEmpty())
                                return jsonBuilder.addField(AccessLogData.getRemoteUserKeyJSON(), ald.getRemoteUser(), false, true);
                            return jsonBuilder;
                        });
                        break;

                }
            }
        }
        return formatter;

    }

    private AccessLogDataFormatter populateDefaultFormatters() {

        AccessLogDataFormatter formatter = new AccessLogDataFormatter();

        formatter = new AccessLogDataFormatter();
        // %h
        formatter.add((jsonBuilder, ald) -> {
            return jsonBuilder.addField(ald.getRemoteHostKey(), ald.getRemoteHost(), false, true);
        });
        // %H
        formatter.add((jsonBuilder, ald) -> {
            return jsonBuilder.addField(ald.getRequestProtocolKey(), ald.getRequestProtocol(), false, true);
        });
        // %A
        formatter.add((jsonBuilder, ald) -> {
            return jsonBuilder.addField(ald.getRequestHostKey(), ald.getRequestHost(), false, true);
        });
        // %B
        formatter.add((jsonBuilder, ald) -> {
            return jsonBuilder.addField(ald.getBytesReceivedKey(), ald.getBytesReceived(), false);
        });
        // %m
        formatter.add((jsonBuilder, ald) -> {
            return jsonBuilder.addField(ald.getRequestMethodKey(), ald.getRequestMethod(), false, true);
        });
        // %p
        formatter.add((jsonBuilder, ald) -> {
            return jsonBuilder.addField(ald.getRequestPortKey(), ald.getRequestPort(), false, true);
        });
        // %q
        formatter.add((jsonBuilder, ald) -> {
            String jsonQueryString = ald.getQueryString();
            if (jsonQueryString != null) {
                try {
                    jsonQueryString = URLDecoder.decode(jsonQueryString, LogFieldConstants.UTF_8);
                } catch (UnsupportedEncodingException e) {
                    // ignore, use the original value;
                }
            }
            return jsonBuilder.addField(ald.getQueryStringKey(), jsonQueryString, false, true);
        });
        // %{R}W
        formatter.add((jsonBuilder, ald) -> {
            return jsonBuilder.addField(ald.getElapsedTimeKey(), ald.getElapsedTime(), false);
        });
        // %s
        formatter.add((jsonBuilder, ald) -> {
            return jsonBuilder.addField(ald.getResponseCodeKey(), ald.getResponseCode(), false);
        });
        // %U
        formatter.add((jsonBuilder, ald) -> {
            return jsonBuilder.addField(ald.getUriPathKey(), ald.getUriPath(), false, true);
        });
        // User agent
        formatter.add((jsonBuilder, ald) -> {
            String userAgent = ald.getUserAgent();
            if (userAgent != null && userAgent.length() > MAX_USER_AGENT_LENGTH)
                userAgent = userAgent.substring(0, MAX_USER_AGENT_LENGTH);
            return jsonBuilder.addField(ald.getUserAgentKey(), userAgent, true, true);
        });
        return formatter;
    }

    private class AccessLogHandler implements AccessLogForwarder {

        private final AtomicLong seq = new AtomicLong();

        /** {@inheritDoc} */
        @Override
        public void process(AccessLogRecordData recordData, FormatSegment[] parsedFormat, String formatString) {
            if (currentSF.checkConfigChange(formatString, AccessLogData.isCustomAccessLogToJSONEnabled, AccessLogData.isCustomAccessLogToJSONEnabledCollector)) {
                System.out.println("config change");
                SetterFormatter newSF = new SetterFormatter();
                List<AccessLogDataFieldSetter> fieldSetters = new ArrayList<AccessLogDataFieldSetter>();
                AccessLogDataFormatter[] formatters = { null, null, null, null };
                Map<String, ArrayList<Object>> fieldsToAdd = new HashMap<String, ArrayList<Object>>();

                // Create the mapping of fields to add in format {<format key> : <data value/null>}
                // Prevents duplicates
                initializeFieldMap(fieldsToAdd, parsedFormat);

                newSF.setFullConfig(formatString, AccessLogData.isCustomAccessLogToJSONEnabled, AccessLogData.isCustomAccessLogToJSONEnabledCollector);

                // Create setter list
                fieldSetters = populateSetters(fieldsToAdd);
                // These fields are always added
                fieldSetters.add((ald,
                                  alrd) -> ald.setSequence(alrd.getStartTime() + "_" + String.format("%013X", seq.incrementAndGet())));
                fieldSetters.add((ald,
                                  alrd) -> ald.setDatetime(recordData.getTimestamp()));

                if (AccessLogData.isCustomAccessLogToJSONEnabled.equals("default")) {
                    formatters[0] = populateDefaultFormatters();
                }
                if (AccessLogData.isCustomAccessLogToJSONEnabled.equals("logFormat")) {
                    formatters[1] = populateCustomFormatters(parsedFormat);
                }
                if (AccessLogData.isCustomAccessLogToJSONEnabledCollector.equals("default")) {
                    //formatters[2] = populateDefaultSetterFormattersLogstash(fieldSetters, formatters);
                }
                if (AccessLogData.isCustomAccessLogToJSONEnabledCollector.equals("logFormat")) {
                    //formatters[3] = populateCustomSetterFormattersLogstash(fieldSetters, formatters, parsedFormat);
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
}
