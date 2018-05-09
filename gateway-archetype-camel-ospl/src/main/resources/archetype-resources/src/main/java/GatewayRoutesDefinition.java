#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package};


import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;



public class GatewayRoutesDefinition {


    public static void main(String[] args) {

        // create Camel context
        final CamelContext ctx = new DefaultCamelContext();

        // add Routes
        try {

            ctx.addRoutes( new RouteBuilder() {

                @Override
                public void configure() throws Exception {

                    // Define your route from DDS as following:
                    //   from("dds:<topicName>:<domainId>/<topicType>[?<options>]")
                    //     .to("...")
                    // replacing <topicName> by your the topic name to subscribe
               	    //		 	 <domainId>  domain id for the DDS global data space (must be an integer)
                	//           <topicType> by the java type of the topic (class generated from yout IDL)
                    //           [?<option>] by Gateway options if you want some (see User Guide)


                }
            });

            // start Camel engine and routes
            ctx.start();

            Thread.currentThread().join();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}