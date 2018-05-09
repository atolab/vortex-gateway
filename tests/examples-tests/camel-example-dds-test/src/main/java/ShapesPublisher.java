
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import DDS.DataWriter;
import DDS.DataWriterQosHolder;
import DDS.DomainParticipant;
import DDS.DomainParticipantFactory;
import DDS.DurabilityQosPolicyKind;
import DDS.HANDLE_NIL;
import DDS.HistoryQosPolicyKind;
import DDS.PARTICIPANT_QOS_DEFAULT;
import DDS.Publisher;
import DDS.PublisherQosHolder;
import DDS.STATUS_MASK_NONE;
import DDS.Topic;
import DDS.TopicQosHolder;


public class ShapesPublisher
{

   private static final Logger logger = LoggerFactory.getLogger(ShapesPublisher.class);

   private static final String CIRCLE_TOPIC_NAME = "Circle";
   private static final String SQUARE_TOPIC_NAME = "Square";
   private static final String TRIANGLE_TOPIC_NAME = "Triangle";

   private DomainParticipantFactory dpf;
   private DomainParticipant participant;
   private TopicQosHolder topicQos = new TopicQosHolder();
   private Topic circle, square, triangle;
   private PublisherQosHolder pubQos = new PublisherQosHolder();
   private Publisher publisher;
   private DataWriterQosHolder WQosH = new DataWriterQosHolder();
   private ShapeTypeDataWriter writerCircle, writerSquare, writerTriangle;

   private String typeName;

   public ShapesPublisher()
   {
      this(0);
   }

   public ShapesPublisher(int domain)
   {
      logger.info("ShapesPublisher starting in domain: " + domain + " ... ");

      // Get the DomainParticipantFactory
      dpf = DomainParticipantFactory.get_instance();
      ErrorHandler.checkHandle(dpf, "DomainParticipantFactory.get_instance");

      // Create DomainParticipant
      participant = dpf.create_participant(domain,
            PARTICIPANT_QOS_DEFAULT.value, null, STATUS_MASK_NONE.value);
      ErrorHandler.checkHandle(dpf,
            "DomainParticipantFactory.create_participant");

      // Register type ShapeType
      ShapeTypeTypeSupport ts = new ShapeTypeTypeSupport();
      typeName = ts.get_type_name();
      int status = ts.register_type(participant, typeName);
      ErrorHandler.checkStatus(status, "register_type");

      participant.get_default_topic_qos(topicQos);

      topicQos.value.durability.kind = DurabilityQosPolicyKind.PERSISTENT_DURABILITY_QOS;
      topicQos.value.durability_service.service_cleanup_delay.sec = 3600;
      topicQos.value.durability_service.service_cleanup_delay.nanosec = 0;
      topicQos.value.durability_service.history_kind = HistoryQosPolicyKind.KEEP_LAST_HISTORY_QOS;
      topicQos.value.durability_service.history_depth = 100;
      topicQos.value.durability_service.max_samples = 8192;
      topicQos.value.durability_service.max_instances = 4196;
      topicQos.value.durability_service.max_samples_per_instance = 8192;
      // Create 3 topics: circle, square and triangle
      circle = participant.create_topic(CIRCLE_TOPIC_NAME, typeName, topicQos.value,
            null, STATUS_MASK_NONE.value);
      ErrorHandler.checkHandle(circle, "DomainParticipant.create_topic");
      square = participant.create_topic(SQUARE_TOPIC_NAME, typeName, topicQos.value,
            null, STATUS_MASK_NONE.value);
      ErrorHandler.checkHandle(square, "DomainParticipant.create_topic");
      triangle = participant.create_topic(TRIANGLE_TOPIC_NAME, typeName, topicQos.value,
            null, STATUS_MASK_NONE.value);
      ErrorHandler.checkHandle(triangle, "DomainParticipant.create_topic");
      // Create Publisher
      status = participant.get_default_publisher_qos(pubQos);
      ErrorHandler.checkStatus(status,
            "DomainParticipant.get_default_publisher_qos");
      publisher = participant.create_publisher(pubQos.value, null,
            STATUS_MASK_NONE.value);
      ErrorHandler.checkHandle(publisher,
            "DomainParticipant.create_publisher");
      // Create 3 DataWriters: Circle, Square and Triangle
      publisher.get_default_datawriter_qos(WQosH);
      publisher.copy_from_topic_qos(WQosH, topicQos.value);

      WQosH.value.writer_data_lifecycle.autodispose_unregistered_instances = false;

      WQosH.value.durability.kind = DurabilityQosPolicyKind.VOLATILE_DURABILITY_QOS;
      DataWriter dw = publisher.create_datawriter(circle, WQosH.value, null,
            STATUS_MASK_NONE.value);
      ErrorHandler.checkHandle(dw, "Publisher.create_datawriter");
      writerCircle = ShapeTypeDataWriterHelper.narrow(dw);

      dw = publisher.create_datawriter(square, WQosH.value, null, STATUS_MASK_NONE.value);
      ErrorHandler.checkHandle(dw, "Publisher.create_datawriter");
      writerSquare = ShapeTypeDataWriterHelper.narrow(dw);

      dw = publisher.create_datawriter(triangle, WQosH.value, null, STATUS_MASK_NONE.value);
      ErrorHandler.checkHandle(dw, "Publisher.create_datawriter");
      writerTriangle = ShapeTypeDataWriterHelper.narrow(dw);

      logger.info("ShapesPublisher started... ");
   }


   public void publishCircle(String color, int x, int y, int size)
   {
      // publish shapeType circle
      ShapeType shape = new ShapeType(color, x, y, size);

      logger.debug("Write Circle: " + shape.color + " (" + shape.x + "," + shape.y + ") size="
            + shape.shapesize);

      writerCircle.register_instance(shape);

      int status = writerCircle.write(shape, HANDLE_NIL.value);
      ErrorHandler.checkStatus(status, "ShapeTypeDataWriter.write");
   }

   public void publishSquare(String color, int x, int y, int size)
   {
      // publish shapeType square
      ShapeType shape = new ShapeType(color, x, y, size);

      logger.debug("Write Square: " + shape.color + " (" + shape.x + "," + shape.y + ") size="
            + shape.shapesize);

      writerSquare.register_instance(shape);

      int status = writerSquare.write(shape, HANDLE_NIL.value);
      ErrorHandler.checkStatus(status, "ShapeTypeDataWriter.write");

   }


   public void publishTriangle(String color, int x, int y, int size)
   {
      // publish shapeType triangle
      ShapeType shape = new ShapeType(color, x, y, size);

      logger.debug("Write Triangle: " + shape.color + " (" + shape.x + "," + shape.y + ") size="
            + shape.shapesize);

      writerTriangle.register_instance(shape);

      int status = writerTriangle.write(shape, HANDLE_NIL.value);
      ErrorHandler.checkStatus(status, "ShapeTypeDataWriter.write");

   }

   public void terminate()
   {
      // delete topics
      participant.delete_topic(circle);
      participant.delete_topic(square);
      participant.delete_topic(triangle);
      // delete data writers
      publisher.delete_datawriter(writerCircle);
      publisher.delete_datawriter(writerSquare);
      publisher.delete_datawriter(writerTriangle);
      // delete publisher
      participant.delete_publisher(publisher);
      // delete contained entities
      participant.delete_contained_entities();
      // delete participants
      dpf.delete_participant(participant);
   }

}
