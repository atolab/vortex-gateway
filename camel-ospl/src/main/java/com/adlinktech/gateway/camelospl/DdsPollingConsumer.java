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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.camel.CamelExecutionException;
import org.apache.camel.Exchange;
import org.apache.camel.FailedToCreateConsumerException;
import org.apache.camel.IsSingleton;
import org.apache.camel.Message;
import org.apache.camel.impl.PollingConsumerSupport;
import org.slf4j.Logger;

import com.adlinktech.gateway.camelext.DynamicPollingConsumer;
import com.adlinktech.gateway.camelospl.utils.Loggers;

import DDS.ConditionSeqHolder;
import DDS.ContentFilteredTopic;
import DDS.DURATION_INFINITE;
import DDS.DataReader;
import DDS.DataReaderQosHolder;
import DDS.Duration_t;
import DDS.RETCODE_TIMEOUT;
import DDS.ReadCondition;
import DDS.SampleInfo;
import DDS.SampleInfoSeqHolder;
import DDS.Subscriber;
import DDS.SubscriberQosHolder;
import DDS.Topic;
import DDS.TopicQosHolder;
import DDS.WaitSet;


/**
 * PollingConsumer implementation for dds endpoint.
 * This consumer performs calls DataReader.read_w_condition() operation on demand.
 * Depending on options passed in receive operations, a ReadCondition or a QueryCondition
 * (if a query expression is provided) is created.
 */
public class DdsPollingConsumer
   extends PollingConsumerSupport
   implements DynamicPollingConsumer, IsSingleton
{

   /**
    * Option name for SampleStateMask to be used in ReadCondition or QueryCondition.
    * Default value is DDS.ANY_SAMPLE_STATE.
    */
   public static final String SAMPLE_STATE_MASK_OPTION = "DDS_SAMPLE_STATE";

   /**
    * Option name for ViewStateMask to be used in ReadCondition or QueryCondition.
    * Default value is DDS.ANY_VIEW_STATE.
    */
   public static final String VIEW_STATE_MASK_OPTION = "DDS_VIEW_STATE";

   /**
    * Option name for InstanceStateMask to be used in ReadCondition or QueryCondition.
    * Default value is DDS.ANY_INSTANCE_STATE.
    */
   public static final String INSTANCE_STATE_MASK_OPTION = "DDS_INSTANCE_STATE";

   /**
    * Option name for query expression to be used in QueryCondition.
    */
   public static final String QUERY_EXPRESSION_OPTION = "DDS_QUERY_EXPRESSION";

   /**
    * Option name for SampleStateMask to be used in ReadCondition or QueryCondition.
    */
   public static final String QUERY_PARAMS_OPTION = "DDS_QUERY_PARAMETERS";


   private static final Logger LOG = Loggers.LOGGER_CONSUMER;

   /** Suffix appended to topic type name for data reader. */
   private static final String READER_CLASS_SUFFIX = "DataReaderImpl";
   /** Suffix appended to topic type name for data sequence. */
   private static final String SEQUENCE_CLASS_SUFFIX = "SeqHolder";
   /** Field name of the SeqHolder class. */
   private static final String SEQ_VAL_FIELD = "value";
   /** Data reader method used to read data. */
   private static final String READ_W_CONDITION_METHOD = "read_w_condition";
   /** Data reader method used to return data loan. */
   private static final String RETURN_LOAN_METHOD = "return_loan";

   /** parent endpoint (as DdsEndpoint object) */
   private final DdsEndpoint endpoint;

   /** Typed DDS DataReader class */
   private Class<?> readerClass;
   /** DDS DataReader read_w_condition() method */
   private Method readWConditionMethod;
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
   /** DDS data reader. */
   private DataReader reader;


   /**
    * Constructor.
    * 
    * @param endpoint the parent Endpoint
    */
   public DdsPollingConsumer(DdsEndpoint endpoint)
   {
      super(endpoint);
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

         // Cache reference to DataReader read_w_condition() method
         Class<?>[] readWCondMethodParams =
         {
               seqHolderClass, SampleInfoSeqHolder.class, int.class, ReadCondition.class
         };
         readWConditionMethod = readerClass.getMethod(
               READ_W_CONDITION_METHOD, readWCondMethodParams);

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
   public boolean isSingleton()
   {
      return true;
   }


   @Override
   protected void doStart() throws Exception
   {
      createSubscriber();
      createDataReader();
   }


   @Override
   protected void doStop() throws Exception
   {
      deleteDataReader();
      deleteSubscriber();
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

         /*****************************************************************************************/
         /*
          * NOTE: This is a workaround for a StackOverflowError exception we get if QueryCondition
          * is created for the first during a Camel route (it seems that creation of
          * QueryConditionImpl object from native code implies lot of calls to ClassLoader,
          * and as Camel already imply a big call stack, we go overflow).
          * Here, we create a foo QueryCondition just to have class loaded.
          */
         DataReader fooReader = reader.get_subscriber()
               .get_participant()
               .get_builtin_subscriber()
               .lookup_datareader("DCPSTopic");

         ReadCondition foo = fooReader.create_querycondition(
               DDS.ANY_SAMPLE_STATE.value, DDS.ANY_VIEW_STATE.value, DDS.ANY_INSTANCE_STATE.value,
               "name='foo'", null);
         fooReader.delete_readcondition(foo);
         // Another non-standard solution: ReadCondition foo = new
         // org.opensplice.dds.dcps.QueryConditionImpl();
         /*****************************************************************************************/
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


   @Override
   public Exchange receive()
   {
      return receive(null, -1);
   }

   @Override
   public Exchange receive(long timeout)
   {
      return receive(null, timeout);
   }

   @Override
   public Exchange receiveNoWait()
   {
      return receive(null, 0);
   }


   @Override
   public Exchange receive(Map<String, Object> options)
   {
      return receive(options, -1);
   }

   @Override
   public Exchange receiveNoWait(Map<String, Object> options)
   {
      return receive(options, 0);
   }

   @Override
   public Exchange receive(Map<String, Object> options, long timeout)
   {
      Exchange result = null;

      ReadCondition condition = null;
      try
      {
         // create ReadCondition according to options
         condition = createCondition(options);
      }
      catch (DdsException e)
      {
         LOG.error("{} : Failed to create ReadCondition: {}", endpoint, e);
         throw new CamelExecutionException("Failed to create ReadCondition", null, e);
      }

      try
      {
         if (timeout > 0 || timeout == -1)
         {
            // Wait on condition with timeout
            boolean conditionTriggered = doWaitOnCondition(condition, timeout);
            if ( !conditionTriggered)
            {
               LOG.warn("{} : PollingConsumer TIME OUT", endpoint);
               // return null exchange
               return null;
            }
         }

         // do read with condition
         result = doReadWithCondition(condition);

      }
      catch (DdsException e)
      {
         throw new CamelExecutionException("Error in DdsPollingConsumer.receive()", null, e);
      }
      finally
      {
         // Invoke delete_readcondition() operation
         int status = reader.delete_readcondition(condition);
         try
         {
            DdsErrorHandler.checkStatus( ((Integer) status).intValue(),
                  "Error in " + reader.getClass() + ".delete_readcondition ()");
         }
         catch (DdsException e)
         {
            LOG.error("{} : failed to delete ReadCondition: {}", endpoint, e);
         }
      }

      return result;
   }


   /**
    * Wait on a ReadCondition with a timeout
    * 
    * @param condition the ReadCondition
    * @param timeout the timeout
    * @return true if condition is triggered, false if timeout occurs
    * @throws DdsException if a DDS error occurs
    */
   private boolean doWaitOnCondition(ReadCondition condition, long timeout)
      throws DdsException
   {
      // Use a WaiSet to wait for data
      ConditionSeqHolder conditionList = new DDS.ConditionSeqHolder();
      Duration_t time =
            (timeout > 0 ?
                  new Duration_t((int) TimeUnit.MILLISECONDS.toSeconds(timeout), 0) :
                  DURATION_INFINITE.value
            );
      WaitSet w = new WaitSet();

      // attach condition to WaiSet
      int status = w.attach_condition(condition);
      DdsErrorHandler.checkStatus(status, "Error in WaiSet.attach_condition()");

      // wait on WaitSet
      status = w._wait(conditionList, time);

      // Check WaitSet return
      if (status == RETCODE_TIMEOUT.value)
      {
         LOG.warn("{} : PollingConsumer TIME OUT", endpoint);
         return false;
      }
      DdsErrorHandler.checkStatus(status, "Error in WaiSet._wait()");

      // Note: no need to check which condition has been triggered
      // since we only attached 1 condition.

      return true;
   }


   /**
    * Do a DDS read with condition, and return an Exchange with resutling data.
    * 
    * @param condition the ReadCondition
    * @return the Exchange with resulting data
    * @throws DdsException if a DDS error occurs
    */
   private Exchange doReadWithCondition(ReadCondition condition)
      throws DdsException
   {
      Exchange result = null;

      try
      {
         // Invoke read_w_condition() operation for 1 sample only
         Object[] readWConditionMethodParams =
         {
               seqHolderObject, infoSeqHolder, 1, condition
         };
         Object invokeStatus;
         invokeStatus = readWConditionMethod.invoke(reader, readWConditionMethodParams);
         DdsErrorHandler.checkStatus( ((Integer) invokeStatus).intValue(),
               reader.getClass() + ".read ()");

         // get "value" field of seqHolderObject
         Object val = seqHolderValueField.get(seqHolderObject);

         if (infoSeqHolder.value.length > 0)
         {
            // check if we must ignore the local publishers
            if (endpoint.isIgnoreLocalPublishers())
            {
               // return the first sample that doesn't come
               // from a local publisher (if any)
               for (int i = 0; i < infoSeqHolder.value.length; i++)
               {
                  SampleInfo info = (SampleInfo) infoSeqHolder.value[i];
                  if ( !endpoint.getComponent().isPublicationHandleLocal(info.publication_handle))
                  {
                     Object sample = Array.get(val, 0);
                     // create Exchange with sample
                     result = endpoint.createExchange(sample, info);
                     break;
                  }
               }

            }
            else
            {
               // return the first sample
               SampleInfo info = (SampleInfo) infoSeqHolder.value[0];
               Object sample = Array.get(val, 0);
               // create Exchange with sample
               result = endpoint.createExchange(sample, info);
            }
         }
         else
         {
            LOG.info("{} : Reader read_w_condition returned nothing", endpoint);
            // return null Exchange
         }

         // Invoke return_loan() operation
         Object[] returnLoanMethodParams =
         {
               seqHolderObject, infoSeqHolder
         };
         invokeStatus = returnLoanMethod.invoke(reader, returnLoanMethodParams);
         DdsErrorHandler.checkStatus( ((Integer) invokeStatus).intValue(),
               reader.getClass() + ".return_loan ()");

      }
      catch (IllegalArgumentException e)
      {
         throw new DdsException(e);
      }
      catch (IllegalAccessException e)
      {
         throw new DdsException(e);
      }
      catch (InvocationTargetException e)
      {
         throw new DdsException(e);
      }

      return result;
   }


   /**
    * Create a DDS ReadCondition according to specified options
    * 
    * @param options the options
    * @return a DDS ReadCondition
    * @throws DdsException if a DDS error occurs creating ReadCondition
    */
   private ReadCondition createCondition(Map<String, Object> options) throws DdsException
   {
      ReadCondition result;

      // Use a temporary Exchange to benefit headers management
      // (including types conversion)
      Exchange e = endpoint.createExchange();
      Message msg = e.getIn();
      if (options != null)
      {
         msg.setHeaders(options);
      }

      // get SAMPLE_STATE_MASK_OPTION
      Integer sampleStateMask = msg.getHeader(SAMPLE_STATE_MASK_OPTION,
            DDS.ANY_SAMPLE_STATE.value,
            Integer.class);
      if (sampleStateMask == null)
      {
         throw new DdsException("Unexpected type for header '" +
               SAMPLE_STATE_MASK_OPTION + "'. Integer was expected.");
      }

      // get VIEW_STATE_MASK_OPTION
      Integer viewStateMask = msg.getHeader(VIEW_STATE_MASK_OPTION,
            DDS.ANY_VIEW_STATE.value,
            Integer.class);
      if (viewStateMask == null)
      {
         throw new DdsException("Unexpected type for header '" +
               VIEW_STATE_MASK_OPTION + "'. Integer was expected.");
      }

      // get INSTANCE_STATE_MASK_OPTION
      Integer instanceStateMask = msg.getHeader(INSTANCE_STATE_MASK_OPTION,
            DDS.ANY_INSTANCE_STATE.value,
            Integer.class);
      if (instanceStateMask == null)
      {
         throw new DdsException("Unexpected type for header '" +
               INSTANCE_STATE_MASK_OPTION + "'. Integer was expected.");
      }


      // get QUERY_EXPRESSION_OPTION
      String queryExpression = msg.getHeader(QUERY_EXPRESSION_OPTION,
            "",
            String.class);

      // get QUERY_PARAMS_OPTION
      String[] queryParameters = msg.getHeader(QUERY_PARAMS_OPTION,
            new String[0],
            String[].class);
      if (queryParameters == null)
      {
         throw new DdsException("Unexpected type for header '" +
               QUERY_PARAMS_OPTION + "'. String[] was expected.");
      }


      if (queryExpression == null || queryExpression.isEmpty())
      {
         result = reader.create_readcondition(
               sampleStateMask, viewStateMask, instanceStateMask);
         DdsErrorHandler.checkHandle(result, "create_readcondition");
      }
      else
      {
         result = reader.create_querycondition(
               sampleStateMask, viewStateMask, instanceStateMask,
               queryExpression, queryParameters);
         DdsErrorHandler.checkHandle(result, "create_querycondition");
      }

      return result;
   }

}
