# camel-example-dds

 The camel-example-dds is a group of examples of Gateway usage with Java DSL.
 
 Note that this example requires the ishapes graphical demo which is bundled with OpenSplice DDS.

## Building the example
 From the camel-example-dds directory run the following command: 

 `mvn package`

## SimpleRoute example
 A simple route from a DDS topic to another DDS topic (using the same type)

 How to run it: 
 - Start a OpenSplice iShapes with the following command:

   `$OSPL_HOME/bin/demo_ishapes`

 - Publish a Circle shape and subscribe to Square shapes

 - From the camel-example-dds directory run the following command:
 
   `mvn exec:java -PSimpleRoute`

 Result: The **Circle** topic is routed to the **Square** topic.


## MulticastRoute example
 A route from a DDS topic to 2 DDS topics.

 How to run it: 
 - Start a OpenSplice iShapes with the following command:
   
   `$OSPL_HOME/bin/demo_ishapes`

 - Publish a Circle shape and subscribe to Square and Triangle shapes

 - From the camel-example-dds directory run the following command:
   
   `mvn exec:java -PMulticastRoute`

 Result: The **Circle** topic is routed to the **Square** and **Triangle** topics


## DataTransformRoute example
 2 routes showing 2 kind of transformation:
  - case 1: a route modifying the data content
  - case 2: a route modifying the data format
 
 How to run it: 
 - Start a OpenSplice iShapes with the following command:
  
   `$OSPL_HOME/bin/demo_ishapes`

 - Publish a Circle shape and subscribe to Square 
 
 - Publish a Triangle shape

 - From the camel-example-dds directory run the following command:
   
   `mvn exec:java -PDataTransformRoute`

 Result: 
  - case 1: the **Circle** topic is routed to the **Square** topic, but with modified shapes coordinates and color.
  - case 1: the **Triangle** topic is routed to the standard output, with the shapes transformed into a String.

  
## FilterRoute example
 A route with a filter implemented as a Java operation.

 How to run it: 
 - Start a OpenSplice iShapes with the following command:
  
   `$OSPL_HOME/bin/demo_ishapes`

 - Publish a Circle shape and subscribe to Square

 - From the camel-example-dds directory run the following command:
   
   `mvn exec:java -PFilterRoute`

 Result: only shapes with X coordinate > 100 are routed from topic **Circle** to topic **Square**.
          
 
## GroovyFilterRoute example
 A route with a filter implemented as a Groovy script.

 How to run it: 
 - Start a OpenSplice iShapes with the following command:
  
   `$OSPL_HOME/bin/demo_ishapes`

 - Publish a Circle shape and subscribe to Square

 - From the camel-example-dds directory run the following command:
   
   `mvn exec:java -PGroovyFilterRoute`

 Result: only shapes with coordinates X > 100 and Y > 100 are routed from topic **Circle** to topic **Square**.

 
## SampleRoute example
 A route with sampling of the data to lower the publication rate.

 How to run it: 
 - Start a OpenSplice iShapes with the following command:
  
   `$OSPL_HOME/bin/demo_ishapes`

 - Publish a Circle shape and subscribe to Square

 - From the camel-example-dds directory run the following command:
   
   `mvn exec:java -PSampleRoute`

Result: The **Circle** topic is forward to the **Square** topic at a lower rate (i.e. 1 sample per second)      


## ContentBasedRoute example
 A route forwarding the data to different topic depending its content. 

 How to run it: 
 - Start a OpenSplice iShapes with the following command:
  
   `$OSPL_HOME/bin/demo_ishapes`

 - Publish a BLUE and a RED Circle and subscribe to Square and Triangle

 - From the camel-example-dds directory run the following command:
   
   `mvn exec:java -PContentBasedRoute`

Result: The **Circle** topic is routed according to the shape color: 
 - BLUE Circles are routed to the **Square** topic
 - RED Circles are routed to the **Triangle** topic
 - other Circles are routed as logging message to stdout      

 
## DynamicPollEnricherRoute example
 A route from a DDS topic, each incoming data causing a poll to another DDS topic with a Query Expression
 to read a data with the same key (the shape color). The 2 data are aggregated a a new data and routed to a 3d DDS topic.

 How to run it: 
 - Start a OpenSplice iShapes with the following command:
  
   `$OSPL_HOME/bin/demo_ishapes`

 - Publish a Circle and a Square shape of the same color and subscribe to Triangle shapes

 - From the camel-example-dds directory run the following command:
   
   `mvn exec:java -PDynamicPollEnricherRoute`

Result: The **Circle** and **Square** topics are forward as a **Triangle** located in the average position of the two input topics

