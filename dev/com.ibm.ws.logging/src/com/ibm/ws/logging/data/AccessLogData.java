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

import java.util.ArrayList;
import java.util.List;
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
                                              LogFieldConstants.IBM_REQUESTSTARTTIME,
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
                                            LogFieldConstants.REMOTEIP,
                                            LogFieldConstants.BYTESSENT,
                                            LogFieldConstants.COOKIE,
                                            LogFieldConstants.REQUESTELAPSEDTIME,
                                            LogFieldConstants.REQUESTHEADER,
                                            LogFieldConstants.RESPONSEHEADER,
                                            LogFieldConstants.REQUESTFIRSTLINE,
                                            LogFieldConstants.REQUESTSTARTTIME,
                                            LogFieldConstants.ACCESSLOGDATETIME,
                                            LogFieldConstants.REMOTEUSERID
    };

    private static NameAliases jsonLoggingNameAliases = new NameAliases(NAMES1_1);
    // lg265 testing
    public static String isCustomAccessLogToJSONEnabled = "";
    public static String isCustomAccessLogToJSONEnabledCollector = "";
    public List<String> formatSpecifiers;

    public void setFormatSpecifierList(List<String> formatSpecifiers) {
        this.formatSpecifiers = formatSpecifiers;
    }

    public List<String> getFormatSpecifierList() {
        return formatSpecifiers;
    }

    public static void newJsonLoggingNameAliases(Map<String, String> newAliases) {
        jsonLoggingNameAliases.newAliases(newAliases);
    }

    public static void resetJsonLoggingNameAliases() {
        jsonLoggingNameAliases.resetAliases();
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

//    public void setRequestStartTime(HashMap<String, String> map, long l) {
//
//    }

    public void setRequestStartTime(long l) {
        setPair(0, l);
    }

    public void setUriPath(String s, ArrayList<String> formatSpecifiers) {
        if (formatSpecifiers.contains("%U"))
            setUriPath(s);
    }

    public void setUriPath(String s) {
        setPair(1, s);
    }

    public void setRequestMethod(String s, ArrayList<String> formatSpecifiers) {
        if (formatSpecifiers.contains("%m"))
            setRequestMethod(s);
    }

    public void setRequestMethod(String s) {
        setPair(2, s);
    }

    public void setQueryString(String s, ArrayList<String> formatSpecifiers) {
        if (formatSpecifiers.contains("%q"))
            setQueryString(s);
    }

    public void setQueryString(String s) {
        setPair(3, s);
    }

    public void setRequestHost(String s, ArrayList<String> formatSpecifiers) {
        if (formatSpecifiers.contains("%A"))
            setRequestHost(s);
    }

    public void setRequestHost(String s) {
        setPair(4, s);
    }

    public void setRequestPort(String s, ArrayList<String> formatSpecifiers) {
        if (formatSpecifiers.contains("%p"))
            setRequestPort(s);
    }

    public void setRequestPort(String s) {
        setPair(5, s);
    }

    public void setRemoteHost(String s, ArrayList<String> formatSpecifiers) {
        if (formatSpecifiers.contains("%h"))
            setRequestFirstLine(s);
    }

    public void setRemoteHost(String s) {
        setPair(6, s);
    }

    // User agent isn't part of logFormat
    public void setUserAgent(String s) {
        setPair(7, s);
    }

    public void setRequestProtocol(String s, ArrayList<String> formatSpecifiers) {
        if (formatSpecifiers.contains("%H"))
            setRequestProtocol(s);
    }

    public void setRequestProtocol(String s) {
        setPair(8, s);
    }

    public void setBytesReceived(long l, ArrayList<String> formatSpecifiers) {
        if (formatSpecifiers.contains("%B"))
            setBytesReceived(l);
    }

    public void setBytesReceived(long l) {
        setPair(9, l);
    }

    public void setResponseCode(int i, ArrayList<String> formatSpecifiers) {
        if (formatSpecifiers.contains("%s"))
            setResponseCode(i);
    }

    public void setResponseCode(int i) {
        setPair(10, i);
    }

    public void setElapsedTime(long l, ArrayList<String> formatSpecifiers) {
        if (formatSpecifiers.contains("%{R}W"))
            setElapsedTime(l);
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

    // LG-265
    public void setRemoteIP(String s, ArrayList<String> formatSpecifiers) {
        if (formatSpecifiers.contains("%a"))
            setRemoteIP(s);
    }

    public void setRemoteIP(String s) {
        setPair(14, s);
    }

    public void setBytesSent(String s, ArrayList<String> formatSpecifiers) {
        if (formatSpecifiers.contains("%b"))
            setBytesSent(s);
    }

    public void setBytesSent(String s) {
        setPair(15, s);
    }

    public void setCookies(KeyValuePairList kvps) {
        setPair(16, kvps);
    }

    public void setRequestElapsedTime(long l, ArrayList<String> formatSpecifiers) {
        if (formatSpecifiers.contains("%D"))
            setRequestElapsedTime(l);
    }

    public void setRequestElapsedTime(long l) {
        setPair(17, l);
    }

    public void setRequestHeader(KeyValuePairList kvps) {
        setPair(18, kvps);
    }

    public void setResponseHeader(KeyValuePairList kvps) {
        setPair(19, kvps);
    }

    public void setRequestFirstLine(String s, ArrayList<String> formatSpecifiers) {
        if (formatSpecifiers.contains("%r"))
            setRequestFirstLine(s);
    }

    public void setRequestFirstLine(String s) {
        setPair(20, s);
    }

    public void setRequestStartTime(String s, ArrayList<String> formatSpecifiers) {
        if (formatSpecifiers.contains("%t"))
            setRequestFirstLine(s);
    }

    public void setRequestStartTime(String s) {
        setPair(21, s);
    }

    public void setAccessLogDatetime(String s, ArrayList<String> formatSpecifiers) {
        if (formatSpecifiers.contains("%{t}W"))
            setAccessLogDatetime(s);
    }

    public void setAccessLogDatetime(String s) {
        setPair(22, s);
    }

    public void setRemoteUser(String s, ArrayList<String> formatSpecifiers) {
        if (formatSpecifiers.contains("%u"))
            setRemoteUser(s);
    }

    public void setRemoteUser(String s) {
        setPair(23, s);
    }
    // END LG-265

    // Check if release bug or optional field
//    public long getRequestStartTime() {
//        return getLongValue(0);
//    }

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

    public String getRequestStartTime() {
        return getStringValue(21);
    }

    public String getAccessLogDatetime() {
        return getStringValue(22);
    }

    public String getRemoteUser() {
        return getStringValue(23);
    }

    private KeyValuePairList getValues(int index) {
        return (KeyValuePairList) getPairs()[index];
    }

//    public String getRequestStartTimeKey() {
//        return NAMES[0];
//    }

    public String getUriPathKey() {
        return NAMES[1];
    }

    public String getRequestMethodKey() {
        return NAMES[2];
    }

    public String getQueryStringKey() {
        return NAMES[3];
    }

    public String getRequestHostKey() {
        return NAMES[4];
    }

    public String getRequestPortKey() {
        return NAMES[5];
    }

    public String getRemoteHostKey() {
        return NAMES[6];
    }

    public String getUserAgentKey() {
        return NAMES[7];
    }

    public String getRequestProtocolKey() {
        return NAMES[8];
    }

    public String getBytesReceivedKey() {
        return NAMES[9];
    }

    public String getResponseCodeKey() {
        return NAMES[10];
    }

    public String getElapsedTimeKey() {
        return NAMES[11];
    }

    public String getDatetimeKey() {
        return NAMES[12];
    }

    public String getSequenceKey() {
        return NAMES[13];
    }

    // LG 265
    public String getRemoteIPKey() {
        return NAMES[14];
    }

    public String getBytesSentKey() {
        return NAMES[15];
    }

    public String getCookieKey(KeyValuePair kvp) {
        return NAMES[16] + "_" + kvp.getKey();
    }

    public String getRequestElapsedTimeKey() {
        return NAMES[17];
    }

    public String getRequestHeaderKey(KeyValuePair kvp) {
        return NAMES[18] + "_" + kvp.getKey();
    }

    public String getResponseHeaderKey(KeyValuePair kvp) {
        return NAMES[19] + "_" + kvp.getKey();
    }

    public String getRequestFirstLineKey() {
        return NAMES[20];
    }

    public String getRequestStartTimeKey() {
        return NAMES[21];
    }

    public String getAccessLogDatetimeKey() {
        return NAMES[22];
    }

    public String getRemoteUserKey() {
        return NAMES[23];
    }
    // LG 265

    public String getRequestStartTimeKey1_1() {
        return NAMES1_1[0];
    }

    public String getUriPathKey1_1() {
        return NAMES1_1[1];
    }

    public String getRequestMethodKey1_1() {
        return NAMES1_1[2];
    }

    public String getQueryStringKey1_1() {
        return NAMES1_1[3];
    }

    public String getRequestHostKey1_1() {
        return NAMES1_1[4];
    }

    public String getRequestPortKey1_1() {
        return NAMES1_1[5];
    }

    public String getRemoteHostKey1_1() {
        return NAMES1_1[6];
    }

    public String getUserAgentKey1_1() {
        return NAMES1_1[7];
    }

    public String getRequestProtocolKey1_1() {
        return NAMES1_1[8];
    }

    public String getBytesReceivedKey1_1() {
        return NAMES1_1[9];
    }

    public String getResponseCodeKey1_1() {
        return NAMES1_1[10];
    }

    public String getElapsedTimeKey1_1() {
        return NAMES1_1[11];
    }

    public String getDatetimeKey1_1() {
        return NAMES1_1[12];
    }

    public String getSequenceKey1_1() {
        return NAMES1_1[13];
    }

    //name aliases

    // TODO: Maybe uncomment this later? -> see if this is a release bug or an optional field
//    public static String getRequestStartTimeKeyJSON() {
//        return jsonLoggingNameAliases.aliases[0];
//    }

    public static String getUriPathKeyJSON() {
        return jsonLoggingNameAliases.aliases[1];
    }

    public static String getRequestMethodKeyJSON() {
        return jsonLoggingNameAliases.aliases[2];
    }

    public static String getQueryStringKeyJSON() {
        return jsonLoggingNameAliases.aliases[3];
    }

    public static String getRequestHostKeyJSON() {
        return jsonLoggingNameAliases.aliases[4];
    }

    public static String getRequestPortKeyJSON() {
        return jsonLoggingNameAliases.aliases[5];
    }

    public static String getRemoteHostKeyJSON() {
        return jsonLoggingNameAliases.aliases[6];
    }

    public static String getUserAgentKeyJSON() {
        return jsonLoggingNameAliases.aliases[7];
    }

    public static String getRequestProtocolKeyJSON() {
        return jsonLoggingNameAliases.aliases[8];
    }

    public static String getBytesReceivedKeyJSON() {
        return jsonLoggingNameAliases.aliases[9];
    }

    public static String getResponseCodeKeyJSON() {
        return jsonLoggingNameAliases.aliases[10];
    }

    public static String getElapsedTimeKeyJSON() {
        return jsonLoggingNameAliases.aliases[11];
    }

    public static String getDatetimeKeyJSON() {
        return jsonLoggingNameAliases.aliases[12];
    }

    public static String getSequenceKeyJSON() {
        return jsonLoggingNameAliases.aliases[13];
    }

    public static String getHostKeyJSON() {
        return jsonLoggingNameAliases.aliases[14];
    }

    public static String getUserDirKeyJSON() {
        return jsonLoggingNameAliases.aliases[15];
    }

    public static String getServerNameKeyJSON() {
        return jsonLoggingNameAliases.aliases[16];
    }

    public static String getTypeKeyJSON() {
        return jsonLoggingNameAliases.aliases[17];
    }

    // LG-265
    public static String getRemoteIPKeyJSON() {
        return jsonLoggingNameAliases.aliases[18];
    }

    public static String getBytesSentKeyJSON() {
        return jsonLoggingNameAliases.aliases[19];
    }

    public static String getCookieKeyJSON(KeyValuePair kvp) {
        return jsonLoggingNameAliases.aliases[20] + "_" + kvp.getKey();
    }

    public static String getRequestElapsedTimeKeyJSON() {
        return jsonLoggingNameAliases.aliases[21];
    }

    public static String getRequestHeaderKeyJSON(KeyValuePair kvp) {
        return jsonLoggingNameAliases.aliases[22] + "_" + kvp.getKey();
    }

    public static String getResponseHeaderKeyJSON(KeyValuePair kvp) {
        return jsonLoggingNameAliases.aliases[23] + "_" + kvp.getKey();
    }

    public static String getRequestFirstLineKeyJSON() {
        return jsonLoggingNameAliases.aliases[24];
    }

    public static String getRequestStartTimeKeyJSON() {
        return jsonLoggingNameAliases.aliases[25];
    }

    public static String getAccessLogDatetimeKeyJSON() {
        return jsonLoggingNameAliases.aliases[26];
    }

    public static String getRemoteUserKeyJSON() {
        return jsonLoggingNameAliases.aliases[27];
    }
    // END LG-265
}
