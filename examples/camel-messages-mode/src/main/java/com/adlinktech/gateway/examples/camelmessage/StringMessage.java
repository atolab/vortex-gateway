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
package com.adlinktech.gateway.examples.camelmessage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;


public class StringMessage {

    private CamelContext ctx;
    private ProducerTemplate template;
    private Endpoint endpoint;

    public void initialize() throws Exception{
        // create Camel context
        ctx = new DefaultCamelContext();

        // create a ProducerTemplate
        template = ctx.createProducerTemplate();

        // URI of DDS endpoint using "ExampleStrTopic" as topic and "0" as domain.
        // No topic class is set (before '?') meaning that the Camel Messages mode for serializable data is used.
        // The "target" option is set to "stringTarget" as a filtering option.
        // The "ignoreLocalPublishers" option is set to "false" for this process to receive its own publications
        // (and thus to display the message that was just sent).
        final String fromURI = "dds:ExampleStrTopic:0/?target=stringTarget&ignoreLocalPublishers=false";
        endpoint = ctx.getEndpoint(fromURI);

        // Define the route from DDS endpoint to a Processor displaying the received data.
        ctx.addRoutes(new RouteBuilder() {
            public void configure() {
                from(fromURI)
                .process(new Processor() {
                    public void process(Exchange e) {
                        displayExchange(e);
                    }
                });
            }
        });

        // Start Camel
        ctx.start();

        System.out.println("Type quit to terminate \n");
        System.out.println("Enter String:");
    }

    // Display received message
    private static void displayExchange(Exchange e) {
        String recd = (String)e.getIn().getBody();
        System.out.println("Received:" + recd);
        System.out.println("\nEnter String:");
    }

    // Send a simple string message to DDS endpoint through CamelOs
    public void sendMessage(String message) {
        final String msg = message;
        // use ProducerTemplate to send Exchange to DDS endpoint
        template.send(endpoint, new Processor() {
            public void process(Exchange exchange) {
                exchange.getIn().setBody(msg);
            }
        });
    }

    // Stop Camel context and ProducerTemplate
    public void stop() {
        try {
            if(template != null) {
                template.stop();
            }
            ctx.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main (String[] args) {

        // Initialization
        StringMessage msg = new StringMessage();
        try {
            msg.initialize();
        } catch(Exception exp) {
            exp.printStackTrace();
        }

        InputStreamReader isr = new InputStreamReader(System.in);
        BufferedReader br = new BufferedReader(isr);

        try {
            while(true) {
                // The user has the hand to write and send a message
                String s = br.readLine();
                // If the user types "quit" then he leaves
                if(s.equalsIgnoreCase("quit")) {
                    msg.stop();
                    break;
                } else {
                    msg.sendMessage(s);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

}
