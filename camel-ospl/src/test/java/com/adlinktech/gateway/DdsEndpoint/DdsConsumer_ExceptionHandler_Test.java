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

import java.lang.reflect.Field;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultExchange;
import org.apache.camel.impl.SimpleRegistry;
import org.apache.camel.spi.ExceptionHandler;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import com.adlinktech.gateway.camelospl.DdsDisposeHeader;
import com.adlinktech.gateway.camelospl.DdsException;
import com.adlinktech.gateway.camelospl.DdsConsumer;
import Chat.SimpleChatMessage;
import DDS.RETCODE_BAD_PARAMETER;
import DDS.SampleInfo;


public class DdsConsumer_ExceptionHandler_Test
{

   private static final String DIRECT = "direct:sendMsg";
   private static final String DDS_URI =
         "dds:Chat_SimpleChatMessage:0/Chat.SimpleChatMessage?ignoreLocalPublishers=false&consumer.exceptionHandler=#MyExceptionHandler";
   private static final String CHAT_USER_ID = "User1";
   private static final String CHAT_MSG_CONTENT = "Hello World!";
   private static final int CHAT_MSG_INDEX = 1;

   private CamelContext ctx;
   private ProducerTemplate pTemplate;

   private static Throwable exceptionOnRead = null;
   private SimpleChatMessage receivedData = null;
   private SampleInfo receivedSampleInfo = null;
   private DdsDisposeHeader receivedDisposeHeader = null;

   private void resetReceivedObjects()
   {
      exceptionOnRead = null;
      receivedData = null;
      receivedSampleInfo = null;
      receivedDisposeHeader = null;
   }

   // Custom ExceptionHandler registered as a bean (see SimpleRegistry in init())
   private static class MyExceptionHandler
      implements ExceptionHandler
   {

      @Override
      public void handleException(Throwable exception)
      {
         handleException(exception.getMessage(), exception);
      }

      @Override
      public void handleException(String message, Throwable exception)
      {
         handleException(message, null, exception);
      }

      @Override
      public void handleException(String message, Exchange exchange, Throwable exception)
      {
         exceptionOnRead = exception;
      }
   }


   @Before
   public void init()
   {
      SimpleRegistry registry = new SimpleRegistry();
      registry.put("MyExceptionHandler", new MyExceptionHandler());

      // create Camel context
      ctx = new DefaultCamelContext(registry);
      try
      {
         ctx.addRoutes(new RouteBuilder()
         {

            @Override
            public void configure() throws Exception
            {
               // route writing data to DDS
               from(DIRECT).to(DDS_URI);

               // route receiving data from DDS
               from(DDS_URI)
                     .id(DDS_URI)
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
   public void testExceptionOnRead()
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
         if (exceptionOnRead != null)
         {
            exceptionOnRead.printStackTrace();
            Assert.fail("Exception receiving DDS sample: " + exceptionOnRead);
         }

         Assert.assertNull(exceptionOnRead);
         Assert.assertNotNull(receivedData);
         Assert.assertNotNull(receivedSampleInfo);
         Assert.assertNull(receivedDisposeHeader);
         Assert.assertTrue(receivedSampleInfo.valid_data);

         // Cause an error in DataReader.read() setting the
         // seqHolderObject object used in read calls to null
         System.out.println("\nCause error in DataReader");
         DdsConsumer consumer = (DdsConsumer) ctx.getRoute(DDS_URI).getConsumer();
         Field f = DdsConsumer.class.getDeclaredField("seqHolderObject");
         f.setAccessible(true);
         f.set(consumer, null);

         // write a data
         System.out.println("\nWrite data");
         sendExchange(null);

         Thread.sleep(100);

         // check an Exception message has been caught
         Assert.assertNotNull("Read on DDS didn't raise an exception as expected", exceptionOnRead);
         Assert.assertTrue("Intercepted exception on DDS read is not a DdsException",
               exceptionOnRead instanceof DdsException);
         Assert.assertEquals(RETCODE_BAD_PARAMETER.value,
               ((DdsException) exceptionOnRead).getRetcode());

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
         // pTemplate.stop();
         // ctx.stop();
      }
      catch (Throwable e)
      {
         // e.printStackTrace();
      }
   }
}
