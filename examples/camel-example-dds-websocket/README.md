# camel-example-dds-websocket

 The camel-example-dds-websocket example demonstrates how to integrate WebSockets and DDS.

 Note that this example requires the ishapes graphical demo which is bundled with OpenSplice DDS.

## Building the example
 From the camel-example-dds-websocket directory run the following command: 
 
 `mvn package`
  
## Running the example
 
 - Start a VortexOpenSplice iShapes with the following command:
  
   `$OSPL_HOME/bin/demo_ishapes`

 - Publish as many shapes and subscribe as many shapes as you want

 - From the camel-example-dds-websocket directory run the following command:
   
   `mvn exec:java`

 - From the camel-example-dds-websocket directory, open the following file in a browser:
 
   `./target/classes/html/index.html`

Result: The current published IShapes will show up in the html page in JSON format.


## Files organisation
 - src/main/idl/ShapeType : the iShapes topic type definition
 - src/main/java/ShapesToWebsocket.java : the WebSocket/DDS Camel routes
 - src/main/resources/html/index.html  : the index.html page

