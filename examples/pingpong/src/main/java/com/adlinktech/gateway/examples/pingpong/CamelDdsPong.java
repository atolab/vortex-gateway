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
package com.adlinktech.gateway.examples.pingpong;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;


public class CamelDdsPong {

   
    private CamelContext ctx;
    private ProducerTemplate template;
    
    /*
     * Configurable parameters (through cmdline)
     * These are the default settings
     */
    static private String write_partition = "PONG";
    static private String read_partition  = "PING";
    
   static String  PP_min_DDS_ENDPOINT = "dds:PP_min_topic:0/pingpong.PP_min_msg";
   static String  PP_seq_DDS_ENDPOINT = "dds:PP_seq_topic:0/pingpong.PP_seq_msg";
   static String  PP_string_DDS_ENDPOINT = "dds:PP_string_topic:0/pingpong.PP_string_msg";
   static String  PP_fixed_DDS_ENDPOINT = "dds:PP_fixed_topic:0/pingpong.PP_fixed_msg";
   static String  PP_array_DDS_ENDPOINT = "dds:PP_array_topic:0/pingpong.PP_array_msg";
   static String  PP_quit_DDS_ENDPOINT = "dds:PP_quit_topic:0/pingpong.PP_quit_msg";

   String[] topic_types = {"PP_min_msg","PP_seq_topic", "PP_string_msg", "PP_fixed_msg","PP_array_msg","PP_quit_msg"};
   
   
    public CamelDdsPong() {

    }


    public void initialize() throws Exception {

        // create Camel context
        ctx = new DefaultCamelContext();

        // create ProducerTemplate
        template = ctx.createProducerTemplate();


        final String read_partition_arg = "?Partition=" + read_partition;
        final String write_partition_arg = "?Partition=" + write_partition;


        // Define the route
        ctx.addRoutes(new RouteBuilder() {
            public void configure() {
                // from DDS endpoint to a Processor displaying the received message.
            	
                from(PP_min_DDS_ENDPOINT + read_partition_arg)
                .to(PP_min_DDS_ENDPOINT + write_partition_arg);
                
                from(PP_seq_DDS_ENDPOINT + read_partition_arg)
                .to(PP_seq_DDS_ENDPOINT + write_partition_arg);
                
                from(PP_string_DDS_ENDPOINT + read_partition_arg)
                .to(PP_string_DDS_ENDPOINT + write_partition_arg);
                
                from(PP_fixed_DDS_ENDPOINT + read_partition_arg)
                .to(PP_fixed_DDS_ENDPOINT + write_partition_arg);
                
                from(PP_array_DDS_ENDPOINT + read_partition_arg)
                .to(PP_array_DDS_ENDPOINT + write_partition_arg);
                
                from(PP_quit_DDS_ENDPOINT + read_partition_arg)
                .process(new Processor() {
                    public void process(Exchange e) {
                        System.out.println("Bye ...");
                        stop();
                    }
                });
            }
        });

        // Start Camel
        ctx.start();

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
    	
    	if (args.length != 0) {
            if (args.length != 2) {
                System.out.println ("Invalid arguments !");
                System.out.println ("Arguments required : READ_PARTITION WRITE_PARTITION");
                return;
            }
            read_partition  = args[0];
            write_partition = args[1];
        }

        // Create CamelDdsPong and initialize it
        CamelDdsPong ponger = new CamelDdsPong();
        try {
            ponger.initialize();
        } catch(Exception exp) {
            exp.printStackTrace();
            return;
        }
        System.out.println("Ready to pong");
        try {
        	InputStreamReader isr = new InputStreamReader(System.in);
            BufferedReader br = new BufferedReader(isr);
            while(true) {
                // The user has the hand to write and send a message
                String s = br.readLine();
                // If the user types "quit" then he leaves
                if(s.equalsIgnoreCase("quit")) {
                    ponger.stop();
                    break;
                }
                System.out.println("type 'quit' to exit ...");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


    }
}