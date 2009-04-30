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

import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.pipe.Fiber;
import com.sun.xml.ws.commons.Logger;
import com.sun.xml.ws.rx.RxRuntimeException;
import com.sun.xml.ws.rx.rm.runtime.delivery.Postman;
import com.sun.xml.ws.rx.rm.runtime.sequence.DuplicateMessageRegistrationException;
import com.sun.xml.ws.rx.util.AbstractResponseHandler;
import java.io.IOException;
import javax.xml.ws.WebServiceException;

/**
 *
 * @author Marek Potociar <marek.potociar at sun.com>
 */
class ServerDestinationDeliveryCallback implements Postman.Callback {

    private static class ResponseCallbackHandler extends AbstractResponseHandler implements Fiber.CompletionCallback {

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
        private final JaxwsApplicationMessage request;
        private final RuntimeContext rc;

        public ResponseCallbackHandler(JaxwsApplicationMessage request, RuntimeContext rc) {
            super(rc.suspendedFiberStorage, request.getCorrelationId());
            this.request = request;
            this.rc = rc;
        }

        public void onCompletion(Packet response) {
            /**
             * This if clause is a part of the RM-JCaps private contract. JCaps may decide
             * that the request it received should be resent and thus it should not be acknowledged.
             *
             * For more information, see documentation of RM_ACK_PROPERTY_KEY constant field.
             */
            String rmAckPropertyValue = String.class.cast(response.invocationProperties.remove(RM_ACK_PROPERTY_KEY));
            if (rmAckPropertyValue == null || Boolean.parseBoolean(rmAckPropertyValue)) {
                rc.destinationMessageHandler.acknowledgeApplicationLayerDelivery(request);
            } else {
                LOGGER.finer(String.format("Value of the '%s' property is '%s'. The request has not been acknowledged.", RM_ACK_PROPERTY_KEY, rmAckPropertyValue));
                rc.redeliveryTask.register(request, rc.configuration.getRetransmissionBackoffAlgorithm().nextResendTime(request.getNextResendCount(), rc.configuration.getMessageRetransmissionInterval()));
                return;
            }

            try {
                if (response.getMessage() == null) { // was one-way request - create empty acknowledgement message
                    response = rc.communicator.setEmptyResponseMessage(response, request.getPacket(), rc.rmVersion.sequenceAcknowledgementAction);
                    rc.protocolHandler.appendAcknowledgementHeaders(response, rc.destinationMessageHandler.getAcknowledgementData(request.getSequenceId()));
                    resumeParentFiber(response);
                } else {
                    JaxwsApplicationMessage message = new JaxwsApplicationMessage(response, getCorrelationId());
                    rc.sourceMessageHandler.registerMessage(message, rc.getBoundSequenceId(request.getSequenceId()));
                    rc.sourceMessageHandler.putToDeliveryQueue(message);
                }

            // TODO handle RM faults
            } catch (DuplicateMessageRegistrationException ex) {
                onCompletion(ex);
            }
        }

        public void onCompletion(Throwable error) {
            if (ServerDestinationDeliveryCallback.isResendPossible(error)) {
                rc.redeliveryTask.register(request, rc.configuration.getRetransmissionBackoffAlgorithm().nextResendTime(request.getNextResendCount(), rc.configuration.getMessageRetransmissionInterval()));
            } else {
                getParentFiber().resume(error);
            }
        }
    }
    private static final Logger LOGGER = Logger.getLogger(ServerDestinationDeliveryCallback.class);
    private final RuntimeContext rc;

    public ServerDestinationDeliveryCallback(RuntimeContext rc) {
        this.rc = rc;
    }

    public void deliver(ApplicationMessage message) {
        if (message instanceof JaxwsApplicationMessage) {
            deliver(JaxwsApplicationMessage.class.cast(message));
        } else {
            // TODO L10N
            throw LOGGER.logSevereException(new RxRuntimeException(String.format(
                    "Unexpected message class '%s', expected class '%s'",
                    message.getClass().getName(),
                    JaxwsApplicationMessage.class.getName())));
        }
    }

    private void deliver(JaxwsApplicationMessage message) {
        Fiber.CompletionCallback responseCallback = new ResponseCallbackHandler(message, rc);

        rc.communicator.sendAsync(message.getPacket().copy(true), responseCallback);
    }

    private static boolean isResendPossible(Throwable throwable) {
        if (throwable instanceof IOException) {
            return true;
        } else if (throwable instanceof WebServiceException) {
            // Unwrap exception and see if it makes sense to retry this request
            // (no need to check for null - handled by instanceof)
            if (throwable.getCause() instanceof IOException) {
                return true;
            }
        }
        return false;
    }
}
