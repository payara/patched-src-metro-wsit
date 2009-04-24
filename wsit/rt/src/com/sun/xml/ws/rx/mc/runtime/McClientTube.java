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
package com.sun.xml.ws.rx.mc.runtime;

import com.sun.xml.ws.rx.util.TimestampedCollection;
import com.sun.xml.ws.rx.RxConfiguration;
import com.sun.xml.ws.rx.util.ScheduledTaskManager;
import com.sun.xml.ws.api.EndpointAddress;
import com.sun.xml.ws.api.addressing.AddressingVersion;
import com.sun.xml.ws.api.addressing.WSEndpointReference;
import com.sun.xml.ws.api.message.Header;
import com.sun.xml.ws.api.message.HeaderList;
import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.pipe.Fiber;
import com.sun.xml.ws.api.pipe.NextAction;
import com.sun.xml.ws.api.pipe.Tube;
import com.sun.xml.ws.api.pipe.TubeCloner;
import com.sun.xml.ws.api.pipe.helper.AbstractFilterTubeImpl;
import com.sun.xml.ws.api.pipe.helper.AbstractTubeImpl;
import com.sun.xml.ws.commons.Logger;
import com.sun.xml.ws.rx.RxRuntimeException;
import com.sun.xml.ws.rx.mc.runtime.spi.ProtocolMessageHandler;
import com.sun.xml.ws.rx.util.Communicator;
import java.util.UUID;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLStreamException;

/**
 *
 * @author Marek Potociar <marek.potociar at sun.com>
 */
public class McClientTube extends AbstractFilterTubeImpl {
    //

    private static final Logger LOGGER = Logger.getLogger(McClientTube.class);
    //
    private final RxConfiguration configuration;
    private final Unmarshaller unmarshaller;
    private final Header wsmcAnnonymousReplyToHeader;
    private final Header wsmcAnnonymousFaultToHeader;
    private final Communicator communicator;
    private final ScheduledTaskManager scheduler;
    private final TimestampedCollection<String, Fiber> suspendedFiberStorage;
    private final MakeConnectionSenderTask mcSenderTask;
    private final WSEndpointReference wsmcAnonymousEndpointReference;

    McClientTube(RxConfiguration configuration, Tube tubelineHead, EndpointAddress endpointAddress) throws RxRuntimeException {
        super(tubelineHead);

        this.configuration = configuration;

        this.unmarshaller = configuration.getMcVersion().getUnmarshaller(configuration.getAddressingVersion());
        this.communicator = new Communicator(
                "McClientTubeCommunicator",
                endpointAddress,
                tubelineHead,
                null,
                configuration.getAddressingVersion(),
                configuration.getSoapVersion(),
                configuration.getMcVersion().getJaxbContext(configuration.getAddressingVersion()),
                configuration.getMcVersion().getUnmarshaller(configuration.getAddressingVersion()));

        final String wsmcAnonymousAddress = configuration.getMcVersion().getWsmcAnonymousAddress(UUID.randomUUID().toString());
        this.wsmcAnonymousEndpointReference = new WSEndpointReference(wsmcAnonymousAddress, configuration.getAddressingVersion());
        this.wsmcAnnonymousReplyToHeader = wsmcAnonymousEndpointReference.createHeader(configuration.getAddressingVersion().replyToTag);
        this.wsmcAnnonymousFaultToHeader = wsmcAnonymousEndpointReference.createHeader(configuration.getAddressingVersion().faultToTag);

        this.suspendedFiberStorage = new TimestampedCollection<String, Fiber>();
        this.mcSenderTask = new MakeConnectionSenderTask(
                communicator,
                suspendedFiberStorage,
                wsmcAnonymousAddress,
                wsmcAnnonymousReplyToHeader,
                wsmcAnnonymousFaultToHeader,
                configuration);
        this.scheduler = new ScheduledTaskManager();
        // TODO P2 make it configurable
        this.scheduler.startTask(mcSenderTask, 2000, 500);
    }

    McClientTube(McClientTube original, TubeCloner cloner) {
        super(original, cloner);

        this.configuration = original.configuration;

        this.unmarshaller = configuration.getMcVersion().getUnmarshaller(configuration.getAddressingVersion());

        this.wsmcAnnonymousReplyToHeader = original.wsmcAnnonymousReplyToHeader;
        this.wsmcAnnonymousFaultToHeader = original.wsmcAnnonymousFaultToHeader;
        this.communicator = original.communicator;
        this.suspendedFiberStorage = original.suspendedFiberStorage;
        this.scheduler = original.scheduler;
        this.mcSenderTask = original.mcSenderTask;

        this.wsmcAnonymousEndpointReference = original.wsmcAnonymousEndpointReference;
    }

    @Override
    public AbstractTubeImpl copy(TubeCloner cloner) {
        LOGGER.entering();
        try {
            return new McClientTube(this, cloner);
        } finally {
            LOGGER.exiting();
        }
    }

    @Override
    public NextAction processRequest(Packet request) {
        final Message message = request.getMessage();
        if (!message.hasHeaders()) {
            // TODO L10N
            throw LOGGER.logSevereException(new RxRuntimeException("Required WS-Addressing headers not found: No SOAP headers present on a client request message."));
        }


        if (needToSetWsmcAnnonymousHeaders(request)) { // annonymous request
            setMcAnnonymousHeaders(
                    message.getHeaders(),
                    configuration.getAddressingVersion(),
                    wsmcAnnonymousReplyToHeader,
                    wsmcAnnonymousFaultToHeader);

            String correlationId = message.getID(configuration.getAddressingVersion(), configuration.getSoapVersion());
            Fiber.CompletionCallback responseHandler;
            if (request.expectReply != null && request.expectReply) { // most likely Req-Resp MEP
                responseHandler = new RequestResponseMepHandler(configuration, mcSenderTask, suspendedFiberStorage, correlationId);
            } else { // most likely One-Way MEP
                responseHandler = new OneWayMepHandler(configuration, mcSenderTask, suspendedFiberStorage, correlationId);
            }

            suspendedFiberStorage.register(correlationId, Fiber.current());
            communicator.sendAsync(request, responseHandler);

            return super.doSuspend();
        } else { // not annonymous request - don't care
            return super.processRequest(request);
        }
    }

    @Override
    public NextAction processResponse(Packet response) {
        return super.processResponse(response);
    }

    @Override
    public NextAction processException(Throwable t) {
        return super.processException(t);
    }

    @Override
    public void preDestroy() {
        scheduler.stopAll();

        super.preDestroy();
    }

    /**
     *  Used by RM client tube
     */
    public final WSEndpointReference getWsmcAnonymousEndpointReference() {
        return wsmcAnonymousEndpointReference;
    }

    public final void registerProtocolMessageHandler(ProtocolMessageHandler handler) {
        mcSenderTask.register(handler);
    }

    /**
     * Method check if the WS-A {@code ReplyTo} header is present or not. If it is not present, or if it is present but is annonymous,
     * method returns true. If the WS-A {@code ReplyTo} header is present and non-annonymous, mehod returns false.
     */
    private boolean needToSetWsmcAnnonymousHeaders(final Packet request) {
        Header replyToHeader = request.getMessage().getHeaders().get(configuration.getAddressingVersion().replyToTag, false);
        if (replyToHeader != null) {
            try {
                return replyToHeader.readAsEPR(configuration.getAddressingVersion()).isAnonymous();
            } catch (XMLStreamException ex) {
                // TODO L10N
                throw LOGGER.logSevereException(new RxRuntimeException("Error unmarshalling content of WS-A ReplyTo header", ex));
            }
        }

        // this request seems to be one-way, need to check if there are any RM-Acks set on it.
        // FIXME: this should be made in a RM-agnostic way
        return isBooleanFlagSet(request, RxConfiguration.ACK_REQUESTED_HEADER_SET);
    }

    static void setMcAnnonymousHeaders(final HeaderList headers, AddressingVersion av, Header wsmcReplyToHeader, Header wsmcFaultToHeader) {
        headers.remove(av.replyToTag);
        headers.add(wsmcReplyToHeader);

        if (headers.remove(av.faultToTag) != null) {
            headers.add(wsmcFaultToHeader);
        }

    }

    private Boolean isBooleanFlagSet(Packet packet, Object flag) {
        Boolean value = Boolean.class.cast(packet.invocationProperties.get(flag));
        return value != null && value.booleanValue();
    }
}