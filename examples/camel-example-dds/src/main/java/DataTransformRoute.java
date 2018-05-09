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
import org.apache.camel.impl.DefaultCamelContext;


public class DataTransformRoute {

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

    /*
     * Inner class for ShapeType conversions
     */
    public static class ShapeConverter {
        // convert to simple String
        public String toString(ShapeType s) {
            return s.color + " shape at (" + s.x + "," + s.y +")";
        }

        // convert to XML String
        public String toXML(ShapeType s) {
            return "<color>" + s.color + "</color>\n" +
                   "<size>" + s.shapesize + "</size>\n" +
                   "<x>" + s.x + "</x>\n" +
                   "<y>" + s.y + "</y>\n";
        }
    }

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
                     * Case 1. A route with type transformation,
                     * modifying shape's coordinates and color
                     */
                    from("dds:Circle:0/ShapeType"+QOS_OPTIONS)
                    .process(new Processor() {
                        public void process(Exchange e) throws Exception {
                            ShapeType s = (ShapeType) e.getIn().getBody();
                            s.x += 20;
                            s.y += 20;
                            s.color = "ORANGE";
                            e.getOut().setBody(s);
                        }
                    })
                    .to("dds:Square:0/ShapeType"+QOS_OPTIONS);

                    /*
                    * Case 2. A route with type transformation,
                    * converting ShapeType as a String and print it on stdout
                    */
                   from("dds:Triangle:0/ShapeType"+QOS_OPTIONS)
                   .bean(ShapeConverter.class, "toString")
                   .to("stream:out");
                    
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

