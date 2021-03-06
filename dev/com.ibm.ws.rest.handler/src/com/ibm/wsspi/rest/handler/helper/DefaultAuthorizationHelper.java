/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.rest.handler.helper;

import java.io.IOException;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.ws.management.security.ManagementSecurityConstants;
import com.ibm.wsspi.rest.handler.RESTRequest;
import com.ibm.wsspi.rest.handler.RESTResponse;

/**
 * This helper service performs the default authorization on the given user.
 *
 * @ibm-spi
 */
@Component(service = { DefaultAuthorizationHelper.class },
           configurationPolicy = ConfigurationPolicy.IGNORE,
           immediate = true,
           property = { "service.vendor=IBM" })
public class DefaultAuthorizationHelper {

    public boolean checkAdministratorRole(RESTRequest request, RESTResponse response) throws IOException {
        boolean isGetMethod = "GET".equals(request.getMethod());

        /*
         * Authorize principals with the administrator role or principals with the reader role if the HTTP
         * request method is GET.
         */
        boolean isAuthorized = request.isUserInRole(ManagementSecurityConstants.ADMINISTRATOR_ROLE_NAME)
                               || (isGetMethod && request.isUserInRole(ManagementSecurityConstants.READER_ROLE_NAME));

        if (!isAuthorized) {
            // Not in admin role, so built error msg
            // TODO: Translate msg
            response.sendError(403, "Forbidden");
        }

        return isAuthorized;
    }
}
