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
import com.adlinktech.gateway.camelospl.test.util.OsplVersionChecker;

import DDS.DataReader;
import DDS.DataReaderQosHolder;
import DDS.Duration_t;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DdsEndpoint_TimebasedFilter_Test
{

   private Duration_t duration = new Duration_t();
   private DdsEndpoint ddsEndpoint;
   private String userID;
   private Endpoint endpoint;
   private CamelContext ctx;
   private static final Logger logger = LoggerFactory
         .getLogger(DdsEndpoint_TimebasedFilter_Test.class);


   @Before
   public void init() throws UnsupportedEncodingException
   {


      // Initialise the duration to the value of 2s and 5ns
      duration.nanosec = 5;
      duration.sec = 2;
      // create Camel context
      ctx = new DefaultCamelContext();

      // DDS QoS Options:
      // - TimebasedFilter set to 2.000000005
      // - filter messages with userID<>'current_userID' (filter out sent messages)
      // - Partition set to "ChatRoom"
      String qos = "?TimebasedFilter=2.000000005";
      String filterExpr = URLEncoder.encode("userID <> '" + userID + "'", "UTF-8");
      String filter = "&contentFilter=" + filterExpr;
      String partition = "&Partition=ChatRoom";

      // URI of DDS endpoint using above QoS.
      // Use "Chat_SimpleChatMessage" topic with "Chat.SimpleChatMessage" type.
      final String fromURI = "dds:Chat_SimpleChatMessage_TimebasedFilter:0/Chat.SimpleChatMessage"
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

      // This test cannot work with OpenSpliceDDS older than v6.1
      // since Timebased Filter were not implemented (see dds756)
      if (OsplVersionChecker.isOsplOlderThan("6.1"))
      {
         logger.warn("Test skipped: OpenSpliceDDS " +
               OsplVersionChecker.getOsplVersion() +
               " don't have Timebased Filtering implemented.");
         return;
      }

      logger.info("Start test");

      DataReaderQosHolder readerHolder = new DataReaderQosHolder();

      ddsEndpoint = (DdsEndpoint) endpoint;

      // Check if TimebasedFilter is well set in the Reader
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

         logger.info("Check if TimebasedFilter is well set in the Reader");
         // Check if TimebasedFilter nanosec is well set in the Reader
         Assert.assertTrue("TimebasedFilter nanosec is not well set in the Reader ",
               readerHolder.value.time_based_filter.minimum_separation.nanosec == duration.nanosec);
         // Check if TimebasedFilter sec is well set in the Reader
         Assert.assertTrue("TimebasedFilter sec is not well set in the Reader ",
               readerHolder.value.time_based_filter.minimum_separation.sec == duration.sec);

         // Stop the consumer
         consumer.stop();

      }
      catch (Exception e)
      {
         // TODO Auto-generated catch block
         e.printStackTrace();
         Assert.fail("Error : " + e);
      }

      // Check if TimebasedFilter is well set in the TimebasedFilter attribute
      try
      {
         logger.info("Check if TimebasedFilter is well set in the ddsEnpoint attribute");
         // Check if TimebasedFilter is well set in in the ddsEnpoint attribute
         Assert.assertTrue("TimebasedFilter is not well set in in the ddsEnpoint attribute ",
               ddsEndpoint.getTimebasedFilter() == 2.000000005);

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
         if (endpoint != null)
         {
            endpoint.stop();
         }
         ctx.stop();
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }

   }
}
