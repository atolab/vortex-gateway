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

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Producer;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultExchange;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.adlinktech.gateway.camelospl.DdsComponent;
import com.adlinktech.gateway.camelospl.DdsDisposeHeader;
import com.adlinktech.gateway.camelospl.DdsEndpoint;
import com.adlinktech.gateway.camelospl.DdsProducer;

import Chat.SimpleChatMessage;
import DDS.DataWriterQos;
import DDS.DataWriterQosHolder;


public class DdsProducer_DynamicQos_Test
{

   /*
    * Extends DdsProducer to intercept publishToDDS call,
    * and to extract DataWriter QoS at this time.
    * In case of dynamic QoS change, QoS is supposed to be changed
    * before this publishToDDS call, and to be revert back after this call.
    */
   private class TestProducer
      extends DdsProducer
   {

      private DataWriterQosHolder lastUsedQos = new DataWriterQosHolder();

      public TestProducer(DdsEndpoint endpoint)
      {
         super(endpoint);
      }

      public DataWriterQos getLastUsedQos()
      {
         return lastUsedQos.value;
      }

      @Override
      protected void publishToDDS(Object data, DdsDisposeHeader dispose)
         throws Exception
      {
         // get DataWriter QoS
         getDataWriter().get_qos(lastUsedQos);

         // go on
         super.publishToDDS(data, dispose);
      }
   }

   /*
    * Override DdsEndpoint to return TestProducer in createProducer()
    */
   private class TestEndpoint
      extends DdsEndpoint
   {

      TestProducer testProd;

      public TestEndpoint(String topicName, int domainId, String typeName,
         String uri, DdsComponent component)
      {
         super(topicName, domainId, typeName, uri, component);
      }

      @Override
      public Producer createProducer() throws Exception
      {
         this.testProd = new TestProducer(this);
         return testProd;
      }

      public TestProducer getProducer()
      {
         return testProd;
      }
   }


   private static final int INITIAL_OWN_STR = 10;
   private static final int INITIAL_TRANS_PRIO = 12;
   private static final double INITIAL_LIFESPAN = 2.000000005;
   private static final int INITIAL_LIFESPAN_SEC = 2;
   private static final int INITIAL_LIFESPAN_NSEC = 5;
   private static final int CHANGED_OWN_STR = 20;
   private static final int CHANGED_TRANS_PRIO = 15;
   private static final double CHANGED_LIFESPAN = 3.000000007;
   private static final int CHANGED_LIFESPAN_SEC = 3;
   private static final int CHANGED_LIFESPAN_NSEC = 7;


   private CamelContext ctx;
   private TestEndpoint ddsEndpoint;
   private ProducerTemplate pTemplate;


   @Before
   public void init() throws Exception
   {

      // create Camel context
      ctx = new DefaultCamelContext();
      ddsEndpoint = new TestEndpoint(
            "Chat_SimpleChatMessage",
            0,
            "Chat.SimpleChatMessage",
            "dds:Chat_SimpleChatMessage:0/Chat.SimpleChatMessage",
            new DdsComponent(ctx));

      // set some values to QoS which will be changed later
      ddsEndpoint.setOwnershipStrength(INITIAL_OWN_STR);
      ddsEndpoint.setTransportPriority(INITIAL_TRANS_PRIO);
      ddsEndpoint.setLifespanDuration(INITIAL_LIFESPAN);

      try
      {
         ctx.addRoutes(new RouteBuilder()
         {

            @Override
            public void configure() throws Exception
            {
               from("direct:test")
                     .to(ddsEndpoint);
            }
         });

         // create ProducerTemplate
         pTemplate = ctx.createProducerTemplate();

         ctx.start();
      }
      catch (Exception e)
      {
         e.printStackTrace();
         Assert.fail("Error : " + e);
      }

   }

   @Test
   public void testOwnershipStrengthChange() throws IOException
   {
      // check initial QoS at first
      DataWriterQosHolder initialQos = new DataWriterQosHolder();
      ddsEndpoint.getProducer().getDataWriter().get_qos(initialQos);
      Assert.assertEquals(INITIAL_OWN_STR,
            initialQos.value.ownership_strength.value);

      // create exchange with with SimpleChatMessage as body
      // and OwnershipStrength as header.
      Exchange ex = new DefaultExchange(ctx);
      SimpleChatMessage msg = new SimpleChatMessage();
      msg.index = 1;
      msg.userID = "User1";
      msg.content = "Hello World!";
      ex.getIn().setBody(msg);
      ex.getIn().setHeader("dds.ownershipStrength", CHANGED_OWN_STR);

      // send exchange
      pTemplate.send(ddsEndpoint, ex);

      // Check QoS what were used to publish to DDS
      DataWriterQos lastUsedQos =
            ddsEndpoint.getProducer().getLastUsedQos();
      Assert.assertEquals(CHANGED_OWN_STR,
            lastUsedQos.ownership_strength.value);

      // Check that QoS are back to original ones
      ddsEndpoint.getProducer().getDataWriter().get_qos(initialQos);
      Assert.assertEquals(INITIAL_OWN_STR,
            initialQos.value.ownership_strength.value);

   }


   @Test
   public void testTransportPriorityChange() throws IOException
   {
      // check initial QoS at first
      DataWriterQosHolder initialQos = new DataWriterQosHolder();
      ddsEndpoint.getProducer().getDataWriter().get_qos(initialQos);
      Assert.assertEquals(INITIAL_TRANS_PRIO,
            initialQos.value.transport_priority.value);

      // create exchange with with SimpleChatMessage as body
      // and TransportPriority as header.
      Exchange ex = new DefaultExchange(ctx);
      SimpleChatMessage msg = new SimpleChatMessage();
      msg.index = 1;
      msg.userID = "User1";
      msg.content = "Hello World!";
      ex.getIn().setBody(msg);
      ex.getIn().setHeader("dds.transportPriority", CHANGED_TRANS_PRIO);

      // send exchange
      pTemplate.send(ddsEndpoint, ex);

      // Check QoS what were used to publish to DDS
      DataWriterQos lastUsedQos =
            ddsEndpoint.getProducer().getLastUsedQos();
      Assert.assertEquals(CHANGED_TRANS_PRIO,
            lastUsedQos.transport_priority.value);

      // Check that QoS are back to original ones
      ddsEndpoint.getProducer().getDataWriter().get_qos(initialQos);
      Assert.assertEquals(INITIAL_TRANS_PRIO,
            initialQos.value.transport_priority.value);


   }


   @Test
   public void testLifespanChange() throws IOException
   {
      // check initial QoS at first
      DataWriterQosHolder initialQos = new DataWriterQosHolder();
      ddsEndpoint.getProducer().getDataWriter().get_qos(initialQos);
      Assert.assertEquals(INITIAL_LIFESPAN_SEC,
            initialQos.value.lifespan.duration.sec);
      Assert.assertEquals(INITIAL_LIFESPAN_NSEC,
            initialQos.value.lifespan.duration.nanosec);

      // create exchange with with SimpleChatMessage as body
      // and OwnershipStrength as header.
      Exchange ex = new DefaultExchange(ctx);
      SimpleChatMessage msg = new SimpleChatMessage();
      msg.index = 1;
      msg.userID = "User1";
      msg.content = "Hello World!";
      ex.getIn().setBody(msg);
      ex.getIn().setHeader("dds.lifespanDuration", CHANGED_LIFESPAN);

      // send exchange
      pTemplate.send(ddsEndpoint, ex);

      // Check QoS what were used to publish to DDS
      DataWriterQos lastUsedQos =
            ddsEndpoint.getProducer().getLastUsedQos();
      Assert.assertEquals(CHANGED_LIFESPAN_SEC,
            lastUsedQos.lifespan.duration.sec);
      Assert.assertEquals(CHANGED_LIFESPAN_NSEC,
            lastUsedQos.lifespan.duration.nanosec);

      // Check that QoS are back to original ones
      ddsEndpoint.getProducer().getDataWriter().get_qos(initialQos);
      Assert.assertEquals(INITIAL_LIFESPAN_SEC,
            initialQos.value.lifespan.duration.sec);
      Assert.assertEquals(INITIAL_LIFESPAN_NSEC,
            initialQos.value.lifespan.duration.nanosec);

   }


   @After
   public void clean()
   {

      try
      {
         ctx.stop();
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }

   }
}
