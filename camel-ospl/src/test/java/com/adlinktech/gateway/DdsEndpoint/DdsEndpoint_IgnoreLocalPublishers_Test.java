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


import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;

import com.adlinktech.gateway.camelospl.DdsEndpoint;

import Chat.SimpleChatMessage;


public class DdsEndpoint_IgnoreLocalPublishers_Test
   extends CamelTestSupport
{

   @Produce(uri = "direct:start")
   private ProducerTemplate pTemplate;

   @EndpointInject(uri = "dds:Chat_SimpleChatMessage:0/Chat.SimpleChatMessage")
   private DdsEndpoint ddsDefaultEndpoint;

   @EndpointInject(
                   uri = "dds:Chat_SimpleChatMessage:0/Chat.SimpleChatMessage?ignoreLocalPublishers=false")
   private DdsEndpoint ddsEndpointDontIgnoreLocalPublishers;

   @EndpointInject(uri = "mock:fromDdsDefaultEndpoint")
   private MockEndpoint fromDdsDefaultEndpoint;

   @EndpointInject(uri = "mock:fromDdsEndpointDontIgnoreLocalPublishers")
   private MockEndpoint fromDdsEndpointDontIgnoreLocalPublishers;

   @Override
   protected RouteBuilder createRouteBuilder() throws Exception
   {
      return new RouteBuilder()
      {

         @Override
         public void configure() throws Exception
         {
            // route from direct to both DDS
            from("direct:start").to(ddsDefaultEndpoint);

            // route from default DDS endpoint
            from(ddsDefaultEndpoint).to(fromDdsDefaultEndpoint);

            // route from DDS endpoint with ignoreLocalPublishers=false
            from(ddsEndpointDontIgnoreLocalPublishers).to(fromDdsEndpointDontIgnoreLocalPublishers);
         }
      };
   }

   @Test
   public void test() throws Exception
   {
      fromDdsDefaultEndpoint.expectedMessageCount(0);
      fromDdsEndpointDontIgnoreLocalPublishers.expectedMessageCount(1);

      sendData();
      Thread.sleep(100);

      fromDdsDefaultEndpoint.assertIsSatisfied();
      fromDdsEndpointDontIgnoreLocalPublishers.assertIsSatisfied();
   }


   private void sendData()
   {
      SimpleChatMessage msg = new SimpleChatMessage();
      msg.index = 1;
      msg.userID = "User1";
      msg.content = "Hello World!";
      pTemplate.sendBody(msg);
   }


}
