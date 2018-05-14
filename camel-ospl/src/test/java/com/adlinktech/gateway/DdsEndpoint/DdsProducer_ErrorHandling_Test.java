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
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import com.adlinktech.gateway.camelospl.DdsDisposeHeader;
import com.adlinktech.gateway.camelospl.DdsException;
import com.adlinktech.gateway.camelospl.DdsEndpoint;

import Chat.SimpleChatMessage;
import DDS.DomainParticipant;
import DDS.DomainParticipantFactory;
import DDS.RETCODE_ALREADY_DELETED;
import DDS.RETCODE_OK;
import DDS.SampleInfo;


public class DdsProducer_ErrorHandling_Test
{

   private static final String DIRECT = "direct:sendMsg";
   private static final String DDS_URI =
         "dds:Chat_SimpleChatMessage:0/Chat.SimpleChatMessage?ignoreLocalPublishers=false";
   private static final String CHAT_USER_ID = "User1";
   private static final String CHAT_MSG_CONTENT = "Hello World!";
   private static final int CHAT_MSG_INDEX = 1;

   private CamelContext ctx;
   private ProducerTemplate pTemplate;

   private Throwable exceptionOnWrite = null;
   private SimpleChatMessage receivedData = null;
   private SampleInfo receivedSampleInfo = null;
   private DdsDisposeHeader receivedDisposeHeader = null;

   private void resetReceivedObjects()
   {
      exceptionOnWrite = null;
      receivedData = null;
      receivedSampleInfo = null;
      receivedDisposeHeader = null;
   }

   @Before
   public void init()
   {
      // create Camel context
      ctx = new DefaultCamelContext();
      try
      {
         ctx.addRoutes(new RouteBuilder()
         {

            @Override
            public void configure() throws Exception
            {

               // route writing data to DDS (with an intermediate seda endpoint
               // to not get the exception sending exchange on DIRECT, but to have
               // the exception caught by the "onFailureOnly" instead)
               from(DIRECT).to("seda:tmp");
               from("seda:tmp")
                     .onCompletion().onFailureOnly()
                     .process(new Processor()
                     {

                        @Override
                        public void process(Exchange exchange) throws Exception
                        {
                           // the caused by exception is stored in a property on the exchange
                           exceptionOnWrite = exchange.getProperty(Exchange.EXCEPTION_CAUGHT,
                                 Throwable.class);
                        }
                     })
                     .end()
                     .to(DDS_URI);

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
   public void testExceptionOnWrite()
   {
      try
      {
         ctx.start();

         // write a data
         System.out.println("\nWrite data");
         sendExchange(null);

         // wait DDS message to be processed
         Thread.sleep(2000);

         System.out.println("Check received data\n");
         if (exceptionOnWrite != null)
         {
            exceptionOnWrite.printStackTrace();
            Assert.fail("Exception writing DDS sample: " + exceptionOnWrite);
         }

         Assert.assertNull(exceptionOnWrite);
         Assert.assertNotNull(receivedData);
         Assert.assertNotNull(receivedSampleInfo);
         Assert.assertNull(receivedDisposeHeader);
         Assert.assertTrue(receivedSampleInfo.valid_data);

         // delete DDS Participant
         System.out.println("\ndelete Participant");
         DdsEndpoint endpoint = (DdsEndpoint) ctx.getEndpoint(DDS_URI);
         Field f = DdsEndpoint.class.getDeclaredField("participant");
         f.setAccessible(true);
         DomainParticipant participant = (DomainParticipant) f.get(endpoint);
         int status = participant.delete_contained_entities();
         Assert.assertEquals(RETCODE_OK.value, status);
         status = DomainParticipantFactory.get_instance().delete_participant(participant);
         Assert.assertEquals(RETCODE_OK.value, status);

         // write a data
         System.out.println("\nWrite data");
         sendExchange(null);

         // wait DDS message to be processed
         Thread.sleep(500);

         // check an Exception message has been caught
         Assert.assertNotNull("Write on DDS didn't raise an exception as expected",
               exceptionOnWrite);
         Assert.assertTrue("Intercepted exception on DDS write is not a DdsException",
               exceptionOnWrite instanceof DdsException);
         Assert.assertEquals(RETCODE_ALREADY_DELETED.value,
               ((DdsException) exceptionOnWrite).getRetcode());

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
      catch (Throwable e)
      {
         // e.printStackTrace();
      }
   }
}
