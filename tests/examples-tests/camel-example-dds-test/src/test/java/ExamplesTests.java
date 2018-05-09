
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

import org.apache.camel.impl.CamelContextTrackerRegistry;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ExamplesTests
{

   private static final Logger logger = LoggerFactory.getLogger(ExamplesTests.class);

   private static CamelContextTracker tracker;

   private ShapesPublisher shapesPublisher;
   private ShapesSubscriber shapesSubscriber;

   @BeforeClass
   public static void registerCamelContextTracker()
   {
      // Register a CamelContextTracker that will store the CamelContext
      // created by each example, thus we can stop it at test end.
      tracker = new CamelContextTracker();
      CamelContextTrackerRegistry.INSTANCE.addTracker(tracker);
   }

   @Before
   public void init()
   {
      logger.info("Create a Shapes Pub & Subs...");
      try
      {
         shapesSubscriber = new ShapesSubscriber(0);
         shapesPublisher = new ShapesPublisher(0);
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
   }

   @After
   public void clean()
   {
      // stop the CamelContext created by the example's main()
      // and that have been stored in the CamelContextTracker.
      logger.info("Stop the example's CamelContext");
      try
      {
         tracker.getCamelContext().stop();
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }

      logger.info("Terminate Pub & Subs shapes");
      try
      {
         shapesSubscriber.terminate();
         shapesPublisher.terminate();
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
   }

   @Test
   public void SimpleRouteTest()
   {
      logger.info("Start SimpleRouteTest... ");
      try
      {
         // Create a thread that start SimpleRoute.main()
         Thread simpleRouteThread = new Thread(new Runnable()
         {

            public void run()
            {
               logger.info("SimpleRoute main... ");
               SimpleRoute.main(null);
            }
         });
         simpleRouteThread.start();

         // wait for Camel engine and route to start
         Thread.sleep(2000);

         // publish a circle
         shapesPublisher.publishCircle("BLUE", 20, 20, 5);
         // read a square
         ShapeType square = shapesSubscriber.readSquare(2000);

         // assert that the read square has same color, position and size than the published circle.
         Assert.assertNotNull("No square was received !", square);
         Assert.assertEquals("BLUE", square.color);
         Assert.assertEquals(20, square.x);
         Assert.assertEquals(20, square.y);
         Assert.assertEquals(5, square.shapesize);
      }
      catch (Exception e)
      {
         Assert.fail(e.toString());
      }
   }

   @Test
   public void FilterRouteTest()
   {
      logger.info("Start FilterRouteTest... ");
      try
      {
         // Create a thread that start FilterRoute.main()
         Thread filterRouteThread = new Thread(new Runnable()
         {

            public void run()
            {
               logger.info("FilterRouteTest main... ");
               FilterRoute.main(null);
            }
         });
         filterRouteThread.start();

         // wait for Camel engine and route to start
         Thread.sleep(2000);

         // publish a circle with x < 100
         shapesPublisher.publishCircle("BLUE", 10, 10, 10);
         // try to read a square (shouldn't be possible since x<=100)
         ShapeType square = shapesSubscriber.readSquare(1000);
         Assert.assertNull("A square was received !", square);

         // publish a circle with x > 100
         shapesPublisher.publishCircle("BLUE", 120, 20, 20);
         // try to read a square (should be possible since x>100)
         square = shapesSubscriber.readSquare(1000);
         Assert.assertNotNull("No square was received !", square);
         Assert.assertEquals("BLUE", square.color);
         Assert.assertEquals(120, square.x);
         Assert.assertEquals(20, square.y);
         Assert.assertEquals(20, square.shapesize);

         // publish a circle with x < 100
         shapesPublisher.publishCircle("BLUE", 30, 30, 30);
         // try to read a square (shouldn't be possible since x<=100)
         square = shapesSubscriber.readSquare(1000);
         Assert.assertNull("A square was received !", square);

         // publish a circle with x > 100
         shapesPublisher.publishCircle("BLUE", 140, 40, 40);
         // try to read a square (should be possible since x>100)
         square = shapesSubscriber.readSquare(1000);
         Assert.assertNotNull("No square was received !", square);
         Assert.assertEquals("BLUE", square.color);
         Assert.assertEquals(140, square.x);
         Assert.assertEquals(40, square.y);
         Assert.assertEquals(40, square.shapesize);
      }
      catch (Exception e)
      {
         e.printStackTrace();
         Assert.fail(e.toString());
      }
   }

   @Test
   public void MulticastRouteTest()
   {
      logger.info("Start MulticastRouteTest... ");

      try
      {
         // Create a thread that start MulticastRoute.main()
         Thread multicastRouteThread = new Thread(new Runnable()
         {

            public void run()
            {
               logger.info("MulticastRouteTest main... ");
               MulticastRoute.main(null);
            }
         });
         multicastRouteThread.start();

         // wait for Camel engine and route to start
         Thread.sleep(2000);

         // publish a circle
         shapesPublisher.publishCircle("BLUE", 20, 20, 5);

         // read a square
         ShapeType square = shapesSubscriber.readSquare(2000);
         // assert that the read square has same color, position and size than the published circle.
         Assert.assertNotNull("No square was received !", square);
         Assert.assertEquals("BLUE", square.color);
         Assert.assertEquals(20, square.x);
         Assert.assertEquals(20, square.y);
         Assert.assertEquals(5, square.shapesize);

         // read a triangle
         ShapeType triangle = shapesSubscriber.readTriangle(2000);
         // assert that the read triangle has same color, position and size than the published
         // circle.
         Assert.assertNotNull("No triangle was received !", triangle);
         Assert.assertEquals("BLUE", triangle.color);
         Assert.assertEquals(20, triangle.x);
         Assert.assertEquals(20, triangle.y);
         Assert.assertEquals(5, triangle.shapesize);
      }
      catch (Exception e)
      {
         Assert.fail(e.toString());
      }
   }

   @Test
   public void DataTransformRouteTest()
   {
      logger.info("Start DataTransformRouteTest... ");
      try
      {
         // Create a thread that start DataTransformRoute.main()
         Thread dataTransformRouteThread = new Thread(new Runnable()
         {

            public void run()
            {
               logger.info("DataTransformRoute main... ");
               DataTransformRoute.main(null);
            }
         });
         dataTransformRouteThread.start();

         // wait for Camel engine and route to start
         Thread.sleep(2000);

         // publish a circle
         shapesPublisher.publishCircle("BLUE", 20, 20, 5);
         // read a square
         ShapeType square = shapesSubscriber.readSquare(2000);

         // assert that the read square is orange and has circle's position +20.
         Assert.assertNotNull("No square was received !", square);
         Assert.assertEquals("ORANGE", square.color);
         Assert.assertEquals(20 + 20, square.x);
         Assert.assertEquals(20 + 20, square.y);
         Assert.assertEquals(5, square.shapesize);
      }
      catch (Exception e)
      {
         Assert.fail(e.toString());
      }
   }

   @Test
   public void GroovyFilterRouteTest()
   {
      logger.info("Start GroovyFilterRouteTest... ");
      try
      {
         // Create a thread that start GroovyFilterRoute.main()
         Thread groovyFilterRouteThread = new Thread(new Runnable()
         {

            public void run()
            {
               logger.info("GroovyFilterRoute main... ");
               GroovyFilterRoute.main(null);
            }
         });
         groovyFilterRouteThread.start();

         // wait for Camel engine and route to start
         Thread.sleep(2000);

         // publish a circle with x < 100
         shapesPublisher.publishCircle("BLUE", 10, 110, 10);
         // try to read a square (shouldn't be possible since x<=100)
         ShapeType square = shapesSubscriber.readSquare(1000);
         Assert.assertNull("A square was received !", square);

         // publish a circle with x > 100 & y > 100
         shapesPublisher.publishCircle("BLUE", 120, 120, 20);
         // try to read a square (should be possible since x>100)
         square = shapesSubscriber.readSquare(1000);
         Assert.assertNotNull("No square was received !", square);
         Assert.assertEquals("BLUE", square.color);
         Assert.assertEquals(120, square.x);
         Assert.assertEquals(120, square.y);
         Assert.assertEquals(20, square.shapesize);

         // publish a circle with y < 100
         shapesPublisher.publishCircle("BLUE", 130, 30, 30);
         // try to read a square (shouldn't be possible since x<=100)
         square = shapesSubscriber.readSquare(1000);
         Assert.assertNull("A square was received !", square);

         // publish a circle with x < 100 and y < 100
         shapesPublisher.publishCircle("BLUE", 40, 40, 40);
         // try to read a square (should be possible since x>100)
         square = shapesSubscriber.readSquare(1000);
         Assert.assertNull("A square was received !", square);
      }
      catch (Exception e)
      {
         Assert.fail(e.toString());
      }
   }

   @Test
   public void SampleRouteTest()
   {
      logger.info("Start SampleRouteTest... ");
      try
      {
         // Create a thread that start SampleRoute.main()
         Thread sampleRouteThread = new Thread(new Runnable()
         {

            public void run()
            {
               logger.info("SampleRoute main... ");
               SampleRoute.main(null);
            }
         });
         sampleRouteThread.start();

         // wait for Camel engine and route to start
         Thread.sleep(2000);

         // publish 3 circles
         shapesPublisher.publishCircle("BLUE", 10, 10, 10);
         Thread.sleep(50);
         shapesPublisher.publishCircle("BLUE", 20, 20, 20);
         Thread.sleep(50);
         shapesPublisher.publishCircle("BLUE", 30, 30, 30);
         Thread.sleep(50);

         // read square
         ShapeType square = shapesSubscriber.readSquare(200);
         // assert that the read square has same color, position and size than the published circle.
         Assert.assertNotNull("No square was received !", square);
         Assert.assertEquals("BLUE", square.color);
         Assert.assertEquals(10, square.x);
         Assert.assertEquals(10, square.y);
         Assert.assertEquals(10, square.shapesize);
         // read square (should fail since sampling is 1 per second)
         square = shapesSubscriber.readSquare(200);
         Assert.assertNull("An unexpected square was received !", square);
         // read square (should fail since sampling is 1 per second)
         square = shapesSubscriber.readSquare(200);
         Assert.assertNull("An unexpected square was received !", square);

         // wait next second
         Thread.sleep(1000);

         // publish 3 circles
         shapesPublisher.publishCircle("BLUE", 40, 40, 40);
         Thread.sleep(50);
         shapesPublisher.publishCircle("BLUE", 50, 50, 50);
         Thread.sleep(50);
         shapesPublisher.publishCircle("BLUE", 60, 60, 60);
         Thread.sleep(50);

         // read square
         square = shapesSubscriber.readSquare(200);
         // assert that the read square has same color, position and size than the published circle.
         Assert.assertNotNull("No square was received !", square);
         Assert.assertEquals("BLUE", square.color);
         Assert.assertEquals(40, square.x);
         Assert.assertEquals(40, square.y);
         Assert.assertEquals(40, square.shapesize);
         // read square (should fail since sampling is 1 per second)
         square = shapesSubscriber.readSquare(200);
         Assert.assertNull("An unexpected square was received !", square);
         // read square (should fail since sampling is 1 per second)
         square = shapesSubscriber.readSquare(200);
         Assert.assertNull("An unexpected square was received !", square);
      }
      catch (Exception e)
      {
         Assert.fail(e.toString());
      }
   }

   @Test
   public void ContentBasedRouteTest()
   {
      logger.info("Start ContentBasedRouteTest... ");
      try
      {
         // Create a thread that start ContentBasedRoute.main()
         Thread contentBasedRouteThread = new Thread(new Runnable()
         {

            public void run()
            {
               logger.info("ContentBasedRoute main... ");
               ContentBasedRoute.main(null);
            }
         });

         contentBasedRouteThread.start();

         // wait for Camel engine and route to start
         Thread.sleep(2000);

         // publish a BLUE circle
         shapesPublisher.publishCircle("BLUE", 20, 20, 5);
         // read a square
         ShapeType square = shapesSubscriber.readSquare(2000);
         // read a triangle
         ShapeType triangle = shapesSubscriber.readTriangle(2000);

         // assert that the read square has same color, position and size than the published circle.
         Assert.assertNotNull("No square was received !", square);
         Assert.assertEquals("BLUE", square.color);
         Assert.assertEquals(20, square.x);
         Assert.assertEquals(20, square.y);
         Assert.assertEquals(5, square.shapesize);
         // assert that no triangle was read.
         Assert.assertNull("An unexpected triangle was received !", triangle);

         // publish a RED circle
         shapesPublisher.publishCircle("RED", 30, 30, 5);
         // read a square
         square = shapesSubscriber.readSquare(2000);
         // read a triangle
         triangle = shapesSubscriber.readTriangle(2000);

         // assert that no square was read.
         Assert.assertNull("An unexpected square was received !", square);
         // assert that the read triangle has same color, position and size than the published
         // circle.
         Assert.assertNotNull("No triangle was received !", triangle);
         Assert.assertEquals("RED", triangle.color);
         Assert.assertEquals(30, triangle.x);
         Assert.assertEquals(30, triangle.y);
         Assert.assertEquals(5, triangle.shapesize);
      }
      catch (Exception e)
      {
         Assert.fail(e.toString());
      }
   }


   @Test
   public void DynamicPollEnricherRouteTest()
   {
      logger.info("Start DynamicPollEnricherRouteTest... ");
      try
      {
         // Create a thread that start DynamicPollEnricherRoute.main()
         Thread dynamicPollEnriherRouteThread = new Thread(new Runnable()
         {

            public void run()
            {
               logger.info("DynamicPollEnricherRoute main... ");
               DynamicPollEnricherRoute.main(null);
            }
         });

         dynamicPollEnriherRouteThread.start();

         // wait for Camel engine and route to start
         Thread.sleep(2000);

         // publish a BLUE circle
         shapesPublisher.publishCircle("BLUE", 10, 10, 5);
         // wait for the DynamicPollEnricher to create the DataReader (as done at first processing)
         // otherwise the square might not be received (as Durability QoS is VOLATILE)
         Thread.sleep(1000);
         // publish a BLUE square
         shapesPublisher.publishSquare("BLUE", 20, 20, 5);

         // read a triangle
         ShapeType triangle = shapesSubscriber.readTriangle(2000);
         // assert that the triangle is BLUE and has an average position between circle and square
         Assert.assertNotNull("No triangle was received !", triangle);
         Assert.assertEquals("BLUE", triangle.color);
         Assert.assertEquals(15, triangle.x);
         Assert.assertEquals(15, triangle.y);
         Assert.assertEquals(5, triangle.shapesize);
      }
      catch (Exception e)
      {
         Assert.fail(e.toString());
      }

   }

}
