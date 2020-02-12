/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.data;

/**
 *
 */
public class AccessLogExtraData {
    String remoteIP;
    String bytesReceivedFormatted;
    KeyValuePairList cookies = new KeyValuePairList("cookies");
    long requestElapsedTime = -1;
    KeyValuePairList requestHeaders = new KeyValuePairList("requestHeaders");
    KeyValuePairList responseHeaders = new KeyValuePairList("responseHeaders");
    String requestFirstLine;
    String requestStartTime;
    String accessLogDatetime;
    String remoteUserID;

    // remote IP
    public void setRemoteIP(String remoteIP) {
        this.remoteIP = remoteIP;
    }

    public String getRemoteIP() {
        return remoteIP;
    }

    // bytes formatted as string
    public void setBytesReceivedFormatted(String bytesReceivedFormatted) {
        this.bytesReceivedFormatted = bytesReceivedFormatted;
    }

    public String getBytesReceivedFormatted() {
        return bytesReceivedFormatted;
    }

    // cookies
    public void setCookie(String cookieName, String cookieValue) {
        cookies.addKeyValuePair(cookieName, cookieValue);
    }

    public KeyValuePairList getCookies() {
        return cookies;
    }

    public int getNumberOfCookies() {
        return cookies.getList().size();
    }

    // elapsed time
    public void setRequestElapsedTime(long requestElapsedTime) {
        this.requestElapsedTime = requestElapsedTime;
    }

    public long getRequestElapsedTime() {
        return requestElapsedTime;
    }

    // header name input
    public void setRequestHeader(String requestHeaderName, String requestHeaderValue) {
        requestHeaders.addKeyValuePair(requestHeaderName, requestHeaderValue);
    }

    public KeyValuePairList getRequestHeaders() {
        return requestHeaders;
    }

    public int getNumberOfRequestHeaders() {
        return requestHeaders.getList().size();
    }

    // header name output

    public void setResponseHeader(String responseHeaderName, String responseHeaderValue) {
        responseHeaders.addKeyValuePair(responseHeaderName, responseHeaderValue);
    }

    public KeyValuePairList getResponseHeaders() {
        return responseHeaders;
    }

    public int getNumberOfResponseHeaders() {
        return responseHeaders.getList().size();
    }

    // request first line
    public void setRequestFirstLine(String requestFirstLine) {
        this.requestFirstLine = requestFirstLine;
    }

    public String getRequestFirstLine() {
        return requestFirstLine;
    }

    // request start time
    public void setRequestStartTime(String requestStartTime) {
        this.requestStartTime = requestStartTime;
    }

    public String getRequestStartTime() {
        return requestStartTime;
    }

    // access log datetime
    public void setAccessLogDatetime(String accessLogDatetime) {
        this.accessLogDatetime = accessLogDatetime;
    }

    public String getAccessLogDatetime() {
        return accessLogDatetime;
    }

    // remote user id
    public void setRemoteUser(String remoteUserID) {
        this.remoteUserID = remoteUserID;
    }

    public String getRemoteUser() {
        return remoteUserID;
    }

    public boolean isSet(String s) {
        switch (s) {
            case "remoteIP":
                if (remoteIP != null)
                    return true;
                return false;

            case "bytesReceivedFormatted":
                if (bytesReceivedFormatted != null)
                    return true;
                return false;

            case "cookies":
                if (getNumberOfCookies() > 0)
                    return true;
                return false;

            case "requestElapsedTime":
                if (requestElapsedTime > 0)
                    return true;
                return false;

            case "requestHeaders":
                if (getNumberOfRequestHeaders() > 0)
                    return true;
                return false;

            case "responseHeaders":
                if (getNumberOfResponseHeaders() > 0)
                    return true;
                return false;

            case "requestFirstLine":
                if (requestFirstLine != null)
                    return true;
                return false;

            case "requestStartTime":
                if (requestStartTime != null)
                    return true;
                return false;

            case "accessLogDatetime":
                if (accessLogDatetime != null)
                    return true;
                return false;

            case "remoteUserID":
                if (remoteUserID != null)
                    return true;
                return false;
        }
        return false;
    }
}
