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
import org.apache.camel.Endpoint;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.impl.DefaultCamelContext;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.adlinktech.gateway.camelospl.DdsEndpoint;
import com.adlinktech.gateway.camelospl.DdsException;
import com.adlinktech.gateway.camelospl.DdsProducer;

import DDS.DataWriter;
import DDS.DataWriterQosHolder;
import DDS.Topic;
import DDS.TopicQosHolder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DdsEndpoint_TransportPriority_Test
{

   private DdsEndpoint ddsEndpoint;
   private String userID;
   private Endpoint endpoint;
   private CamelContext ctx;
   private ProducerTemplate template;
   private static final Logger logger = LoggerFactory
         .getLogger(DdsEndpoint_TransportPriority_Test.class);


   @Before
   public void init() throws UnsupportedEncodingException
   {

      // create Camel context
      ctx = new DefaultCamelContext();

      // create ProducerTemplate
      template = ctx.createProducerTemplate();

      // DDS QoS Options:
      // - TransportPriority set to 4
      // - filter messages with userID<>'current_userID' (filter out sent messages)
      // - Partition set to "ChatRoom"
      String qos = "?TransportPriority=4";
      String filterExpr = URLEncoder.encode("userID <> '" + userID + "'", "UTF-8");
      String filter = "&contentFilter=" + filterExpr;
      String partition = "&Partition=ChatRoom";

      // URI of DDS endpoint using above QoS.
      // Use "Chat_SimpleChatMessage" topic with "Chat.SimpleChatMessage" type.
      final String fromURI = "dds:Chat_SimpleChatMessage_TransportPriority:0/Chat.SimpleChatMessage"
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

      TopicQosHolder holder = new TopicQosHolder();
      DataWriterQosHolder writerHolder = new DataWriterQosHolder();

      ddsEndpoint = (DdsEndpoint) endpoint;
      Topic topic = null;

      // Check if TransportPriority is well set in the Topic
      topic = ddsEndpoint.getTopic();

      topic.get_qos(holder);

      logger.info("Check if TransportPriority is well set in the topic");
      // Check if TransportPriority is well set in the Topic
      Assert.assertTrue("TransportPriority is not well set in the topic ",
            holder.value.transport_priority.value == 4);

      // Check if TransportPriority is well set in the Writer
      try
      {
         DdsProducer producer = (DdsProducer) endpoint.createProducer();
         producer.start();
         DataWriter writer = producer.getDataWriter();
         writer.get_qos(writerHolder);

         logger.info("Check if TransportPriority is well set in the Writer");
         // Check if TransportPriority is well set in the writer
         Assert.assertTrue("TransportPriority is not well set in the writer ",
               writerHolder.value.transport_priority.value == 4);

         // Stop the producer
         producer.stop();

      }
      catch (Exception e)
      {
         // TODO Auto-generated catch block
         e.printStackTrace();
         Assert.fail("Error : " + e);
      }


      // Check if TransportPriority is well set in the TransportPriority attribute
      try
      {
         logger.info("Check if TransportPriority is well set in the ddsEnpoint attribute");
         // Check if TransportPriority is well set in in the TransportPriority attribute
         Assert.assertTrue("TransportPriority is not well set in in the ddsEnpoint attribute ",
               ddsEndpoint.getTransportPriority() == 4);

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
         if (template != null)
         {
            // endpoint.stop();
            template.stop();
         }
         ctx.stop();
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }

   }
}
