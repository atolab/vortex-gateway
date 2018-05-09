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
package com.adlinktech.gateway.camelospl;

import java.lang.reflect.Method;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.FailedToCreateProducerException;
import org.apache.camel.Message;
import org.apache.camel.RuntimeExchangeException;
import org.apache.camel.SuspendableService;
import org.apache.camel.impl.DefaultProducer;
import org.slf4j.Logger;

import DDS.DataWriter;
import DDS.DataWriterQos;
import DDS.DataWriterQosHolder;
import DDS.HANDLE_NIL;
import DDS.Publisher;
import DDS.PublisherQos;
import DDS.PublisherQosHolder;
import DDS.Topic;
import DDS.TopicQosHolder;

import com.adlinktech.gateway.camelospl.utils.Loggers;


/**
 * A {@link org.apache.camel.Producer Producer} which writes DDS data.
 */
public class DdsProducer
   extends DefaultProducer
   implements SuspendableService
{

   /** Logger */

   private static final Logger LOG = Loggers.LOGGER_PRODUCER;

   /** Suffix appended to topic type name for data writer. */
   private static final String WRITER_CLASS_SUFFIX = "DataWriterImpl";
   /** Data writer method used to write data. */
   private static final String WRITE_METHOD = "write";
   /** Data writer dispose method used to write data. */
   private static final String DISPOSE_METHOD = "dispose";
   /** Data writer writedispose method used to write data. */
   private static final String WRITEDISPOSE_METHOD = "writedispose";
   /** Data writer writedispose method used to write data. */
   private static final String UNREGISTER_METHOD = "unregister_instance";

   /** Prefix of headers for dynamic QoS changes */
   private static final String HEADER_QOS_PREFIX = "dds.";
   /** Header name for OwnershipStrength QoS */
   private static final String HEADER_OWNERSHIP_STRENGTH =
         HEADER_QOS_PREFIX + "ownershipStrength";
   /** Header name for OwnershipStrength QoS */
   private static final String HEADER_TRANSPORT_PRIORITY =
         HEADER_QOS_PREFIX + "transportPriority";
   /** Header name for OwnershipStrength QoS */
   private static final String HEADER_LIFESPAN_DURATION =
         HEADER_QOS_PREFIX + "lifespanDuration";

   private static final double NANOSECONDS_IN_SECONDS = 1000000000;


   /** parent endpoint (as DdsEndpoint object) */
   private final DdsEndpoint endpoint;

   /** Typed DDS DataWriter class */
   private Class<?> writerClass;

   /** DDS DataWriter write() method */
   private Method writeMethod;
   /** DDS DataWriter writeDispose() method */
   private Method writeDisposeMethod;
   /** DDS DataWriter dispose() method */
   private Method disposeMethod;
   /** DDS DataWriter unregister() method */
   private Method unregisterMethod;

   /** DDS Publisher */
   private Publisher publisher = null;
   /** DDS DataWrite. */
   private DataWriter writer = null;
   /** lock for DataWrite concurrent access */
   private Object writerLock = new Object();


   /**
    * Constructor.
    * 
    * @param endpoint the parent Endpoint
    */
   public DdsProducer(DdsEndpoint endpoint)
   {
      super(endpoint);

      LOG.debug("DdsProducer[{}] constructor", endpoint);
      this.endpoint = endpoint;

      try
      {
         // Get DataWriter implementation class
         String writerName = endpoint.getTypeName() + WRITER_CLASS_SUFFIX;
         LOG.debug("load class {}", writerName);
         writerClass = endpoint.getCamelContext().getClassResolver()
               .resolveMandatoryClass(writerName);

         // Cache references to DataWriter methods
         Class<?>[] writeMethodParams =
         {
               endpoint.getTopicType(), long.class
         };
         writeMethod = writerClass.getMethod(
               WRITE_METHOD, writeMethodParams);
         disposeMethod = writerClass.getMethod(
               DISPOSE_METHOD, writeMethodParams);
         writeDisposeMethod = writerClass.getMethod(
               WRITEDISPOSE_METHOD, writeMethodParams);
         unregisterMethod = writerClass.getMethod(
               UNREGISTER_METHOD, writeMethodParams);
      }
      catch (Exception e)
      {
         throw new FailedToCreateProducerException(endpoint, e);
      }
   }

   @Override
   protected synchronized void doStart() throws Exception
   {
      LOG.debug("{} : doStart()", this);
      super.doStart();
      createPublisher();
      createDataWriter();
   }

   @Override
   protected synchronized void doStop() throws Exception
   {
      LOG.debug("{} : doStop()", this);
      deleteDataWriter();
      deletePublisher();
      super.doStop();
   }

   @Override
   protected synchronized void doSuspend() throws Exception
   {
      if (publisher != null)
      {
         int status = publisher.suspend_publications();
         DdsErrorHandler.checkStatus(status, "Suspend publications");
      }
      super.doSuspend();
   }

   @Override
   protected synchronized void doResume() throws Exception
   {
      if (publisher != null)
      {
         int status = publisher.resume_publications();
         DdsErrorHandler.checkStatus(status, "Resume publications");
      }
      super.doResume();
   }


   /**
    * Create the DDS Publisher (if already created, do nothing).
    * 
    * @throws DdsException if a DDS error occurs.
    */
   private void createPublisher() throws DdsException
   {
      if (publisher == null)
      {
         LOG.debug("{} : createPublisher", endpoint);

         // get default Publisher QoS
         PublisherQosHolder publisherQosHolder = new PublisherQosHolder();
         int status = endpoint.getParticipant().get_default_publisher_qos(publisherQosHolder);
         DdsErrorHandler.checkStatus(status, "Get default publisher QoS");

         // add extra QoS set on endpoint
         QosUtils.updateQos(publisherQosHolder, endpoint.getPublisherQosMap());

         // create DDS Publisher
         publisher = endpoint.getParticipant().create_publisher(publisherQosHolder.value,
               null, // listener
               DDS.STATUS_MASK_NONE.value);
         DdsErrorHandler.checkHandle(publisher, "create publisher");
      }
   }

   /**
    * Delete the DDS Publisher (if not created, do nothing).
    * 
    * @throws DdsException if a DDS error occurs.
    */
   private void deletePublisher() throws DdsException
   {
      if (publisher != null)
      {
         LOG.debug("{} : deletePublisher", endpoint);

         // delete Publisher
         int status = endpoint.getParticipant().delete_publisher(publisher);
         DdsErrorHandler.checkStatus(status, "Delete Publisher");

         publisher = null;
      }
   }

   /**
    * Get the DDS Publisher (or null if not created).
    * 
    * @return the DDS Publisher or null
    */
   public Publisher getPublisher()
   {
      return publisher;
   }

   private PublisherQosHolder getPublisherQoS() throws DdsException
   {
      PublisherQosHolder qos = new PublisherQosHolder();
      int status = publisher.get_qos(qos);
      DdsErrorHandler.checkStatus(status, "Get Publisher QoS");
      return qos;
   }

   private void setPublisherQoS(PublisherQos qos) throws DdsException
   {
      int status = publisher.set_qos(qos);
      DdsErrorHandler.checkStatus(status, "Set Publisher QoS");
   }


   /**
    * Delete the DDS DataWriter (if not created, do nothing).
    * 
    * @throws DdsException if a DDS error occurs.
    */
   private void createDataWriter() throws DdsException
   {
      synchronized (writerLock)
      {
         if (writer == null)
         {
            LOG.debug("{} : createDataWriter", endpoint);

            Topic topic = endpoint.getTopic();

            // Get Topic QoS to copy into DataWriter QoS
            TopicQosHolder topicQosHolder = new TopicQosHolder();
            int status = topic.get_qos(topicQosHolder);
            DdsErrorHandler.checkStatus(status, "Get topic QoS");

            // Get default DataWriter QoS
            DataWriterQosHolder writerQosHolder = new DataWriterQosHolder();
            status = publisher.get_default_datawriter_qos(writerQosHolder);
            DdsErrorHandler.checkStatus(status, "Get default DataWriter QoS");

            // Copy Topic QoS
            status = publisher.copy_from_topic_qos(writerQosHolder,
                  topicQosHolder.value);
            DdsErrorHandler.checkStatus(status, "Copy topic QoS");

            // Add extra QoS set on endpoint
            QosUtils.updateQos(writerQosHolder, endpoint.getDataWriterQosMap());

            // Create DataWriter
            LOG.debug("{} : create DataWriter on topic {}", endpoint, endpoint.getTopicName());
            writer = publisher.create_datawriter(
                  topic,
                  writerQosHolder.value,
                  null, // listener
                  DDS.STATUS_MASK_NONE.value);
            DdsErrorHandler.checkHandle(writer, "Create DataWriter");

            // Add its handle as publisherId (for filtering in colocalized DdsConsumers)
            endpoint.getComponent().addLocalPublicationHandle(writer.get_instance_handle());

         }
      }
   }

   /**
    * Delete the DDS DataWriter (if not created, do nothing).
    * 
    * @throws DdsException if a DDS error occurs.
    */
   private void deleteDataWriter() throws DdsException
   {
      synchronized (writerLock)
      {
         if (writer != null)
         {
            LOG.debug("{} : deleteDataWriter", endpoint);

            long publisherId = writer.get_instance_handle();

            // delete DataWriter
            int status = publisher.delete_datawriter(writer);
            DdsErrorHandler.checkStatus(status, "Delete DataWriter");

            // remove its publisherId
            endpoint.getComponent().removeLocalPublicationHandle(publisherId);

            writer = null;
         }
      }
   }

   /**
    * Get the DDS DataWriter (or null if not created).
    * 
    * @return the DDS DataWriter or null
    */
   public DataWriter getDataWriter()
   {
      return this.writer;
   }

   private DataWriterQosHolder getDataWriterQoS() throws DdsException
   {
      DataWriterQosHolder qos = new DataWriterQosHolder();
      int status = writer.get_qos(qos);
      DdsErrorHandler.checkStatus(status, "Get DataWriter QoS");
      return qos;
   }

   private void setDataWriterQoS(DataWriterQos qos) throws DdsException
   {
      int status = writer.set_qos(qos);
      DdsErrorHandler.checkStatus(status, "Set DataWriter QoS");
   }

   public void process(Exchange exchange)
   {
      // Get DDS data from IN message body
      Object data = exchange.getIn().getBody();

      // Check if the data type is the expected one
      if ( !endpoint.getTopicType().isInstance(data))
      {
         throw new RuntimeExchangeException(
               "Body is not a " + endpoint.getTopicType() + " object", exchange);
      }

      // Synchronize on DataWriter, in case of dynamic QoS change
      synchronized (writerLock)
      {
         try
         {
            boolean changeQoS = hasDynamicQos(exchange.getIn().getHeaders());
            DataWriterQosHolder oldQoS = null;
            if (changeQoS)
               oldQoS = changeDataWriterQoS(exchange.getIn());

            // Publish to DDS
            publishToDDS(data, exchange.getIn().getHeader("DDS_DISPOSE", DdsDisposeHeader.class));

            if (changeQoS)
               setDataWriterQoS(oldQoS.value);
         }
         catch (Exception e)
         {
            throw new RuntimeExchangeException(e.getMessage(), exchange, e);
         }
      }
   }


   /**
    * Do a DDS publication of specified data.
    * The DataWriter operation which is called depends from
    * the DdsDisposeHeader value:
    * <ul>
    * <li>if null : write()
    * <li>if WRITEDISPOSE : writedispose()
    * <li>if DISPOSE : dispose()
    * <li>if UNREGISTER : unregister_instance()
    * </ul>
    * 
    * @param data the DDS data to be published
    * @param dispose a DdsDisposeHeader enum
    * @throws Exception in case of error
    */
   protected void publishToDDS(Object data, DdsDisposeHeader dispose) throws Exception
   {
      Object[] arrWriteMethodParams =
      {
            data, HANDLE_NIL.value
      };
      if (dispose == null)
      {
         // call write()
         writeMethod.invoke(writer, arrWriteMethodParams);
      }
      else
      {
         if (dispose == DdsDisposeHeader.WRITEDISPOSE)
         {
            // call writedispose()
            writeDisposeMethod.invoke(writer, arrWriteMethodParams);
         }
         else if (dispose == DdsDisposeHeader.DISPOSE)
         {
            // call dispose()
            disposeMethod.invoke(writer, arrWriteMethodParams);
         }
         else if (dispose == DdsDisposeHeader.UNREGISTER)
         {
            // call unregister_instance()
            unregisterMethod.invoke(writer, arrWriteMethodParams);
         }
      }
   }


   private boolean hasDynamicQos(Map<String, Object> headers)
   {
      for (String key : headers.keySet())
      {
         if (key.startsWith(HEADER_QOS_PREFIX))
            return true;
      }
      return false;
   }


   private DataWriterQosHolder changeDataWriterQoS(Message msg) throws DdsException
   {
      // get old QoS to return after change
      DataWriterQosHolder oldQoS = getDataWriterQoS();

      // get old QoS to be overwritten
      DataWriterQosHolder newQoS = getDataWriterQoS();

      // copy QoS changes from headers
      updateQoS(newQoS, msg);

      // change DataWriter QoS
      setDataWriterQoS(newQoS.value);

      return oldQoS;
   }


   private void updateQoS(DataWriterQosHolder newQoS, Message msg)
   {
      // get OwnershipStrength and update QoS (if header is present)
      Integer strength = msg.getHeader(HEADER_OWNERSHIP_STRENGTH, Integer.class);
      if (strength != null)
      {
         newQoS.value.ownership_strength.value = strength.intValue();
      }

      // get TransportPriority and update QoS (if header is present)
      Integer prio = msg.getHeader(HEADER_TRANSPORT_PRIORITY, Integer.class);
      if (prio != null)
      {
         newQoS.value.transport_priority.value = prio.intValue();
      }

      // get OwnershipStrength and update QoS (if header is present)
      Double lifespan = msg.getHeader(HEADER_LIFESPAN_DURATION, Double.class);
      if (lifespan != null)
      {
         double duration = lifespan.doubleValue();
         // Convert the float to seconds and nanoseconds.
         newQoS.value.lifespan.duration.sec = (int) duration;
         newQoS.value.lifespan.duration.nanosec =
               (int) ( (duration - newQoS.value.lifespan.duration.sec) *
               NANOSECONDS_IN_SECONDS);
      }
   }

   public void changeDeadlinePeriod(double period) throws DdsException
   {
      int seconds = QosUtils.timeToSec(period);
      int nanoseconds = QosUtils.timeToNanosec(period);

      synchronized (writerLock)
      {
         // get DataWriter QoS
         DataWriterQosHolder qos = getDataWriterQoS();

         // change deadline period
         qos.value.deadline.period.sec = seconds;
         qos.value.deadline.period.nanosec = nanoseconds;

         // apply modified DataWriter QoS
         setDataWriterQoS(qos.value);
      }
   }

   public void changeLatencyDuration(double duration) throws DdsException
   {
      int seconds = QosUtils.timeToSec(duration);
      int nanoseconds = QosUtils.timeToNanosec(duration);

      synchronized (writerLock)
      {
         // get DataWriter QoS
         DataWriterQosHolder qos = getDataWriterQoS();

         // change latency_budget duration
         qos.value.latency_budget.duration.sec = seconds;
         qos.value.latency_budget.duration.nanosec = nanoseconds;

         // apply modified DataWriter QoS
         setDataWriterQoS(qos.value);
      }
   }


   public void changeOwnershipStrength(int strength) throws DdsException
   {
      synchronized (writerLock)
      {
         // get DataWriter QoS
         DataWriterQosHolder qos = getDataWriterQoS();

         // change transport priority
         qos.value.ownership_strength.value = strength;

         // apply modified DataWriter QoS
         setDataWriterQoS(qos.value);
      }
   }


   public void changeTransportPriority(int priority) throws DdsException
   {
      synchronized (writerLock)
      {
         // get DataWriter QoS
         DataWriterQosHolder qos = getDataWriterQoS();

         // change transport priority
         qos.value.transport_priority.value = priority;

         // apply modified DataWriter QoS
         setDataWriterQoS(qos.value);
      }
   }


   public void changeLifespanDuration(double duration) throws DdsException
   {
      int seconds = QosUtils.timeToSec(duration);
      int nanoseconds = QosUtils.timeToNanosec(duration);

      synchronized (writerLock)
      {
         // get DataWriter QoS
         DataWriterQosHolder qos = getDataWriterQoS();

         // change transport priority
         qos.value.lifespan.duration.sec = seconds;
         qos.value.lifespan.duration.nanosec = nanoseconds;

         // apply modified DataWriter QoS
         setDataWriterQoS(qos.value);
      }
   }


   public void changePartition(String partitionStr) throws DdsException
   {
      String[] partition = partitionStr.split(DdsEndpoint.PARTITION_DELIMITER);

      synchronized (writerLock)
      {
         // get Publisher QoS
         PublisherQosHolder qos = getPublisherQoS();

         // change transport priority
         qos.value.partition.name = partition;

         // apply modified Publisher QoS
         setPublisherQoS(qos.value);
      }
   }

}
