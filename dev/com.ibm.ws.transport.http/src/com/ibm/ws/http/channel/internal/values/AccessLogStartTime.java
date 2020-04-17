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

import java.util.Date;
import java.util.concurrent.TimeUnit;

import com.ibm.ws.http.channel.internal.HttpRequestMessageImpl;
import com.ibm.ws.http.dispatcher.internal.HttpDispatcher;
import com.ibm.wsspi.http.channel.HttpRequestMessage;
import com.ibm.wsspi.http.channel.HttpResponseMessage;

public class AccessLogStartTime extends AccessLogData {

    public AccessLogStartTime() {
        super("%t");
        // %T - Note the start time for the request
    }

    @Override
    public boolean set(StringBuilder accessLogEntry,
                       HttpResponseMessage response, HttpRequestMessage request,
                       Object data) {
        // Can we just use "getStartTimeAsString" instead, or will that impact performance?
        long startTime = getStartTime(response, request, data);

        if (startTime != 0) {
            Date startDate = new Date(startTime);
            accessLogEntry.append("[");
            accessLogEntry.append(HttpDispatcher.getDateFormatter().getNCSATime(startDate));
            accessLogEntry.append("]");
        } else {
            accessLogEntry.append("-");
        }

        return true;
    }

    public static long getStartTime(HttpResponseMessage response, HttpRequestMessage request, Object data) {
        HttpRequestMessageImpl requestMessageImpl = null;
        long startTime = 0;
        if (request != null) {
            requestMessageImpl = (HttpRequestMessageImpl) request;
        }

        if (requestMessageImpl != null) {
            long elapsedTime = System.nanoTime() - requestMessageImpl.getStartNanoTime();
            startTime = System.currentTimeMillis() - TimeUnit.NANOSECONDS.toMillis(elapsedTime);
        }
        return startTime;
    }

    public static String getStartTimeAsString(HttpResponseMessage response, HttpRequestMessage request, Object data) {
        StringBuilder requestStartTime = new StringBuilder();
        long startTime = getStartTime(response, request, data);
        if (startTime != 0) {
            Date startDate = new Date(startTime);
            requestStartTime.append("[");
            requestStartTime.append(HttpDispatcher.getDateFormatter().getNCSATime(startDate));
            requestStartTime.append("]");
        } else {
            requestStartTime.append("-");
        }
        return requestStartTime.toString();
    }
}
