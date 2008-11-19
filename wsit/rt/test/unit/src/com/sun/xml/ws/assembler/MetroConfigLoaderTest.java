/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 1997-2008 Sun Microsystems, Inc. All rights reserved.
 * 
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License. You can obtain
 * a copy of the License at https://glassfish.dev.java.net/public/CDDL+GPL.html
 * or glassfish/bootstrap/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 * 
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code.  If applicable, add the following below the License
 * Header, with the fields enclosed by brackets [] replaced by your own
 * identifying information: "Portions Copyrighted [year]
 * [name of copyright owner]"
 * 
 * Contributor(s):
 * 
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package com.sun.xml.ws.assembler;

import com.sun.xml.ws.api.ResourceLoader;
import com.sun.xml.ws.runtime.config.TubeFactoryConfig;
import com.sun.xml.ws.runtime.config.TubeFactoryList;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import junit.framework.TestCase;

/**
 *
 * @author Marek Potociar <marek.potociar at sun.com>
 */
public class MetroConfigLoaderTest extends TestCase {

    private final String UNIT_TEST_RESOURCE_ROOT = "metro-config/";

    public MetroConfigLoaderTest(String testName) {
        super(testName);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test of getTubeline method, of class MetroConfigLoader.
     */
//    public void testGetTubeline() throws URISyntaxException {
//        MetroConfigLoader configLoader = new MetroConfigLoader(new ResourceLoader() {
//
//            @Override
//            public URL getResource(String resource) throws MalformedURLException {
//                return Thread.currentThread().getContextClassLoader().getResource(UNIT_TEST_RESOURCE_ROOT + resource);
//            }
//        });
//
//        TubelineDefinition result;
//
//        result = configLoader.getTubeline(new URI("#default-tubeline"));
//        assertNotNull(result);
//
//        result = configLoader.getTubeline(new URI("#ss-transport-message-dump-tubeline"));
//        assertNotNull(result);
//
//        result = configLoader.getTubeline(new URI("#cs-application-message-dump-tubeline"));
//        assertNotNull(result);
//
//        result = configLoader.getTubeline(new URI("#non-existent-tubeline"));
//        assertNull(result);
//    }

    /**
     * Test of getTubelineForEndpoint method, of class MetroConfigLoader.
     */
    public void testGetEndpointSideTubeFactoriesTest() throws URISyntaxException {
        MetroConfigLoader configLoader = new MetroConfigLoader(new ResourceLoader() {

            @Override
            public URL getResource(String resource) throws MalformedURLException {
                return Thread.currentThread().getContextClassLoader().getResource(UNIT_TEST_RESOURCE_ROOT + resource);
            }
        });

        TubeFactoryList result;
        result = configLoader.getEndpointSideTubeFactories(new URI("http://org.sample#wsdl11.port(PingService/HttpPingPort)"));
        assertTrue(containsTubeFactoryConfig(result, "server"));

        result = configLoader.getEndpointSideTubeFactories(new URI("http://org.sample#wsdl11.port(PingService/JmsPingPort)"));
        assertTrue(containsTubeFactoryConfig(result, "default-server"));

        result = configLoader.getEndpointSideTubeFactories(new URI("http://org.sample#wsdl11.port(PingService/OtherPingPort)"));
        assertTrue(containsTubeFactoryConfig(result, "default-server"));
    }

    /**
     * Test of getTubelineForEndpoint method, of class MetroConfigLoader - loading from default Metro config
     */
    public void testGetEndpointSideTubeFactoriesLoadFromDefaultConfig() throws URISyntaxException {
        MetroConfigLoader configLoader = new MetroConfigLoader(new ResourceLoader() {

            @Override
            public URL getResource(String resource) throws MalformedURLException {
                if ("metro.xml".equals(resource)) {
                    return Thread.currentThread().getContextClassLoader().getResource(UNIT_TEST_RESOURCE_ROOT + "metro-no-default.xml");
                } else {
                    return null;
                }
            }
        });

        TubeFactoryList result;
        result = configLoader.getEndpointSideTubeFactories(new URI("http://org.sample#wsdl11.port(PingService/HttpPingPort)"));
        assertTrue(containsTubeFactoryConfig(result, "server"));

        result = configLoader.getEndpointSideTubeFactories(new URI("http://org.sample#wsdl11.port(PingService/JmsPingPort)"));
        assertFalse(result.getTubeFactoryConfigs().isEmpty());
        assertFalse(containsTubeFactoryConfig(result, "server"));

        result = configLoader.getEndpointSideTubeFactories(new URI("http://org.sample#wsdl11.port(PingService/OtherPingPort)"));
        assertFalse(result.getTubeFactoryConfigs().isEmpty());
        assertFalse(containsTubeFactoryConfig(result, "server"));
    }

    /**
     * Test of getTubelineForEndpoint method, of class MetroConfigLoader.
     */
    public void testGetClientSideTubeFactoriesTest() throws URISyntaxException {
        MetroConfigLoader configLoader = new MetroConfigLoader(new ResourceLoader() {

            @Override
            public URL getResource(String resource) throws MalformedURLException {
                return Thread.currentThread().getContextClassLoader().getResource(UNIT_TEST_RESOURCE_ROOT + resource);
            }
        });

        TubeFactoryList result;
        result = configLoader.getClientSideTubeFactories(new URI("http://org.sample#wsdl11.port(PingService/HttpPingPort)"));
        assertTrue(containsTubeFactoryConfig(result, "default-client"));

        result = configLoader.getClientSideTubeFactories(new URI("http://org.sample#wsdl11.port(PingService/JmsPingPort)"));
        assertTrue(containsTubeFactoryConfig(result, "client"));

        result = configLoader.getClientSideTubeFactories(new URI("http://org.sample#wsdl11.port(PingService/OtherPingPort)"));
        assertTrue(containsTubeFactoryConfig(result, "default-client"));
    }

    /**
     * Test of getTubelineForEndpoint method, of class MetroConfigLoader - loading from default Metro config
     */
    public void testGetClientSideTubeFactoriesLoadFromDefaultConfig() throws URISyntaxException {
        MetroConfigLoader configLoader = new MetroConfigLoader(new ResourceLoader() {

            @Override
            public URL getResource(String resource) throws MalformedURLException {
                if ("metro.xml".equals(resource)) {
                    return Thread.currentThread().getContextClassLoader().getResource(UNIT_TEST_RESOURCE_ROOT + "metro-no-default.xml");
                } else {
                    return null;
                }
            }
        });

        TubeFactoryList result;
        result = configLoader.getClientSideTubeFactories(new URI("http://org.sample#wsdl11.port(PingService/HttpPingPort)"));
        assertFalse(result.getTubeFactoryConfigs().isEmpty());
        assertFalse(containsTubeFactoryConfig(result, "client"));

        result = configLoader.getClientSideTubeFactories(new URI("http://org.sample#wsdl11.port(PingService/JmsPingPort)"));
        assertTrue(containsTubeFactoryConfig(result, "client"));

        result = configLoader.getClientSideTubeFactories(new URI("http://org.sample#wsdl11.port(PingService/OtherPingPort)"));
        assertFalse(result.getTubeFactoryConfigs().isEmpty());
        assertFalse(containsTubeFactoryConfig(result, "client"));
    }

    private boolean containsTubeFactoryConfig(TubeFactoryList tubeList, String tubeFactoryName) {
        for (TubeFactoryConfig config : tubeList.getTubeFactoryConfigs()) {
            if (config.getClassName().equals(tubeFactoryName)) {
                return true;
            }
        }

        return false;
    }
}