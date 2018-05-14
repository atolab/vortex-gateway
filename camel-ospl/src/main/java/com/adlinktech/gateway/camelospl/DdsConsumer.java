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

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.apache.camel.FailedToCreateConsumerException;
import org.apache.camel.Processor;
import org.apache.camel.SuspendableService;
import org.apache.camel.impl.DefaultConsumer;
import org.slf4j.Logger;

import DDS.ALIVE_INSTANCE_STATE;
import DDS.ANY_INSTANCE_STATE;
import DDS.ANY_SAMPLE_STATE;
import DDS.ANY_VIEW_STATE;
import DDS.ConditionSeqHolder;
import DDS.ContentFilteredTopic;
import DDS.DataReader;
import DDS.DataReaderQos;
import DDS.DataReaderQosHolder;
import DDS.GuardCondition;
import DDS.LENGTH_UNLIMITED;
import DDS.NOT_ALIVE_INSTANCE_STATE;
import DDS.NOT_READ_SAMPLE_STATE;
import DDS.SampleInfo;
import DDS.SampleInfoSeqHolder;
import DDS.StatusCondition;
import DDS.Subscriber;
import DDS.SubscriberQos;
import DDS.SubscriberQosHolder;
import DDS.Topic;
import DDS.TopicQosHolder;
import DDS.WaitSet;

import com.adlinktech.gateway.camelospl.utils.Loggers;


/**
 * A {@link org.apache.camel.Consumer Consumer} which listens to DDS data.
 */
public class DdsConsumer
   extends DefaultConsumer
   implements SuspendableService, Runnable
{

   /** Logger */
   private static final Logger LOG = Loggers.LOGGER_CONSUMER;

   /** Suffix appended to topic type name for data reader. */
   private static final String READER_CLASS_SUFFIX = "DataReaderImpl";
   /** Suffix appended to topic type name for data sequence. */
   private static final String SEQUENCE_CLASS_SUFFIX = "SeqHolder";
   /** Field name of the SeqHolder class. */
   private static final String SEQ_VAL_FIELD = "value";
   /** Data reader method used to take data. */
   private static final String TAKE_METHOD = "take";
   /** Data reader method used to read data. */
   private static final String READ_METHOD = "read";
   /** Data reader method used to return data loan. */
   private static final String RETURN_LOAN_METHOD = "return_loan";

   /** parent endpoint (as DdsEndpoint object) */
   private final DdsEndpoint endpoint;

   /** Typed DDS DataReader class */
   private Class<?> readerClass;
   /** DDS DataReader take() method */
   private Method takeMethod;
   /** DDS DataReader read() method */
   private Method readMethod;
   /** DDS DataReader return_loan() method */
   private Method returnLoanMethod;
   /** DDS SeqHolder class. */
   private Class<?> seqHolderClass;
   /** "value" field of the DDS SeqHolder class. */
   private Field seqHolderValueField;
   /** Pre-allocated instance of SeqHolder class. **/
   private Object seqHolderObject;
   /** Pre-allocated instance of SampleInfoSeqHolder. */
   private SampleInfoSeqHolder infoSeqHolder;

   /** DDS Subscriber */
   private Subscriber subscriber = null;
   /** DDS DataReader */
   private DataReader reader = null;
   /** DDS WaitSet */
   private WaitSet waitSet = null;
   /** DDS StatusCondition triggered by WaitSet */
   private StatusCondition statusCondition = null;
   /** DDS GuardConditon to interrupt the WaitSet */
   private GuardCondition guardCondition = null;

   /** lock for DataReader concurrent access */
   private Object readerLock = new Object();

   /** Thread waiting on DDS WaitSet and taking DDS samples */
   private Thread readerThread = null;


   /**
    * Constructor.
    * 
    * @param endpoint the parent Endpoint
    * @param processor the first Processor to send exchanges
    */
   public DdsConsumer(DdsEndpoint endpoint, Processor processor)
   {
      super(endpoint, processor);
      this.endpoint = endpoint;

      try
      {
         // Get DataReader implementation class
         String readerName = endpoint.getTypeName() + READER_CLASS_SUFFIX;
         LOG.debug("load class {}", readerName);
         readerClass = endpoint.getCamelContext().getClassResolver()
               .resolveMandatoryClass(readerName);

         // Get data sequence class
         String seqName = endpoint.getTypeName() + SEQUENCE_CLASS_SUFFIX;
         LOG.debug("load class {}", seqName);
         seqHolderClass = endpoint.getCamelContext().getClassResolver()
               .resolveMandatoryClass(seqName);

         // Get data sequence class' value field
         seqHolderValueField = seqHolderClass.getDeclaredField(SEQ_VAL_FIELD);

         // Cache reference to DataReader take() method
         Class<?>[] takeMethodParams =
         {
               seqHolderClass,
               SampleInfoSeqHolder.class,
               int.class,
               int.class,
               int.class,
               int.class
         };
         takeMethod = readerClass.getMethod(
               TAKE_METHOD, takeMethodParams);

         // Cache reference to DataReader read() method
         Class<?>[] readMethodParams =
         {
               seqHolderClass,
               SampleInfoSeqHolder.class,
               int.class,
               int.class,
               int.class,
               int.class
         };
         readMethod = readerClass.getMethod(
               READ_METHOD, readMethodParams);

         // Cache reference to DataReader return_loan() method
         Class<?>[] returnLoanMethodParams =
         {
               seqHolderClass, SampleInfoSeqHolder.class
         };
         returnLoanMethod = readerClass.getMethod(
               RETURN_LOAN_METHOD, returnLoanMethodParams);

         // Pre-allocate a data sequence object
         seqHolderObject = seqHolderClass.newInstance();
         // Pre-allocate a SampleInfo sequence object
         infoSeqHolder = new SampleInfoSeqHolder();

      }
      catch (Exception e)
      {
         throw new FailedToCreateConsumerException(endpoint, e);
      }
   }


   @Override
   protected synchronized void doStart() throws Exception
   {
      LOG.debug("{} : doStart()", this);
      super.doStart();
      createSubscriber();
      createDataReader();
      createWaitSet();
      // create ReaderThread
      readerThread = new Thread(this, toString() + " ReaderThread");
      // do resume to start the thread
      doResume();
   }


   @Override
   protected synchronized void doStop() throws Exception
   {
      LOG.debug("{} : doStop()", this);
      // do suspend at first to stop the thread
      doSuspend();
      deleteWaitSet();
      deleteDataReader();
      deleteSubscriber();
      super.doStop();
   }

   @Override
   protected synchronized void doSuspend() throws Exception
   {
      LOG.debug("{} : doSuspend()", this);

      // trigger GuardCondition to gracefully stop the thread
      if (guardCondition != null)
      {
         guardCondition.set_trigger_value(true);
      }

      // wait thread to terminate
      if (readerThread != null)
      {
         readerThread.join(1000);
         if (readerThread.isAlive())
         {
            LOG.warn("{} : ReaderThread to suspend is still running. Interrupt it.", this);
            readerThread.interrupt();
         }
         readerThread = null;
      }
      super.doSuspend();
   }

   @Override
   protected synchronized void doResume() throws Exception
   {
      LOG.debug("{} : doResume()", this);
      super.doResume();
      // start the thread
      readerThread.start();
   }

   /**
    * Create the DDS Subscriber (if already created, do nothing).
    * 
    * @throws DdsException if a DDS error occurs.
    */
   private void createSubscriber() throws DdsException
   {
      if (subscriber == null)
      {
         LOG.debug("{} : createSubscriber", endpoint);

         // get default Subscriber QoS
         SubscriberQosHolder subscriberQosHolder = new SubscriberQosHolder();
         int status = endpoint.getParticipant().get_default_subscriber_qos(subscriberQosHolder);
         DdsErrorHandler.checkStatus(status, "Get default subscriber QoS");

         // add extra QoS set on endpoint
         QosUtils.updateQos(subscriberQosHolder, endpoint.getSubscriberQosMap());

         // create DDS Publisher
         subscriber = endpoint.getParticipant().create_subscriber(subscriberQosHolder.value,
               null, // listener
               DDS.STATUS_MASK_NONE.value);
         DdsErrorHandler.checkHandle(subscriber, "create subscriber");
      }
   }

   /**
    * Delete the DDS Subscriber (if not created, do nothing).
    * 
    * @throws DdsException if a DDS error occurs.
    */
   private void deleteSubscriber() throws DdsException
   {
      if (subscriber != null)
      {
         LOG.debug("{} : deleteSubscriber", endpoint);

         // delete Publisher
         int status = endpoint.getParticipant().delete_subscriber(subscriber);
         DdsErrorHandler.checkStatus(status, "Delete Publisher");

         subscriber = null;
      }
   }

   /**
    * Get the DDS Subscriber (or null if not created).
    * 
    * @return the DDS Subscriber or null
    */
   public Subscriber getSubscriber()
   {
      return subscriber;
   }

   private SubscriberQosHolder getSubscriberQoS() throws DdsException
   {
      SubscriberQosHolder qos = new SubscriberQosHolder();
      int status = subscriber.get_qos(qos);
      DdsErrorHandler.checkStatus(status, "Get Subscriber QoS");
      return qos;
   }

   private void setSubscriberQoS(SubscriberQos qos) throws DdsException
   {
      int status = subscriber.set_qos(qos);
      DdsErrorHandler.checkStatus(status, "Set Subscriber QoS");
   }


   /**
    * Delete the DDS DataReader (if not created, do nothing).
    * 
    * @throws DdsException if a DDS error occurs.
    */
   private void createDataReader() throws DdsException
   {
      if (reader == null)
      {
         LOG.debug("{} : createDataReader", endpoint);

         Topic topic = endpoint.getTopic();
         ContentFilteredTopic cfTopic = endpoint.getContentFilteredTopic();

         // Get Topic QoS to copy into DataReader QoS
         TopicQosHolder topicQosHolder = new TopicQosHolder();
         int status = topic.get_qos(topicQosHolder);
         DdsErrorHandler.checkStatus(status, "Get topic QoS");

         // Get default DataReader QoS
         DataReaderQosHolder readerQosHolder = new DataReaderQosHolder();
         status = subscriber.get_default_datareader_qos(readerQosHolder);
         DdsErrorHandler.checkStatus(status, "Get default DataReader QoS");

         // Copy Topic QoS
         status = subscriber.copy_from_topic_qos(readerQosHolder,
               topicQosHolder.value);
         DdsErrorHandler.checkStatus(status, "Copy topic QoS");

         // Add extra QoS set on endpoint
         QosUtils.updateQos(readerQosHolder, endpoint.getDataReaderQosMap());

         // Create DataReader
         if (cfTopic != null)
         {
            LOG.debug("{} : create DataReader on filtered topic {}", endpoint, cfTopic.get_name());
            reader = subscriber.create_datareader(
                  cfTopic,
                  readerQosHolder.value,
                  null, // listener
                  DDS.STATUS_MASK_NONE.value);
            DdsErrorHandler.checkHandle(reader, "Create DataReader on filtered topic");
         }
         else
         {
            LOG.debug("{} : create DataReader on topic {}", endpoint, topic.get_name());
            reader = subscriber.create_datareader(
                  topic,
                  readerQosHolder.value,
                  null, // listener
                  DDS.STATUS_MASK_NONE.value);
            DdsErrorHandler.checkHandle(reader, "Create DataReader");
         }

      }
   }

   /**
    * Delete the DDS DataReader (if not created, do nothing).
    * 
    * @throws DdsException if a DDS error occurs.
    */
   private void deleteDataReader() throws DdsException
   {
      if (reader != null)
      {
         LOG.debug("{} : deleteDataReader", endpoint);

         // delete DataReader
         int status = subscriber.delete_datareader(reader);
         DdsErrorHandler.checkStatus(status, "Delete DataReader");

         reader = null;
      }
   }

   /**
    * Get the DDS DataReader (or null if not created).
    * 
    * @return the DDS DataReader or null
    */
   public DataReader getDataReader()
   {
      return reader;
   }

   private DataReaderQosHolder getDataReaderQoS() throws DdsException
   {
      DataReaderQosHolder qos = new DataReaderQosHolder();
      int status = reader.get_qos(qos);
      DdsErrorHandler.checkStatus(status, "Get DataReader QoS");
      return qos;
   }

   private void setDataReaderQoS(DataReaderQos qos) throws DdsException
   {
      int status = reader.set_qos(qos);
      DdsErrorHandler.checkStatus(status, "Set DataReader QoS");
   }


   private void createWaitSet() throws DdsException
   {
      // create a new WaitSet
      waitSet = new WaitSet();
      DdsErrorHandler.checkHandle(waitSet, "New WaitSet");

      // create a new GuardCondition to interrupt WaitSet
      guardCondition = new GuardCondition();
      DdsErrorHandler.checkHandle(guardCondition, "New GuardCondition");

      // get DataReader's StatusCondition
      statusCondition = reader.get_statuscondition();
      DdsErrorHandler.checkHandle(statusCondition, "Get StatusCondition");
      DdsErrorHandler.checkStatus(
            statusCondition.set_enabled_statuses(DDS.DATA_AVAILABLE_STATUS.value),
            "StatusCondition set_enabled_statuses(DATA_AVAILABLE_STATUS)");

      // attach conditions to WaitSet
      DdsErrorHandler.checkStatus(
            waitSet.attach_condition(guardCondition),
            "WaitSet attach GuardCondition");
      DdsErrorHandler.checkStatus(
            waitSet.attach_condition(statusCondition),
            "WaitSet attach StatusCondition");
   }

   private void deleteWaitSet() throws DdsException
   {
      if (waitSet != null)
      {
         // detach conditions from WaitSet
         DdsErrorHandler.checkStatus(
               waitSet.detach_condition(guardCondition),
               "WaitSet detach GuardCondition");
         DdsErrorHandler.checkStatus(
               waitSet.detach_condition(statusCondition),
               "WaitSet detach StatusCondition");
      }

      statusCondition = null;
      guardCondition = null;
      waitSet = null;
   }

   @Override
   public void run()
   {
      LOG.debug("{} : run()", this);
      boolean stopped = false;
      ConditionSeqHolder triggeredConditions = new ConditionSeqHolder();

      while ( !stopped)
      {
         try
         {
            DdsErrorHandler.checkStatus(
                  waitSet._wait(triggeredConditions, DDS.DURATION_INFINITE.value),
                  "WaitSet waiting");
         }
         catch (DdsException e)
         {
            LOG.warn("{} : Exception waiting on WaitSet: {}", this, e.toString());
            getExceptionHandler().handleException(e);
         }

         for (int i = 0; i < triggeredConditions.value.length; i++)
         {
            if (triggeredConditions.value[i] == statusCondition)
            {
               LOG.debug("{} : data available", this);
               if (endpoint.isReadMode())
               {
                  // read mode: take disposed data and read alive data
                  getAndProcessData(false,
                        ANY_SAMPLE_STATE.value,
                        ANY_VIEW_STATE.value,
                        NOT_ALIVE_INSTANCE_STATE.value);
                  getAndProcessData(true,
                        NOT_READ_SAMPLE_STATE.value,
                        ANY_VIEW_STATE.value,
                        ALIVE_INSTANCE_STATE.value);
               }
               else
               {
                  // take mode: take all data
                  getAndProcessData(false,
                        ANY_SAMPLE_STATE.value,
                        ANY_VIEW_STATE.value,
                        ANY_INSTANCE_STATE.value);
               }

            }
            else if (triggeredConditions.value[i] == guardCondition)
            {
               LOG.debug("{} : run stopped", this);
               stopped = true;
            }
         }
      }
   }


   /**
    * This method is called by the readerThread when new available DDS samples are detected.
    * We take (or read) each sample and create an exchange, which is then
    * processed to push the message on to the next part of the Camel route.
    */
   private void getAndProcessData(boolean readMode,
         int sampleState,
         int viewState,
         int instanceState)
   {
      Object status = null;
      try
      {
         // Set read()/take() method parameters
         Object[] methodParams =
         {
               seqHolderObject,
               infoSeqHolder,
               LENGTH_UNLIMITED.value,
               sampleState,
               viewState,
               instanceState
         };

         // do read() or take()
         synchronized (readerLock)
         {
            if (readMode)
            {
               status = readMethod.invoke(reader, methodParams);
            }
            else
            {
               status = takeMethod.invoke(reader, methodParams);
            }
         }
         DdsErrorHandler.checkStatus( ((Integer) status).intValue(),
               reader.getClass() + (readMode ? ".read()" : ".take()"));

         // get "value" field of seqHolderObject
         Object val = seqHolderValueField.get(seqHolderObject);

         // Process each sample
         int len = Array.getLength(val);
         Object sample = null;
         for (int i = 0; i < len; ++i)
         {
            SampleInfo info = (SampleInfo) infoSeqHolder.value[i];

            // Check if we must ignore local publishers and
            // if sample is coming from a local publisher
            if (endpoint.isIgnoreLocalPublishers() &&
                  endpoint.getComponent().isPublicationHandleLocal(info.publication_handle))
            {
               // drop sample
               LOG.trace("Sample from local publisher {} is dropped", info.publication_handle);
            }
            else
            {
               sample = Array.get(val, i);
               // create and send Exchange to first Processor
               getProcessor().process(endpoint.createExchange(sample, info));
            }
         }
      }
      catch (Exception e)
      {
         LOG.warn("{} : Reader" + (readMode ? ".read()" : ".take()") + " failed",
               endpoint, e);
         getExceptionHandler().handleException(e);
         // no need to go further since return_loan() operation will fail the same.
         return;
      }

      try
      {
         // Invoke return_loan() operation
         Object[] returnLoanMethodParams =
         {
               seqHolderObject, infoSeqHolder
         };
         status = returnLoanMethod.invoke(reader, returnLoanMethodParams);
         DdsErrorHandler.checkStatus( ((Integer) status).intValue(),
               reader.getClass() + ".return_loan ()");
      }
      catch (Exception e)
      {
         LOG.warn("{} : Reader return_loan failed", endpoint, e);
         getExceptionHandler().handleException(e);
      }
   }

   /**
    * Dynamically change the DeadlinePeriod QoS of the {@link DataReader} associated with this
    * DdsConsumer.
    * 
    * @param period the new period (in seconds)
    * @throws DdsException if an error occurs setting the new DeadlinePeriod
    */
   public void changeDeadlinePeriod(double period)
      throws DdsException
   {
      int seconds = QosUtils.timeToSec(period);
      int nanoseconds = QosUtils.timeToNanosec(period);

      synchronized (readerLock)
      {
         // get DataReader QoS
         DataReaderQosHolder qos = getDataReaderQoS();

         // change deadline period
         qos.value.deadline.period.sec = seconds;
         qos.value.deadline.period.nanosec = nanoseconds;

         // apply modified DataReader QoS
         setDataReaderQoS(qos.value);
      }
   }

   /**
    * Dynamically change the LatencyDuration QoS of the {@link DataReader} associated with this
    * DdsConsumer.
    * 
    * @param duration the new duration (in seconds)
    * @throws DdsException if an error occurs setting the new LatencyDuration
    */
   public void changeLatencyDuration(double duration)
      throws DdsException
   {
      int seconds = QosUtils.timeToSec(duration);
      int nanoseconds = QosUtils.timeToNanosec(duration);

      synchronized (readerLock)
      {
         // get DataReader QoS
         DataReaderQosHolder qos = getDataReaderQoS();

         // change latency_budget duration
         qos.value.latency_budget.duration.sec = seconds;
         qos.value.latency_budget.duration.nanosec = nanoseconds;

         // apply modified DataReader QoS
         setDataReaderQoS(qos.value);
      }
   }

   /**
    * Dynamically change the TimebasedFilter QoS of the {@link DataReader} associated with this
    * DdsConsumer.
    * 
    * @param timebasedFilter the new TimebasedFilter minimum_separation (in seconds)
    * @throws DdsException if an error occurs setting the new TimebasedFilter
    */
   public void changeTimebasedFilter(double timebasedFilter)
      throws DdsException
   {
      int seconds = QosUtils.timeToSec(timebasedFilter);
      int nanoseconds = QosUtils.timeToNanosec(timebasedFilter);

      synchronized (readerLock)
      {
         // get DataReader QoS
         DataReaderQosHolder qos = getDataReaderQoS();

         // change time_based_filter minimum_separation
         qos.value.time_based_filter.minimum_separation.sec = seconds;
         qos.value.time_based_filter.minimum_separation.nanosec = nanoseconds;

         // apply modified DataReader QoS
         setDataReaderQoS(qos.value);
      }
   }

   /**
    * Dynamically change the Partition QoS of the {@link Subscriber} associated with this
    * DdsConsumer.
    * 
    * @param partitionStr the new Partition expression (using ',' as delimiter)
    * @throws DdsException if an error occurs setting the new Partition
    */
   public void changePartition(String partitionStr)
      throws DdsException
   {
      String[] partition = partitionStr.split(DdsEndpoint.PARTITION_DELIMITER);

      synchronized (readerLock)
      {
         // get Subscriber QoS
         SubscriberQosHolder qos = getSubscriberQoS();

         // change transport priority
         qos.value.partition.name = partition;

         // apply modified Subscriber QoS
         setSubscriberQoS(qos.value);
      }
   }

}
