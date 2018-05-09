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
package com.adlinktech.gateway.camelospl.test.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;


/**
 * Utility class to get and check the OpenSpliceDDS version (executing "ospl -v" command).
 */
public class OsplVersionChecker
{

   /**
    * Retrieve the version of OpenSpliceDDS configured in actual environment.
    * This operation executes the "ospl -v" version and parse the result.
    * 
    * @return the OpenSpliceDDS version number or null if version cannot be determined
    */
   public static String getOsplVersion()
   {
      String version = null;

      ProcessBuilder pb = (new ProcessBuilder("ospl", "-v")).redirectErrorStream(true);

      Process proc;
      try
      {
         proc = pb.start();


         BufferedReader outReader = new BufferedReader(
               new InputStreamReader(proc.getInputStream(), "UTF-8"));

         proc.waitFor();

         Pattern p = Pattern.compile("OpenSplice(.*) version : V(.*)(.*)");
         while (outReader.ready())
         {
            String out = outReader.readLine();
            if (out != null)
            {
               Matcher m = p.matcher(out);
               if (m.matches())
               {
                  version = m.group(2);
                  break;
               }
            }
         }
      }
      catch (IOException e)
      {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }
      catch (InterruptedException e)
      {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }


      return version;
   }

   /**
    * Compare the version of OpenSpliceDDS configured in actual environment
    * with a version number passed as parameter, and return true if
    * OpenSpliceDDS version is older than the specified version.
    * 
    * @param version the version number to compare with
    * @return true if OpenSpliceDDS is older then the specified version
    */
   public static boolean isOsplOlderThan(String version)
   {
      String v = getOsplVersion();

      // Assume it's an old OpenSpliceDDS version without "ospl -v" option
      // (older than v5.1.0)
      if (v == null)
         return true;

      DefaultArtifactVersion actualVersion =
            new DefaultArtifactVersion(v);
      DefaultArtifactVersion otherVersion =
            new DefaultArtifactVersion(version);

      return actualVersion.compareTo(otherVersion) < 0;

   }

}
