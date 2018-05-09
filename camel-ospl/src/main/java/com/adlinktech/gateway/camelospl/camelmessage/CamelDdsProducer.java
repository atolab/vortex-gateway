/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.adlinktech.gateway.camelospl.camelmessage;

import org.apache.camel.Exchange;
import org.apache.camel.RuntimeExchangeException;

import com.adlinktech.gateway.camelospl.DdsDisposeHeader;
import com.adlinktech.gateway.camelospl.DdsProducer;

import CamelDDS.CamelMessage;


/**
 * Producer for "Camel Messages mode".
 * It's the same than a DdsProducer, except we publish a CamelMessage object
 * instead of a custom-typed object.
 */
public class CamelDdsProducer
   extends DdsProducer
{

   // NOTE: similar to a DdsConsumer, but override the
   // process(Exchange) operation to convert the IN Camel Message
   // into a CamelMessage sample and send it to DDS.


   /** parent endpoint (as CamelDdsEndpoint object) */
   private CamelDdsEndpoint endpoint;

   /**
    * Constructor.
    * 
    * @param endpoint the parent Endpoint
    */
   public CamelDdsProducer(CamelDdsEndpoint endpoint)
   {
      super(endpoint);
      this.endpoint = endpoint;
   }


   @Override
   public void process(Exchange exchange)
   {
      // Override DdsProducer.process() operation to convert
      // IN message as a CamelMessage and publish this one.

      try
      {
         // Convert IN message to CamelMessage
         CamelMessage msg =
               CamelMessageUtils.createCamelMessage(exchange.getIn(), endpoint.getTarget());

         // Publish CamelMessage to DDS
         publishToDDS(msg, exchange.getIn().getHeader("DDS_DISPOSE", DdsDisposeHeader.class));
      }
      catch (Exception e)
      {
         throw new RuntimeExchangeException(e.getMessage(), exchange, e);
      }
   }

}
