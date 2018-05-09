# camel-messages-mode

The camel-messages-mode examples demonstrate routing of camel messages (i.e. Java objects) with DDS as the underlying transport,
without requiring an IDL topic type definition.

## Building the example
 From the camel-messages-mode directory run the following command: 
 
 `mvn package`
  
## Running the example
 
### Example 1: StringMessage

 This example demonstrates routing of camel messages with string as the message body.
 
 - run this example with the following command:
 
   `mvn exec:java -Dexec.mainClass=com.adlinktech.gateway.examples.camelmessage.StringMessage`

 - Type a string message and press <ENTER>.

   The message will be routed back to the application and displayed.
   If multiple instances of the application are executed, all the instances will receive the message.

 - Type `quit` and press <ENTER> to exit the application.

   
### Example 2. POJOMessage

 This example demonstrates routing of camel messages with a serializable Java Object as the message body.
 The Java Object in this example is has two variables describing a location: "city" of type String and "zip" of type integer.

 - run this example with the following command:

   `mvn exec:java -Dexec.mainClass=com.adlinktech.gateway.examples.camelmessage.POJOMessage`

 - Type city,zip and press <ENTER>. (Example: `Boston,01803` )

   The Java Object will be serialized and routed back to the application and displayed.
   If multiple instances of the application are executed, all the instances will receive the message.

 - Type `quit` and press <ENTER> to exit the application.
 