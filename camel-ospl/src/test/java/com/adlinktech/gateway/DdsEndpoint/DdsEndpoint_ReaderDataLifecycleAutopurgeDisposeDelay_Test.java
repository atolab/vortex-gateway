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
import DDS.Duration_t;
import DDS.Topic;
import DDS.TopicQosHolder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DdsEndpoint_ReaderDataLifecycleAutopurgeDisposeDelay_Test
{

   private Duration_t period = new Duration_t();
   private DdsEndpoint ddsEndpoint;
   private String userID;
   private Endpoint endpoint;
   private CamelContext ctx;
   private ProducerTemplate template;
   private static final Logger logger = LoggerFactory
         .getLogger(DdsEndpoint_ReaderDataLifecycleAutopurgeDisposeDelay_Test.class);


   @Before
   public void init() throws UnsupportedEncodingException
   {


      // Initialise the period to the value of 2s and 5ns
      period.nanosec = 5;
      period.sec = 2;
      // create Camel context
      ctx = new DefaultCamelContext();

      // create ProducerTemplate
      template = ctx.createProducerTemplate();

      // DDS QoS Options:
      // - ReaderDataLifecycleAutopurgeDisposeDelay set to 2.000000005
      // - filter messages with userID<>'current_userID' (filter out sent messages)
      // - Partition set to "ChatRoom"
      String qos = "?ReaderDataLifecycleAutopurgeDisposedDelay=2.000000005";
      String filterExpr = URLEncoder.encode("userID <> '" + userID + "'", "UTF-8");
      String filter = "&contentFilter=" + filterExpr;
      String partition = "&Partition=ChatRoom";

      // URI of DDS endpoint using above QoS.
      // Use "Chat_SimpleChatMessage" topic with "Chat.SimpleChatMessage" type.
      final String fromURI = "dds:Chat_SimpleChatMessage_ReaderDataLifecycleAutopurgeDisposedDelay:0/Chat.SimpleChatMessage"
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
      DataReaderQosHolder readerHolder = new DataReaderQosHolder();
      ddsEndpoint = (DdsEndpoint) endpoint;
      Topic topic = null;

      // Check if ReaderDataLifecycleAutopurgedDisposeDelay is well set in the reader
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

         logger.info("Check if ReaderDataLifecycleAutopurgedDisposeDelay is well set in the Reader");
         // Check if ReaderDataLifecycleAutopurgeDisposedDelay nanosec is well set in the Reader
         Assert.assertTrue(
               "ReaderDataLifecycleAutopurgeDisposedDelay nanosec is not well set in the Reader ",
               readerHolder.value.reader_data_lifecycle.autopurge_disposed_samples_delay.nanosec == period.nanosec);
         // Check if ReaderDataLifecycleAutopurgeDisposedDelay sec is well set in the Reader
         Assert.assertTrue(
               "ReaderDataLifecycleAutopurgeDisposedDelay sec is not well set in the Reader ",
               readerHolder.value.reader_data_lifecycle.autopurge_disposed_samples_delay.sec == period.sec);

         // Stop the consumer
         consumer.stop();

      }
      catch (Exception e)
      {
         // TODO Auto-generated catch block
         e.printStackTrace();
         Assert.fail("Error : " + e);
      }

      // Check if ReaderDataLifecycleAutopurgeDisposedDelay is well set in the
      // ReaderDataLifecycleAutopurgeDisposedDelay attribute
      try
      {
         logger.info("Check if ReaderDataLifecycleAutopurgeDisposedDelay is well set in the ddsEnpoint attribute");
         // Check if ReaderDataLifecycleAutopurgeDisposedDelay is well set in in the ddsEnpoint
         // attribute
         Assert.assertTrue(
               "ReaderDataLifecycleAutopurgeDisposedDelay is not well set in in the ddsEnpoint attribute ",
               ddsEndpoint.getReaderDataLifecycleAutopurgeDisposedDelay() == 2.000000005);

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
