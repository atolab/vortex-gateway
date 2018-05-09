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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import org.junit.Test;


/**
 * This class is a trick to run "ospl start" before all other unit tests.
 * (see in pom.xml: surefire is configured with runOrder='alphabetical')
 * This is required with OpenSpliceDDS v5.x and v6.x not configured in SingleProcess.
 */
public class AAA_OsplStart_BeforeTest
{


   @Test
   public void doOsplStart()
   {
      System.out.println("==== ospl start ====");

      ProcessBuilder pb = new ProcessBuilder("ospl", "start");
      pb.redirectErrorStream(true);
      try
      {
         Process p = pb.start();
         BufferedReader outReader = new BufferedReader(
               new InputStreamReader(p.getInputStream(), Charset.defaultCharset()));
         p.waitFor();
         while (outReader.ready())
         {
            System.out.println(outReader.readLine());
         }
      }
      catch (IOException e)
      {
         e.printStackTrace();
      }
      catch (InterruptedException e)
      {
         e.printStackTrace();
      }

   }

}
