# camel-example-dds-spring

 The camel-example-dds-spring example demonstrates how specify DDS routes in XML via the Spring framework.

 Note that this example requires the ishapes graphical demo which is bundled with OpenSplice DDS.

## Building the example
 From the camel-example-dds-spring directory run the following command: 
 
 `mvn package`
  
## Running the example
 
 - Start a VortexOpenSplice iShapes with the following command:
   
   `$OSPL_HOME/bin/demo_ishapes`
    
 - Publish a Circle and a Square shape and subscribe to Circle and Square

 - From the camel-example-dds-spring directory run the following command:
   
   `mvn camel:run`

 Results:
  - A route forwarding shapes from Circle topic to Square topic
  - A route forwarding shapes from Square topic to Triangle topic but changing the shape size and position  		
