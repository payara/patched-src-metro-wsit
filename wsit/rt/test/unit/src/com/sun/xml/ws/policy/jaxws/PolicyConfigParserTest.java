/*
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License).  You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the license at
 * https://glassfish.dev.java.net/public/CDDLv1.0.html.
 * See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at https://glassfish.dev.java.net/public/CDDLv1.0.html.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * you own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Copyright 2006 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.xml.ws.policy.jaxws;

import com.sun.xml.stream.buffer.XMLStreamBuffer;
import com.sun.xml.ws.api.model.wsdl.WSDLModel;
import com.sun.xml.ws.api.server.Container;
import com.sun.xml.ws.policy.Policy;
import com.sun.xml.ws.policy.PolicyException;
import com.sun.xml.ws.policy.PolicyMap;
import com.sun.xml.ws.policy.PolicyMapKey;
import com.sun.xml.ws.policy.testutils.PolicyResourceLoader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.FileChannel;
import javax.xml.namespace.QName;
import junit.framework.TestCase;

/**
 *
 */
public class PolicyConfigParserTest extends TestCase {
    
    public PolicyConfigParserTest(String testName) {
        super(testName);
    }
    
    protected void setUp() throws Exception {
    }
    
    protected void tearDown() throws Exception {
    }
    
    public void testParseContainerNullWithoutConfig() throws Exception {
        Container container = null;
        
        WSDLModel expResult = null;
        WSDLModel result = null;
        
        try {
            PolicyConfigParser.parse(container);
            fail("Expected PolicyException");
        } catch (PolicyException e) {
        }
    }
    
    public void testParseContainerNullWithConfig() throws Exception {
        Container container = null;
        
        WSDLModel expResult = null;
        WSDLModel result = null;
        
        try {
            copyFile("test/unit/data/policy/config/wsit.xml", "test/unit/data/wsit.xml");
            result = PolicyConfigParser.parse(container);
        } finally {
            File wsitxml = new File("test/unit/data/wsit.xml");
            wsitxml.delete();
        }
        WSDLPolicyMapWrapper wrapper = result.getExtension(WSDLPolicyMapWrapper.class);
        PolicyMap map = wrapper.getPolicyMap();
        PolicyMapKey key = map.createWsdlEndpointScopeKey(new QName("http://example.org/", "AddNumbersService"), new QName("http://example.org/", "AddNumbersPort"));
        Policy policy = map.getEndpointEffectivePolicy(key);
        assertNotNull(policy);
        assertEquals("MutualCertificate10Sign_IPingService_policy", policy.getId());
    }
    
    public void testParseContainerWithoutContextWithoutConfig() throws Exception {
        Container container = new MockContainer(null);
        
        WSDLModel expResult = null;
        WSDLModel result = null;
        
        try {
            PolicyConfigParser.parse(container);
            fail("Expected PolicyException");
        } catch (PolicyException e) {
        }
    }
    
    public void testParseContainerWithoutContext() throws Exception {
        Container container = new MockContainer(null);
        
        WSDLModel expResult = null;
        WSDLModel result = null;
        
        try {
            copyFile("test/unit/data/policy/config/wsit.xml", "test/unit/data/wsit.xml");
            result = PolicyConfigParser.parse(container);
        } finally {
            File wsitxml = new File("test/unit/data/wsit.xml");
            wsitxml.delete();
        }
        WSDLPolicyMapWrapper wrapper = result.getExtension(WSDLPolicyMapWrapper.class);
        PolicyMap map = wrapper.getPolicyMap();
        PolicyMapKey key = map.createWsdlEndpointScopeKey(new QName("http://example.org/", "AddNumbersService"), new QName("http://example.org/", "AddNumbersPort"));
        Policy policy = map.getEndpointEffectivePolicy(key);
        assertNotNull(policy);
        assertEquals("MutualCertificate10Sign_IPingService_policy", policy.getId());
    }
    
    public void testParseContainerWithContext() throws Exception {
        // TODO Need MockServletContext
    }
    
    /**
     * Test of parse method, of class com.sun.xml.ws.policy.jaxws.PolicyConfigParser.
     */
    public void testParseBufferNull() throws Exception {
        XMLStreamBuffer buffer = null;
        WSDLModel result = null;
        
        try {
            result = PolicyConfigParser.parse(new URL("http://example.org/wsit"), buffer);
            fail("Expected PolicyException");
        } catch (PolicyException e) {
        }
        assertNull(result);
    }
    
    public void testParseBufferSimple() throws Exception {
        XMLStreamBuffer buffer = null;
        WSDLModel result = null;
        
        buffer = PolicyResourceLoader.getResourceXmlBuffer("config/simple.wsdl");
        result = PolicyConfigParser.parse(new URL("http://example.org/wsit"), buffer);
        WSDLPolicyMapWrapper wrapper = result.getExtension(WSDLPolicyMapWrapper.class);
        PolicyMap map = wrapper.getPolicyMap();
        PolicyMapKey key = map.createWsdlEndpointScopeKey(new QName("http://example.org/", "AddNumbersService"), new QName("http://example.org/", "AddNumbersPort"));
        Policy policy = map.getEndpointEffectivePolicy(key);
        assertNotNull(policy);
        assertEquals("MutualCertificate10Sign_IPingService_policy", policy.getId());
    }
    
    public void testParseBufferSingleImport() throws Exception {
        XMLStreamBuffer buffer = null;
        WSDLModel result = null;
        
        buffer = PolicyResourceLoader.getResourceXmlBuffer("config/single-import.wsdl");
        result = PolicyConfigParser.parse(new URL("file:test/unit/data/policy/"), buffer);
        WSDLPolicyMapWrapper wrapper = result.getExtension(WSDLPolicyMapWrapper.class);
        
        PolicyMap map = wrapper.getPolicyMap();
        assertNotNull(map);
        
        PolicyMapKey key1 = map.createWsdlEndpointScopeKey(new QName("http://example.org/", "AddNumbersService"),
                new QName("http://example.org/", "AddNumbersPort"));
        Policy policy1 = map.getEndpointEffectivePolicy(key1);
        assertNotNull(policy1);
        assertEquals("MutualCertificate10Sign_IPingService_policy", policy1.getId());
        
        PolicyMapKey key2 = map.createWsdlEndpointScopeKey(new QName("http://example.net/", "AddNumbersService"),
                new QName("http://example.net/", "AddNumbersPort"));
        Policy policy2 = map.getEndpointEffectivePolicy(key2);
        assertNotNull(policy2);
        assertEquals("MutualCertificate10Sign_IPingService_policy", policy2.getId());
    }
    
    public void testParseBufferMultiImport() throws Exception {
        XMLStreamBuffer buffer = null;
        WSDLModel result = null;
        
        buffer = PolicyResourceLoader.getResourceXmlBuffer("config/import.wsdl");
        result = PolicyConfigParser.parse(new URL("file:test/unit/data/policy/"), buffer);
        WSDLPolicyMapWrapper wrapper = result.getExtension(WSDLPolicyMapWrapper.class);
        
        PolicyMap map = wrapper.getPolicyMap();
        assertNotNull(map);
        
        PolicyMapKey key1 = map.createWsdlEndpointScopeKey(new QName("http://example.org/", "AddNumbersService"),
                new QName("http://example.org/", "AddNumbersPort"));
        Policy policy1 = map.getEndpointEffectivePolicy(key1);
        assertNotNull(policy1);
        assertEquals("MutualCertificate10Sign_IPingService_policy", policy1.getId());
        
        PolicyMapKey key2 = map.createWsdlEndpointScopeKey(new QName("http://example.net/", "AddNumbersService"),
                new QName("http://example.net/", "AddNumbersPort"));
        Policy policy2 = map.getEndpointEffectivePolicy(key2);
        assertNotNull(policy2);
        assertEquals("MutualCertificate10Sign_IPingService_policy", policy2.getId());
        
        PolicyMapKey key3 = map.createWsdlEndpointScopeKey(new QName("http://example.com/", "AddNumbersService"),
                new QName("http://example.com/", "AddNumbersPort"));
        Policy policy3 = map.getEndpointEffectivePolicy(key3);
        assertNotNull(policy3);
        assertEquals("MutualCertificate10Sign_IPingService_policy", policy3.getId());
        
        PolicyMapKey key4 = map.createWsdlEndpointScopeKey(new QName("http://example.com/import3/", "AddNumbersService"),
                new QName("http://example.com/import3/", "AddNumbersPort"));
        Policy policy4 = map.getEndpointEffectivePolicy(key4);
        assertNotNull(policy4);
        assertEquals("MutualCertificate10Sign_IPingService_policy", policy4.getId());
    }
    
    public void testParseBufferCyclicImport() throws Exception {
        XMLStreamBuffer buffer = null;
        WSDLModel result = null;
        
        buffer = PolicyResourceLoader.getResourceXmlBuffer("config/cyclic.wsdl");
        result = PolicyConfigParser.parse(new URL("file:test/unit/data/policy/config/"), buffer);
        WSDLPolicyMapWrapper wrapper = result.getExtension(WSDLPolicyMapWrapper.class);
        PolicyMap map = wrapper.getPolicyMap();
        PolicyMapKey key = map.createWsdlEndpointScopeKey(new QName("http://example.org/", "AddNumbersService"), new QName("http://example.org/", "AddNumbersPort"));
        Policy policy = map.getEndpointEffectivePolicy(key);
        assertNotNull(policy);
        assertEquals("MutualCertificate10Sign_IPingService_policy", policy.getId());
    }
    
    /**
     * Copy a file
     */
    private static final void copyFile(String sourceName, String destName) throws IOException {
        FileChannel source = null;
        FileChannel dest = null;
        try {
            // Create channel on the source
            source = new FileInputStream(sourceName).getChannel();
            
            // Create channel on the destination
            dest = new FileOutputStream(destName).getChannel();
            
            // Copy file contents from source to destination
            dest.transferFrom(source, 0, source.size());
            
        } finally {
            // Close the channels
            if (source != null) {
                try {
                    source.close();
                } catch (IOException e) {
                }
            }
            if (dest != null) {
                dest.close();
            }
        }
    }
    
    class MockContainer extends Container {
        private final Object spi;
        
        public <T> MockContainer(T spi) {
            this.spi = spi;
        }
        
        public <T> T getSPI(Class<T> spiType) {
            return (T) spi;
        }
        
    }
}
