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

import java.io.File;

import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.project.MavenProject;

import com.adlinktech.gateway.idlcompiler.IDLMojo;


public class IDLMojoTest
   extends AbstractMojoTestCase
{

   private IDLMojo mojo;

   public void setUp() throws Exception
   {
      super.setUp();

      File pom = new File(getBasedir(), "src/test/resources/project1/pom.xml");
      mojo = (IDLMojo) lookupMojo("idl-compile", pom);
      assertNotNull("Mojo found.", mojo);

      MavenProject mavenProject = (MavenProject) getVariableValueFromObject(
            mojo, "project");
      assertNotNull(mavenProject);

   }

   /**
    * Test whether IDL file gets compiled
    */
   public void testIDLCompilation() throws Exception
   {
      mojo.execute();
      File file = new File(getBasedir(),
            "target/test-classes/durable/Message.java");
      assertTrue(file.exists());
      file = new File(getBasedir(),
            "target/test-classes/durable/Message_1.java");
      assertTrue(file.exists());

   }

   /**
    * Test injection of fields values from POM into MOJO
    */
   public void testFields() throws Exception
   {
      String[] macros = (String[]) getVariableValueFromObject(mojo, "macros");
      assertTrue(macros.length == 2);
      assertTrue(macros[0].equals("Hello"));
      assertTrue(macros[1].equals("World"));

      File[] includeDirs = (File[]) getVariableValueFromObject(mojo,
            "includeDirs");
      assertTrue(includeDirs.length == 2);
      assertTrue(includeDirs[0].getName().equals("myDir1"));
      assertTrue(includeDirs[1].getName().equals("myDir2"));

      String idlc = (String) getVariableValueFromObject(mojo, "idlc");
      assertTrue(idlc.equals("idlpp"));

      String language = (String) getVariableValueFromObject(mojo, "language");
      assertTrue(language.equals("java"));

      String mode = (String) getVariableValueFromObject(mojo, "mode");
      assertTrue(mode.equals("-S"));

      File idlDir = (File) getVariableValueFromObject(mojo, "idlDir");
      assertTrue(idlDir.getName().equals("project1"));

      File outDir = (File) getVariableValueFromObject(mojo, "outDir");
      assertTrue(outDir.getName().equals("test-classes"));

   }

}
