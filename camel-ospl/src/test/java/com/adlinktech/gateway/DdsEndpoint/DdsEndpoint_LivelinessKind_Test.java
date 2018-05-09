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

import DDS.DataReader;
import DDS.DataReaderQosHolder;
import DDS.DataWriter;
import DDS.DataWriterQosHolder;
import DDS.Topic;
import DDS.TopicQosHolder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DdsEndpoint_LivelinessKind_Test
{

   private DdsEndpoint ddsEndpoint;
   private String userID;
   private Endpoint endpoint;
   private CamelContext ctx;
   private ProducerTemplate template;
   private static final Logger logger = LoggerFactory
         .getLogger(DdsEndpoint_LivelinessKind_Test.class);


   @Before
   public void init() throws UnsupportedEncodingException
   {

      // create Camel context
      ctx = new DefaultCamelContext();

      // create ProducerTemplate
      template = ctx.createProducerTemplate();

      // DDS QoS Options:
      // - Liveliness set to "automatic"
      // - filter messages with userID<>'current_userID' (filter out sent messages)
      // - Partition set to "ChatRoom"
      String qos = "?LivelinessKind=AUTOMATIC";
      String filterExpr = URLEncoder.encode("userID <> '" + userID + "'", "UTF-8");
      String filter = "&contentFilter=" + filterExpr;
      String partition = "&Partition=ChatRoom";

      // URI of DDS endpoint using above QoS.
      // Use "Chat_SimpleChatMessage" topic with "Chat.SimpleChatMessage" type.
      final String fromURI = "dds:Chat_SimpleChatMessage_liveliness:0/Chat.SimpleChatMessage" + qos
            + filter + partition;

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
      DataReaderQosHolder readerHolder = new DataReaderQosHolder();
      DataWriterQosHolder writerHolder = new DataWriterQosHolder();

      ddsEndpoint = (DdsEndpoint) endpoint;
      Topic topic = null;

      // Check if Liveliness is well set in the Topic
      topic = ddsEndpoint.getTopic();
      topic.get_qos(holder);

      logger.info("Check if Reliability is well set in the topic");
      // Check if Liveliness is well set in the Topic
      Assert.assertTrue("Reliability is not well set in the topic ",
            holder.value.liveliness.kind == DDS.LivelinessQosPolicyKind.AUTOMATIC_LIVELINESS_QOS);

      // Check if Liveliness is well set in the Writer
      try
      {
         DdsProducer producer = (DdsProducer) endpoint.createProducer();
         producer.start();
         DataWriter writer = producer.getDataWriter();
         writer.get_qos(writerHolder);

         logger.info("Check if Reliability is well set in the Writer");
         // Check if Liveliness is well set in the writer
         Assert.assertTrue(
               "Reliability is not well set in the writer ",
               writerHolder.value.liveliness.kind == DDS.LivelinessQosPolicyKind.AUTOMATIC_LIVELINESS_QOS);

         // Stop the producer
         producer.stop();

      }
      catch (Exception e)
      {
         // TODO Auto-generated catch block
         e.printStackTrace();
         Assert.fail("Error : " + e);
      }

      // Check if Liveliness is well set in the Reader
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
         DataReader reader = consumer.getDataReader();
         reader.get_qos(readerHolder);

         logger.info("Check if Reliability is well set in the Reader");
         // Check if Liveliness is well set in the reader
         Assert.assertTrue(
               "Reliability is not well set in the reader ",
               readerHolder.value.liveliness.kind == DDS.LivelinessQosPolicyKind.AUTOMATIC_LIVELINESS_QOS);
         consumer.stop();

      }
      catch (Exception e)
      {
         // TODO Auto-generated catch block
         e.printStackTrace();
         Assert.fail("Error : " + e);
      }


      // Check if LivelinessKind is well set in the LivelinessKind attribute
      try
      {
         logger.info("Check if LivelinessKind is well set in the ddsEnpoint attribute");
         // Check if LivelinessKind is well set in in the ddsEnpoint attribute
         Assert.assertTrue(
               "LivelinessKind is not well set in in the ddsEnpoint attribute ",
               ddsEndpoint.getLivelinessKind() == DDS.LivelinessQosPolicyKind.AUTOMATIC_LIVELINESS_QOS);

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
            endpoint.stop();
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
