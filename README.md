
# Vortex Gateway

 [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

## Introduction

 Vortex Gateway is an [Apache Camel](http://camel.apache.org/) Component for [DDS](http://portals.omg.org/dds/)
 using [OpenSplice DDS](https://github.com/ADLINK-IST/opensplice) (community or commercial versions).

 By leveraging Apache Camel, Vortex Gateway allows the integration of DDS-based systems with hundereds of
 transports or messaging technologies supported by Camel. A complete list of the components provided with Apache Camel
 is available here: http://camel.apache.org/components.html .

## Prerequisites

### Java

 Apache Camel is a Java software. Please be sure to have a JDK installed (version 7 minimum).

### OpenSplice DDS

 OpenSplice DDS must be installed and available in your environement (i.e. its *release.com* must have been sourced).
 Note that Vortex Gateway supports any v5+ or v6+ version of OpenSplice DDS.

### Apache Maven

 Vortex Gateway is available on Maven Central repo:

 The examples we provide here are using [Apache Maven](http://maven.apache.org/).
 Please be sure to install it before compiling and running the examples.


## Usage

### DDS Camel Component usage

The complete documentation of the camel-ospl component (including all the endpoint options) can is here:
[camel-ospl/README.adoc](camel-ospl/README.adoc)

### Maven project configuration

You can use the provided Maven archetype (*gateway-archetype-camel-ospl*) to quickly generate a Maven project
configured with Vortex Gateway usage and with a Java class to be completed with your Camel routes definition.

It's usage is decribed here: [gateway-archetype-camel-ospl/README.adoc](gateway-archetype-camel-ospl/README.adoc)

Otherwise, just add the following dependencies to your Maven POM file:

```XML
   <dependencies>
      <!-- Gateway's Camel OpensSpliceDDS endpoint -->
      <dependency>
         <groupId>com.adlinktech.gateway</groupId>
         <artifactId>camel-ospl</artifactId>
         <version>${gateway-version}</version>
      </dependency>

      <!-- Vortex OpenSplice -->
      <dependency>
         <groupId>org.opensplice</groupId>
         <artifactId>dcpssaj</artifactId>
         <version>${opensplice-version}</version>
         <scope>system</scope>
         <!-- NOTE: leading '/' in following <systemPath> is a workaround
              for a Maven strange issue: without this '/' compilation of an individual
              example leads to an error message complaining that opensplice-idl-plugin's
              POM is not valid since managed dependency org.opensplice.dcpssaj has a non-absolute
              systemPath -->
         <systemPath>/${env.OSPL_HOME}/jar/dcpssaj.jar</systemPath>
      </dependency>
   </dependencies>
```

Note with such dependencies, OpenSplice's *dcpssaj.jar* doesn't require to be installed in your Maven repository.
You just need to ensure that the OpenSplice's *release.com* is sourced in your environment.
Using the *system* scope in the dependency, Maven will use the the jar at *$OSPL_HOME/jar/dcpssaj.jar*.

And for the compilation of you IDL files, you need to configure the *opensplice-idl-plugin* provided with Vortex Gateway:

```XML
   <build>
      <plugins>
         <!-- OpenSplice IDL compilation plugin -->
         <plugin>
            <groupId>com.adlinktech.gateway</groupId>
            <artifactId>opensplice-idl-plugin</artifactId>
            <version>${gateway-version}</version>
            <executions>
               <execution>
                  <phase>generate-sources</phase>
                  <goals>
                     <goal>idl-compile</goal>
                  </goals>
               </execution>
            </executions>
         </plugin>
      </plugins>
   </build>
```

More details on *opensplice-idl-plugin* in its documentation: [opensplice-idl-plugin/README.adoc](opensplice-idl-plugin/README.adoc)

## Examples

Some examples of Vortex Gateway usage can be found here: [Examples](examples)


### [Changelog](CHANGELOG.md)
