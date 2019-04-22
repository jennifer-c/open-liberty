/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.opentracing.driver;

import java.io.File;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Iterator;
import java.util.ServiceLoader;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.wsspi.library.Library;
import com.ibm.wsspi.library.LibraryChangeListener;

import io.opentracing.Tracer;
import io.opentracing.contrib.tracerresolver.TracerFactory;
import io.opentracing.contrib.tracerresolver.TracerResolver;

/**
 *
 */
@Component(configurationPid = "com.ibm.ws.opentracing.driver", configurationPolicy = ConfigurationPolicy.OPTIONAL, immediate = true, property = { "service.vendor=IBM" })
public class TracerDriverService implements LibraryChangeListener {

    private static TracerDriverService instance = null;
    private Library sharedLib;

    public static TracerDriverService getInstance() {
        return instance;
    }

    @Activate
    protected void activate(ComponentContext context) {
        instance = this;
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        instance = null;
    }

    @Reference(service = Library.class, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
    protected void setSharedLibrary(Library library) {
        sharedLib = library;
    }

    protected void unsetSharedLibrary(Library library) {
        sharedLib = null;
    }

    /** {@inheritDoc} */
    @Override
    public void libraryNotification() {
        // TODO Auto-generated method stub

    }

    private static ClassLoader getClassLoaderWithPriv(final Library lib) {
        if (System.getSecurityManager() == null)
            return lib.getClassLoader();
        else
            return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
                @Override
                public ClassLoader run() {
                    return lib.getClassLoader();
                }
            });
    }

    public Tracer resolveTracer(String appName) {
        System.out.println("FW share lib: " + sharedLib.toString());
        for (File file : sharedLib.getFiles()) {
            System.out.println("FW share lib file: " + file.getAbsolutePath());
        }

        ClassLoader classloader = getClassLoaderWithPriv(sharedLib);
        ServiceLoader<TracerFactory> sl = ServiceLoader.load(TracerFactory.class, classloader);
        Iterator<TracerFactory> it = sl.iterator();
        while (it.hasNext()) {
            TracerFactory tf = it.next();
            System.out.println("FW TracerFactory: " + tf.getClass().getName());
        }
        System.setProperty("JAEGER_SERVICE_NAME", appName);
        return TracerResolver.resolveTracer(classloader);
    }
}
