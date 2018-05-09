
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

import DDS.ANY_INSTANCE_STATE;
import DDS.ANY_SAMPLE_STATE;
import DDS.ANY_VIEW_STATE;
import DDS.DataReader;
import DDS.DataReaderQosHolder;
import DDS.DomainParticipant;
import DDS.DomainParticipantFactory;
import DDS.DurabilityQosPolicyKind;
import DDS.HistoryQosPolicyKind;
import DDS.PARTICIPANT_QOS_DEFAULT;
import DDS.STATUS_MASK_NONE;
import DDS.SampleInfoSeqHolder;
import DDS.Subscriber;
import DDS.SubscriberQosHolder;
import DDS.Topic;
import DDS.TopicQosHolder;


public class ShapesSubscriber
{

   private static final Logger logger = LoggerFactory.getLogger(ShapesSubscriber.class);

   private static final String CIRCLE_TOPIC_NAME = "Circle";
   private static final String SQUARE_TOPIC_NAME = "Square";
   private static final String TRIANGLE_TOPIC_NAME = "Triangle";

   private DomainParticipantFactory dpf;
   private DomainParticipant participant;
   private String typeName;
   private TopicQosHolder topicQos = new TopicQosHolder();
   private Topic circle, square, triangle;
   private SubscriberQosHolder subQos = new SubscriberQosHolder();
   private Subscriber subscriber;
   private DataReaderQosHolder RQosH = new DataReaderQosHolder();
   private ShapeTypeDataReader readerCircle, readerSquare, readerTriangle;

   public ShapesSubscriber()
   {
      this(0);
   }

   public ShapesSubscriber(int domain)
   {
      logger.info("ShapesSubscriber starting in domain: " + domain + " ... ");
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
      // Create Subscriber
      status = participant.get_default_subscriber_qos(subQos);
      ErrorHandler.checkStatus(status,
            "DomainParticipant.get_default_subscriber_qos");
      subscriber = participant.create_subscriber(subQos.value, null,
            STATUS_MASK_NONE.value);
      ErrorHandler.checkHandle(subscriber,
            "DomainParticipant.create_subscriber");
      // Create 3 DataReaders: circle, square and triangle
      subscriber.get_default_datareader_qos(RQosH);
      subscriber.copy_from_topic_qos(RQosH, topicQos.value);
      RQosH.value.durability.kind = DurabilityQosPolicyKind.VOLATILE_DURABILITY_QOS;
      RQosH.value.history.kind = HistoryQosPolicyKind.KEEP_LAST_HISTORY_QOS;
      RQosH.value.history.depth = 100;

      DataReader dr = subscriber.create_datareader(circle, RQosH.value, null,
            STATUS_MASK_NONE.value);
      ErrorHandler.checkHandle(dr, "Subscriber.create_datareader");
      readerCircle = ShapeTypeDataReaderHelper.narrow(dr);

      dr = subscriber.create_datareader(square, RQosH.value, null, STATUS_MASK_NONE.value);
      ErrorHandler.checkHandle(dr, "Subscriber.create_datareader");
      readerSquare = ShapeTypeDataReaderHelper.narrow(dr);

      dr = subscriber.create_datareader(triangle, RQosH.value, null, STATUS_MASK_NONE.value);
      ErrorHandler.checkHandle(dr, "Subscriber.create_datareader");
      readerTriangle = ShapeTypeDataReaderHelper.narrow(dr);

      logger.info("ShapesSubscriber started... ");
   }


   public ShapeType readCircle(int timeOutMilliSec)
   {
      ShapeType sample = null;
      long timeout = System.currentTimeMillis() + timeOutMilliSec;

      // take in loop until a sample is read or until timeout
      ShapeTypeSeqHolder shapeSeq = new ShapeTypeSeqHolder();
      SampleInfoSeqHolder infoSeq = new SampleInfoSeqHolder();
      while (sample == null && System.currentTimeMillis() < timeout)
      {
         try
         {
            readerCircle.take(shapeSeq, infoSeq, 1,
                  ANY_SAMPLE_STATE.value, ANY_VIEW_STATE.value,
                  ANY_INSTANCE_STATE.value);
            if (shapeSeq.value.length > 0 && infoSeq.value[0].valid_data)
            {
               sample = shapeSeq.value[0];
               logger.debug("Circle read: " + sample.color + " (" + sample.x + "," + sample.y
                     + ") size=" + sample.shapesize);
            }
            Thread.sleep(100);
         }
         catch (InterruptedException ie)
         {
         }
      }
      if (sample == null)
      {
         logger.debug("Timeout (" + timeOutMilliSec + ") expired trying to read a Circle.");
      }
      return sample;
   }

   public ShapeType readSquare(int timeOutMilliSec)
   {
      ShapeType sample = null;
      long timeout = System.currentTimeMillis() + timeOutMilliSec;

      // take in loop until a sample is read or until timeout
      ShapeTypeSeqHolder shapeSeq = new ShapeTypeSeqHolder();
      SampleInfoSeqHolder infoSeq = new SampleInfoSeqHolder();
      while (sample == null && System.currentTimeMillis() < timeout)
      {
         try
         {
            readerSquare.take(shapeSeq, infoSeq, 1,
                  ANY_SAMPLE_STATE.value, ANY_VIEW_STATE.value,
                  ANY_INSTANCE_STATE.value);
            if (shapeSeq.value.length > 0 && infoSeq.value[0].valid_data)
            {
               sample = shapeSeq.value[0];
               logger.debug("Square read: " + sample.color + " (" + sample.x + "," + sample.y
                     + ") size=" + sample.shapesize);
            }
            Thread.sleep(100);
         }
         catch (InterruptedException ie)
         {
         }
      }
      if (sample == null)
      {
         logger.debug("Timeout (" + timeOutMilliSec + ") expired trying to read a Square.");
      }
      return sample;
   }

   public ShapeType readTriangle(int timeOutMilliSec)
   {
      ShapeType sample = null;
      long timeout = System.currentTimeMillis() + timeOutMilliSec;

      // take in loop until a sample is read or until timeout
      ShapeTypeSeqHolder shapeSeq = new ShapeTypeSeqHolder();
      SampleInfoSeqHolder infoSeq = new SampleInfoSeqHolder();
      while (sample == null && System.currentTimeMillis() < timeout)
      {
         try
         {
            readerTriangle.take(shapeSeq, infoSeq, 1,
                  ANY_SAMPLE_STATE.value, ANY_VIEW_STATE.value,
                  ANY_INSTANCE_STATE.value);
            if (shapeSeq.value.length > 0 && infoSeq.value[0].valid_data)
            {
               sample = shapeSeq.value[0];
               logger.debug("Triangle read: " + sample.color + " (" + sample.x + "," + sample.y
                     + ") size=" + sample.shapesize);
            }
            Thread.sleep(100);
         }
         catch (InterruptedException ie)
         {
         }
      }
      if (sample == null)
      {
         logger.debug("Timeout (" + timeOutMilliSec + ") expired trying to read a Triangle.");
      }
      return sample;
   }

   public void terminate()
   {
      // delete topics
      participant.delete_topic(circle);
      participant.delete_topic(square);
      participant.delete_topic(triangle);
      // delete data readers
      subscriber.delete_datareader(readerCircle);
      subscriber.delete_datareader(readerSquare);
      subscriber.delete_datareader(readerTriangle);
      // delete publisher
      participant.delete_subscriber(subscriber);
      // delete contained entities
      participant.delete_contained_entities();
      // delete participant
      dpf.delete_participant(participant);
   }

}
