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
package com.adlinktech.gateway.camelospl.camelmessage;

import org.apache.camel.Processor;

import com.adlinktech.gateway.camelospl.DdsConsumer;


/**
 * A {@link org.apache.camel.Consumer Consumer} which listens to DDS data
 * in "Camel Messages mode".
 */
public class CamelDdsConsumer
   extends DdsConsumer
{

   // NOTE: nothing different from a DdsConsumer, since
   // DDS DataType is forced to CamelMessage in CamelDdsEndpoint,
   // and appropriate Exchange creation is done in overriding method:
   // CamelDdsEndpoint.createExchange(sample, info)

   /**
    * Constructor.
    * 
    * @param endpoint the parent Endpoint
    * @param processor the first Processor to send exchanges
    */
   public CamelDdsConsumer(CamelDdsEndpoint endpoint, Processor processor)
   {
      super(endpoint, processor);
   }

}
