/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http.logging.internal;

import com.ibm.ws.http.logging.internal.AccessLogger.FormatSegment;
import com.ibm.wsspi.http.channel.HttpRequestMessage;
import com.ibm.wsspi.http.channel.HttpResponseMessage;
import com.ibm.wsspi.http.logging.AccessLogRecordData;

/**
 *
 */
public class AccessLogRecordDataExt implements AccessLogRecordData {

    AccessLogRecordData delegateRecordData;
    FormatSegment[] parsedFormat;
    String formatString;

    AccessLogRecordDataExt(AccessLogRecordData alrd, FormatSegment[] parsedFormat, String formatString) {
        this.delegateRecordData = alrd;
        this.parsedFormat = parsedFormat;
        this.formatString = formatString;
    }

    public FormatSegment[] getParsedFormat() {
        return parsedFormat;
    }

    public String getFormatString() {
        return formatString;
    }

    @Override
    public HttpRequestMessage getRequest() {
        return delegateRecordData.getRequest();
    }

    @Override
    public HttpResponseMessage getResponse() {
        return delegateRecordData.getResponse();
    }

    @Override
    public long getTimestamp() {
        return delegateRecordData.getTimestamp();
    }

    @Override
    public String getVersion() {
        return delegateRecordData.getVersion();
    }

    @Override
    public String getUserId() {
        return delegateRecordData.getUserId();
    }

    @Override
    public String getRemoteAddress() {
        return delegateRecordData.getRemoteAddress();
    }

    @Override
    public long getBytesWritten() {
        return delegateRecordData.getBytesWritten();
    }

    @Override
    public long getStartTime() {
        return delegateRecordData.getStartTime();
    }

    @Override
    public long getElapsedTime() {
        return delegateRecordData.getElapsedTime();
    }

    @Override
    public String getLocalIP() {
        return delegateRecordData.getLocalIP();
    }

    @Override
    public String getLocalPort() {
        return delegateRecordData.getLocalPort();
    }
}
