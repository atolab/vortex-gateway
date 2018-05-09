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
import java.util.Map;
import java.util.Properties;

import org.apache.camel.CamelContext;
import org.apache.camel.Component;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.Endpoint;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.impl.DefaultCamelContext;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adlinktech.gateway.camelospl.DdsEndpoint;


public class DdsEndpoint_DomainId_Test
{

   private DdsEndpoint ddsEndpoint;
   private String userID = "foo";
   private Endpoint endpoint;
   private CamelContext ctx;
   private ProducerTemplate pTemplate;
   private ConsumerTemplate cTemplate;
   private static final Logger logger = LoggerFactory.getLogger(DdsEndpoint_DomainId_Test.class);


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
      // - DomainId set to "persistent"
      // - filter messages with userID<>'current_userID' (filter out sent messages)
      // - Partition set to "ChatRoom"
      String filterExpr = URLEncoder.encode("userID <> '" + userID + "'", "UTF-8");
      String filter = "?contentFilter=" + filterExpr;
      String partition = "&Partition=ChatRoom";

      // URI of DDS endpoint using above QoS.
      // Use "Chat_SimpleChatMessage" topic with "Chat.SimpleChatMessage" type.
      final String fromURI = "dds:Chat_SimpleChatMessage_DomainId:12/Chat.SimpleChatMessage"
            + filter + partition;

      // We can't call ctx.getEndpoint() because endpoint will be automatically started,
      // and since OSPL_URI doesn't point to a conf file with appropriate domain, it will fail.
      // Thus, we create Endpoint via it's Component.
      Component c = ctx.getComponent("dds");
      if (c == null)
         Assert.fail("ctx.getComponent(\"dds\") returned null !!");

      try
      {
         endpoint = c.createEndpoint(fromURI);
      }
      catch (Exception e)
      {
         // TODO Auto-generated catch block
         e.printStackTrace();
         Assert.fail("Error : " + e);
      }
   }

   @Test
   public void testExample() throws IOException
   {

      logger.info("Start test");
      ddsEndpoint = (DdsEndpoint) endpoint;
      // Check if DomainId is well set in the DomainId attribute
      try
      {
         logger.info("Check if DeadlinePeriod is well set in the ddsEnpoint attribute");
         // Check if DomainId is well set in in the DomainId attribute
         Assert.assertTrue("DomainId is not well set in in the ddsEnpoint attribute ",
               ddsEndpoint.getDomainId() == 12);

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
