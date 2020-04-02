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

import java.util.ArrayList;

import com.ibm.ws.logging.data.JSONObject.JSONObjectBuilder;

/**
 *
 */
public class AccessLogDataFormatter {
    enum Format {
        FORMATTER_DEFAULT_JSON,
        FORMATTER_CUSTOM_JSON
        // FORMATTER_LOGSTASH_COLLECTOR_1.0_DEFAULT
        // FORMATTER_LOGSTASH_COLLECTOR_1.0_CUSTOM
    }

    public AccessLogDataFormatter() {
        // do something?
    }

    // list of actions to populate JSONObjectBuilder
    private final ArrayList<JsonFieldAdder> jsonFieldAdders = new ArrayList<JsonFieldAdder>();

    // adds another JsonFieldAdder to the list
    public AccessLogDataFormatter add(JsonFieldAdder jsonFieldAdder) {
        this.jsonFieldAdders.add(jsonFieldAdder);
        return this;
    }

    // adds fields to JSONObjectBuilder by running all jsonFieldAdders
    public JSONObjectBuilder populate(JSONObjectBuilder j, AccessLogData a) {
        for (JsonFieldAdder jfa : jsonFieldAdders) {
            jfa.populate(j, a);
        }
        return j;
    }
    // each jsonFieldAdder formats a single field from AccessLogData as it adds it to the JSONObjectBuilder

}
