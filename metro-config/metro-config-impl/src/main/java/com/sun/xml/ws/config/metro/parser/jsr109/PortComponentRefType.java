/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
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

//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vhudson-jaxb-ri-2.2-28 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2010.08.18 at 11:59:48 AM EEST 
//


package com.sun.xml.ws.config.metro.parser.jsr109;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import com.sun.xml.bind.Locatable;
import com.sun.xml.bind.annotation.XmlLocation;
import org.xml.sax.Locator;


/**
 * 
 * 
 *         The port-component-ref element declares a client dependency
 *         on the container for resolving a Service Endpoint Interface
 *         to a WSDL port. It optionally associates the Service Endpoint
 *         Interface with a particular port-component. This is only used
 *         by the container for a Service.getPort(Class) method call.
 *         
 *       
 * 
 * <p>Java class for port-component-refType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="port-component-refType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="service-endpoint-interface" type="{http://java.sun.com/xml/ns/javaee}fully-qualified-classType"/>
 *         &lt;element name="enable-mtom" type="{http://java.sun.com/xml/ns/javaee}true-falseType" minOccurs="0"/>
 *         &lt;element name="mtom-threshold" type="{http://java.sun.com/xml/ns/javaee}xsdNonNegativeIntegerType" minOccurs="0"/>
 *         &lt;element name="addressing" type="{http://java.sun.com/xml/ns/javaee}addressingType" minOccurs="0"/>
 *         &lt;element name="respect-binding" type="{http://java.sun.com/xml/ns/javaee}respect-bindingType" minOccurs="0"/>
 *         &lt;element name="port-component-link" type="{http://java.sun.com/xml/ns/javaee}string" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="id" type="{http://www.w3.org/2001/XMLSchema}ID" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "port-component-refType", propOrder = {
    "serviceEndpointInterface",
    "enableMtom",
    "mtomThreshold",
    "addressing",
    "respectBinding",
    "portComponentLink"
})
public class PortComponentRefType
    implements Locatable
{

    @XmlElement(name = "service-endpoint-interface", required = true)
    protected FullyQualifiedClassType serviceEndpointInterface;
    @XmlElement(name = "enable-mtom")
    protected TrueFalseType enableMtom;
    @XmlElement(name = "mtom-threshold")
    protected XsdNonNegativeIntegerType mtomThreshold;
    protected AddressingType addressing;
    @XmlElement(name = "respect-binding")
    protected RespectBindingType respectBinding;
    @XmlElement(name = "port-component-link")
    protected com.sun.xml.ws.config.metro.parser.jsr109.String portComponentLink;
    @XmlAttribute(name = "id")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlID
    @XmlSchemaType(name = "ID")
    protected java.lang.String id;
    @XmlLocation
    @XmlTransient
    protected Locator locator;

    /**
     * Gets the value of the serviceEndpointInterface property.
     * 
     * @return
     *     possible object is
     *     {@link FullyQualifiedClassType }
     *     
     */
    public FullyQualifiedClassType getServiceEndpointInterface() {
        return serviceEndpointInterface;
    }

    /**
     * Sets the value of the serviceEndpointInterface property.
     * 
     * @param value
     *     allowed object is
     *     {@link FullyQualifiedClassType }
     *     
     */
    public void setServiceEndpointInterface(FullyQualifiedClassType value) {
        this.serviceEndpointInterface = value;
    }

    /**
     * Gets the value of the enableMtom property.
     * 
     * @return
     *     possible object is
     *     {@link TrueFalseType }
     *     
     */
    public TrueFalseType getEnableMtom() {
        return enableMtom;
    }

    /**
     * Sets the value of the enableMtom property.
     * 
     * @param value
     *     allowed object is
     *     {@link TrueFalseType }
     *     
     */
    public void setEnableMtom(TrueFalseType value) {
        this.enableMtom = value;
    }

    /**
     * Gets the value of the mtomThreshold property.
     * 
     * @return
     *     possible object is
     *     {@link XsdNonNegativeIntegerType }
     *     
     */
    public XsdNonNegativeIntegerType getMtomThreshold() {
        return mtomThreshold;
    }

    /**
     * Sets the value of the mtomThreshold property.
     * 
     * @param value
     *     allowed object is
     *     {@link XsdNonNegativeIntegerType }
     *     
     */
    public void setMtomThreshold(XsdNonNegativeIntegerType value) {
        this.mtomThreshold = value;
    }

    /**
     * Gets the value of the addressing property.
     * 
     * @return
     *     possible object is
     *     {@link AddressingType }
     *     
     */
    public AddressingType getAddressing() {
        return addressing;
    }

    /**
     * Sets the value of the addressing property.
     * 
     * @param value
     *     allowed object is
     *     {@link AddressingType }
     *     
     */
    public void setAddressing(AddressingType value) {
        this.addressing = value;
    }

    /**
     * Gets the value of the respectBinding property.
     * 
     * @return
     *     possible object is
     *     {@link RespectBindingType }
     *     
     */
    public RespectBindingType getRespectBinding() {
        return respectBinding;
    }

    /**
     * Sets the value of the respectBinding property.
     * 
     * @param value
     *     allowed object is
     *     {@link RespectBindingType }
     *     
     */
    public void setRespectBinding(RespectBindingType value) {
        this.respectBinding = value;
    }

    /**
     * Gets the value of the portComponentLink property.
     * 
     * @return
     *     possible object is
     *     {@link com.sun.xml.ws.config.metro.parser.jsr109.String }
     *     
     */
    public com.sun.xml.ws.config.metro.parser.jsr109.String getPortComponentLink() {
        return portComponentLink;
    }

    /**
     * Sets the value of the portComponentLink property.
     * 
     * @param value
     *     allowed object is
     *     {@link com.sun.xml.ws.config.metro.parser.jsr109.String }
     *     
     */
    public void setPortComponentLink(com.sun.xml.ws.config.metro.parser.jsr109.String value) {
        this.portComponentLink = value;
    }

    /**
     * Gets the value of the id property.
     * 
     * @return
     *     possible object is
     *     {@link java.lang.String }
     *     
     */
    public java.lang.String getId() {
        return id;
    }

    /**
     * Sets the value of the id property.
     * 
     * @param value
     *     allowed object is
     *     {@link java.lang.String }
     *     
     */
    public void setId(java.lang.String value) {
        this.id = value;
    }

    public Locator sourceLocation() {
        return locator;
    }

    public void setSourceLocation(Locator newLocator) {
        locator = newLocator;
    }

}