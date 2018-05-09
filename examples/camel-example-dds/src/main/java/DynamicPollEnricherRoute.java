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
import org.apache.camel.CamelExecutionException;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.processor.aggregate.AggregationStrategy;

import com.adlinktech.gateway.camelext.DynamicPollEnricher;


public class DynamicPollEnricherRoute {

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

                    /*
                     * An example of DynamicPollEnricher:
                     * this example compute the average position between a
                     * circle and a square with same color, and publish a
                     * triangle at this place.
                     * Note that the DynamicPollEnricher is created with a timeout of 2000ms,
                     * to wait for the reception of a square.
                     */
                    DynamicPollEnricher pollEnricher =
                          new DynamicPollEnricher("dds:Square:0/ShapeType"+QOS_OPTIONS, 2000);
                    pollEnricher.setAggregationStrategy(new AggregationStrategy() {
                        public Exchange aggregate(Exchange fromCircles, Exchange fromSquares) {
                            if (fromSquares == null)
                                throw new CamelExecutionException("No square found", fromCircles);
                            ShapeType circle = (ShapeType) fromCircles.getIn().getBody();
                            ShapeType square = (ShapeType) fromSquares.getIn().getBody();
                            circle.x = (circle.x + square.x) / 2;
                            circle.y = (circle.y + square.y) / 2;
                            return fromCircles;
                        }
                    });
                    from("dds:Circle:0/ShapeType"+QOS_OPTIONS)
                    .setHeader("DDS_QUERY_EXPRESSION", constant("color=%0"))
                    .setHeader("DDS_QUERY_PARAMETERS").groovy("[request.body.color]")
                    .process(pollEnricher)
                    .to("dds:Triangle:0/ShapeType"+QOS_OPTIONS);
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

