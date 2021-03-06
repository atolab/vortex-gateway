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
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dataformat.xstream.JsonDataFormat;
import org.apache.camel.impl.DefaultCamelContext;



public class ShapesToRest {

    /*
     * QoS set to be compatible with the OpenSplice iShapes demo
     */
    private static String QOS_OPTIONS =
            "?topicDurabilityKind=PERSISTENT"
            + "&topicDurabilityServiceDepth=100"
            + "&topicDurabilityServiceCleanupDelay=3600"
            + "&topicDurabilityServiceKind=KEEP_LAST"
            + "&topicDurabilityServiceMaxInstances=4196"
            + "&topicDurabilityServiceMaxSamples=8192"
            + "&topicDurabilityServiceMaxSamplesPerInstance=8192"
            + "&durabilityKind=VOLATILE";

    public static void main(String[] args) {

        // create Camel context
        final CamelContext ctx = new DefaultCamelContext();

        // add Routes
        try {

            ctx.addRoutes( new RouteBuilder() {

                @Override
                public void configure() throws Exception {

                    // Define your route from DDS as following:
                    //   from("dds:<topicName>:<domainId>/<topicType>[?<options>]")
                    //     .to("...")
                    // replacing <topicName> by your the topic name to subscribe
                    //		 <domainId>  domain id for the DDS global data space (must be an integer)
                    //           <topicType> by the java type of the topic (class generated from yout IDL)
                    //           [?<option>] by Gateway options if you want some (see User Guide)


                    // Route for index.html URL
                    from("restlet:http://localhost:4444/index.html")
                    .pollEnrich("file:target/classes/html?fileName=index.html&noop=true", 10000)
                    .setHeader(Exchange.CONTENT_TYPE).constant(org.restlet.data.MediaType.TEXT_HTML);

                    // Route for raphael.js URL (Graphical JavaScript library)
                    from("restlet:http://localhost:4444/raphael.js")
                    .pollEnrich("file:target/classes/html?fileName=raphael.js&noop=true", 10000)
                    .setHeader(Exchange.CONTENT_TYPE).constant(org.restlet.data.MediaType.TEXT_JAVASCRIPT);

                    // Route to poll shapes (with 10ms timeout) via a REST URL
                    // The shapes are returned as Json objects
                    // Note: the "Access-Control-Allow-Origin" header in the reply is required by Chrome
                    from("restlet:http://localhost:4444/rest/{shape}")
                    .choice()
                        .when(header("shape").isEqualTo("circle"))
                            .pollEnrich("dds:Circle:0/ShapeType"+QOS_OPTIONS, 10)
                            .marshal(new JsonDataFormat())
                            .setHeader("Access-Control-Allow-Origin", constant("*"))
                        .when(header("shape").isEqualTo("square"))
                            .pollEnrich("dds:Square:0/ShapeType"+QOS_OPTIONS, 10)
                            .marshal(new JsonDataFormat())
                            .setHeader("Access-Control-Allow-Origin", constant("*"))
                        .when(header("shape").isEqualTo("triangle"))
                            .pollEnrich("dds:Triangle:0/ShapeType"+QOS_OPTIONS, 10)
                            .marshal(new JsonDataFormat())
                            .setHeader("Access-Control-Allow-Origin", constant("*"))
                        .otherwise()
                            .throwException(new Exception("Unknown Shape!"));

                }
            });

            // start Camel engine and routes
            ctx.start();

            Thread.currentThread().join();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}

