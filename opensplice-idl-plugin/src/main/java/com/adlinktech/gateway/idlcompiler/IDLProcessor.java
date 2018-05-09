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
package com.adlinktech.gateway.idlcompiler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;


public class IDLProcessor
{

   /**
    * log reference for logging
    */
   Log log;

   protected IDLProcessor(Log lg)
   {
      log = lg;
   }

   /**
    * Invoke the idl compiler for the given IDL File
    * 
    * @param arguments
    *           command line to be passed to the process builder
    * @throws MojoExecutionException in case of error
    */
   protected void process(List<String> arguments)
      throws MojoExecutionException
   {
      BufferedReader results = null, errors = null;
      try
      {
         ProcessBuilder pb = new ProcessBuilder(arguments);
         Process p = null;
         try
         {
            log.info("Executing idl command" + arguments.toString());
            p = pb.start();

            results = new BufferedReader(new InputStreamReader(
                  p.getInputStream(), "UTF-8"));
            String s;
            boolean error = false;
            while ( (s = results.readLine()) != null)
            {
               log.info(s);
            }

            errors = new BufferedReader(new InputStreamReader(
                  p.getErrorStream(), "UTF-8"));
            while ( (s = errors.readLine()) != null)
            {
               error = true;
               log.error(s);
            }
            try
            {
               p.waitFor();
            }
            catch (InterruptedException exp)
            {
               // do nothing
            }
            if (error)
               throw new MojoExecutionException("");
         }
         catch (IOException exp)
         {
            log.error(exp);
            throw new MojoExecutionException("");
         }
      }
      finally
      {
         try
         {
            if (results != null)
               results.close();
            if (errors != null)
               errors.close();
         }
         catch (IOException exp)
         {
            log.error(exp);
            throw new MojoExecutionException("");
         }

      }
   }

}
