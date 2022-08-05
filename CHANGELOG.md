# Changelog

## [UNRELEASED](https://github.com/atolab/vortex-gateway/tree/master)
 - add `renamePackages` to `opensplice-idl-plugin` (uses idlpp's `-j` option)

## [3.0.1](https://github.com/atolab/vortex-gateway/tree/V3.0.0) - 2018-05-14

### Fixed
 - the code changes made between 2.3.1 and 2.3.2 (i.e. BB-326 and BB-327) were missing in 3.0.0.
   They now have been added back.
  

## [3.0.0](https://github.com/atolab/vortex-gateway/tree/V3.0.0) - 2018-05-11
 This is the first release of Vortex Gateway as open-source under Apache Licence v2.0.

 This implied the following changes wrt. the previous commercial version:

### Removed
 - the RLM license checking mechanism
 - the camel-ddsi component
 - the Maven archetype for camel-ddsi (gateway-archetype-camel-ddsi)
 
### Changed
 - the Maven groupId for all modules changed from `com.prismtech.gateway` to `com.adlinktech.gateway`
 - the Java package `com.prismtech.gateway` have been renamed as `com.adlinktech.gateway`
 - the camel-ospl component have been ported to Apache Camel 2.21
 - the examples have been reworked (some have been removed, other have been added)
 - the documentation have been reworked for hosting on Github


___________________________________________________________________________________________________________

## Historical Changelog (pre-3.0.0 - commercial versions)

### 2.3.2 - 2017-04-31
 Maintenance release

 - **BB-327** update

   In camel-ospl component, added the "consumer.exceptionHandler" and "consumer.bridgeErrorHandler"
   to configure handling of errors that may occur in Consumer (calling DataReader.read() for instance).
   See User Guide for more details.

 - **BB-326** fix

   In camel-ospl Producer, the error code returned by calls to DataWriter.write() was not checked.
   It is now, and a DdsException is raised in case of error. This exception can be handled by user
   on the Camel route. See User Guide for more details.

### 2.3.1 - 2017-03-10
 Vortex V2.4
 - **BB-298** update

   Vortex Gateway is now based on Vortex Caf� 2.3.1.
   The camel-ddsi component benefits some of its fixes and improvements (see Caf�'s changes).

 - **BB-299** fix

   Fixed "%1 is not a valid Win32 application" error occuring on Windows with Maven 3.3.9, when installing
   Vortex Gateway with command "mvn -f VortexGateway-installation.pom".

### 2.3.0 - 2016-09-22
 Vortex V2.3.0
 - **BB-287** update

   Vortex Gateway is now based on Vortex Caf� 2.3.0.
   The camel-ddsi component benefits some of its fixes and improvements (see Caf�'s changes).

 - **BB-288** update

   Vortex Caf� now supports Java 7 and 8. It's tested with both Oracle Java 8 and OpenJDK 8.
   Note that Java 6 is no longer supported.

 - **BB-289** update

   The Maven build system for examples have been updated to build with Apache Maven 3.3.x.
   Apache Maven 3.3.9 is now delivered with Vortex Gateway, in place of Apache Maven 3.0.5.

### 2.2.2 - 2016-04-22
 Vortex V2.2.0
 - **BB-276** update

   Vortex Gateway is now based on Vortex Caf� 2.2.2.
   The camel-ddsi component benefits some of its fixes and improvements (see Caf�'s changes).

 - **BB-277** fix

   Fix tables rendering in PDF User Guide.

### 2.2.1 - 2016-02-17
 Vortex V2.1.0
 - **BB-251** update

   Update all the "shapes-*" examples to work with Vortex Caf� 2.2.1 iShapes example,
   but also with OpenSplice and Lite iShapes demos.

 - **BB-268** update

   Vortex Gateway is now based on Vortex Caf� 2.2.1.
   The camel-ddsi component benefits some of its fixes and improvements (see Caf�'s changes).

 - **BB-271** fix

   Fix the shapes-java example when using the DynamicPollEnricher.

### 2.2.0 - 2015-11-25
 Vortex V2.0.0
 - **BB-248** update

   Vortex Gateway is now based on Vortex Caf� 2.2.0.
   The camel-ddsi component benefits some of its fixes and improvements (see Caf�'s changes).

### 2.1.2 - 2015-07-08
 Maintenance release
 - **BB-207** add

   Add a 'ignoreLocalPublishers' option (boolean; true by default)
   to the camel-ospl component. When true, this option avoids loop
   in case of bridge pattern (1 route to DDS + 1 route from DDS).

 - **BB-238** add

   Add options to the camel-ddsi component to set QoS at topic-level
   (see user guide for the full list of options).

 - **BB-240** fix

   Fix opensplice-idl-plugin to accept files with ".IDL" (uppercase)
   as extension.

### 2.1.1 - 2015-03-09
 Vortex V1.2.0
 - **BB-215** add

   Add an etc/ directory in Vortex Gateway installation directory.
   User can copy its license file within.

 - **BB-222** update

   Change user guide document to be available in both HTML and PDF formats.
   Remove installation guide, as now part of the Vortex installer.

 - **BB-229** update

   Vortex Gateway is now based on Vortex Caf� 2.1.1.
   The camel-ddsi component benefits some of its fixes and improvements (see Caf�'s changes).

 - **OSPL-5470** fix

   Fix warning displayed by idl2j, the camel-ospl component and the
   camel-ddsi component: "Java HotSpot(TM) Server VM warning:
   You have loaded library /tmp/librlm913-5386386880551815362.so which
   might have disabled stack guard".

 - **BB-236** fix

   Fix issue with OpenSplice 6.5.0 (and upper versions) leading a camel-ospl
   endpoint to not receive DDS samples under certain circumstances. NOTE that
   this means that Gateway version prior to 2.1.1 won't work correctly with
   OpenSplice 6.5.0 and following versions.

### 2.1.0 - 2014-10-06
 Vortex V1.1.0
 - **BB-212** update

   Update RLM licensing library to no more require environment variables.
   As a consequence the "release.com" file is no more generated and doesn't need to be sourced any more.
   Please see the Vortex Installation Guide for the installation of your license file.

 - **BB-211** update

   Vortex Web is now based on Vortex Caf� 2.1.0.
   The camel-ddsi component benefits some of its fixes and improvements (see Caf�'s changes).

 - **BB-213** update

   Update the LICENSE terms (see LICENSE.html).

### 2.0.1 - 2014-08-01
 Vortex V1.0.1
 - **BB-204** update

   Vortex Gateway's camel-ddsi is now based on Vortex Caf� 2.0.1.
   The camel-ddsi component benefits some of its fixes and improvements (see Caf�'s changes).

### 2.0.0 - 2014-07-11
 Vortex V1.0.0
 - **BB-157** update

   Changed product name to "Vortex Gateway"
   This implies following changes:
   1. In all pom.xml file Maven groupId change org.opensplice.gateway to com.prismtech.gateway
   2. In Java source code : change org.opensplice.gateway to com.prismtech.gateway .

 - **BB-141** update

   Changed default Camel version to 2.10.0.redhat-60060 .

 - **BB-158** update

   The camel-ddsi component uses the same DDSI implementation than Vortex Caf� v2.0.0 .

 - **BB-160** update

   The logging have been improved. The logs are prefixed with "gateway.ospl" for the camel-ospl component,
   and with "gateway.ddsi" for the camel-ddsi component.

 - **BB-187** update

   Added descriptions in User Guide of the Camel headers used by each component.

 - **BB-192** update

   In Camel-ddsi component, the "networkInterfaces" option has been renamed to "networkInterface" and accepts only 1 interface name.
   Multi-homed hosts (with several interfaces) was actually badly supported.

 - **BB-195** update

   Removed the "preprocessor" option from opensplice-idl-plugin since latest idlpp versions
   (since Vortex OpenSplice 6.4.1) removed the equivalent -c option.

 - **BB-193** fix

   Fix "java.null.NullPointerException" in camel-ddsi when receiving a message with a null payload
   (e.g. a message indicating an instance has been disposed).
    
### 1.3.1 - 2013-09-13
 Add readMode for camel-ospl component
 - **BB-149** add

   Add a 'readMode' option to camel-ospl to have the consumer endpoint
   using the DDS DataReader.read() operation instead of DataReader.take() operation.

### 1.3.0 - 2013-03-28
 Now based on the same DDSI implementation than OpenSplice Mobile
 - **BB-92** add

   Add DDSI over TCP mode.
   NOTE: this is experimental. No dynamic discovery (static configuration of peers)
   and no NAT trasversal capabilities.

 - **BB-140** update

   OpenSplice Gateway now uses the the same DDSI implementation than OpenSplice Mobile.
   It also uses OpenSplice Mobile's iShapes demo for it's own shapes-* examples.
   (WARNING: Java 7 is required for iShapes demo)

 - **BB-128** update

   Add 'networkInterfaces' option to camel-ddsi to choose the
   network insterfaces to use.

 - **BB-143** update

   Add 'registerType' option to camel-ddsi to register the Topic type with
   another name that the classname.

 - **BB-130** fix

   Fix ResolveEndpointFailedException in camel-message-mode example.

### 1.2.0 - 2012-08-02
 Add dynamic QoS for camel-ospl and fragmentation for camel-ddsi
 - **BB-90** add

   Add dynamic QoS for camel-ospl endpoint.
   See User Guide for more information.

 - **BB-106** add

   Add support of fragmentation of large data in DDSI protocol implementation
   (used by camel-ddsi endpoint). This is transparent to user and is automatically
   activated for data larger than 1280 bytes.

 - **BB-83** add

   Add examples of DDS bridging over TCP/HTTP/HTTPS/WebSocket using camel-ddsi endpoint
   (see examples/shapes-ddsi-bridges).

 - **BB-115** update

   Add a graphical example of a graphical Web page polling DDS data via REST URLs
   (see examples/shapes-java).

 - **BB-103** update

   Change the dependency to Apache Camel version 2.9.0.fuse-7-061.
   Note that OpenSplice Gateway 1.2.0 has also been successfully tested with versions 2.8.5, 2.9.3 and 2.10.0.

 - **BB-119** update

   Add list of dependencies licenses in third_party_licenses.html file.

 - **BB-102** fix

   Using camel-ospl endpoint, some routes may cause a StackOverflowError for each message,
   with lot of calls to ClassLoader in stack trace.

 - **BB-108** fix

   Using camel-ddsi on Windows, when 2 Gateway processes are started on the same host
   with routes using the same DDS Domain, one of the process doesn't receive any DDS data.

 - **BB-109** fix

   Using camel-ddsi, a process might be blocked at the end of main() operation, because of pending resources.

### 1.1.1 - 2012-04-13
 Bug fix
 - **BB-78** fix

   examples/shapes-ddsi compilation fails on Windows.
    
### 1.1.0 - 2012-04-13
 New camel-ddsi component
 - **BB-60** add

   Add a new Camel component based on Java DDSI implementation.
   See User Guide for more information.

 - **BB-74** add

   Add creation of a Maven Repository package for offline installation and examples run.
   See Installation Guide for more information.

 - **BB-8** add

   Add a new PingPong example (compatible with Vortex OpenSplice's PingPong example).

 - **BB-10** add

   Add in shapes-java example an route using CEP (Complex Event Processing)
   with camel-esper component.

 - **BB-67** update

   Add blocking behavior to the DDSPollingConsumer's receive() and receive(timeout) operations
   (receiveNoWait() operation keeps non-blocking behavior).

 - **BB-72** update

   Change the separator character to be used in "partition" option for "dds:" URI.
   ',' (comma) has to be used instead of '%' ('%' is also the escape character in URIs).

 - **BB-73** update

   Change the Camel version to 2.8.0-fuse-03-06.

 - **BB-76** fix

   With Vortex OpenSplice evaluation versions, the opensplice-idl-plugin fail to compile a valid IDL.

### 1.0.4 - 2012-01-04
 Code obfuscation and some bug fixes
 - **BB-29** update

   Obfuscate camel-ospl jar.

 - **BB-65** fix

   DDS entities are not deleted at Camel route stop. And in DdsPollingConsumer, 
   ReadCondition/QueryCondition objects are not deleted after the pull has finished.

 - **BB-23** fix

   After switch to Camel 2.7.1 chatroom-webservice example failed.
   Consequently it was removed from releases.

 - **BB-59** fix

   The ZIP delivery file contains directories in 777 mode.

 - **BB-66** update

   Update copyright headers for 2012.

### 1.0.3 - 2011-11-16
 Provide support for 64 bit Windows and Solaris
 - **BB-57** add

   Provide support for 64 bit Windows and Solaris.

### 1.0.2 - 2011-10-26
 Changed product name to OpenSplice Gateway
 - **BB-56** update

   Change "BlendBox" name to "Gateway".

 - **BB-54** update

   Documentation improvement.

### 1.0.1 - 2011-10-12
 Minor corrections to 1.0.0
 - **BB-51** add

   Add a way for user to generate configuration information 
   to be sent to support in case of troubles.

 - **BB-52** fix

   Importing a BlendBox Maven project in Eclipse causes following error: 
   "Plugin execution not covered by lifecycle configuration: 
   org.opensplice.b2:opensplice-idl-plugin:1.0.0:idl-compile"

 - **BB-49** fix

   In v1.0.0 the rlm/pom.xml has version 1.0.0 
   but the parent-pom/pom.xml depends on rlm version 9.1.3.

### 1.0.0 - 2011-10-05
 First release

 
