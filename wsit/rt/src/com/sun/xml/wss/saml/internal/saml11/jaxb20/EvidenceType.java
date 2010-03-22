//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vhudson-jaxb-ri-3509 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2006.09.12 at 08:53:21 PM IST 
//


package com.sun.xml.wss.saml.internal.saml11.jaxb20;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementRefs;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for EvidenceType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="EvidenceType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;choice maxOccurs="unbounded">
 *         &lt;element ref="{urn:oasis:names:tc:SAML:1.0:assertion}AssertionIDReference"/>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:1.0:assertion}Assertion"/>
 *       &lt;/choice>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlRootElement(name="Evidence")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "EvidenceType", propOrder = {
    "assertionIDReferenceOrAssertion"
})
public class EvidenceType {

    @XmlElementRefs({
        @XmlElementRef(name = "AssertionIDReference", namespace = "urn:oasis:names:tc:SAML:1.0:assertion", type = JAXBElement.class),
        @XmlElementRef(name = "Assertion", namespace = "urn:oasis:names:tc:SAML:1.0:assertion", type = JAXBElement.class)
    })
    protected List<JAXBElement<?>> assertionIDReferenceOrAssertion;

    /**
     * Gets the value of the assertionIDReferenceOrAssertion property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the assertionIDReferenceOrAssertion property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getAssertionIDReferenceOrAssertion().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link JAXBElement }{@code <}{@link AssertionType }{@code >}
     * {@link JAXBElement }{@code <}{@link String }{@code >}
     * 
     * 
     */
    public List<JAXBElement<?>> getAssertionIDReferenceOrAssertion() {
        if (assertionIDReferenceOrAssertion == null) {
            assertionIDReferenceOrAssertion = new ArrayList<JAXBElement<?>>();
        }
        return this.assertionIDReferenceOrAssertion;
    }

}