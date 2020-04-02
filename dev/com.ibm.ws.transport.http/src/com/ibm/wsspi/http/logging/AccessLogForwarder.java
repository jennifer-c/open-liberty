/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.http.logging;

import com.ibm.ws.http.logging.internal.AccessLogger.FormatSegment;

/**
 * The Access log forwarder is invoked after each http request.
 */
public interface AccessLogForwarder {
    /**
     *
     * @param logData
     * @param parsedFormat A parsed version of logFormat
     * @param formatString The original logFormat string
     */
    public void process(AccessLogRecordData logData, FormatSegment[] parsedFormat, String formatString);
}
