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
package com.adlinktech.gateway.DdsEndpoint;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.camel.CamelContext;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.impl.DefaultCamelContext;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.adlinktech.gateway.camelospl.DdsConsumer;
import com.adlinktech.gateway.camelospl.DdsEndpoint;
import com.adlinktech.gateway.camelospl.DdsException;
import com.adlinktech.gateway.camelospl.DdsProducer;

import DDS.PublisherQosHolder;
import DDS.SubscriberQosHolder;
import DDS.Publisher;
import DDS.Subscriber;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DdsEndpoint_PresentationCoherentAccess_Test
{

   private DdsEndpoint ddsEndpoint;
   private String userID;
   private Endpoint endpoint;
   private CamelContext ctx;
   private ProducerTemplate pTemplate;
   private ConsumerTemplate cTemplate;
   private static final Logger logger = LoggerFactory
         .getLogger(DdsEndpoint_PresentationCoherentAccess_Test.class);


   @Before
   public void init() throws UnsupportedEncodingException
   {

      // create Camel context
      ctx = new DefaultCamelContext();

      // create ProducerTemplate
      pTemplate = ctx.createProducerTemplate();

      // create ConsumerTemplate
      cTemplate = ctx.createConsumerTemplate();

      // DDS QoS Options:
      // - PresentationCoherentAccess set to "true"
      // - filter messages with userID<>'current_userID' (filter out sent messages)
      // - Partition set to "ChatRoom"
      String qos = "?PresentationCoherentAccess=true";
      String filterExpr = URLEncoder.encode("userID <> '" + userID + "'", "UTF-8");
      String filter = "&contentFilter=" + filterExpr;
      String partition = "&Partition=ChatRoom";

      // URI of DDS endpoint using above QoS.
      // Use "Chat_SimpleChatMessage" topic with "Chat.SimpleChatMessage" type.
      final String fromURI = "dds:Chat_SimpleChatMessage_PresentationCoherentAccess:0/Chat.SimpleChatMessage"
            + qos + filter + partition;

      // get endpoint for later use in sendMessage()
      endpoint = ctx.getEndpoint(fromURI);
      try
      {
         endpoint.start();
      }
      catch (Exception e)
      {
         e.printStackTrace();
         Assert.fail("Error : " + e);
      }
   }

   @Test
   public void testExample() throws IOException
   {

      logger.info("Start test");

      PublisherQosHolder publiserHolder = new PublisherQosHolder();
      SubscriberQosHolder subcriberHolder = new SubscriberQosHolder();

      ddsEndpoint = (DdsEndpoint) endpoint;

      // Check if PresentationCoherentAccess is well set in the Publisher
      try
      {
         DdsProducer producer = (DdsProducer) endpoint.createProducer();
         producer.start();
         Publisher publisher = producer.getPublisher();
         publisher.get_qos(publiserHolder);

         logger.info("Check if PresentationCoherentAccess is well set in the Publisher");
         // Check if PresentationCoherentAccess is well set in the publisher
         Assert.assertTrue("PresentationCoherentAccess is not well set in the publisher ",
               publiserHolder.value.presentation.coherent_access == true);

         // Stop the producer
         producer.stop();

      }
      catch (Exception e)
      {
         // TODO Auto-generated catch block
         e.printStackTrace();
         Assert.fail("Error : " + e);
      }

      // Check if PresentationCoherentAccess is well set in the Subscriber
      try
      {
         Processor process = new Processor()
         {

            @Override
            public void process(Exchange exchange) throws Exception
            {
               // TODO Auto-generated method stub

            }
         };

         DdsConsumer consumer = (DdsConsumer) endpoint.createConsumer((Processor) process);
         consumer.start();
         Subscriber subscriber = consumer.getSubscriber();
         subscriber.get_qos(subcriberHolder);

         logger.info("Check if PresentationCoherentAccess is well set in the Subscriber");
         // Check if PresentationCoherentAccess is well set in the Subscriber
         Assert.assertTrue("PresentationCoherentAccess is not well set in the Subscriber ",
               subcriberHolder.value.presentation.coherent_access == true);

         // Stop the consumer
         consumer.stop();

      }
      catch (Exception e)
      {
         // TODO Auto-generated catch block
         e.printStackTrace();
         Assert.fail("Error : " + e);
      }

      // Check if PresentationCoherentAccess is well set in the PresentationCoherentAccess attribute
      try
      {
         logger.info("Check if PresentationCoherentAccess is well set in the ddsEnpoint attribute");
         // Check if PresentationCoherentAccess is well set in in the PresentationCoherentAccess
         // attribute
         Assert.assertTrue(
               "PresentationCoherentAccess is not well set in in the ddsEnpoint attribute ",
               ddsEndpoint.getPresentationCoherentAccess() == true);

      }
      catch (Exception e)
      {
         // TODO Auto-generated catch block
         e.printStackTrace();
         Assert.fail("Error : " + e);
      }

   }

   @After
   public void clean()
   {

      try
      {
         if (cTemplate != null && pTemplate != null)
         {
            endpoint.stop();
            pTemplate.stop();
            cTemplate.stop();
         }
         ctx.stop();
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }

   }
}