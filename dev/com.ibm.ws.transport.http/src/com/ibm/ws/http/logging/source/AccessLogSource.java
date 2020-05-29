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
import java.util.concurrent.ConcurrentHashMap;
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
import com.ibm.ws.http.logging.internal.AccessLogRecordDataExt;
import com.ibm.ws.http.logging.internal.AccessLogger.FormatSegment;
import com.ibm.ws.logging.collector.CollectorJsonHelpers;
import com.ibm.ws.logging.collector.LogFieldConstants;
import com.ibm.ws.logging.data.AccessLogConfig;
import com.ibm.ws.logging.data.AccessLogData;
import com.ibm.ws.logging.data.AccessLogDataFormatter;
import com.ibm.ws.logging.data.AccessLogDataFormatter.AccessLogDataFormatterBuilder;
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
    Map<Configuration, SetterFormatter> setterFormatterMap = new ConcurrentHashMap<Configuration, SetterFormatter>();
    private SetterFormatter currentSF = new SetterFormatter("", "", "");
    public String jsonAccessLogFieldsConfig = "";
    public static String jsonAccessLogFieldsLogstashConfig = "";
    public Map<String, Object> configuration;

    // A representation of the current configuration; to be used in the setterFormatterMap
    private class Configuration {
        String logFormat;
        String loggingConfig;
        String logstashConfig;

        private Configuration(String logFormat, String loggingConfig, String logstashConfig) {
            this.logFormat = logFormat;
            this.loggingConfig = loggingConfig;
            this.logstashConfig = logstashConfig;
        }

        //@formatter:off
        String getLogFormat()      { return this.logFormat; }
        String getLoggingConfig()  { return this.loggingConfig; }
        String getLogstashConfig() { return this.logstashConfig; }
        //@formatter:on

        // We need to put this object into a HashMap, so we're overriding hashCode
        @Override
        public int hashCode() {
            int hash;
            hash = logFormat.hashCode() * loggingConfig.hashCode() * logstashConfig.hashCode();
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Configuration config = (Configuration) obj;
            if (!config.getLogFormat().equals(this.logFormat) || !config.getLoggingConfig().equals(this.loggingConfig) || !config.getLogstashConfig().equals(this.logstashConfig))
                return false;
            return true;
        }
    }

    private static class SetterFormatter {
        // The HTTP access logging logFormat value, e.g. "%a %b"
        String logFormat;
        // The jsonAccessLogFields configuration value for JSON logging, default or logFormat
        String loggingConfig;
        // The jsonAccessLogFields configuration value for Logstash Collector, default or logFormat
        String logstashConfig;
        // List of formatters for each type of logging format; null if not applicable to current configuration
        // { <default JSON logging>, <logFormat JSON logging>, <default logstashCollector>, <logFormat logstashCollector> }
        AccessLogDataFormatter[] formatters = { null, null, null, null };
        List<AccessLogDataFieldSetter> setters;

        private SetterFormatter(String logFormat, String loggingConfig, String logstashConfig) {
            this.logFormat = logFormat;
            this.loggingConfig = loggingConfig;
            this.logstashConfig = logstashConfig;
        }

        void setSettersAndFormatters(List<AccessLogDataFieldSetter> setters, AccessLogDataFormatter[] formatters) {
            this.setters = setters;
            this.formatters = formatters;
        }

        // Setters should not be modified to avoid concurrency issues with other threads using the same setter list
        List<AccessLogDataFieldSetter> getSetters() {
            return this.setters;
        }

        // Formatters should not be modified to avoid concurrency issues with other threads using the same formatter list
        AccessLogDataFormatter[] getFormatters() {
            return this.formatters;
        }

        boolean checkConfigChange(String logFormat, String loggingConfig, String logstashConfig) {
            if (!logFormat.equals(this.logFormat) || !loggingConfig.equals(this.loggingConfig) || !logstashConfig.equals(this.logstashConfig))
                return true;
            return false;
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

    private void addDefaultFields(Map<String, HashSet<Object>> map) {
        String[] defaultFields = { "%h", "%H", "%A", "%B", "%m", "%p", "%q", "%{R}W", "%s", "%U" };
        for (String s : defaultFields) {
            map.put(s, null);
        }
        // User-Agent is a default field that is a specific request header
        HashSet<Object> data = new HashSet<Object>();
        data.add("User-Agent");
        map.put("%i", data);
    }

    private void initializeFieldMap(Map<String, HashSet<Object>> map, FormatSegment[] parsedFormat) {
        if (jsonAccessLogFieldsConfig.equals("default") || jsonAccessLogFieldsLogstashConfig.equals("default")) {
            addDefaultFields(map);
        }
        if (jsonAccessLogFieldsConfig.equals("logFormat") || jsonAccessLogFieldsLogstashConfig.equals("logFormat")) {
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

    private ArrayList<AccessLogDataFieldSetter> populateSetters(Map<String, HashSet<Object>> fields) {

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
                case "%D": fieldSetters.add((ald, alrd) -> ald.setRequestElapsedTime(AccessLogElapsedTime.getElapsedTimeForJSON(alrd.getResponse(), alrd.getRequest(), null))); break;
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
                case "%t": fieldSetters.add((ald, alrd) -> ald.setRequestStartTime(AccessLogStartTime.getStartTimeAsStringForJSON(alrd.getResponse(), alrd.getRequest(), null))); break;
                case "%{t}W": fieldSetters.add((ald, alrd) -> ald.setAccessLogDatetime(AccessLogCurrentTime.getAccessLogCurrentTimeAsString(alrd.getResponse(), alrd.getRequest(), null))); break;
                case "%u": fieldSetters.add((ald, alrd) -> ald.setRemoteUser(AccessLogRemoteUser.getRemoteUser(alrd.getResponse(), alrd.getRequest(), null))); break;
                //@formatter:on
            }
        }
        return fieldSetters;
    }

    AccessLogDataFormatter populateCustomFormatters(FormatSegment[] parsedFormat, int format) {
        AccessLogDataFormatterBuilder builder = new AccessLogDataFormatterBuilder();
        boolean isFirstCookie = true;
        boolean isFirstRequestHeader = true;
        boolean isFirstResponseHeader = true;
        for (FormatSegment s : parsedFormat) {
            if (s.log != null) {
                switch (s.log.getName()) {
                    // Original - default fields
                    //@formatter:off
                    case "%h": builder.add(addRemoteHostField     (format)); break;
                    case "%H": builder.add(addRequestProtocolField(format)); break;
                    case "%A": builder.add(addRequestHostField    (format)); break;
                    case "%B": builder.add(addBytesReceivedField  (format)); break;
                    case "%m": builder.add(addRequestMethodField  (format)); break;
                    case "%p": builder.add(addRequestPortField    (format)); break;
                    case "%q": builder.add(addQueryStringField    (format)); break;
                    case "%{R}W": builder.add(addElapsedTimeField (format)); break;
                    case "%s": builder.add(addResponseCodeField   (format)); break;
                    case "%U": builder.add(addUriPathField        (format)); break;
                    // New - access log only fields
                    case "%a": builder.add(addRemoteIPField       (format)); break;
                    case "%b": builder.add(addBytesSentField      (format)); break;
                    case "%C":
                        if (isFirstCookie) {
                            builder.add(addCookiesField(format));
                            isFirstCookie = false;
                        } break;
                    case "%D": builder.add(addRequestElapsedTimeField(format)); break;
                    case "%i":
                        if (s.data.equals(USER_AGENT_HEADER)) {
                            builder.add(addUserAgentField(format));
                        } else if (isFirstRequestHeader) {
                            builder.add(addRequestHeaderField(format));
                            isFirstRequestHeader = false;
                        } break;
                    case "%o":
                        if (isFirstResponseHeader) {
                            builder.add(addResponseHeaderField(format));
                            isFirstResponseHeader = false;
                        } break;
                    case "%r": builder.add(addRequestFirstLineField    (format)); break;
                    case "%t": builder.add(addRequestStartTimeField    (format)); break;
                    case "%{t}W": builder.add(addAccessLogDatetimeField(format)); break;
                    case "%u": builder.add(addRemoteUserField          (format)); break;
                    //@formatter:on
                }
            }
        }
        //@formatter:off
        builder.add(addDatetimeField(format))  // Sequence, present in all access logs
               .add(addSequenceField(format)); // Datetime, present in all access logs
        //@formatter:on
        return new AccessLogDataFormatter(builder);

    }

    private AccessLogDataFormatter populateDefaultFormatters(int format) {

        // Note: @formatter is Eclipse's formatter - does not relate to the AccessLogDataFormatter
        //@formatter:off
        AccessLogDataFormatterBuilder builder = new AccessLogDataFormatterBuilder();
        builder.add(addRemoteHostField(format))  // %h
        .add(addRequestProtocolField  (format))  // %H
        .add(addRequestHostField      (format))  // %A
        .add(addBytesReceivedField    (format))  // %B
        .add(addRequestMethodField    (format))  // %m
        .add(addRequestPortField      (format))  // %p
        .add(addQueryStringField      (format))  // %q
        .add(addElapsedTimeField      (format))  // %{R}W
        .add(addResponseCodeField     (format))  // %s
        .add(addUriPathField          (format))  // %U
        .add(addUserAgentField        (format))  // User agent
        .add(addDatetimeField         (format))  // Datetime, present in all access logs
        .add(addSequenceField         (format)); // Sequence, present in all access logs

        return new AccessLogDataFormatter(builder);
        //@formatter:on
    }

    private void initializeSetterFormatter(String formatString, String jsonAccessLogFieldsConfig, String jsonAccessLogFieldsLogstashConfig, FormatSegment[] parsedFormat,
                                           AtomicLong seq) {
        SetterFormatter newSF = new SetterFormatter(formatString, jsonAccessLogFieldsConfig, jsonAccessLogFieldsLogstashConfig);
        List<AccessLogDataFieldSetter> fieldSetters = new ArrayList<AccessLogDataFieldSetter>();
        AccessLogDataFormatter[] formatters = { null, null, null, null };
        Map<String, HashSet<Object>> fieldsToAdd = new HashMap<String, HashSet<Object>>();

        // Create the mapping of fields to add:{<format key> : <data value/null>}
        // Prevents duplicates
        initializeFieldMap(fieldsToAdd, parsedFormat);

        // Create setter list
        fieldSetters = populateSetters(fieldsToAdd);
        // These fields are always added
        fieldSetters.add((ald, alrd) -> ald.setSequence(alrd.getStartTime() + "_" + String.format("%013X", seq.incrementAndGet())));
        fieldSetters.add((ald, alrd) -> ald.setDatetime(alrd.getTimestamp()));

        if (jsonAccessLogFieldsConfig.equals("default")) {
            formatters[0] = populateDefaultFormatters(AccessLogData.KEYS_JSON);
        } else if (jsonAccessLogFieldsConfig.equals("logFormat")) {
            formatters[1] = populateCustomFormatters(parsedFormat, AccessLogData.KEYS_JSON);
        }

        if (jsonAccessLogFieldsLogstashConfig.equals("default")) {
            formatters[2] = populateDefaultFormatters(AccessLogData.KEYS_LOGSTASH);
        } else if (jsonAccessLogFieldsLogstashConfig.equals("logFormat")) {
            formatters[3] = populateCustomFormatters(parsedFormat, AccessLogData.KEYS_LOGSTASH);
        }
        newSF.setSettersAndFormatters(fieldSetters, formatters);

        currentSF = newSF;
    }

    private class AccessLogHandler implements AccessLogForwarder {
        private final AtomicLong seq = new AtomicLong();

        /** {@inheritDoc} */
        @Override

        public void process(AccessLogRecordData recordData) {
            // The logFormat, as a string: e.g. "%a %b %C"
            String formatString = ((AccessLogRecordDataExt) recordData).getFormatString();
            // A parsed version of the logFormat
            FormatSegment[] parsedFormat = ((AccessLogRecordDataExt) recordData).getParsedFormat();
            jsonAccessLogFieldsConfig = AccessLogConfig.jsonAccessLogFieldsConfig;

            Configuration config = new Configuration(formatString, jsonAccessLogFieldsConfig, jsonAccessLogFieldsLogstashConfig);

            if (currentSF.checkConfigChange(formatString, jsonAccessLogFieldsConfig, jsonAccessLogFieldsLogstashConfig))
                if (setterFormatterMap.containsKey(config)) {
                    // If we've created a setterFormatter in the past for this configuration, we'll use it instead of making a new one
                    currentSF = setterFormatterMap.get(config);
                } else {
                    initializeSetterFormatter(formatString, jsonAccessLogFieldsConfig, jsonAccessLogFieldsLogstashConfig, parsedFormat, seq);
                    setterFormatterMap.put(config, currentSF);
                }
            // Take a copy of the current SetterFormatter because we don't want the SetterFormatter to change mid-use
            SetterFormatter temp = currentSF;
            AccessLogData accessLogData = new AccessLogData();
            for (AccessLogDataFieldSetter s : temp.getSetters()) {
                s.add(accessLogData, recordData);
            }

            accessLogData.addFormatters(temp.getFormatters());
            accessLogData.setSourceName(sourceName);

            bufferMgr.add(accessLogData);
            // CollectorJSONUtils does the rest of the work from here

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Added a event to buffer " + accessLogData);
            }
        }
    }

    // Field formatters
    private static JsonFieldAdder addRemoteHostField(int format) {
        return (jsonBuilder, ald) -> {
            return jsonBuilder.addField(AccessLogData.getRemoteHostKey(format), ald.getRemoteHost(), false, true);
        };
    }

    private static JsonFieldAdder addRequestProtocolField(int format) {
        return (jsonBuilder, ald) -> {
            return jsonBuilder.addField(AccessLogData.getRequestProtocolKey(format), ald.getRequestProtocol(), false, true);
        };
    }

    private static JsonFieldAdder addRequestHostField(int format) {
        return (jsonBuilder, ald) -> {
            return jsonBuilder.addField(AccessLogData.getRequestHostKey(format), ald.getRequestHost(), false, true);
        };
    }

    private static JsonFieldAdder addBytesReceivedField(int format) {
        return (jsonBuilder, ald) -> {
            return jsonBuilder.addField(AccessLogData.getBytesReceivedKey(format), ald.getBytesReceived(), false);
        };
    }

    private static JsonFieldAdder addRequestMethodField(int format) {
        return (jsonBuilder, ald) -> {
            return jsonBuilder.addField(AccessLogData.getRequestMethodKey(format), ald.getRequestMethod(), false, true);
        };
    }

    private static JsonFieldAdder addRequestPortField(int format) {
        return (jsonBuilder, ald) -> {
            return jsonBuilder.addField(AccessLogData.getRequestPortKey(format), ald.getRequestPort(), false, true);
        };
    }

    private static JsonFieldAdder addQueryStringField(int format) {
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

    private static JsonFieldAdder addElapsedTimeField(int format) {
        return (jsonBuilder, ald) -> {
            return jsonBuilder.addField(AccessLogData.getElapsedTimeKey(format), ald.getElapsedTime(), false);
        };
    }

    private static JsonFieldAdder addResponseCodeField(int format) {
        return (jsonBuilder, ald) -> {
            return jsonBuilder.addField(AccessLogData.getResponseCodeKey(format), ald.getResponseCode(), false);
        };
    }

    private static JsonFieldAdder addUriPathField(int format) {
        return (jsonBuilder, ald) -> {
            return jsonBuilder.addField(AccessLogData.getUriPathKey(format), ald.getUriPath(), false, true);
        };
    }

    private static JsonFieldAdder addRemoteIPField(int format) {
        return (jsonBuilder, ald) -> {
            return jsonBuilder.addField(AccessLogData.getRemoteIPKey(format), ald.getRemoteIP(), false, true);
        };
    }

    private static JsonFieldAdder addBytesSentField(int format) {
        return ((jsonBuilder, ald) -> {
            return jsonBuilder.addField(AccessLogData.getBytesSentKey(format), ald.getBytesSent(), false, true);
        });
    }

    private static JsonFieldAdder addCookiesField(int format) {
        return (jsonBuilder, ald) -> {
            if (ald.getCookies() != null)
                ald.getCookies().getList().forEach(c -> jsonBuilder.addField(AccessLogData.getCookieKey(format, c), c.getStringValue(), true, true));
            return jsonBuilder;
        };
    }

    private static JsonFieldAdder addRequestElapsedTimeField(int format) {
        return (jsonBuilder, ald) -> {
            return jsonBuilder.addField(AccessLogData.getRequestElapsedTimeKey(format), ald.getRequestElapsedTime(), false);
        };
    }

    private static JsonFieldAdder addUserAgentField(int format) {
        return (jsonBuilder, ald) -> {
            String userAgent = ald.getUserAgent();
            if (userAgent != null && userAgent.length() > MAX_USER_AGENT_LENGTH)
                userAgent = userAgent.substring(0, MAX_USER_AGENT_LENGTH);
            return jsonBuilder.addField(AccessLogData.getUserAgentKey(format), userAgent, true, true);
        };
    }

    private static JsonFieldAdder addRequestHeaderField(int format) {
        return (jsonBuilder, ald) -> {
            if (ald.getRequestHeaders() != null)
                ald.getRequestHeaders().getList().forEach(h -> jsonBuilder.addField(AccessLogData.getRequestHeaderKey(format, h), h.getStringValue(), false, true));
            return jsonBuilder;
        };
    }

    private static JsonFieldAdder addResponseHeaderField(int format) {
        return (jsonBuilder, ald) -> {
            if (ald.getResponseHeaders() != null)
                ald.getResponseHeaders().getList().forEach(h -> jsonBuilder.addField(AccessLogData.getResponseHeaderKey(format, h), h.getStringValue(), true, true));
            return jsonBuilder;
        };
    }

    private static JsonFieldAdder addRequestFirstLineField(int format) {
        return (jsonBuilder, ald) -> {
            return jsonBuilder.addField(AccessLogData.getRequestFirstLineKey(format), ald.getRequestFirstLine(), false, true);
        };
    }

    private static JsonFieldAdder addRequestStartTimeField(int format) {
        return (jsonBuilder, ald) -> {
            return jsonBuilder.addField(AccessLogData.getRequestStartTimeKey(format), ald.getRequestStartTime(), false, true);
        };
    }

    private static JsonFieldAdder addAccessLogDatetimeField(int format) {
        return (jsonBuilder, ald) -> {
            return jsonBuilder.addField(AccessLogData.getAccessLogDatetimeKey(format), ald.getAccessLogDatetime(), false, true);
        };
    }

    private static JsonFieldAdder addRemoteUserField(int format) {
        return (jsonBuilder, ald) -> {
            if (ald.getRemoteUser() != null && !ald.getRemoteUser().isEmpty())
                return jsonBuilder.addField(AccessLogData.getRemoteUserKey(format), ald.getRemoteUser(), false, true);
            return jsonBuilder;
        };
    }

    private static JsonFieldAdder addSequenceField(int format) {
        return (jsonBuilder, ald) -> {
            return jsonBuilder.addField(AccessLogData.getSequenceKey(format), ald.getSequence(), false, true);
        };
    }

    private static JsonFieldAdder addDatetimeField(int format) {
        return (jsonBuilder, ald) -> {
            String datetime = CollectorJsonHelpers.dateFormatTL.get().format(ald.getDatetime());
            return jsonBuilder.addField(AccessLogData.getDatetimeKey(format), datetime, false, true);
        };
    }
}
