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
import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultExchange;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.adlinktech.gateway.camelospl.DdsException;
import com.adlinktech.gateway.camelospl.DdsProducer;

import Chat.SimpleChatMessage;
import DDS.DataWriterQosHolder;
import DDS.PublisherQosHolder;


public class DdsProducer_QosChange_Test
{


   private static final double INITIAL_DEADLINE = 1.000000002;
   private static final int INITIAL_DEADLINE_SEC = 1;
   private static final int INITIAL_DEADLINE_NSEC = 2;
   private static final double CHANGED_DEADLINE = 3.000000004;
   private static final int CHANGED_DEADLINE_SEC = 3;
   private static final int CHANGED_DEADLINE_NSEC = 4;

   private static final double INITIAL_LATENCY = 5.000000006;
   private static final int INITIAL_LATENCY_SEC = 5;
   private static final int INITIAL_LATENCY_NSEC = 6;
   private static final double CHANGED_LATENCY = 7.000000008;
   private static final int CHANGED_LATENCY_SEC = 7;
   private static final int CHANGED_LATENCY_NSEC = 8;

   private static final int INITIAL_OWN_STR = 10;
   private static final int CHANGED_OWN_STR = 20;

   private static final int INITIAL_TRANS_PRIO = 12;
   private static final int CHANGED_TRANS_PRIO = 15;

   private static final double INITIAL_LIFESPAN = 9.000000001;
   private static final int INITIAL_LIFESPAN_SEC = 9;
   private static final int INITIAL_LIFESPAN_NSEC = 1;
   private static final double CHANGED_LIFESPAN = 2.000000003;
   private static final int CHANGED_LIFESPAN_SEC = 2;
   private static final int CHANGED_LIFESPAN_NSEC = 3;

   private static final String INITIAL_PARTITION_STR = "PartitionA,PartitionB";
   private static final String INITIAL_PARTITION_0 = "PartitionA";
   private static final String INITIAL_PARTITION_1 = "PartitionB";
   private static final String CHANGED_PARTITION_STR = "PartitionC,PartitionD";
   private static final String CHANGED_PARTITION_0 = "PartitionC";
   private static final String CHANGED_PARTITION_1 = "PartitionD";


   private CamelContext ctx;
   private Endpoint ddsEndpoint;
   private DdsProducer ddsProducer;
   private ProducerTemplate pTemplate;


   @Before
   public void init() throws UnsupportedEncodingException
   {

      // create Camel context
      ctx = new DefaultCamelContext();
      ddsEndpoint = ctx.getEndpoint(
            "dds:Chat_SimpleChatMessage:0/Chat.SimpleChatMessage?" +
                  "deadlinePeriod=" + INITIAL_DEADLINE + "&" +
                  "latencyDuration=" + INITIAL_LATENCY + "&" +
                  "ownershipStrength=" + INITIAL_OWN_STR + "&" +
                  "transportPriority=" + INITIAL_TRANS_PRIO + "&" +
                  "lifespanDuration=" + INITIAL_LIFESPAN + "&" +
                  "partition=" + INITIAL_PARTITION_STR
            );
      try
      {
         ddsEndpoint.start();
      }
      catch (Exception e1)
      {
         e1.printStackTrace();
         Assert.fail("Error : " + e1);
      }

      try
      {
         // create DdsProducer to change its QoS later
         ddsProducer = (DdsProducer) ddsEndpoint.createProducer();
      }
      catch (Exception e)
      {
         e.printStackTrace();
         Assert.fail("Error : " + e);
      }

      try
      {
         ctx.addRoutes(new RouteBuilder()
         {

            @Override
            public void configure() throws Exception
            {
               from("direct:test")
                     .process(ddsProducer);
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

   private void sendExchange()
   {
      Exchange ex = new DefaultExchange(ctx);
      SimpleChatMessage msg = new SimpleChatMessage();
      msg.index = 1;
      msg.userID = "User1";
      msg.content = "Hello World!";
      ex.getIn().setBody(msg);
      pTemplate.send(ddsEndpoint, ex);
   }


   @Test
   public void testDeadlinePeriodChange() throws IOException
   {
      // check initial QoS at first
      DataWriterQosHolder qos = new DataWriterQosHolder();
      ddsProducer.getDataWriter().get_qos(qos);
      Assert.assertEquals(INITIAL_DEADLINE_SEC,
            qos.value.deadline.period.sec);
      Assert.assertEquals(INITIAL_DEADLINE_NSEC,
            qos.value.deadline.period.nanosec);

      // change Qos
      try
      {
         ddsProducer.changeDeadlinePeriod(CHANGED_DEADLINE);
      }
      catch (DdsException e)
      {
         e.printStackTrace();
         Assert.fail("Error : " + e);
      }

      // check QoS has changed
      ddsProducer.getDataWriter().get_qos(qos);
      Assert.assertEquals(CHANGED_DEADLINE_SEC,
            qos.value.deadline.period.sec);
      Assert.assertEquals(CHANGED_DEADLINE_NSEC,
            qos.value.deadline.period.nanosec);

      // send an exchange
      sendExchange();

      // check QoS is still changed
      ddsProducer.getDataWriter().get_qos(qos);
      Assert.assertEquals(CHANGED_DEADLINE_SEC,
            qos.value.deadline.period.sec);
      Assert.assertEquals(CHANGED_DEADLINE_NSEC,
            qos.value.deadline.period.nanosec);
   }

   @Test
   public void testLatencyDurationChange() throws IOException
   {
      // check initial QoS at first
      DataWriterQosHolder qos = new DataWriterQosHolder();
      ddsProducer.getDataWriter().get_qos(qos);
      Assert.assertEquals(INITIAL_LATENCY_SEC,
            qos.value.latency_budget.duration.sec);
      Assert.assertEquals(INITIAL_LATENCY_NSEC,
            qos.value.latency_budget.duration.nanosec);

      // change Qos
      try
      {
         ddsProducer.changeLatencyDuration(CHANGED_LATENCY);
      }
      catch (DdsException e)
      {
         e.printStackTrace();
         Assert.fail("Error : " + e);
      }

      // check QoS has changed
      ddsProducer.getDataWriter().get_qos(qos);
      Assert.assertEquals(CHANGED_LATENCY_SEC,
            qos.value.latency_budget.duration.sec);
      Assert.assertEquals(CHANGED_LATENCY_NSEC,
            qos.value.latency_budget.duration.nanosec);

      // send an exchange
      sendExchange();

      // check QoS is still changed
      ddsProducer.getDataWriter().get_qos(qos);
      Assert.assertEquals(CHANGED_LATENCY_SEC,
            qos.value.latency_budget.duration.sec);
      Assert.assertEquals(CHANGED_LATENCY_NSEC,
            qos.value.latency_budget.duration.nanosec);
   }

   @Test
   public void testOwnershipStrengthChange() throws IOException
   {
      // check initial QoS at first
      DataWriterQosHolder qos = new DataWriterQosHolder();
      ddsProducer.getDataWriter().get_qos(qos);
      Assert.assertEquals(INITIAL_OWN_STR,
            qos.value.ownership_strength.value);

      // change Qos
      try
      {
         ddsProducer.changeOwnershipStrength(CHANGED_OWN_STR);
      }
      catch (DdsException e)
      {
         e.printStackTrace();
         Assert.fail("Error : " + e);
      }

      // check QoS has changed
      ddsProducer.getDataWriter().get_qos(qos);
      Assert.assertEquals(CHANGED_OWN_STR,
            qos.value.ownership_strength.value);

      // send an exchange
      sendExchange();

      // check QoS is still changed
      ddsProducer.getDataWriter().get_qos(qos);
      Assert.assertEquals(CHANGED_OWN_STR,
            qos.value.ownership_strength.value);
   }


   @Test
   public void testTransportPriorityChange() throws IOException
   {
      // check initial QoS at first
      DataWriterQosHolder qos = new DataWriterQosHolder();
      ddsProducer.getDataWriter().get_qos(qos);
      Assert.assertEquals(INITIAL_TRANS_PRIO,
            qos.value.transport_priority.value);

      // change Qos
      try
      {
         ddsProducer.changeTransportPriority(CHANGED_TRANS_PRIO);
      }
      catch (DdsException e)
      {
         e.printStackTrace();
         Assert.fail("Error : " + e);
      }

      // check QoS has changed
      ddsProducer.getDataWriter().get_qos(qos);
      Assert.assertEquals(CHANGED_TRANS_PRIO,
            qos.value.transport_priority.value);

      // send an exchange
      sendExchange();

      // check QoS is still changed
      ddsProducer.getDataWriter().get_qos(qos);
      Assert.assertEquals(CHANGED_TRANS_PRIO,
            qos.value.transport_priority.value);
   }


   @Test
   public void testLifespanChange() throws IOException
   {
      // check initial QoS at first
      DataWriterQosHolder qos = new DataWriterQosHolder();
      ddsProducer.getDataWriter().get_qos(qos);
      Assert.assertEquals(INITIAL_LIFESPAN_SEC,
            qos.value.lifespan.duration.sec);
      Assert.assertEquals(INITIAL_LIFESPAN_NSEC,
            qos.value.lifespan.duration.nanosec);

      // change Qos
      try
      {
         ddsProducer.changeLifespanDuration(CHANGED_LIFESPAN);
      }
      catch (DdsException e)
      {
         e.printStackTrace();
         Assert.fail("Error : " + e);
      }

      // check QoS has changed
      ddsProducer.getDataWriter().get_qos(qos);
      Assert.assertEquals(CHANGED_LIFESPAN_SEC,
            qos.value.lifespan.duration.sec);
      Assert.assertEquals(CHANGED_LIFESPAN_NSEC,
            qos.value.lifespan.duration.nanosec);

      // send an exchange
      sendExchange();

      // check QoS is still changed
      ddsProducer.getDataWriter().get_qos(qos);
      Assert.assertEquals(CHANGED_LIFESPAN_SEC,
            qos.value.lifespan.duration.sec);
      Assert.assertEquals(CHANGED_LIFESPAN_NSEC,
            qos.value.lifespan.duration.nanosec);
   }

   @Test
   public void testPartitionChange() throws IOException
   {
      // check initial QoS at first
      PublisherQosHolder qos = new PublisherQosHolder();
      ddsProducer.getPublisher().get_qos(qos);
      Assert.assertEquals(INITIAL_PARTITION_0,
            qos.value.partition.name[0]);
      Assert.assertEquals(INITIAL_PARTITION_1,
            qos.value.partition.name[1]);

      // change Qos
      try
      {
         ddsProducer.changePartition(CHANGED_PARTITION_STR);
      }
      catch (DdsException e)
      {
         e.printStackTrace();
         Assert.fail("Error : " + e);
      }

      // check QoS has changed
      ddsProducer.getPublisher().get_qos(qos);
      Assert.assertEquals(CHANGED_PARTITION_0,
            qos.value.partition.name[0]);
      Assert.assertEquals(CHANGED_PARTITION_1,
            qos.value.partition.name[1]);

      // send an exchange
      sendExchange();

      // check QoS is still changed
      ddsProducer.getPublisher().get_qos(qos);
      Assert.assertEquals(CHANGED_PARTITION_0,
            qos.value.partition.name[0]);
      Assert.assertEquals(CHANGED_PARTITION_1,
            qos.value.partition.name[1]);
   }


   @After
   public void clean()
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
