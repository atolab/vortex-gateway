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

import com.adlinktech.gateway.camelospl.DdsConsumer;
import com.adlinktech.gateway.camelospl.DdsException;
import com.adlinktech.gateway.camelospl.test.util.OsplVersionChecker;

import Chat.SimpleChatMessage;
import DDS.DataReaderQosHolder;
import DDS.SubscriberQosHolder;


public class DdsConsumer_QosChange_Test
{


   private static final double INITIAL_DEADLINE = 8.000000002;
   private static final int INITIAL_DEADLINE_SEC = 8;
   private static final int INITIAL_DEADLINE_NSEC = 2;
   private static final double CHANGED_DEADLINE = 9.000000004;
   private static final int CHANGED_DEADLINE_SEC = 9;
   private static final int CHANGED_DEADLINE_NSEC = 4;

   private static final double INITIAL_LATENCY = 5.000000006;
   private static final int INITIAL_LATENCY_SEC = 5;
   private static final int INITIAL_LATENCY_NSEC = 6;
   private static final double CHANGED_LATENCY = 7.000000008;
   private static final int CHANGED_LATENCY_SEC = 7;
   private static final int CHANGED_LATENCY_NSEC = 8;

   private static final double INITIAL_TIMEBASED_FILTER = 1.000000002;
   private static final int INITIAL_TIMEBASED_FILTER_SEC = 1;
   private static final int INITIAL_TIMEBASED_FILTER_NSEC = 2;
   private static final double CHANGED_TIMEBASED_FILTER = 2.000000003;
   private static final int CHANGED_TIMEBASED_FILTER_SEC = 2;
   private static final int CHANGED_TIMEBASED_FILTER_NSEC = 3;

   private static final String INITIAL_PARTITION_STR = "PartitionA,PartitionB";
   private static final String INITIAL_PARTITION_0 = "PartitionA";
   private static final String INITIAL_PARTITION_1 = "PartitionB";
   private static final String CHANGED_PARTITION_STR = "PartitionC,PartitionD";
   private static final String CHANGED_PARTITION_0 = "PartitionC";
   private static final String CHANGED_PARTITION_1 = "PartitionD";


   private CamelContext ctx;
   private Endpoint ddsEndpoint;
   private ProducerTemplate pTemplate;


   @Before
   public void init() throws UnsupportedEncodingException
   {

      String ddsURI = "dds:Chat_SimpleChatMessage:0/Chat.SimpleChatMessage" +
            "?deadlinePeriod=" + INITIAL_DEADLINE +
            "&latencyDuration=" + INITIAL_LATENCY +
            "&partition=" + INITIAL_PARTITION_STR;

      if ( !OsplVersionChecker.isOsplOlderThan("6.1.0"))
      {
         // TIME_BASED_FILTER QoSPolicy was not supported before v6.1.0
         ddsURI += "&timebasedFilter=" + INITIAL_TIMEBASED_FILTER;
      }

      // create Camel context
      ctx = new DefaultCamelContext();
      ddsEndpoint = ctx.getEndpoint(ddsURI);

      try
      {
         ctx.addRoutes(new RouteBuilder()
         {

            @Override
            public void configure() throws Exception
            {
               from(ddsEndpoint).routeId("DdsTestRoute")
                     .log("${body}");
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
      // get Route consumer
      DdsConsumer ddsConsumer =
            (DdsConsumer) ctx.getRoute("DdsTestRoute").getConsumer();

      // check initial QoS at first
      DataReaderQosHolder qos = new DataReaderQosHolder();
      ddsConsumer.getDataReader().get_qos(qos);
      Assert.assertEquals(INITIAL_DEADLINE_SEC,
            qos.value.deadline.period.sec);
      Assert.assertEquals(INITIAL_DEADLINE_NSEC,
            qos.value.deadline.period.nanosec);

      // change Qos
      try
      {
         ddsConsumer.changeDeadlinePeriod(CHANGED_DEADLINE);
      }
      catch (DdsException e)
      {
         e.printStackTrace();
         Assert.fail("Error : " + e);
      }

      // check QoS has changed
      ddsConsumer.getDataReader().get_qos(qos);
      Assert.assertEquals(CHANGED_DEADLINE_SEC,
            qos.value.deadline.period.sec);
      Assert.assertEquals(CHANGED_DEADLINE_NSEC,
            qos.value.deadline.period.nanosec);

      // send an exchange
      sendExchange();

      // check QoS is still changed
      ddsConsumer.getDataReader().get_qos(qos);
      Assert.assertEquals(CHANGED_DEADLINE_SEC,
            qos.value.deadline.period.sec);
      Assert.assertEquals(CHANGED_DEADLINE_NSEC,
            qos.value.deadline.period.nanosec);
   }

   @Test
   public void testLatencyDurationChange() throws IOException
   {
      // get Route consumer
      DdsConsumer ddsConsumer =
            (DdsConsumer) ctx.getRoute("DdsTestRoute").getConsumer();

      // check initial QoS at first
      DataReaderQosHolder qos = new DataReaderQosHolder();
      ddsConsumer.getDataReader().get_qos(qos);
      Assert.assertEquals(INITIAL_LATENCY_SEC,
            qos.value.latency_budget.duration.sec);
      Assert.assertEquals(INITIAL_LATENCY_NSEC,
            qos.value.latency_budget.duration.nanosec);

      // change Qos
      try
      {
         ddsConsumer.changeLatencyDuration(CHANGED_LATENCY);
      }
      catch (DdsException e)
      {
         e.printStackTrace();
         Assert.fail("Error : " + e);
      }

      // check QoS has changed
      ddsConsumer.getDataReader().get_qos(qos);
      Assert.assertEquals(CHANGED_LATENCY_SEC,
            qos.value.latency_budget.duration.sec);
      Assert.assertEquals(CHANGED_LATENCY_NSEC,
            qos.value.latency_budget.duration.nanosec);

      // send an exchange
      sendExchange();

      // check QoS is still changed
      ddsConsumer.getDataReader().get_qos(qos);
      Assert.assertEquals(CHANGED_LATENCY_SEC,
            qos.value.latency_budget.duration.sec);
      Assert.assertEquals(CHANGED_LATENCY_NSEC,
            qos.value.latency_budget.duration.nanosec);
   }


   @Test
   public void testTimeBasedFilterChange() throws IOException
   {
      if (OsplVersionChecker.isOsplOlderThan("6.1.0"))
      {
         // TIME_BASED_FILTER QoSPolicy was not supported before v6.1.0
         System.out.println(
               "NOTE: skip testTimeBasedFilterChange() because " +
                     "TIME_BASED_FILTER QoSPolicy is not supported with OpenSpliceDDS " +
                     OsplVersionChecker.getOsplVersion());
         return;
      }

      // get Route consumer
      DdsConsumer ddsConsumer =
            (DdsConsumer) ctx.getRoute("DdsTestRoute").getConsumer();

      // check initial QoS at first
      DataReaderQosHolder qos = new DataReaderQosHolder();
      ddsConsumer.getDataReader().get_qos(qos);
      Assert.assertEquals(INITIAL_TIMEBASED_FILTER_SEC,
            qos.value.time_based_filter.minimum_separation.sec);
      Assert.assertEquals(INITIAL_TIMEBASED_FILTER_NSEC,
            qos.value.time_based_filter.minimum_separation.nanosec);

      // change Qos
      try
      {
         ddsConsumer.changeTimebasedFilter(CHANGED_TIMEBASED_FILTER);
      }
      catch (DdsException e)
      {
         e.printStackTrace();
         Assert.fail("Error : " + e);
      }

      // check QoS has changed
      ddsConsumer.getDataReader().get_qos(qos);
      Assert.assertEquals(CHANGED_TIMEBASED_FILTER_SEC,
            qos.value.time_based_filter.minimum_separation.sec);
      Assert.assertEquals(CHANGED_TIMEBASED_FILTER_NSEC,
            qos.value.time_based_filter.minimum_separation.nanosec);

      // send an exchange
      sendExchange();

      // check QoS is still changed
      ddsConsumer.getDataReader().get_qos(qos);
      Assert.assertEquals(CHANGED_TIMEBASED_FILTER_SEC,
            qos.value.time_based_filter.minimum_separation.sec);
      Assert.assertEquals(CHANGED_TIMEBASED_FILTER_NSEC,
            qos.value.time_based_filter.minimum_separation.nanosec);
   }

   @Test
   public void testPartitionChange() throws IOException
   {
      // get Route consumer
      DdsConsumer ddsConsumer =
            (DdsConsumer) ctx.getRoute("DdsTestRoute").getConsumer();

      // check initial QoS at first
      SubscriberQosHolder qos = new SubscriberQosHolder();
      ddsConsumer.getSubscriber().get_qos(qos);
      Assert.assertEquals(INITIAL_PARTITION_0,
            qos.value.partition.name[0]);
      Assert.assertEquals(INITIAL_PARTITION_1,
            qos.value.partition.name[1]);

      // change Qos
      try
      {
         ddsConsumer.changePartition(CHANGED_PARTITION_STR);
      }
      catch (DdsException e)
      {
         e.printStackTrace();
         Assert.fail("Error : " + e);
      }

      // check QoS has changed
      ddsConsumer.getSubscriber().get_qos(qos);
      Assert.assertEquals(CHANGED_PARTITION_0,
            qos.value.partition.name[0]);
      Assert.assertEquals(CHANGED_PARTITION_1,
            qos.value.partition.name[1]);

      // send an exchange
      sendExchange();

      // check QoS is still changed
      ddsConsumer.getSubscriber().get_qos(qos);
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
