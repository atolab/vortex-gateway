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

import org.apache.camel.Consumer;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.slf4j.Logger;

import com.adlinktech.gateway.camelospl.DdsComponent;
import com.adlinktech.gateway.camelospl.DdsEndpoint;
import com.adlinktech.gateway.camelospl.utils.Loggers;

import CamelDDS.CamelMessage;
import DDS.SampleInfo;


/**
 * Concrete implementation of DDS Endpoint for "Camel Message mode".
 */
public class CamelDdsEndpoint
   extends DdsEndpoint
{

   // NOTE: this is implemented as a regular DdsEndpoint,
   // but forcing topic type to CamelDDS.CamelMessage
   // and implementing "target" option as a content filter.


   /** Logger */
   private static final Logger LOG = Loggers.LOGGER_ENDPOINT;

   /** target option */
   private String target = null;

   /**
    * Constructor
    * 
    * @param topicName the Topic's name
    * @param domainId the Domain ID
    * @param uri the Endpoint URI
    * @param component the parent Component
    */
   public CamelDdsEndpoint(String topicName,
      int domainId,
      String uri,
      DdsComponent component)
   {
      // Same as DdsEndpoint but force data type to CamelMessage
      super(topicName, domainId, CamelMessage.class.getName(), uri, component);
   }

   /*
    * Target option
    */
   public void setTarget(String target)
   {
      // 'target' is implemented as a content-filtered Topic
      this.target = target;
      if (target == null)
      {
         setContentFilter(null);
      }
      else
      {
         setContentFilter("target = '" + target + "'");
      }
   }

   public String getTarget()
   {
      return target;
   }


   @Override
   public Producer createProducer() throws Exception
   {
      return new CamelDdsProducer(this);
   }

   @Override
   public Consumer createConsumer(Processor processor) throws Exception
   {
      return new CamelDdsConsumer(this, processor);
   }


   /*
    * Override DdsEndpoint.createExchange(Object, SampleInfo)
    * to convert the received CamelMessage sample
    * as a Camel Message in created Exchange.
    */
   @Override
   public Exchange createExchange(Object sample, SampleInfo info)
   {
      // create an new empty Exchange
      Exchange exchange = createExchange();

      try
      {
         // sample is a CamelMessage
         CamelMessage msg = (CamelMessage) sample;


         // extract Message from CamelMessage
         CamelMessageUtils.extractCamelMessage(msg, exchange.getIn());

      }
      catch (Exception e)
      {
         // log exception
         LOG.error("{} : Error creating Exchange from CamelMessage: ", this, e);
         // but create Exchange anyway, setting exception within
         exchange.setException(e);
      }

      return exchange;
   }

}
