# camel-example-dds-rest

 The camel-example-dds-rest example demonstrates how a REST API can be defined to
 poll the data from a DDS system.

 Note that this example requires the ishapes graphical demo which is bundled with OpenSplice DDS.

## Building the example
 From the camel-example-dds-rest directory run the following command: 
 
 `mvn package`
 
## Running the example
 
 - Start a VortexOpenSplice iShapes with the following command:
  
   `$OSPL_HOME/bin/demo_ishapes`

 - Publish as many shapes and subscribe as many shapes as you want

 - From the camel-example-dds-rest directory run the following command:
   
   `mvn exec:java`

 - In a Web browser, go to following URL:
    http://localhost:4444/index.html

 - Click on "Refresh" button to poll a snapshot of iShapes demo via REST requests.

Result: The current published IShapes will show up in the html page.
    
## Files organisation
 - src/main/idl/ShapeType : the iShapes topic type definition
 - src/main/java/ShapesToRest.java : the REST/DDS Camel routes
 - src/main/resources/html/index.html  : the index.html page
 - src/main/resources/html/raphael.js  : a JavaScript file for the graphical rendering the web browser

