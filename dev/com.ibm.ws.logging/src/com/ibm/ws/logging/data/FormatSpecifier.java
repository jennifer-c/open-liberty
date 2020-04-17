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
public enum FormatSpecifier {
    JSON(0, "JSON"),
    LOGSTASH(1, "LOGSTASH");

    private int value;
    private String name;

    private FormatSpecifier(int value, String name) {
        this.value = value;
        this.name = name;
    }

    public boolean equals(String s) {
        return name.equals(s);
    }

    public int getValue() {
        return value;
    }
}