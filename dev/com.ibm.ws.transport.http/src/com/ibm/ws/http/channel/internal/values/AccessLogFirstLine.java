/*******************************************************************************
 * Copyright (c) 2004, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http.channel.internal.values;

import com.ibm.ws.genericbnf.internal.GenericUtils;
import com.ibm.wsspi.http.channel.HttpRequestMessage;
import com.ibm.wsspi.http.channel.HttpResponseMessage;

public class AccessLogFirstLine extends AccessLogData {

    public AccessLogFirstLine() {
        super("%r");
        // %r
        // First line of request
    }

    @Override
    public boolean set(StringBuilder accessLogEntry,
                       HttpResponseMessage response, HttpRequestMessage request, Object data) {
        String requestMethod = null;
        String requestURI = null;
        String requestQueryString = null;
        String requestVersion = null;

        if (request != null) {
            requestMethod = getRequestMethod(response, request, data);
            requestURI = getRequestURI(response, request, data);
            requestQueryString = getRequestQueryString(response, request, data);
            requestVersion = getRequestVersion(response, request, data);
        }

        logSafe(accessLogEntry, requestMethod);
        accessLogEntry.append(" ");
        logSafe(accessLogEntry, requestURI);

        if (requestQueryString != null) {
            accessLogEntry.append("?");
            accessLogEntry.append(GenericUtils.nullOutPasswords(requestQueryString, (byte) '&'));
        }

        accessLogEntry.append(" ");
        logSafe(accessLogEntry, requestVersion);

        return true;
    }

    public static String getRequestMethod(HttpResponseMessage response, HttpRequestMessage request, Object data) {
        String requestMethod = null;
        if (request != null) {
            requestMethod = request.getMethod();
        }
        return requestMethod;
    }

    public static String getRequestURI(HttpResponseMessage response, HttpRequestMessage request, Object data) {
        String requestURI = null;
        if (request != null) {
            requestURI = request.getRequestURI();
        }
        return requestURI;
    }

    public static String getRequestQueryString(HttpResponseMessage response, HttpRequestMessage request, Object data) {
        String requestQueryString = null;
        if (request != null) {
            requestQueryString = request.getQueryString();
        }
        return requestQueryString;
    }

    public static String getRequestVersion(HttpResponseMessage response, HttpRequestMessage request, Object data) {
        String requestVersion = null;
        if (request != null) {
            requestVersion = request.getVersion();
        }
        return requestVersion;
    }

    public static String getFirstLineAsString(HttpResponseMessage response, HttpRequestMessage request, Object data) {
        if (request != null) {
            StringBuilder sb = new StringBuilder();
            String requestMethod = getRequestMethod(response, request, data);
            String requestURI = getRequestURI(response, request, data);
            String requestQueryString = getRequestQueryString(response, request, data);
            String requestVersion = getRequestVersion(response, request, data);
            sb.append(requestMethod);
            sb.append(" ");
            sb.append(requestURI);
            if (requestQueryString != null) {
                sb.append("?");
                sb.append(GenericUtils.nullOutPasswords(requestQueryString, (byte) '&'));
            }
            sb.append(" ");
            sb.append(requestVersion);
            return sb.toString();
        }
        return null;
    }

}
