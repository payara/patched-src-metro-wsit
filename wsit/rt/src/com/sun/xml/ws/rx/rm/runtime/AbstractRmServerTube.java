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
package com.sun.xml.ws.rx.rm.runtime;

import com.sun.xml.ws.rx.RxConfiguration;
import com.sun.xml.ws.rx.rm.faults.CreateSequenceRefusedFault;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.pipe.Fiber;
import com.sun.xml.ws.api.pipe.NextAction;
import com.sun.xml.ws.api.pipe.Tube;
import com.sun.xml.ws.api.pipe.TubeCloner;
import com.sun.xml.ws.api.pipe.helper.AbstractFilterTubeImpl;
import com.sun.xml.ws.assembler.ServerTubelineAssemblyContext;
import com.sun.xml.ws.commons.Logger;
import com.sun.xml.ws.rx.RxRuntimeException;
import com.sun.xml.ws.rx.rm.ReliableMessagingFeature;
import com.sun.xml.ws.rx.rm.faults.AbstractRmSoapFault;
import com.sun.xml.ws.rx.rm.faults.SequenceTerminatedFault;
import com.sun.xml.ws.rx.rm.faults.UnknownSequenceFault;
import com.sun.xml.ws.rx.rm.localization.LocalizationMessages;
import com.sun.xml.ws.rx.rm.runtime.sequence.Sequence;
import com.sun.xml.ws.rx.rm.runtime.sequence.SequenceManager;
import com.sun.xml.ws.rx.rm.runtime.sequence.SequenceManagerFactory;
import com.sun.xml.ws.rx.rm.runtime.sequence.UnknownSequenceException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

/**
 *
 * @author Marek Potociar (marek.potociar at sun.com)
 */
abstract class AbstractRmServerTube extends AbstractFilterTubeImpl {

    private static final Logger LOGGER = Logger.getLogger(AbstractRmServerTube.class);
    private static final Lock FLOW_CONTROL_ACCESS_LOCK = new ReentrantLock();
    /**
     * The property wih this key may be set by JCaps in the message context to indicate 
     * whether the message that was delivered to the application endpoint should be 
     * acknowledged or not.
     * 
     * The property value may be "true" or "false", "true" s default.
     * 
     * Introduction of this property is required as a temporary workaround for missing
     * concept of distinguishing between system and application errors in JAXWS RI.
     * The workaround should be removed once the missing concept is introduced.
     */
    private static final String RM_ACK_PROPERTY_KEY = "RM_ACK";
    //
    final RxConfiguration configuration;
    final SequenceManager sequenceManager;
    //
    private PacketAdapter requestAdapter;

    static AbstractRmServerTube getInstance(RxConfiguration configuration, Tube tubelineHead, ServerTubelineAssemblyContext context) {
        switch (configuration.getRmVersion()) {
            case WSRM200502:
                return new Rm10ServerTube(configuration, tubelineHead, context);
            case WSRM200702:
                return new Rm11ServerTube(configuration, tubelineHead, context);
            default:
                throw new IllegalStateException(LocalizationMessages.WSRM_1104_RM_VERSION_NOT_SUPPORTED(configuration.getRmVersion().namespaceUri));
        }
    }

    AbstractRmServerTube(AbstractRmServerTube original, TubeCloner cloner) {
        super(original, cloner);

        this.configuration = original.configuration;
        this.sequenceManager = original.sequenceManager;

        this.requestAdapter = null;
    }

    AbstractRmServerTube(RxConfiguration configuration, Tube tubelineHead, ServerTubelineAssemblyContext context) {
        super(tubelineHead);

        this.configuration = configuration;

        // TODO don't take the first config alternative automatically...

        if (this.configuration.getAddressingVersion() == null) {
            throw new RxRuntimeException(LocalizationMessages.WSRM_1140_NO_ADDRESSING_VERSION_ON_ENDPOINT());
        }
//        if (this.configuration.getAddressingVersion() != AddressingVersion.W3C) {
//            throw new RxRuntimeException(LocalizationMessages.WSRM_1120_UNSUPPORTED_WSA_VERSION(this.configuration.getAddressingVersion()));
//        }
        
        this.sequenceManager = SequenceManagerFactory.INSTANCE.getServerSequenceManager(context.getEndpoint(), this.configuration.getManagedObjectManager());
        this.requestAdapter = null;
    }

    @Override
    public NextAction processRequest(Packet requestPacket) {
        LOGGER.entering();

        requestAdapter = PacketAdapter.getInstance(configuration, requestPacket);
        try {
            if (requestAdapter.isProtocolMessage()) {
                if (requestAdapter.isProtocolRequest()) {
                    PacketAdapter protocolResponseAdapter = processProtocolRequest(requestAdapter);
                    return doReturnWith(protocolResponseAdapter.getPacket());
                } else {
                    return doThrow(new RxRuntimeException(LocalizationMessages.WSRM_1128_INVALID_WSA_ACTION_IN_PROTOCOL_REQUEST(requestAdapter.getWsaAction())));
                }
            } else {
                Sequence inboundSequence = getSequenceOrSoapFault(requestAdapter.getPacket(), requestAdapter.getSequenceId());

                if (!requestAdapter.isSecurityContextTokenIdValid(inboundSequence.getBoundSecurityTokenReferenceId())) {
                    // TODO L10N + maybe throw SOAP fault exception?
                    throw new RxRuntimeException("Security context token on the message does not match the token bound to the sequence");
                }

                processNonSequenceRmHeaders(requestAdapter);

                if (duplicatesNotAllowed() && inboundSequence.isAcknowledged(requestAdapter.getMessageNumber())) {
                    return doReturnWith(createResponseForDuplicate(inboundSequence, requestAdapter).getPacket());
                }

                if (!requestAdapter.hasSession()) { // security did not set session - we must do it
                    requestAdapter.setSession(inboundSequence.getId());
                }
                requestAdapter.exposeSequenceDataToUser();

                if (configuration.isOrderedDeliveryEnabled() && !isMessageInOrder(requestAdapter)) {
                    boolean finerLoggable = LOGGER.isLoggable(Level.FINER);

                    if (FlowControledFibers.INSTANCE.getUsedBufferSize(inboundSequence.getId()) > configuration.getDestinationBufferQuota()) {
                        PacketAdapter responseAdapter = requestAdapter.createAckResponse(inboundSequence, configuration.getRmVersion().sequenceAcknowledgementAction);

                        if (finerLoggable) {
                            LOGGER.finer(String.format("Message buffer is full. Returning ACK response for a mesage with id [ %d ] on the sequence [ %s ]", requestAdapter.getMessageNumber(), requestAdapter.getSequenceId()));
                        }
                        return doReturnWith(responseAdapter.getPacket());
                    }

                    try {
                        FLOW_CONTROL_ACCESS_LOCK.lock();

                        if (inboundSequence.isAcknowledged(requestAdapter.getMessageNumber()) || !FlowControledFibers.INSTANCE.registerForResume(Fiber.current(), requestAdapter)) {
                            if (finerLoggable) {
                                LOGGER.finer(String.format("Message with id [ %d ] on the sequence [ %s ] cannot be registered for resume - possibly a resend of already suspended or processed message. Processing as duplicate.", requestAdapter.getMessageNumber(), requestAdapter.getSequenceId()));
                            }
                            return doReturnWith(createResponseForDuplicate(inboundSequence, requestAdapter).getPacket());
                        } else {
                            if (finerLoggable) {
                                LOGGER.finer(String.format("Message with id [ %d ] on the sequence [ %s ] registered for resume. Suspending the processing now.", requestAdapter.getMessageNumber(), requestAdapter.getSequenceId()));
                            }
                            return doSuspend(super.next);
                        }
                    } finally {
                        FLOW_CONTROL_ACCESS_LOCK.unlock();
                    }
                }

                return super.processRequest(requestAdapter.keepTransportBackChannelOpen().getPacket());
            }
        } catch (AbstractRmSoapFault ex) {
            return doReturnWith(ex.getSoapFaultResponse());
        } catch (RxRuntimeException ex) {
            LOGGER.logSevereException(ex);
            return doThrow(ex);
        } finally {
            LOGGER.exiting();
        }
    }

    @Override
    public NextAction processResponse(Packet responsePacket) {
        LOGGER.entering();
        try {
            Sequence inboundSequence = sequenceManager.getSequence(requestAdapter.getSequenceId());
            PacketAdapter responseAdapter = PacketAdapter.getInstance(configuration, responsePacket);

            /**
             * This if clause is a part of the RM-JCaps private contract. JCaps may decide
             * that the request it received should be resent and thus it should not be acknowledged.
             * 
             * For more information, see documentation of RM_ACK_PROPERTY_KEY constant field.
             */
            String rmAckPropertyValue = (String) requestAdapter.getPacket().invocationProperties.get(RM_ACK_PROPERTY_KEY);
            if (rmAckPropertyValue == null || Boolean.parseBoolean(rmAckPropertyValue)) {
                inboundSequence.acknowledgeMessageId(requestAdapter.getMessageNumber());

                if (responseAdapter.containsMessage()) {
                    // response in req-resp MEP
                    Sequence outboundSequence = sequenceManager.getBoundSequence(inboundSequence.getId());
                    if (outboundSequence != null) {
                        responseAdapter.appendSequenceHeader(
                                outboundSequence.getId(),
                                outboundSequence.generateNextMessageId());

                        // we allways request acknowledgement (at least for this response)
                        responseAdapter.appendAckRequestedHeader(outboundSequence.getId());

                        if (duplicatesNotAllowed()) {
                            outboundSequence.storeMessage(
                                    requestAdapter.getMessageNumber(),
                                    responseAdapter.getMessageNumber(),
                                    responseAdapter.getPacket());
                        }
                    } else {
                        // we don't have a sequence for outgoing messages
                        throw new IllegalStateException(LocalizationMessages.WSRM_1139_NO_OUTBOUND_SEQUENCE_FOR_RESPONSE(inboundSequence.getId()));
                    }
                    // we apply acknowledgement only after the message was possibly stored, because otherwise we would
                    // send a stale acknowledgement data in case of resend
                    responseAdapter.appendSequenceAcknowledgementHeader(sequenceManager.getSequence(inboundSequence.getId()));
                } else {
                    // response in one-way MEP - just send a sequence acknowledgement                
                    responseAdapter.setEmptyResponseMessage(requestAdapter, configuration.getRmVersion().sequenceAcknowledgementAction);
                    responseAdapter.appendSequenceAcknowledgementHeader(sequenceManager.getSequence(inboundSequence.getId()));
                }

                if (configuration.isOrderedDeliveryEnabled()) {
                    try {
                        if (LOGGER.isLoggable(Level.FINER)) {
                            LOGGER.finer(String.format("Request [ %d ] processed. Trying to resume next request", requestAdapter.getMessageNumber()));
                        }

                        FLOW_CONTROL_ACCESS_LOCK.lock();

                        FlowControledFibers.INSTANCE.tryResume(inboundSequence.getId(), inboundSequence.getLastMessageId() + 1);
                    } finally {
                        FLOW_CONTROL_ACCESS_LOCK.unlock();
                    }
                }
            } else if (LOGGER.isLoggable(Level.FINER)) {
                LOGGER.finer(String.format("Value of the '%s' property is '%s'. The request has not been acknowledged.", RM_ACK_PROPERTY_KEY, rmAckPropertyValue));
            }

            return super.processResponse(responseAdapter.getPacket());
        } finally {
            this.requestAdapter = null;
            LOGGER.exiting();
        }
    }

    @Override
    public NextAction processException(Throwable t) {
        this.requestAdapter = null;
        return super.processException(t);
    }

    @Override
    public void preDestroy() {
        LOGGER.entering();
        try {
            super.preDestroy();
        } finally {
            LOGGER.exiting();
        }
    }

    private PacketAdapter createResponseForDuplicate(Sequence inboundSequence, PacketAdapter requestAdapter) throws RxRuntimeException, AssertionError, UnknownSequenceException, UnsupportedOperationException {
        Sequence outboundSequence = sequenceManager.getBoundSequence(inboundSequence.getId());
        Object storedMessage = (outboundSequence != null) ? outboundSequence.retrieveMessage(requestAdapter.getMessageNumber()) : null;

        PacketAdapter responseAdapter;
        if (storedMessage instanceof Packet) {
            responseAdapter = PacketAdapter.getInstance(configuration, (Packet) storedMessage);
            responseAdapter.appendSequenceAcknowledgementHeader(sequenceManager.getSequence(inboundSequence.getId()));
        } else if (storedMessage == null) {
            responseAdapter = requestAdapter.createAckResponse(inboundSequence, configuration.getRmVersion().sequenceAcknowledgementAction);
        } else {
            throw new AssertionError("Unexpected message packet type: " + storedMessage.getClass().getName());
        }

        return responseAdapter;
    }

    private boolean isMessageInOrder(PacketAdapter requestAdapter) {
        Sequence inboundSequence = sequenceManager.getSequence(requestAdapter.getSequenceId());
        return inboundSequence.getLastMessageId() + 1 == requestAdapter.getMessageNumber();
    }

    /**
     * Handles all protocol message request processing
     * 
     * @param  requestAdapter request packet adapter
     * 
     * @return protocol message response packet adapter
     * 
     * @exception AbstractRmSoapFault exception representing a protocol request 
     *            message processing SOAP fault
     */
    private PacketAdapter processProtocolRequest(PacketAdapter requestAdapter) throws AbstractRmSoapFault {
        if (configuration.getRmVersion().createSequenceAction.equals(requestAdapter.getWsaAction())) {
            return handleCreateSequenceAction(requestAdapter);
        } else if (configuration.getRmVersion().terminateSequenceAction.equals(requestAdapter.getWsaAction())) {
            return handleTerminateSequenceAction(requestAdapter);
        } else if (configuration.getRmVersion().ackRequestedAction.equals(requestAdapter.getWsaAction())) {
            return handleAckRequestedAction(requestAdapter);
        } else if (configuration.getRmVersion().sequenceAcknowledgementAction.equals(requestAdapter.getWsaAction())) {
            return handleSequenceAcknowledgementAction(requestAdapter);
        } else {
            return processVersionSpecificProtocolRequest(requestAdapter);
        }
    }

    /**
     * Handles all RM version-specific protocol message request processing
     * 
     * @param  requestAdapter request packet adapter
     * 
     * @return protocol message response packet adapter
     * 
     * @exception AbstractRmSoapFault exception representing a protocol request 
     *            message processing SOAP fault
     */
    PacketAdapter processVersionSpecificProtocolRequest(PacketAdapter requestAdapter) throws AbstractRmSoapFault {
        throw new UnsupportedOperationException(LocalizationMessages.WSRM_1134_UNSUPPORTED_PROTOCOL_MESSAGE(requestAdapter.getWsaAction()));
    }

    /**
     * Handles create sequence request processing
     * 
     * @param  requestAdapter create sequence request packet adapter
     * 
     * @return create sequence response message wrapped in a response packet adapter
     * 
     * @exception CreateSequenceRefusedFault in case of any problems while creating the sequence
     */
    abstract PacketAdapter handleCreateSequenceAction(PacketAdapter requestAdapter) throws CreateSequenceRefusedFault;

    /**
     * Handles terminate sequence request processing
     * 
     * @param  requestAdapter terminate sequence request packet adapter
     * 
     * @return terminate sequence response message wrapped in a response packet adapter
     * 
     * @exception UnknownSequenceFault if there is no such sequence registered with current 
     *            sequence manager.
     */
    abstract PacketAdapter handleTerminateSequenceAction(PacketAdapter requestAdapter) throws UnknownSequenceFault;

    /**
     * Handles acknowledgement request message processing
     * 
     * @param  requestAdapter  acknowledgement request message packet adapter
     * 
     * @return response for the acknowledgement request message wrapped in a response packet adapter
     * 
     * @exception UnknownSequenceFault if there is no such sequence registered with current 
     *            sequence manager.
     * 
     * @exception SequenceTerminatedFault if the sequence is currently in TERMINATING state
     */
    PacketAdapter handleAckRequestedAction(PacketAdapter requestAdapter) throws UnknownSequenceFault, SequenceTerminatedFault {

        Sequence inboundSequence;
        try {
            inboundSequence = sequenceManager.getSequence(requestAdapter.getAckRequestedHeaderSequenceId());
        } catch (UnknownSequenceException e) {
            LOGGER.logException(e, getProtocolFaultLoggingLevel());
            throw LOGGER.logException(new UnknownSequenceFault(configuration, requestAdapter.getPacket(), e.getMessage()), getProtocolFaultLoggingLevel());
        }

        if (inboundSequence.getStatus() == Sequence.Status.TERMINATING) {
            throw LOGGER.logException(new SequenceTerminatedFault(configuration, requestAdapter.getPacket(), ""), getProtocolFaultLoggingLevel());
        }

        inboundSequence.updateLastActivityTime();

        return requestAdapter.createAckResponse(inboundSequence, configuration.getRmVersion().sequenceAcknowledgementAction);
    }

    /**
     * Handles sequence acknowledgement message processing
     * 
     * @param  requestAdapter sequence acknowledgement message packet adapter
     * 
     * @return closes the transport and returns {@code null}
     * 
     * @exception UnknownSequenceFault if there is no such sequence registered with current 
     *            sequence manager.
     */
    PacketAdapter handleSequenceAcknowledgementAction(PacketAdapter requestAdapter) throws UnknownSequenceFault {
        processNonSequenceRmHeaders(requestAdapter);

        // FIXME maybe we should send acknowledgements back if any?
        return requestAdapter.closeTransportAndReturnNull();
    }

    /**
     * Returns a preconfigured logging level that should be used to log exceptions 
     * related to protocol message processing.
     * 
     * @return common logging level for protocol message processing errors
     */
    final Level getProtocolFaultLoggingLevel() {
        return Level.WARNING;
    }

    /**
     * Processes the WS-RM headers on the request message. Does not however process Sequence header.
     * 
     * @param requestAdapter packet adapter containing the request message to be processed
     * 
     * @exception UnknownSequenceFault if there is no such sequence registered with current 
     *            sequence manager.
     */
    private void processNonSequenceRmHeaders(PacketAdapter requestAdapter) throws UnknownSequenceFault {
        String ackRequestedSequenceId = requestAdapter.getAckRequestedHeaderSequenceId();
        if (ackRequestedSequenceId != null) {
            getSequenceOrSoapFault(requestAdapter.getPacket(), ackRequestedSequenceId).setAckRequestedFlag();
        }

        requestAdapter.processAcknowledgements(sequenceManager, getOutboundSequenceId4Request(requestAdapter));
    }

    final String getOutboundSequenceId4Request(PacketAdapter requestAdapter) throws UnknownSequenceFault {
        String sequenceId = requestAdapter.getSequenceId();
        if (sequenceId == null) {
            return null;
        }

        Sequence boundSequence;
        try {
            boundSequence = sequenceManager.getBoundSequence(requestAdapter.getSequenceId());
        } catch (UnknownSequenceException e) {
            LOGGER.logException(e, getProtocolFaultLoggingLevel());
            throw new UnknownSequenceFault(configuration, requestAdapter.getPacket(), requestAdapter.getSequenceId());
        }

        return (boundSequence != null) ? boundSequence.getId() : null;
    }

    final Sequence getSequenceOrSoapFault(Packet packet, String sequenceId) throws UnknownSequenceFault {
        try {
            return sequenceManager.getSequence(sequenceId);
        } catch (UnknownSequenceException e) {
            LOGGER.logException(e, getProtocolFaultLoggingLevel());
            throw LOGGER.logException(new UnknownSequenceFault(configuration, packet, e.getMessage()), getProtocolFaultLoggingLevel());
        }
    }

    private boolean duplicatesNotAllowed() {
        return configuration.getDeliveryAssurance() != ReliableMessagingFeature.DeliveryAssurance.AT_LEAST_ONCE;
    }
}