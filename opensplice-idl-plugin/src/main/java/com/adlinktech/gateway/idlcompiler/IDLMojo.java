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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;


/**
 * Opensplice IDL Compiler.
 */
@Mojo(name = "idl-compile",
      defaultPhase = LifecyclePhase.GENERATE_SOURCES,
      requiresDependencyResolution = ResolutionScope.TEST)
public class IDLMojo
   extends AbstractMojo
{

   /**
    * The Maven project
    * @ parameter expression="${project}"
    * @ required
    * @ readonly
    */
   @Parameter(defaultValue = "${project}", required = true, readonly = true)
   private MavenProject project;

   /**
    * Location of the outputDirectory.
    * @ parameter expression="${opensplice.idlc.outDir}"
    * default-value="${project.build.directory}/generated-sources/idl"
    */
   @Parameter(property = "opensplice.idlc.outDir",
              defaultValue = "${project.build.directory}/generated-sources/id")
   private File outDir;

   /**
    * include directories
    * @ parameter
    */
   @Parameter
   private File[] includeDirs;

   /**
    * IDL Directory
    * @ parameter expression="${opensplice.idlc.idlDir}"
    * default-value="${basedir}/src/main/idl"
    */
   @Parameter(property = "opensplice.idlc.idlDir", defaultValue = "${basedir}/src/main/idl")
   private File idlDir;

   /**
    * macro definitions
    * @ parameter
    */
   @Parameter
   private String[] macros;

   /**
    * Compiler Executable
    * @ parameter expression="${opensplice.idlc}" default-value="idlpp"
    */
   @Parameter(property = "opensplice.idlc", defaultValue = "idlpp")
   private String idlc;

   /**
    * language can be java, c++ or c
    * @ parameter expression="${opensplice.idlc.lang}" default-value="java"
    */
   @Parameter(property = "opensplice.idlc.lang", defaultValue = "java")
   private String language;

   /**
    * mode can be stand-alone or corba (-S | -C)
    * @ parameter expression="${opensplice.idlc.mode}" default-value="-S"
    */
   @Parameter(property = "opensplice.idlc.mode", defaultValue = "-S")
   private String mode;

   /**
    * Process arguments
    */
   private List<String> arguments;

   /**
    * Creates the IDL compiler command and processes every IDL
    * 
    * @throws MojoExecutionException in case of error
    */
   public void execute() throws MojoExecutionException
   {
      if ( !idlDir.exists())
      {
         getLog().info(
               "IDL Directory:" + idlDir.getName() + " does not exist");
         return;
      }

      File[] idlFiles = idlDir.listFiles();
      if (idlFiles == null || idlFiles.length == 0)
      {
         getLog().info("No IDL Files found");
         return;
      }

      if ( !outDir.exists())
      {
         if ( !outDir.mkdirs())
         {
            getLog().info("Failed to create directory: " + outDir);
            return;
         }
      }

      arguments = new ArrayList<String>();

      arguments.add(getCompiler());

      arguments.add("-d");
      arguments.add(outDir.getAbsolutePath());

      arguments.add(mode);

      arguments.add("-l");
      arguments.add(language);

      if (macros != null && macros.length > 0)
      {
         for (int x = 0; x < macros.length; x++)
         {
            arguments.add("-D");
            arguments.add(macros[x]);
         }
      }

      if (includeDirs != null && includeDirs.length > 0)
      {
         for (int x = 0; x < includeDirs.length; x++)
         {
            arguments.add("-I");
            arguments.add(includeDirs[x].getAbsolutePath());
         }
      }

      boolean idlExists = false;
      IDLProcessor proc = new IDLProcessor(getLog());
      String idlFile = null;
      for (File idlf : idlFiles)
      {
         if ( ! (idlf.getName().endsWith(".idl") || idlf.getName().endsWith(
               ".IDL")))
            continue;
         idlFile = idlf.getAbsolutePath();
         arguments.add(idlFile);
         idlExists = true;
         try
         {
            proc.process(arguments);

            // add generated source dir to compile source root
            project.addCompileSourceRoot(outDir.getAbsolutePath());
         }
         catch (MojoExecutionException e)
         {
            throw e;
         }
         finally
         {
            arguments.remove(idlFile);
         }
      }

      if ( !idlExists)
      {
         getLog().info("No IDL Files found");
      }

   }

   /**
    * @return Absolute Path to the IDL Compiler
    * @throws MojoExecutionException if the OSPL_HOME environment variable is not set
    */
   protected String getCompiler() throws MojoExecutionException
   {
      Map<String, String> env = System.getenv();
      String OSPL_HOME_VAR = env.get("OSPL_HOME");
      if (OSPL_HOME_VAR == null)
         throw new MojoExecutionException("OSPL_HOME not set");
      return OSPL_HOME_VAR + File.separatorChar + "bin" + File.separatorChar
            + idlc;
   }

}
