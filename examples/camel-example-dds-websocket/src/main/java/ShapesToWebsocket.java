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


import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.websocket.WebsocketComponent;
import org.apache.camel.dataformat.xstream.JsonDataFormat;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.spi.DataType;


public class ShapesToWebsocket
{

   /*
    * QoS set to be compatible with the OpenSplice iShapes demo
    */
   private static String QOS_OPTIONS = "?topicDurabilityKind=PERSISTENT"
         + "&topicDurabilityServiceDepth=100"
         + "&topicDurabilityServiceCleanupDelay=3600"
         + "&topicDurabilityServiceKind=KEEP_LAST"
         + "&topicDurabilityServiceMaxInstances=4196"
         + "&topicDurabilityServiceMaxSamples=8192"
         + "&topicDurabilityServiceMaxSamplesPerInstance=8192"
         + "&durabilityKind=VOLATILE";

   public static void main(String[] args)
   {

      // create Camel context
      final CamelContext ctx = new DefaultCamelContext();

      // starting from Camel 2.18: MinThreads/MaxThreads or threadPool fields
      // are required due to switch to Jetty 9.
      // But the maxThreads value is not big enough...
      ctx.getComponent("websocket", WebsocketComponent.class).setMaxThreads(12);
      
      // add Routes
      try
      {

         ctx.addRoutes(new RouteBuilder()
         {

            @Override
            public void configure() throws Exception
            {

               // Define your route from DDS as following:
               // from("dds:<topicName>:<domainId>/<topicType>[?<options>]")
               // .to("...")
               // replacing <topicName> by your the topic name to subscribe
               // <domainId> domain id for the DDS global data space (must be an integer)
               // <topicType> by the java type of the topic (class generated from yout IDL)
               // [?<option>] by Gateway options if you want some (see User Guide)


               // poll all DDS ishapes with ShapeType Circle
               from("dds:Circle:0/ShapeType" + QOS_OPTIONS)
                     .process(new Processor()
                     {
                        @Override
                        public void process(Exchange exchange) throws Exception
                        {
                           ShapeType shape = exchange.getIn().getBody(ShapeType.class);
                           StringBuffer json = new StringBuffer();
                           json.append("{ShapeType: color: " + shape.color + ", x: " + shape.x
                                 + ", y: " + shape.y + ", shapesize: " + shape.shapesize + " }");
                           exchange.getOut().setBody(json);
                        }
                     })
                     // and push ishapes to all web socket subscribers on circle
                     .to("websocket://circle?sendToAll=true");

               // poll all DDS ishapes with ShapeType Square
               from("dds:Square:0/ShapeType" + QOS_OPTIONS)
                     .process(new Processor()
                     {
                        @Override
                        public void process(Exchange exchange) throws Exception
                        {
                           ShapeType shape = exchange.getIn().getBody(ShapeType.class);
                           StringBuffer json = new StringBuffer();
                           json.append("{ShapeType: color: " + shape.color + ", x: " + shape.x
                                 + ", y: " + shape.y + ", shapesize: " + shape.shapesize + " }");
                           exchange.getOut().setBody(json);
                        }
                     })
                     // and push ishapes to all web socket subscribers on square
                     .to("websocket://square?sendToAll=true");

               // poll all DDS ishapes with ShapeType Triangle
               from("dds:Triangle:0/ShapeType" + QOS_OPTIONS)
                     .process(new Processor()
                     {
                        @Override
                        public void process(Exchange exchange) throws Exception
                        {
                           ShapeType shape = exchange.getIn().getBody(ShapeType.class);
                           StringBuffer json = new StringBuffer();
                           json.append("{ShapeType: color: " + shape.color + ", x: " + shape.x
                                 + ", y: " + shape.y + ", shapesize: " + shape.shapesize + " }");
                           exchange.getOut().setBody(json);
                        }
                     })
                     // and push ishapes to all web socket subscribers on square
                     .to("websocket://triangle?sendToAll=true");

               System.out.println("To test open the file://" + System.getProperty("user.dir")
                     + "/target/classes/html/index.html in a web browser.");
            }
         });

         // start Camel engine and routes
         ctx.start();

         Thread.currentThread().join();

      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
   }
}
