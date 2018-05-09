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

import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultExchange;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.adlinktech.gateway.camelospl.DdsDisposeHeader;

import Chat.SimpleChatMessage;
import DDS.SampleInfo;


public class Regression_BB_149_Test
{

   private static final String DIRECT = "direct:sendMsg";
   private static final String DDS_URI =
         "dds:Chat_SimpleChatMessage:0/Chat.SimpleChatMessage?readMode=true&ignoreLocalPublishers=false";
   private static final String CHAT_USER_ID = "User1";
   private static final String CHAT_MSG_CONTENT = "Hello World!";
   private static final int CHAT_MSG_INDEX = 1;

   private CamelContext ctx;
   private ProducerTemplate pTemplate;

   private Throwable receivedError = null;
   private SimpleChatMessage receivedData = null;
   private SampleInfo receivedSampleInfo = null;
   private DdsDisposeHeader receivedDisposeHeader = null;

   private void resetReceivedObjects()
   {
      receivedError = null;
      receivedData = null;
      receivedSampleInfo = null;
      receivedDisposeHeader = null;
   }

   @Before
   public void init()
   {
      System.out.println("------------------------------------");
      for (Map.Entry<String, String> e : System.getenv().entrySet())
      {
         System.out.println("   " + e.getKey() + " = " + e.getValue());
      }
      System.out.println("------------------------------------\n");

      // create Camel context
      ctx = new DefaultCamelContext();
      try
      {
         ctx.addRoutes(new RouteBuilder()
         {

            @Override
            public void configure() throws Exception
            {
               onException(Throwable.class)
                     .process(new Processor()
                     {

                        @Override
                        public void process(Exchange exchange) throws Exception
                        {
                           // the caused by exception is stored in a property on the exchange
                           receivedError = exchange.getProperty(Exchange.EXCEPTION_CAUGHT,
                                 Throwable.class);

                           if (receivedError != null)
                           {
                              // get the original cause
                              while (receivedError.getCause() != null)
                              {
                                 receivedError = receivedError.getCause();
                              }
                           }
                        }
                     })
                     .stop();

               // route writing data to DDS
               from(DIRECT).to(DDS_URI);

               // route receiving data from DDS
               from(DDS_URI)
                     .process(new Processor()
                     {

                        @Override
                        public void process(Exchange e) throws Exception
                        {
                           receivedData = e.getIn().getBody(SimpleChatMessage.class);
                           receivedDisposeHeader = e.getIn().getHeader("DDS_DISPOSE",
                                 DdsDisposeHeader.class);
                           receivedSampleInfo = e.getIn().getHeader("DDS_SAMPLE_INFO",
                                 SampleInfo.class);
                           System.out.println("Received "
                                 +
                                 (receivedSampleInfo.valid_data ? "valid " : "invalid ")
                                 + "data"
                                 +
                                 (receivedDisposeHeader == null ? "" : " (" + receivedDisposeHeader
                                       + ")") + ":");
                           System.out.println("   " + receivedData.userID + " : ("
                                 + receivedData.index + ") " + receivedData.content);
                        }
                     });
            }
         });

         // create ProducerTemplate
         pTemplate = ctx.createProducerTemplate();


      }
      catch (Exception e)
      {
         e.printStackTrace();
         Assert.fail("Error : " + e);
      }
   }

   private void sendExchange(DdsDisposeHeader dispose) throws Exception
   {
      resetReceivedObjects();

      Exchange ex = new DefaultExchange(ctx);
      SimpleChatMessage msg = new SimpleChatMessage();
      msg.index = CHAT_MSG_INDEX;
      msg.userID = CHAT_USER_ID;
      msg.content = CHAT_MSG_CONTENT;
      ex.getIn().setBody(msg);
      if (dispose != null)
      {
         ex.getIn().setHeader("DDS_DISPOSE", dispose);
      }
      ex = pTemplate.send(DIRECT, ex);
      Exception e = ex.getException();
      if (e != null)
      {
         throw e;
      }
   }

   @Test
   public void testDisposeMessageValidity()
   {
      try
      {
         ctx.start();

         // write a data
         System.out.println("\nWrite data");
         sendExchange(null);

         // wait DDS message to be processed
         Thread.sleep(1000);

         System.out.println("Check received data\n");
         if (receivedError != null)
         {
            receivedError.printStackTrace();
            Assert.fail("Exception receiving DDS sample: " + receivedError);
         }

         Assert.assertNotNull(receivedData);
         Assert.assertNotNull(receivedSampleInfo);
         Assert.assertNull(receivedDisposeHeader);
         Assert.assertTrue(receivedSampleInfo.valid_data);

         // dispose the data
         System.out.println("\nDispose data");
         sendExchange(DdsDisposeHeader.DISPOSE);

         // wait DDS message to be processed
         Thread.sleep(1000);

         System.out.println("Check received data\n");
         if (receivedError != null)
         {
            receivedError.printStackTrace();
            Assert.fail("Exception receiving DDS sample: " + receivedError);
         }

         Assert.assertNotNull(receivedSampleInfo);
         Assert.assertTrue(receivedSampleInfo.valid_data);
         Assert.assertNotNull(receivedData);
         Assert.assertEquals(CHAT_USER_ID, receivedData.userID);
         Assert.assertEquals(CHAT_MSG_INDEX, receivedData.index);
         Assert.assertEquals(CHAT_MSG_CONTENT, receivedData.content);
         Assert.assertNotNull(receivedDisposeHeader);
         Assert.assertEquals(DdsDisposeHeader.DISPOSE, receivedDisposeHeader);


      }
      catch (Exception e)
      {
         e.printStackTrace();
         Assert.fail("Error : " + e);
      }
   }


   @After
   public void end()
   {
      try
      {
         pTemplate.stop();
         ctx.stop();
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
   }
}
