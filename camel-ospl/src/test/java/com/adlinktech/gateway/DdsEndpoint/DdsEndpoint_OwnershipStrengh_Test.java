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
import org.apache.camel.ProducerTemplate;
import org.apache.camel.impl.DefaultCamelContext;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import DDS.DataWriter;
import DDS.DataWriterQosHolder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adlinktech.gateway.camelospl.DdsEndpoint;
import com.adlinktech.gateway.camelospl.DdsProducer;


public class DdsEndpoint_OwnershipStrengh_Test
{

   private String userID;
   private DdsEndpoint ddsEndpoint;
   private Endpoint endpoint;
   private CamelContext ctx;
   private ProducerTemplate pTemplate;
   private ConsumerTemplate cTemplate;
   private static final Logger logger = LoggerFactory
         .getLogger(DdsEndpoint_OwnershipStrengh_Test.class);
   private int strengh;


   @Before
   public void init() throws UnsupportedEncodingException
   {

      // Define the Ownership_Strengh
      strengh = 500;

      // create Camel context
      ctx = new DefaultCamelContext();

      // create ProducerTemplate
      pTemplate = ctx.createProducerTemplate();

      // DDS QoS Options:
      // - Ownership_strengh is set to 500
      // - filter messages with userID<>'current_userID' (filter out sent messages)
      // - Partition set to "ChatRoom"
      String qos = "?OwnershipStrength=" + strengh;
      String filterExpr = URLEncoder.encode("userID <> '" + userID + "'", "UTF-8");
      String filter = "&contentFilter=" + filterExpr;
      String partition = "&Partition=ChatRoom";

      // URI of DDS endpoint using above QoS.
      // Use "Chat_SimpleChatMessage" topic with "Chat.SimpleChatMessage" type.
      final String fromURI = "dds:Chat_SimpleChatMessage_ownership_strengh:0/Chat.SimpleChatMessage"
            + qos + filter + partition;

      // get endpoint for later use.
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
      DataWriterQosHolder writerHolder = new DataWriterQosHolder();

      ddsEndpoint = (DdsEndpoint) endpoint;

      // Check if Ownership_Strengh is well set in the writer.
      try
      {
         DdsProducer producer = (DdsProducer) endpoint.createProducer();
         producer.start();
         DataWriter writer = producer.getDataWriter();
         writer.get_qos(writerHolder);

         logger.info("Check if Ownership_Strengh is well set in the Writer");
         // Check if Ownership_Strengh is well set in the writer.
         Assert.assertTrue("Ownership_Strengh is not well set in the writer ",
               writerHolder.value.ownership_strength.value == strengh);

         // Chek if Ownership_Stregh is well set in the variable defined in DdsEndpoint.
         Assert.assertTrue("ownershipStrengh variable is not well set in the ddsEndpoint instance",
               ddsEndpoint.getOwnershipStrength() == strengh);

         // Stop the producer
         producer.stop();

      }
      catch (Exception e)
      {
         // TODO Auto-generated catch block
         e.printStackTrace();
         Assert.fail("Error : " + e);
      }

      // Check if OwnershipStrength is well set in the OwnershipStrength attribute
      try
      {
         logger.info("Check if OwnershipStrength is well set in the ddsEnpoint attribute");
         // Check if OwnershipStrength is well set in in the ddsEnpoint attribute
         Assert.assertTrue("OwnershipStrength is not well set in in the ddsEnpoint attribute ",
               ddsEndpoint.getOwnershipStrength() == strengh);

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
