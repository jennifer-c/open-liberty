/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.data;

import java.util.HashMap;
import java.util.Map;

import com.ibm.ws.logging.collector.LogFieldConstants;

/**
 *
 */
public class AccessLogData extends GenericData {
    public static final String[] NAMES1_1 = {
                                              LogFieldConstants.IBM_REQUESTSTARTTIME,
                                              LogFieldConstants.IBM_URIPATH,
                                              LogFieldConstants.IBM_REQUESTMETHOD,
                                              LogFieldConstants.IBM_QUERYSTRING,
                                              LogFieldConstants.IBM_REQUESTHOST,
                                              LogFieldConstants.IBM_REQUESTPORT,
                                              LogFieldConstants.IBM_REMOTEHOST,
                                              LogFieldConstants.IBM_USERAGENT,
                                              LogFieldConstants.IBM_REQUESTPROTOCOL,
                                              LogFieldConstants.IBM_BYTESRECEIVED,
                                              LogFieldConstants.IBM_RESPONSECODE,
                                              LogFieldConstants.IBM_ELAPSEDTIME,
                                              LogFieldConstants.IBM_DATETIME,
                                              LogFieldConstants.IBM_SEQUENCE,
                                              LogFieldConstants.HOST,
                                              LogFieldConstants.IBM_USERDIR,
                                              LogFieldConstants.IBM_SERVERNAME,
                                              LogFieldConstants.TYPE,
                                              LogFieldConstants.IBM_REMOTEIP,
                                              LogFieldConstants.IBM_BYTESSENT,
                                              LogFieldConstants.IBM_COOKIE,
                                              LogFieldConstants.IBM_REQUESTELAPSEDTIME,
                                              LogFieldConstants.IBM_REQUESTHEADER,
                                              LogFieldConstants.IBM_RESPONSEHEADER,
                                              LogFieldConstants.IBM_REQUESTFIRSTLINE,
                                              LogFieldConstants.IBM_ACCESSLOGDATETIME,
                                              LogFieldConstants.IBM_REMOTEUSERID

    };

    private final static String[] NAMES = {
                                            LogFieldConstants.REQUESTSTARTTIME,
                                            LogFieldConstants.URIPATH,
                                            LogFieldConstants.REQUESTMETHOD,
                                            LogFieldConstants.QUERYSTRING,
                                            LogFieldConstants.REQUESTHOST,
                                            LogFieldConstants.REQUESTPORT,
                                            LogFieldConstants.REMOTEHOST,
                                            LogFieldConstants.USERAGENT,
                                            LogFieldConstants.REQUESTPROTOCOL,
                                            LogFieldConstants.BYTESRECEIVED,
                                            LogFieldConstants.RESPONSECODE,
                                            LogFieldConstants.ELAPSEDTIME,
                                            LogFieldConstants.DATETIME,
                                            LogFieldConstants.SEQUENCE,
                                            LogFieldConstants.HOSTNAME,
                                            LogFieldConstants.WLPUSERDIR,
                                            LogFieldConstants.SERVERNAME,
                                            LogFieldConstants.TYPE,
                                            LogFieldConstants.REMOTEIP,
                                            LogFieldConstants.BYTESSENT,
                                            LogFieldConstants.COOKIE,
                                            LogFieldConstants.REQUESTELAPSEDTIME,
                                            LogFieldConstants.REQUESTHEADER,
                                            LogFieldConstants.RESPONSEHEADER,
                                            LogFieldConstants.REQUESTFIRSTLINE,
                                            LogFieldConstants.ACCESSLOGDATETIME,
                                            LogFieldConstants.REMOTEUSERID
    };

    // For renaming/omitting fields
    private static Map<String, String> cookieMap = new HashMap<>();
    private static Map<String, String> requestHeaderMap = new HashMap<>();
    private static Map<String, String> responseHeaderMap = new HashMap<>();

    private static NameAliases jsonLoggingNameAliases = new NameAliases(NAMES1_1);
    private static NameAliases jsonLoggingNameAliasesLogstash = new NameAliases(NAMES);

    private static NameAliases[] nameAliases = { jsonLoggingNameAliases, jsonLoggingNameAliasesLogstash };

    public AccessLogDataFormatter[] formatters = new AccessLogDataFormatter[4];
    KeyValuePairList kvplCookies = new KeyValuePairList("cookies");
    KeyValuePairList kvplRequestHeaders = new KeyValuePairList("requestHeaders");
    KeyValuePairList kvplResponseHeaders = new KeyValuePairList("responseHeaders");

    public static void populateDataMaps(Map<String, String> cookies, Map<String, String> requestHeaders, Map<String, String> responseHeaders) {
        cookieMap = cookies;
        requestHeaderMap = requestHeaders;
        responseHeaderMap = responseHeaders;
    }

    public void addFormatters(AccessLogDataFormatter[] formatters) {
        this.formatters = formatters;
    }

    public AccessLogDataFormatter[] getFormatters() {
        return this.formatters;
    }

    public static void newJsonLoggingNameAliases(Map<String, String> newAliases) {
        jsonLoggingNameAliases.newAliases(newAliases);
    }

    public static void resetJsonLoggingNameAliases() {
        jsonLoggingNameAliases.resetAliases();
        cookieMap.clear();
        requestHeaderMap.clear();
        responseHeaderMap.clear();
    }

    public AccessLogData() {
        super(24);
    }

    private void setPair(int index, String s) {
        setPair(index, NAMES1_1[index], s);
    }

    private void setPair(int index, int i) {
        setPair(index, NAMES1_1[index], i);
    }

    private void setPair(int index, long l) {
        setPair(index, NAMES1_1[index], l);
    }

    public void setRequestStartTime(String s) {
        setPair(0, s);
    }

    public void setUriPath(String s) {
        setPair(1, s);
    }

    public void setRequestMethod(String s) {
        setPair(2, s);
    }

    public void setQueryString(String s) {
        setPair(3, s);
    }

    public void setRequestHost(String s) {
        setPair(4, s);
    }

    public void setRequestPort(String s) {
        setPair(5, s);
    }

    public void setRemoteHost(String s) {
        setPair(6, s);
    }

    public void setUserAgent(String s) {
        setPair(7, s);
    }

    public void setRequestProtocol(String s) {
        setPair(8, s);
    }

    public void setBytesReceived(long l) {
        setPair(9, l);
    }

    public void setResponseCode(int i) {
        setPair(10, i);
    }

    public void setElapsedTime(long l) {
        setPair(11, l);
    }

    // Datetime also not part of logFormat
    public void setDatetime(long l) {
        setPair(12, l);
    }

    // Sequence also not part of logFormat
    public void setSequence(String s) {
        setPair(13, s);
    }

    // New optional access log fields
    public void setRemoteIP(String s) {
        setPair(14, s);
    }

    public void setBytesSent(String s) {
        setPair(15, s);
    }

    public void setCookies(String name, String value) {
        kvplCookies.addKeyValuePair(name, value);
        setPair(16, kvplCookies);
    }

    public void setRequestElapsedTime(long l) {
        setPair(17, l);
    }

    public void setRequestHeader(String name, String value) {
        kvplRequestHeaders.addKeyValuePair(name, value);
        setPair(18, kvplRequestHeaders);
    }

    public void setResponseHeader(String name, String value) {
        kvplResponseHeaders.addKeyValuePair(name, value);
        setPair(19, kvplResponseHeaders);
    }

    public void setRequestFirstLine(String s) {
        setPair(20, s);
    }

    public void setAccessLogDatetime(String s) {
        setPair(21, s);
    }

    public void setRemoteUser(String s) {
        setPair(22, s);
    }

    public String getRequestStartTime() {
        return getStringValue(0);
    }

    public String getUriPath() {
        return getStringValue(1);
    }

    public String getRequestMethod() {
        return getStringValue(2);
    }

    public String getQueryString() {
        return getStringValue(3);
    }

    public String getRequestHost() {
        return getStringValue(4);
    }

    public String getRequestPort() {
        return getStringValue(5);
    }

    public String getRemoteHost() {
        return getStringValue(6);
    }

    public String getUserAgent() {
        return getStringValue(7);
    }

    public String getRequestProtocol() {
        return getStringValue(8);
    }

    public long getBytesReceived() {
        try {
            return getLongValue(9);
        } catch (Exception e) {
            // Do nothing, the field hasn't been set in the logFormat
        }
        return -1;
    }

    public int getResponseCode() {
        try {
            return getIntValue(10);
        } catch (Exception e) {
            // Do nothing, the field hasn't been set in the logFormat
        }
        return -1;
    }

    public long getElapsedTime() {
        try {
            return getLongValue(11);
        } catch (Exception e) {
            // Do nothing, the field hasn't been set in the logFormat
        }
        return -1;
    }

    public long getDatetime() {
        try {
            return getLongValue(12);
        } catch (Exception e) {
            // Do nothing, the field hasn't been set in the logFormat
        }
        return -1;
    }

    public String getSequence() {
        return getStringValue(13);
    }

    public String getRemoteIP() {
        return getStringValue(14);
    }

    public String getBytesSent() {
        return getStringValue(15);
    }

    public KeyValuePairList getCookies() {
        return getValues(16);
    }

    public long getRequestElapsedTime() {
        try {
            return getLongValue(17);
        } catch (Exception e) {
            // Do nothing, the field hasn't been set in the logFormat
        }
        return -1;
    }

    public KeyValuePairList getRequestHeaders() {
        return getValues(18);
    }

    public KeyValuePairList getResponseHeaders() {
        return getValues(19);
    }

    public String getRequestFirstLine() {
        return getStringValue(20);
    }

    public String getAccessLogDatetime() {
        return getStringValue(21);
    }

    public String getRemoteUser() {
        return getStringValue(22);
    }

    private KeyValuePairList getValues(int index) {
        return (KeyValuePairList) getPairs()[index];
    }

    public static String getRequestStartTimeKey(FormatSpecifier format) {
        return nameAliases[format.getValue()].aliases[0];
    }

    public static String getUriPathKey(FormatSpecifier format) {
        return nameAliases[format.getValue()].aliases[1];
    }

    public static String getRequestMethodKey(FormatSpecifier format) {
        return nameAliases[format.getValue()].aliases[2];
    }

    public static String getQueryStringKey(FormatSpecifier format) {
        return nameAliases[format.getValue()].aliases[3];
    }

    public static String getRequestHostKey(FormatSpecifier format) {
        return nameAliases[format.getValue()].aliases[4];
    }

    public static String getRequestPortKey(FormatSpecifier format) {
        return nameAliases[format.getValue()].aliases[5];
    }

    public static String getRemoteHostKey(FormatSpecifier format) {
        return nameAliases[format.getValue()].aliases[6];
    }

    public static String getUserAgentKey(FormatSpecifier format) {
        return nameAliases[format.getValue()].aliases[7];
    }

    public static String getRequestProtocolKey(FormatSpecifier format) {
        return nameAliases[format.getValue()].aliases[8];
    }

    public static String getBytesReceivedKey(FormatSpecifier format) {
        return nameAliases[format.getValue()].aliases[9];
    }

    public static String getResponseCodeKey(FormatSpecifier format) {
        return nameAliases[format.getValue()].aliases[10];
    }

    public static String getElapsedTimeKey(FormatSpecifier format) {
        return nameAliases[format.getValue()].aliases[11];
    }

    public static String getDatetimeKey(FormatSpecifier format) {
        return nameAliases[format.getValue()].aliases[12];
    }

    public static String getSequenceKey(FormatSpecifier format) {
        return nameAliases[format.getValue()].aliases[13];
    }

    public static String getHostKey(FormatSpecifier format) {
        return nameAliases[format.getValue()].aliases[14];
    }

    public static String getUserDirKey(FormatSpecifier format) {
        return nameAliases[format.getValue()].aliases[15];
    }

    public static String getServerNameKey(FormatSpecifier format) {
        return nameAliases[format.getValue()].aliases[16];
    }

    public static String getTypeKey(FormatSpecifier format) {
        return nameAliases[format.getValue()].aliases[17];
    }

    public static String getRemoteIPKey(FormatSpecifier format) {
        return nameAliases[format.getValue()].aliases[18];
    }

    public static String getBytesSentKey(FormatSpecifier format) {
        return nameAliases[format.getValue()].aliases[19];
    }

    public static String getCookieKey(FormatSpecifier format, KeyValuePair kvp) {
        String cookieName = kvp.getKey();
        if (cookieMap.containsKey(cookieName) && format.equals("JSON")) {
            return cookieMap.get(cookieName);
        }
        return nameAliases[format.getValue()].aliases[20] + "_" + kvp.getKey();
    }

    public static String getRequestElapsedTimeKey(FormatSpecifier format) {
        return nameAliases[format.getValue()].aliases[21];
    }

    public static String getRequestHeaderKey(FormatSpecifier format, KeyValuePair kvp) {
        String requestHeader = kvp.getKey();
        if (requestHeaderMap.containsKey(requestHeader) && format.equals("JSON")) {
            return requestHeaderMap.get(requestHeader);
        }
        return nameAliases[format.getValue()].aliases[22] + "_" + kvp.getKey();
    }

    public static String getResponseHeaderKey(FormatSpecifier format, KeyValuePair kvp) {
        String responseHeader = kvp.getKey();
        if (responseHeaderMap.containsKey(responseHeader) && format.equals("JSON")) {
            return responseHeaderMap.get(responseHeader);
        }
        return nameAliases[format.getValue()].aliases[23] + "_" + kvp.getKey();
    }

    public static String getRequestFirstLineKey(FormatSpecifier format) {
        return nameAliases[format.getValue()].aliases[24];
    }

    public static String getAccessLogDatetimeKey(FormatSpecifier format) {
        return nameAliases[format.getValue()].aliases[25];
    }

    public static String getRemoteUserKey(FormatSpecifier format) {
        return nameAliases[format.getValue()].aliases[26];
    }
}
