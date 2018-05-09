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

import java.text.DateFormat;
import java.util.concurrent.TimeoutException;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.processor.aggregate.AggregationStrategy;

public class CamelDdsPing {

	private CamelContext ctx;
	private ProducerTemplate template;

	/*
	 * Configurable parameters (through cmdline) These are the default settings
	 */
	static private String write_partition = "PING";
	static private String read_partition = "PONG";
	static private char topic_id = 's';
	static int nof_cycles = 100;
	static int nof_blocks = 20;

	static String PP_min_DDS_ENDPOINT = "dds:PP_min_topic:0/pingpong.PP_min_msg";
	static String PP_seq_DDS_ENDPOINT = "dds:PP_seq_topic:0/pingpong.PP_seq_msg";
	static String PP_string_DDS_ENDPOINT = "dds:PP_string_topic:0/pingpong.PP_string_msg";
	static String PP_fixed_DDS_ENDPOINT = "dds:PP_fixed_topic:0/pingpong.PP_fixed_msg";
	static String PP_array_DDS_ENDPOINT = "dds:PP_array_topic:0/pingpong.PP_array_msg";
	static String PP_quit_DDS_ENDPOINT = "dds:PP_quit_topic:0/pingpong.PP_quit_msg";

	String[] topic_types = { "PP_min_msg", "PP_seq_topic", "PP_string_msg",
			"PP_fixed_msg", "PP_array_msg", "PP_quit_msg" };

	String read_partition_arg;
	String write_partition_arg;

	private time roundTripTime = new time();
	private time preWriteTime = new time();
	private time postWriteTime = new time();
	private time preTakeTime = new time();
	private time postTakeTime = new time();
	private stats roundtrip = new stats("roundtrip");
	private stats write_access = new stats("write_access");
	private stats read_access = new stats("read_access");
	private long wait_timeout = 4000; // millisec

	Endpoint pongEndpoint = null;

	public CamelDdsPing() {

	}

	public void initialize() throws Exception {
		
		// create Camel context
		ctx = new DefaultCamelContext();

		// create ProducerTemplate
		template = ctx.createProducerTemplate();

		// Define the route
		ctx.addRoutes(new PingPongRouteBuilder());

		// Start Camel
		ctx.start();

	}

	private void sendMessages() {

		/*
		 * Send Initial message
		 */

		switch (topic_id) {
		case 'm': {
			/* System.out.println ("PING: sending initial ping_min"); */
			final pingpong.PP_min_msg PPdata = new pingpong.PP_min_msg();
			PPdata.count = 0;
			PPdata.block = 0;

			template.send("direct:instances", new Processor() {
				public void process(Exchange exchange) {
					exchange.getIn().setBody(PPdata);
					exchange.setProperty("BlocksLoopIndex", 0);
				}
			});
		}
			break;
		case 'q': {
			/* System.out.println ("PING: sending initial ping_seq"); */
			final pingpong.PP_seq_msg PPdata = new pingpong.PP_seq_msg();
			PPdata.count = 0;
			PPdata.block = 0;
			template.send("direct:instances", new Processor() {
				public void process(Exchange exchange) {
					exchange.getIn().setBody(PPdata);
					exchange.setProperty("BlocksLoopIndex", 0);
				}
			});
		}
			break;
		case 's': {
			/* System.out.println ("PING: sending initial ping_string"); */
			final pingpong.PP_string_msg PPdata = new pingpong.PP_string_msg();
			PPdata.count = 0;
			PPdata.block = 0;
			PPdata.a_string = "a_string";
			template.send("direct:instances", new Processor() {
				public void process(Exchange exchange) {
					exchange.getIn().setBody(PPdata);
					exchange.setProperty("BlocksLoopIndex", 0);
				}
			});

		}
			break;
		case 'f': {
			/* System.out.println ("PING: sending initial ping_fixed"); */
			final pingpong.PP_fixed_msg PPdata = new pingpong.PP_fixed_msg();
			PPdata.count = 0;
			PPdata.block = 0;
			template.send("direct:instances", new Processor() {
				public void process(Exchange exchange) {
					exchange.getIn().setBody(PPdata);
					exchange.setProperty("BlocksLoopIndex", 0);
				}
			});

		}
			break;
		case 'a': {
			/* System.out.println ("PING: sending initial ping_array"); */
			final pingpong.PP_array_msg PPdata = new pingpong.PP_array_msg();
			PPdata.count = 0;
			PPdata.block = 0;
			PPdata.str_arr_char = new char[10];
			PPdata.str_arr_octet = new byte[10];
			PPdata.str_arr_short = new short[10];
			PPdata.str_arr_ushort = new short[10];
			PPdata.str_arr_long = new int[10];
			PPdata.str_arr_ulong = new int[10];
			PPdata.str_arr_longlong = new long[10];
			PPdata.str_arr_ulonglong = new long[10];
			PPdata.str_arr_float = new float[10];
			PPdata.str_arr_double = new double[10];
			PPdata.str_arr_boolean = new boolean[11];
			template.send("direct:instances", new Processor() {
				public void process(Exchange exchange) {
					exchange.getIn().setBody(PPdata);
					exchange.setProperty("BlocksLoopIndex", 0);
				}
			});

		}
			break;
		case 't': {
			/* System.out.println ("PING: sending initial ping_quit"); */
			final pingpong.PP_quit_msg PPdata = new pingpong.PP_quit_msg();
			PPdata.quit = true;
			template.send("direct:instances", new Processor() {
				public void process(Exchange exchange) {
					exchange.getIn().setBody(PPdata);
					exchange.setProperty("BlocksLoopIndex", 0);
				}
			});
		}
			break;
		default:
			System.out.println("Invalid topic-id");
			return;
		}

	}

	private static void print_formatted(int width, int value) {
		String val = java.lang.Integer.toString(value);
		int i;

		for (i = 0; i < (width - val.length()); i++) {
			System.out.print(" ");
		}
		System.out.print(val);
	}

	private static void print_formatted(int width, long value) {
		String val = java.lang.Long.toString(value);
		int i;

		for (i = 0; i < (width - val.length()); i++) {
			System.out.print(" ");
		}
		System.out.print(val);
	}

	// Stop Camel context and ProducerTemplate
	public void stop() {
		try {
			if (template != null) {
				template.stop();
			}
			ctx.stop();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	public static void main(String[] args) {

		/*
		 * Evaluate cmdline arguments
		 */
		if (args.length != 0) {
			if (args.length != 5) {
				System.out.println("Invalid.....");
				System.out
						.println("Usage: java ping [blocks blocksize topic_id WRITE_PARTITION READ_PARTITION]");
				return;
			}

			try {
				nof_blocks = java.lang.Integer.parseInt(args[0]);
				nof_cycles = java.lang.Integer.parseInt(args[1]);
				topic_id = args[2].charAt(0);
				write_partition = args[3];
				read_partition = args[4];
			} catch (Exception e) {
				System.out.println("Invalid.....");
				System.out
						.println("Usage: java ping [blocks blocksize topic_id WRITE_PARTITION READ_PARTITION]");
				return;
			}
		}

		// Create CamelDdsPing and initialize it
		CamelDdsPing pinger = new CamelDdsPing();
		try {
			pinger.initialize();
			pinger.sendMessages();
		} catch (Exception exp) {
			exp.printStackTrace();
			return;
		} finally {
			pinger.stop();
		}

	}
	
	class PingPongRouteBuilder extends RouteBuilder {
	
		public void configure() {

			read_partition_arg = "?Partition=" + read_partition;
			write_partition_arg = "?Partition=" + write_partition;
			
			onException(TimeoutException.class).process(new Processor() {
				
				@Override
				public void process(Exchange e) throws Exception {
					Throwable caused = e.getProperty(Exchange.EXCEPTION_CAUGHT, Throwable.class);
					System.out.println(caused.getMessage());
				}
			});
			
			switch (topic_id) {
			case 's': configure_string_msg_route();
								break;
			case 'q': configure_seq_msg_route();
				break;
				
			case 'm': configure_min_msg_route();
				break;
				
			case 'f': configure_fixed_msg_route();
				break;
				
			case 'a': configure_array_msg_route();
				break;
				
			case 't': configure_quit_msg_route();
				break;
				
			default:
				System.out.println("Invalid topic-id");
			}
		}

		private void configure_quit_msg_route() {
			from("direct:instances")
			.bean(preWriteTime, "timeGet")
			.to(PP_quit_DDS_ENDPOINT + write_partition_arg)  //sending the quit message
			.bean(postWriteTime, "timeGet")
			.stop();
			
		}

		private void configure_array_msg_route() {
			from("direct:instances")
			.process(new StatHeaderDisplayer())
			.loop(nof_blocks) // looping over the instances
				.process(new Processor() { //setting the first instance sample
	
					@Override
					public void process(Exchange e)
							throws Exception {
						pingpong.PP_array_msg PPdata = (pingpong.PP_array_msg) e
								.getIn().getBody();
						PPdata.count = 0;
						PPdata.block = Integer.parseInt(e
								.getProperty("BlocksLoopIndex")
								.toString());
						e.setProperty("BlocksLoopIndex", PPdata.block + 1);
					}
				})
				.loop(nof_cycles) //looping over over each instance samples
					.bean(preWriteTime, "timeGet")
					.to(PP_array_DDS_ENDPOINT + write_partition_arg)  //sending the ping message
					.bean(postWriteTime, "timeGet")
					
					.process(new Processor() { // setting the instance next sample	
		
						@Override
						public void process(Exchange e)
								throws Exception {
							
							// increment the count
							pingpong.PP_array_msg PPdata = (pingpong.PP_array_msg) e
									.getIn().getBody();
							PPdata.count ++;
						}
					})
					.bean(preTakeTime, "timeGet")
	            .doTry()
   				   .pollEnrich(
   					   PP_array_DDS_ENDPOINT + read_partition_arg,
   						wait_timeout, new PingPongAggregationStrategy())	// waiting for the pong message
                  .bean(postTakeTime, "timeGet")
               .doCatch(TimeoutException.class)
                  .log("TIMEOUT: didn't receive pong!")
               .end()
					.process(new StatProcessor())
				
				.end() // end of the nof_cycles loop
				.process(new StatDisplayer())
			.end();
			
		}

		private void configure_fixed_msg_route() {
			from("direct:instances")
			.process(new StatHeaderDisplayer())
			.loop(nof_blocks) // looping over the instances
				.process(new Processor() { //setting the first instance sample
	
					@Override
					public void process(Exchange e)
							throws Exception {
						pingpong.PP_fixed_msg PPdata = (pingpong.PP_fixed_msg) e
								.getIn().getBody();
						PPdata.count = 0;
						PPdata.block = Integer.parseInt(e
								.getProperty("BlocksLoopIndex")
								.toString());
						e.setProperty("BlocksLoopIndex", PPdata.block + 1);
					}
				})
				.loop(nof_cycles) //looping over over each instance samples
					.bean(preWriteTime, "timeGet")
					.to(PP_fixed_DDS_ENDPOINT + write_partition_arg)  //sending the ping message
					.bean(postWriteTime, "timeGet")
					
					.process(new Processor() { // setting the instance next sample	
		
						@Override
						public void process(Exchange e)
								throws Exception {
							
							// increment the count
							pingpong.PP_fixed_msg PPdata = (pingpong.PP_fixed_msg) e
									.getIn().getBody();
							PPdata.count ++;
						}
					})
					.bean(preTakeTime, "timeGet")
					.doTry()
   					.pollEnrich(
							PP_fixed_DDS_ENDPOINT + read_partition_arg,
							wait_timeout, new PingPongAggregationStrategy())	// waiting for the pong message
		            .bean(postTakeTime, "timeGet")
               .doCatch(TimeoutException.class)
                  .log("TIMEOUT: didn't receive pong!")
               .end()
					.process(new StatProcessor())
				
				.end() // end of the nof_cycles loop
				.process(new StatDisplayer())
			.end();
			
		}

		private void configure_min_msg_route() {
			from("direct:instances")
			.process(new StatHeaderDisplayer())
			.loop(nof_blocks) // looping over the instances
				.process(new Processor() { //setting the first instance sample
	
					@Override
					public void process(Exchange e)
							throws Exception {
						pingpong.PP_min_msg PPdata = (pingpong.PP_min_msg) e
								.getIn().getBody();
						PPdata.count = 0;
						PPdata.block = Integer.parseInt(e
								.getProperty("BlocksLoopIndex")
								.toString());
						e.setProperty("BlocksLoopIndex", PPdata.block + 1);
					}
				})
				.loop(nof_cycles) //looping over over each instance samples
					.bean(preWriteTime, "timeGet")
					.to(PP_min_DDS_ENDPOINT + write_partition_arg)  //sending the ping message
					.bean(postWriteTime, "timeGet")
					
					.process(new Processor() { // setting the instance next sample	
		
						@Override
						public void process(Exchange e)
								throws Exception {
							
							// increment the count
							pingpong.PP_min_msg PPdata = (pingpong.PP_min_msg) e
									.getIn().getBody();
							PPdata.count ++;
						}
					})
					.bean(preTakeTime, "timeGet")
					.doTry()
					   .pollEnrich(
							PP_min_DDS_ENDPOINT + read_partition_arg,
							wait_timeout, new PingPongAggregationStrategy())	// waiting for the pong message
		            .bean(postTakeTime, "timeGet")
               .doCatch(TimeoutException.class)
                  .log("TIMEOUT: didn't receive pong!")
               .end()
					.process(new StatProcessor())
				
				.end() // end of the nof_cycles loop
				.process(new StatDisplayer())
			.end();
			
		}

		private void configure_seq_msg_route() {
			from("direct:instances")
			.process(new StatHeaderDisplayer())
			.loop(nof_blocks) // looping over the instances
				.process(new Processor() { //setting the first instance sample
	
					@Override
					public void process(Exchange e)
							throws Exception {
						pingpong.PP_seq_msg PPdata = (pingpong.PP_seq_msg) e
								.getIn().getBody();
						PPdata.count = 0;
						PPdata.block = Integer.parseInt(e
								.getProperty("BlocksLoopIndex")
								.toString());
						e.setProperty("BlocksLoopIndex", PPdata.block + 1);
					}
				})
				.loop(nof_cycles) //looping over over each instance samples
					.bean(preWriteTime, "timeGet")
					.to(PP_seq_DDS_ENDPOINT + write_partition_arg)  //sending the ping message
					.bean(postWriteTime, "timeGet")
					
					.process(new Processor() { // setting the instance next sample	
		
						@Override
						public void process(Exchange e)
								throws Exception {
							
							// increment the count
							pingpong.PP_seq_msg PPdata = (pingpong.PP_seq_msg) e
									.getIn().getBody();
							PPdata.count ++;
						}
					})
					.bean(preTakeTime, "timeGet")
					.doTry()
					   .pollEnrich(
							PP_seq_DDS_ENDPOINT + read_partition_arg,
							wait_timeout, new PingPongAggregationStrategy())	// waiting for the pong message
		            .bean(postTakeTime, "timeGet")
               .doCatch(TimeoutException.class)
                  .log("TIMEOUT: didn't receive pong!")
               .end()
					.process(new StatProcessor())
				
				.end() // end of the nof_cycles loop
				.process(new StatDisplayer())
			.end();
			
		}

		private void configure_string_msg_route() {
			from("direct:instances")
			.process(new StatHeaderDisplayer())
			.loop(nof_blocks) // looping over the instances
				.process(new Processor() { //setting the first instance sample
	
					@Override
					public void process(Exchange e)
							throws Exception {
						pingpong.PP_string_msg PPdata = (pingpong.PP_string_msg) e
								.getIn().getBody();
						PPdata.count = 0;
						PPdata.block = Integer.parseInt(e
								.getProperty("BlocksLoopIndex")
								.toString());
						e.setProperty("BlocksLoopIndex", PPdata.block + 1);
					}
				})
				.loop(nof_cycles) //looping over over each instance samples
					.bean(preWriteTime, "timeGet")
					.to(PP_string_DDS_ENDPOINT + write_partition_arg)  //sending the ping message
					.bean(postWriteTime, "timeGet")
					.process(new Processor() { // setting the instance next sample	
		
						@Override
						public void process(Exchange e)
								throws Exception {
							
							// increment the count
							pingpong.PP_string_msg PPdata = (pingpong.PP_string_msg) e
									.getIn().getBody();
							PPdata.count ++;
						}
					})
					.bean(preTakeTime, "timeGet")
					.doTry()
					   .pollEnrich(
							PP_string_DDS_ENDPOINT + read_partition_arg,
							wait_timeout, new PingPongAggregationStrategy())	// waiting for the pong message
	               .bean(postTakeTime, "timeGet")
		         .doCatch(TimeoutException.class)
		            .log("TIMEOUT: didn't receive pong!")
		         .end()
					.process(new StatProcessor())
				
				.end() // end of the nof_cycles loop
				.process(new StatDisplayer())
			.end();
			
		}
	}	
	
	class PingPongAggregationStrategy implements AggregationStrategy{
		
		public Exchange aggregate(Exchange oldEx, Exchange newEx) {
        	if(newEx == null)
        		oldEx.setException(new TimeoutException("PING: TIMEOUT - message lost or not available"));
        	
        	return oldEx;
		}
	}
	
	
	class StatHeaderDisplayer implements Processor {
		
		public void process(Exchange e) throws Exception {
			System.out
					.println("# PING PONG measurements (in us)");
			System.out.print("# Executed at: ");
			System.out.println(DateFormat
					.getDateTimeInstance().format(
							new java.util.Date()));
			System.out
					.println("#           Roundtrip time [us]             Write-access time [us]          Read-access time [us]");
			System.out
					.println("# Block     Count   mean    min    max      Count   mean    min    max      Count   mean    min    max");

		}
	}
	
	class StatProcessor implements Processor {
		
		public void process(Exchange e) throws Exception {
			roundTripTime.set(preWriteTime.get());
			write_access.add_stats(postWriteTime.sub(preWriteTime));
			read_access.add_stats(postTakeTime.sub(preTakeTime));
			roundtrip.add_stats(postTakeTime.sub(roundTripTime));
		}
	}
	
	class StatDisplayer implements Processor {
		public void process(Exchange e) throws Exception {
			int block = Integer.parseInt(e.getProperty("BlocksLoopIndex")
					.toString()) - 1;
			print_formatted(6, block);
			System.out.print(" ");
			print_formatted(10, roundtrip.count);
			System.out.print(" ");
			print_formatted(6, roundtrip.average);
			System.out.print(" ");
			print_formatted(6, roundtrip.min);
			System.out.print(" ");
			print_formatted(6, roundtrip.max);
			System.out.print(" ");
			print_formatted(10, write_access.count);
			System.out.print(" ");
			print_formatted(6, write_access.average);
			System.out.print(" ");
			print_formatted(6, write_access.min);
			System.out.print(" ");
			print_formatted(6, write_access.max);
			System.out.print(" ");
			print_formatted(10, read_access.count);
			System.out.print(" ");
			print_formatted(6, read_access.average);
			System.out.print(" ");
			print_formatted(6, read_access.min);
			System.out.print(" ");
			print_formatted(6, read_access.max);
			System.out.println();
			write_access.init_stats();
			read_access.init_stats();
			roundtrip.init_stats();

		}

	}
	
	
}
