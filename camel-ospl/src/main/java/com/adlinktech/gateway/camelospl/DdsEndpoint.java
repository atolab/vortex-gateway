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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.apache.camel.Consumer;
import org.apache.camel.Exchange;
import org.apache.camel.PollingConsumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.ResolveEndpointFailedException;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.impl.DefaultEndpoint;
import org.slf4j.Logger;

import DDS.ContentFilteredTopic;
import DDS.DestinationOrderQosPolicyKind;
import DDS.DomainParticipant;
import DDS.DomainParticipantFactory;
import DDS.DomainParticipantQosHolder;
import DDS.DurabilityQosPolicyKind;
import DDS.HistoryQosPolicyKind;
import DDS.LivelinessQosPolicyKind;
import DDS.NEW_VIEW_STATE;
import DDS.NOT_ALIVE_DISPOSED_INSTANCE_STATE;
import DDS.OwnershipQosPolicyKind;
import DDS.PresentationQosPolicyAccessScopeKind;
import DDS.ReliabilityQosPolicyKind;
import DDS.SampleInfo;
import DDS.Topic;
import DDS.TopicQosHolder;

import com.adlinktech.gateway.camelospl.utils.Loggers;


/**
 * Concrete implementation of DDS Endpoint.
 */
public class DdsEndpoint
   extends DefaultEndpoint
{

   /** Logger */

   private static final Logger LOG = Loggers.LOGGER_ENDPOINT;

   /** Suffix appended to topic type name for type support. */
   private static final String TYPE_SUPPORT_CLASS_SUFFIX = "TypeSupport";

   /** Type support method used to get type name. */
   private static final String GET_TYPE_METHOD = "get_type_name";

   /** Type support method used to register type. */
   private static final String REGISTER_TYPE_METHOD = "register_type";

   /** The delimiter for the partition strings. */
   static final String PARTITION_DELIMITER = ",";


   // NOTE: the DdsEndpoint options are directly stored in relevant QoS maps
   // (for topic, subscriber, publisher, DataReader and/or DataWriter)
   // with the path to the QoS attribute as index.
   //
   // E.g. for "partition" option :
   // in both SubscriberQos and PublisherQos classes, partition value is
   // stored in "partition.name" attribute. We use this path as index in
   // both subscriberQosMap and publisherQosMap.
   //
   // Below are all attribute paths used as index:
   //
   private static final String QOS_PARITION = "partition.name";

   private static final String QOS_DEADLINE_PERIOD_SEC = "deadline.period.sec";

   private static final String QOS_DEADLINE_PERIOD_NSEC = "deadline.period.nanosec";

   private static final String QOS_DESTINATION_ORDER = "destination_order.kind";

   private static final String QOS_DURABILITY_KIND = "durability.kind";

   private static final String QOS_DURABILITY_SERVICE_CLEANUP_DELAY_SEC = "durability_service.service_cleanup_delay.sec";

   private static final String QOS_DURABILITY_SERVICE_CLEANUP_DELAY_NSEC = "durability_service.service_cleanup_delay.nanosec";

   private static final String QOS_DURABILITY_SERVICE_DEPTH = "durability_service.history_depth";

   private static final String QOS_DURABILITY_SERVICE_KIND = "durability_service.history_kind";

   private static final String QOS_DURABILITY_SERVICE_MAX_INSTANCES = "durability_service.max_instances";

   private static final String QOS_DURABILITY_SERVICE_MAX_SAMPLES = "durability_service.max_samples";

   private static final String QOS_DURABILITY_SERVICE_MAX_SAMPLES_PER_INSTANCE = "durability_service.max_samples_per_instance";

   private static final String QOS_HISTORY_DEPTH = "history.depth";

   private static final String QOS_HISTORY_KIND = "history.kind";

   private static final String QOS_LATENCY_DURATION_SEC = "latency_budget.duration.sec";

   private static final String QOS_LATENCY_DURATION_NSEC = "latency_budget.duration.nanosec";

   private static final String QOS_LIFESPAN_DURATION_SEC = "lifespan.duration.sec";

   private static final String QOS_LIFESPAN_DURATION_NSEC = "lifespan.duration.nanosec";

   private static final String QOS_LIVELINESS_DURATION_SEC = "liveliness.lease_duration.sec";

   private static final String QOS_LIVELINESS_DURATION_NSEC = "liveliness.lease_duration.nanosec";

   private static final String QOS_LIVELINESS_KIND = "liveliness.kind";

   private static final String QOS_OWNERSHIP_KIND = "ownership.kind";

   private static final String QOS_OWNERSHIP_STRENGTH = "ownership_strength.value";

   private static final String QOS_PRESENTATION_ACCESS_SCOPE = "presentation.access_scope";

   private static final String QOS_PRESENTATION_COHERENT_ACCESS = "presentation.coherent_access";

   private static final String QOS_PRESENTATION_ORDERED_ACCESS = "presentation.ordered_access";

   private static final String QOS_READER_DATA_LIFECYCLE_AUTOPURGE_DISPOSE_DELAY_SEC = "reader_data_lifecycle.autopurge_disposed_samples_delay.sec";

   private static final String QOS_READER_DATA_LIFECYCLE_AUTOPURGE_DISPOSE_DELAY_NSEC = "reader_data_lifecycle.autopurge_disposed_samples_delay.nanosec";

   private static final String QOS_READER_DATA_LIFECYCLE_AUTOPURGE_NOWRITER_DELAY_SEC = "reader_data_lifecycle.autopurge_nowriter_samples_delay.sec";

   private static final String QOS_READER_DATA_LIFECYCLE_AUTOPURGE_NOWRITER_DELAY_NSEC = "reader_data_lifecycle.autopurge_nowriter_samples_delay.nanosec";

   private static final String QOS_RELIABILITY_BLOCKING_TIME_SEC = "reliability.max_blocking_time.sec";

   private static final String QOS_RELIABILITY_BLOCKING_TIME_NSEC = "reliability.max_blocking_time.nanosec";

   private static final String QOS_RELIABILITY_KIND = "reliability.kind";

   private static final String QOS_RESOURCE_LIMITS_MAX_INSTANCES = "resource_limits.max_instances";

   private static final String QOS_RESOURCE_LIMITS_MAX_SAMPLES = "resource_limits.max_samples";

   private static final String QOS_RESOURCE_LIMITS_MAX_SAMPLES_PER_INSTANCE = "resource_limits.max_samples_per_instance";

   private static final String QOS_TIME_BASED_FILTER_SEC = "time_based_filter.minimum_separation.sec";

   private static final String QOS_TIME_BASED_FILTER_NSEC = "time_based_filter.minimum_separation.nanosec";

   private static final String QOS_TRANSPORT_PRIORITY = "transport_priority.value";

   private static final String QOS_WRITER_DATA_LIFECYCLE_AUTODISPOSE = "writer_data_lifecycle.autodispose_unregistered_instances";

   private static final String QOS_GROUP_DATA = "group_data.value";

   private static final String QOS_TOPIC_DATA = "topic_data.value";

   private static final String QOS_USER_DATA = "user_data.value";

   private DdsComponent ddsComponent;

   //
   // Endpoint parameters
   //
   private int domainId;

   private String topicName;

   private String typeName;

   private Class<?> topicType;

   private boolean readMode;

   private boolean ignoreLocalPublishers;

   private String contentFilter;


   //
   // Maps of extra QoS to be set (overriding default ones)
   //
   private Map<String, Object> participantQosMap;

   private Map<String, Object> topicQosMap;

   private Map<String, Object> subscriberQosMap;

   private Map<String, Object> publisherQosMap;

   private Map<String, Object> dataReaderQosMap;

   private Map<String, Object> dataWriterQosMap;


   //
   // DDS entities
   //
   /** DDS domain participant */
   private DomainParticipant participant;

   /** DDS topic */
   private Topic ddsTopic;

   /** DDS contentFilteredTopic */
   private ContentFilteredTopic contentFilteredTopic;


   /**
    * Constructor.
    * 
    * @param topicName
    *           the Topic's name
    * @param domainId
    *           the Domain ID
    * @param typeName
    *           the Topic's type name
    * @param uri
    *           the Endpoint URI
    * @param component
    *           the parent Component
    */
   public DdsEndpoint(
      String topicName, int domainId, String typeName, String uri, DdsComponent component)
   {
      super(uri, component);

      this.ddsComponent = component;
      this.topicName = topicName;
      this.domainId = domainId;
      this.typeName = typeName;

      try
      {
         this.topicType = getCamelContext().getClassResolver().resolveMandatoryClass(typeName);
      }
      catch (ClassNotFoundException e)
      {
         throw new ResolveEndpointFailedException(uri, e);
      }

      this.readMode = false;
      this.ignoreLocalPublishers = true;
      this.participantQosMap = new HashMap<String, Object>();
      this.topicQosMap = new HashMap<String, Object>();
      this.subscriberQosMap = new HashMap<String, Object>();
      this.publisherQosMap = new HashMap<String, Object>();
      this.dataReaderQosMap = new HashMap<String, Object>();
      this.dataWriterQosMap = new HashMap<String, Object>();
   }

   @Override
   public DdsComponent getComponent()
   {
      return (DdsComponent) super.getComponent();
   }

   /**
    * Get the DDS Domain ID.
    * 
    * @return the DDS Domain ID.
    */
   public int getDomainId()
   {
      return domainId;
   }

   /**
    * Get the DDS Topic's name.
    * 
    * @return the DDS Topic's name.
    */
   public String getTopicName()
   {
      return topicName;
   }

   /**
    * Get the DDS Topic's type name.
    * 
    * @return the DDS Topic's type name.
    */
   public String getTypeName()
   {
      return typeName;
   }

   /**
    * Get the DDS Topic's type.
    * 
    * @return the DDS Topic's type.
    */
   public Class<?> getTopicType()
   {
      return topicType;
   }

   /*
    * Read mode
    */
   public void setReadMode(boolean readMode)
   {
      this.readMode = readMode;
   }

   public boolean isReadMode()
   {
      return this.readMode;
   }

   /*
    * Ignore local publishers
    */
   public void setIgnoreLocalPublishers(boolean ignoreLocalPublishers)
   {
      this.ignoreLocalPublishers = ignoreLocalPublishers;
   }

   public boolean isIgnoreLocalPublishers()
   {
      return this.ignoreLocalPublishers;
   }

   /*
    * Content Filter.
    */
   public void setContentFilter(String contentFilter)
   {
      this.contentFilter = contentFilter;
   }

   public String getContentFilter()
   {
      return contentFilter;
   }


   /*
    * Partition
    */
   public void setPartition(String partitionStr)
   {
      String[] partition = partitionStr.split(PARTITION_DELIMITER);

      // Partition only applies to publisher and subscriber.
      publisherQosMap.put(QOS_PARITION, partition);
      subscriberQosMap.put(QOS_PARITION, partition);
   }

   public String[] getPartition()
   {
      return (String[]) publisherQosMap.get(QOS_PARITION);
   }


   /*
    * Deadline Period.
    */
   public void setDeadlinePeriod(double deadlinePeriod)
   {
      // Convert the float to seconds and nanoseconds.
      int seconds = QosUtils.timeToSec(deadlinePeriod);
      int nanoseconds = QosUtils.timeToNanosec(deadlinePeriod);

      // Deadline only applies to topic, data reader, and data writer.
      topicQosMap.put(QOS_DEADLINE_PERIOD_SEC, seconds);
      topicQosMap.put(QOS_DEADLINE_PERIOD_NSEC, nanoseconds);
      dataWriterQosMap.put(QOS_DEADLINE_PERIOD_SEC, seconds);
      dataWriterQosMap.put(QOS_DEADLINE_PERIOD_NSEC, nanoseconds);
      dataReaderQosMap.put(QOS_DEADLINE_PERIOD_SEC, seconds);
      dataReaderQosMap.put(QOS_DEADLINE_PERIOD_NSEC, nanoseconds);
   }

   public void setTopicDeadlinePeriod(double deadlinePeriod)
   {
      // Convert the float to seconds and nanoseconds.
      int seconds = QosUtils.timeToSec(deadlinePeriod);
      int nanoseconds = QosUtils.timeToNanosec(deadlinePeriod);

      topicQosMap.put(QOS_DEADLINE_PERIOD_SEC, seconds);
      topicQosMap.put(QOS_DEADLINE_PERIOD_NSEC, nanoseconds);
   }

   public double getDeadlinePeriod()
   {
      if ( !dataWriterQosMap.containsKey(QOS_DEADLINE_PERIOD_SEC))
      {
         // default value of deadline period is infinite
         return Double.MAX_VALUE;
      }
      Object sec = dataWriterQosMap.get(QOS_DEADLINE_PERIOD_SEC);
      Object nanosec = dataWriterQosMap.get(QOS_DEADLINE_PERIOD_NSEC);

      return QosUtils.timeToDouble((Integer) sec, (Integer) nanosec);
   }


   /*
    * Destination Order.
    */
   public void setDestinationOrder(String destinationOrderString) throws DdsException
   {
      DestinationOrderQosPolicyKind destinationOrderKind = QosUtils
            .readDestinationOrderQosPolicyKind(destinationOrderString);

      // Destination order only applies to topic, data reader and data writer.
      topicQosMap.put(QOS_DESTINATION_ORDER, destinationOrderKind);
      dataWriterQosMap.put(QOS_DESTINATION_ORDER, destinationOrderKind);
      dataReaderQosMap.put(QOS_DESTINATION_ORDER, destinationOrderKind);
   }

   public void setTopicDestinationOrder(String destinationOrderString) throws DdsException
   {
      // Topic only
      topicQosMap.put(QOS_DESTINATION_ORDER,
            QosUtils.readDestinationOrderQosPolicyKind(destinationOrderString));
   }

   public DestinationOrderQosPolicyKind getDestinationOrder()
   {
      return (DestinationOrderQosPolicyKind) dataWriterQosMap.get(QOS_DESTINATION_ORDER);
   }


   /*
    * Durability Kind.
    */
   public void setDurabilityKind(String durabilityKindString) throws DdsException
   {
      DurabilityQosPolicyKind durabilityKind = QosUtils
            .readDurabilityQosPolicyKind(durabilityKindString);

      // Durability only applies to topic, data reader and data writer.
      topicQosMap.put(QOS_DURABILITY_KIND, durabilityKind);
      dataWriterQosMap.put(QOS_DURABILITY_KIND, durabilityKind);
      dataReaderQosMap.put(QOS_DURABILITY_KIND, durabilityKind);
   }

   public void setTopicDurabilityKind(String durabilityKindString) throws DdsException
   {
      // Topic only
      topicQosMap.put(QOS_DURABILITY_KIND,
            QosUtils.readDurabilityQosPolicyKind(durabilityKindString));
   }

   public DurabilityQosPolicyKind getDurabilityKind()
   {
      return (DurabilityQosPolicyKind) dataWriterQosMap.get(QOS_DURABILITY_KIND);
   }


   /*
    * Durability service cleanup delay.
    */
   public void setDurabilityServiceCleanupDelay(double durabilityServiceCleanupDelay)
   {
      // Convert the float to seconds and nanoseconds.
      int seconds = QosUtils.timeToSec(durabilityServiceCleanupDelay);
      int nanoseconds = QosUtils.timeToNanosec(durabilityServiceCleanupDelay);

      // Durability service only applies to topic and data writer.
      topicQosMap.put(QOS_DURABILITY_SERVICE_CLEANUP_DELAY_SEC, seconds);
      topicQosMap.put(QOS_DURABILITY_SERVICE_CLEANUP_DELAY_NSEC, nanoseconds);
      // BB-42: No durability service support for data writers?
      // dataWriterQosMap.put(QOS_DURABILITY_SERVICE_CLEANUP_DELAY_SEC, seconds);
      // dataWriterQosMap.put(QOS_DURABILITY_SERVICE_CLEANUP_DELAY_NSEC, nanoseconds);
   }

   public void setTopicDurabilityServiceCleanupDelay(double durabilityServiceCleanupDelay)
   {
      // Convert the float to seconds and nanoseconds.
      int seconds = QosUtils.timeToSec(durabilityServiceCleanupDelay);
      int nanoseconds = QosUtils.timeToNanosec(durabilityServiceCleanupDelay);

      // Topic only
      topicQosMap.put(QOS_DURABILITY_SERVICE_CLEANUP_DELAY_SEC, seconds);
      topicQosMap.put(QOS_DURABILITY_SERVICE_CLEANUP_DELAY_NSEC, nanoseconds);
   }


   public double getDurabilityServiceCleanupDelay()
   {
      if ( !topicQosMap.containsKey(QOS_DURABILITY_SERVICE_CLEANUP_DELAY_SEC))
      {
         // default value of Durability service cleanup delay is 0
         return 0;
      }
      Object sec = topicQosMap.get(QOS_DURABILITY_SERVICE_CLEANUP_DELAY_SEC);
      Object nanosec = topicQosMap.get(QOS_DURABILITY_SERVICE_CLEANUP_DELAY_NSEC);

      return QosUtils.timeToDouble((Integer) sec, (Integer) nanosec);
   }


   /*
    * Durability service depth.
    */
   public void setDurabilityServiceDepth(int durabilityServiceDepth)
   {
      // Durability service only applies to topic and data writer.
      topicQosMap.put(QOS_DURABILITY_SERVICE_DEPTH, durabilityServiceDepth);
      // BB-42: No durability service support for data writers?
      // dataWriterQosMap.put(QOS_DURABILITY_SERVICE_DEPTH, durabilityServiceDepth);
   }

   public void setTopicDurabilityServiceDepth(int durabilityServiceDepth)
   {
      // Topic only
      topicQosMap.put(QOS_DURABILITY_SERVICE_DEPTH, durabilityServiceDepth);
   }

   public int getDurabilityServiceDepth()
   {
      if ( !topicQosMap.containsKey(QOS_DURABILITY_SERVICE_DEPTH))
      {
         // default value of Durability service history depth is 1
         return 1;
      }
      return (Integer) topicQosMap.get(QOS_DURABILITY_SERVICE_DEPTH);
   }


   /*
    * Durability service kind.
    */
   public void setDurabilityServiceKind(String durabilityServiceKindString) throws DdsException
   {
      // Durability service only applies to topic and data writer.
      topicQosMap.put(QOS_DURABILITY_SERVICE_KIND,
            QosUtils.readHistoryQosPolicyKind(durabilityServiceKindString));
      // BB-42: No durability service support for data writers?
      // dataWriterQosMap.put(QOS_DURABILITY_SERVICE_KIND,
      // QosUtils.readHistoryQosPolicyKind(durabilityServiceKindString));
   }

   public void setTopicDurabilityServiceKind(String durabilityServiceKindString)
      throws DdsException
   {
      // Topic only
      topicQosMap.put(QOS_DURABILITY_SERVICE_KIND,
            QosUtils.readHistoryQosPolicyKind(durabilityServiceKindString));
   }

   public HistoryQosPolicyKind getDurabilityServiceKind()
   {
      return (HistoryQosPolicyKind) topicQosMap.get(QOS_DURABILITY_SERVICE_KIND);
   }


   /*
    * Durability Service Max Instances
    */
   public void setDurabilityServiceMaxInstances(int durabilityServiceMaxInstances)
   {
      // Durability service only applies to topic and data writer.
      topicQosMap.put(QOS_DURABILITY_SERVICE_MAX_INSTANCES, durabilityServiceMaxInstances);
      // BB-42: No durability service support for data writers?
      // dataWriterQosMap.put(QOS_DURABILITY_SERVICE_MAX_INSTANCES, durabilityServiceMaxInstances);
   }

   public void setTopicDurabilityServiceMaxInstances(int durabilityServiceMaxInstances)
   {
      // Topic only
      topicQosMap.put(QOS_DURABILITY_SERVICE_MAX_INSTANCES, durabilityServiceMaxInstances);
   }

   public int getDurabilityServiceMaxInstances()
   {
      if ( !topicQosMap.containsKey(QOS_DURABILITY_SERVICE_MAX_INSTANCES))
      {
         // by default DurabilityService max instances value is LENGTH_UNLIMITED
         return Integer.MAX_VALUE;
      }
      return (Integer) topicQosMap.get(QOS_DURABILITY_SERVICE_MAX_INSTANCES);
   }


   /*
    * Durability Service Max Samples
    */
   public void setDurabilityServiceMaxSamples(int durabilityServiceMaxSamples)
   {
      // Durability service only applies to topic and data writer.
      topicQosMap.put(QOS_DURABILITY_SERVICE_MAX_SAMPLES, durabilityServiceMaxSamples);
      // BB-42: No durability service support for data writers?
      // dataWriterQosMap.put(QOS_DURABILITY_SERVICE_MAX_SAMPLES, durabilityServiceMaxSamples);
   }

   public void setTopicDurabilityServiceMaxSamples(int durabilityServiceMaxSamples)
   {
      // Topic only
      topicQosMap.put(QOS_DURABILITY_SERVICE_MAX_SAMPLES, durabilityServiceMaxSamples);
   }

   public int getDurabilityServiceMaxSamples()
   {
      if ( !topicQosMap.containsKey(QOS_DURABILITY_SERVICE_MAX_SAMPLES))
      {
         // by default DurabilityService max samples value is LENGTH_UNLIMITED
         return Integer.MAX_VALUE;
      }
      return (Integer) topicQosMap.get(QOS_DURABILITY_SERVICE_MAX_SAMPLES);
   }


   /*
    * Durability service max samples per instance
    */
   public void
      setDurabilityServiceMaxSamplesPerInstance(int durabilityServiceMaxSamplesPerInstance)
   {
      // Durability service only applies to topic and data writer.
      topicQosMap.put(QOS_DURABILITY_SERVICE_MAX_SAMPLES_PER_INSTANCE,
            durabilityServiceMaxSamplesPerInstance);
      // BB-42: No durability service support for data writers?
      // dataWriterQosMap.put(QOS_DURABILITY_SERVICE_MAX_SAMPLES_PER_INSTANCE,
      // durabilityServiceMaxSamplesPerInstance);
   }

   public void setTopicDurabilityServiceMaxSamplesPerInstance(
         int durabilityServiceMaxSamplesPerInstance)
   {
      // Topic only
      topicQosMap.put(QOS_DURABILITY_SERVICE_MAX_SAMPLES_PER_INSTANCE,
            durabilityServiceMaxSamplesPerInstance);
   }

   public int getDurabilityServiceMaxSamplesPerInstance()
   {
      if ( !topicQosMap.containsKey(QOS_DURABILITY_SERVICE_MAX_SAMPLES_PER_INSTANCE))
      {
         // by default DurabilityService max samples per instance value is LENGTH_UNLIMITED
         return Integer.MAX_VALUE;
      }
      return (Integer) topicQosMap.get(QOS_DURABILITY_SERVICE_MAX_SAMPLES_PER_INSTANCE);
   }


   /*
    * History depth
    */
   public void setHistoryDepth(int historyDepth)
   {
      // History only applies to topic, data reader, and data writer.
      topicQosMap.put(QOS_HISTORY_DEPTH, historyDepth);
      dataWriterQosMap.put(QOS_HISTORY_DEPTH, historyDepth);
      dataReaderQosMap.put(QOS_HISTORY_DEPTH, historyDepth);
   }

   public void setTopicHistoryDepth(int historyDepth)
   {
      // Topic only
      topicQosMap.put(QOS_HISTORY_DEPTH, historyDepth);
   }

   public int getHistoryDepth()
   {
      if ( !dataWriterQosMap.containsKey(QOS_HISTORY_DEPTH))
      {
         // by default History depth value is 1
         return 1;
      }
      return (Integer) dataWriterQosMap.get(QOS_HISTORY_DEPTH);
   }


   /*
    * History Kind
    */
   public void setHistoryKind(String historyKindString) throws DdsException
   {

      DDS.HistoryQosPolicyKind historyKind = QosUtils.readHistoryQosPolicyKind(historyKindString);

      // History only applies to topic, data reader, and data writer.
      topicQosMap.put(QOS_HISTORY_KIND, historyKind);
      dataWriterQosMap.put(QOS_HISTORY_KIND, historyKind);
      dataReaderQosMap.put(QOS_HISTORY_KIND, historyKind);
   }

   public void setTopicHistoryKind(String historyKindString) throws DdsException
   {
      // Topic only
      topicQosMap.put(QOS_HISTORY_KIND, QosUtils.readHistoryQosPolicyKind(historyKindString));
   }

   public HistoryQosPolicyKind getHistoryKind()
   {
      return (HistoryQosPolicyKind) dataWriterQosMap.get(QOS_HISTORY_KIND);
   }


   /*
    * Latency duration
    */
   public void setLatencyDuration(double latencyDuration)
   {
      // Convert the float to seconds and nanoseconds.
      int seconds = QosUtils.timeToSec(latencyDuration);
      int nanoseconds = QosUtils.timeToNanosec(latencyDuration);

      // Latency only applies to topic, data reader, and data writer.
      topicQosMap.put(QOS_LATENCY_DURATION_SEC, seconds);
      topicQosMap.put(QOS_LATENCY_DURATION_NSEC, nanoseconds);
      dataWriterQosMap.put(QOS_LATENCY_DURATION_SEC, seconds);
      dataWriterQosMap.put(QOS_LATENCY_DURATION_NSEC, nanoseconds);
      dataReaderQosMap.put(QOS_LATENCY_DURATION_SEC, seconds);
      dataReaderQosMap.put(QOS_LATENCY_DURATION_NSEC, nanoseconds);
   }

   public void setTopicLatencyDuration(double latencyDuration)
   {
      // Convert the float to seconds and nanoseconds.
      int seconds = QosUtils.timeToSec(latencyDuration);
      int nanoseconds = QosUtils.timeToNanosec(latencyDuration);

      // Topic only
      topicQosMap.put(QOS_LATENCY_DURATION_SEC, seconds);
      topicQosMap.put(QOS_LATENCY_DURATION_NSEC, nanoseconds);
   }

   public double getLatencyDuration()
   {
      if ( !dataWriterQosMap.containsKey(QOS_LATENCY_DURATION_SEC))
      {
         // default value of Latency Budget duration is 0
         return 0;
      }
      Object sec = dataWriterQosMap.get(QOS_LATENCY_DURATION_SEC);
      Object nanosec = dataWriterQosMap.get(QOS_LATENCY_DURATION_NSEC);

      return QosUtils.timeToDouble((Integer) sec, (Integer) nanosec);
   }


   /*
    * Lifespan duration
    */
   public void setLifespanDuration(double lifespanDuration)
   {
      // Convert the float to seconds and nanoseconds.
      int seconds = QosUtils.timeToSec(lifespanDuration);
      int nanoseconds = QosUtils.timeToNanosec(lifespanDuration);

      // Lifespan only applies to topic and data writer.
      topicQosMap.put(QOS_LIFESPAN_DURATION_SEC, seconds);
      topicQosMap.put(QOS_LIFESPAN_DURATION_NSEC, nanoseconds);
      dataWriterQosMap.put(QOS_LIFESPAN_DURATION_SEC, seconds);
      dataWriterQosMap.put(QOS_LIFESPAN_DURATION_NSEC, nanoseconds);
   }

   public void setTopicLifespanDuration(double lifespanDuration)
   {
      // Convert the float to seconds and nanoseconds.
      int seconds = QosUtils.timeToSec(lifespanDuration);
      int nanoseconds = QosUtils.timeToNanosec(lifespanDuration);

      // Topic only
      topicQosMap.put(QOS_LIFESPAN_DURATION_SEC, seconds);
      topicQosMap.put(QOS_LIFESPAN_DURATION_NSEC, nanoseconds);
   }

   public double getLifespanDuration()
   {
      if ( !dataWriterQosMap.containsKey(QOS_LIFESPAN_DURATION_SEC))
      {
         // default value of Lifespan duration is infinite
         return Integer.MAX_VALUE;
      }
      Object sec = dataWriterQosMap.get(QOS_LIFESPAN_DURATION_SEC);
      Object nanosec = dataWriterQosMap.get(QOS_LIFESPAN_DURATION_NSEC);

      return QosUtils.timeToDouble((Integer) sec, (Integer) nanosec);
   }


   /*
    * Liveliness duration
    */
   public void setLivelinessDuration(double livelinessDuration)
   {
      // Convert the float to seconds and nanoseconds.
      int seconds = QosUtils.timeToSec(livelinessDuration);
      int nanoseconds = QosUtils.timeToNanosec(livelinessDuration);

      // Liveliness lease duration only applies to topic, data reader and data writer.
      topicQosMap.put(QOS_LIVELINESS_DURATION_SEC, seconds);
      topicQosMap.put(QOS_LIVELINESS_DURATION_NSEC, nanoseconds);
      dataWriterQosMap.put(QOS_LIVELINESS_DURATION_SEC, seconds);
      dataWriterQosMap.put(QOS_LIVELINESS_DURATION_NSEC, nanoseconds);
      dataReaderQosMap.put(QOS_LIVELINESS_DURATION_SEC, seconds);
      dataReaderQosMap.put(QOS_LIVELINESS_DURATION_NSEC, nanoseconds);
   }

   public void setTopicLivelinessDuration(double livelinessDuration)
   {
      // Convert the float to seconds and nanoseconds.
      int seconds = QosUtils.timeToSec(livelinessDuration);
      int nanoseconds = QosUtils.timeToNanosec(livelinessDuration);

      // Topic only
      topicQosMap.put(QOS_LIVELINESS_DURATION_SEC, seconds);
      topicQosMap.put(QOS_LIVELINESS_DURATION_NSEC, nanoseconds);
   }

   public double getLivelinessDuration()
   {
      if ( !dataWriterQosMap.containsKey(QOS_LIVELINESS_DURATION_SEC))
      {
         // default value of Liveliness lease duration is infinite
         return Integer.MAX_VALUE;
      }
      Object sec = dataWriterQosMap.get(QOS_LIVELINESS_DURATION_SEC);
      Object nanosec = dataWriterQosMap.get(QOS_LIVELINESS_DURATION_NSEC);

      return QosUtils.timeToDouble((Integer) sec, (Integer) nanosec);
   }


   /*
    * Liveliness Kind
    */
   public void setLivelinessKind(String livelinessKindString) throws Exception
   {
      DDS.LivelinessQosPolicyKind livelinessKind = QosUtils
            .readLivelinessQosPolicyKind(livelinessKindString);

      // Liveliness only applies to topic, data reader, and data writer.
      topicQosMap.put(QOS_LIVELINESS_KIND, livelinessKind);
      dataWriterQosMap.put(QOS_LIVELINESS_KIND, livelinessKind);
      dataReaderQosMap.put(QOS_LIVELINESS_KIND, livelinessKind);
   }

   public void setTopicLivelinessKind(String livelinessKindString) throws DdsException
   {
      // Topic only
      topicQosMap.put(QOS_LIVELINESS_KIND,
            QosUtils.readLivelinessQosPolicyKind(livelinessKindString));
   }

   public LivelinessQosPolicyKind getLivelinessKind()
   {
      return (LivelinessQosPolicyKind) dataWriterQosMap.get(QOS_LIVELINESS_KIND);
   }


   /*
    * Ownership kind
    */
   public void setOwnershipKind(String ownershipKindString) throws DdsException
   {
      DDS.OwnershipQosPolicyKind ownershipKind = QosUtils
            .readOwnershipQosPolicyKind(ownershipKindString);

      // Ownership kind only applies to topic, data reader, and data writer.
      topicQosMap.put(QOS_OWNERSHIP_KIND, ownershipKind);
      dataWriterQosMap.put(QOS_OWNERSHIP_KIND, ownershipKind);
      dataReaderQosMap.put(QOS_OWNERSHIP_KIND, ownershipKind);
   }

   public void setTopicOwnershipKind(String ownershipKindString) throws DdsException
   {
      // Topic only
      topicQosMap.put(QOS_OWNERSHIP_KIND, QosUtils.readOwnershipQosPolicyKind(ownershipKindString));
   }

   public OwnershipQosPolicyKind getOwnershipKind()
   {
      return (OwnershipQosPolicyKind) dataWriterQosMap.get(QOS_OWNERSHIP_KIND);
   }


   /*
    * Ownership strength
    */
   public void setOwnershipStrength(int strength)
   {
      // Ownership strength only applies to data writer.
      dataWriterQosMap.put(QOS_OWNERSHIP_STRENGTH, strength);
   }

   public int getOwnershipStrength()
   {
      if ( !dataWriterQosMap.containsKey(QOS_OWNERSHIP_STRENGTH))
      {
         // default value of Ownership strength is 0
         return 0;
      }
      return (Integer) dataWriterQosMap.get(QOS_OWNERSHIP_STRENGTH);
   }


   /*
    * Presentation Access Scope
    */
   public void setPresentationAccessScope(String presentationAccessScopeString) throws DdsException
   {
      DDS.PresentationQosPolicyAccessScopeKind presentationAccessScope = QosUtils
            .readPresentationQosPolicyAccessScopeKind(presentationAccessScopeString);

      // Presentation only applies to publisher and subscriber.
      publisherQosMap.put(QOS_PRESENTATION_ACCESS_SCOPE, presentationAccessScope);
      subscriberQosMap.put(QOS_PRESENTATION_ACCESS_SCOPE, presentationAccessScope);
   }

   public PresentationQosPolicyAccessScopeKind getPresentationAccessScope()
   {
      return (PresentationQosPolicyAccessScopeKind) publisherQosMap
            .get(QOS_PRESENTATION_ACCESS_SCOPE);
   }


   /*
    * Presentation Coherent Access
    */
   public void setPresentationCoherentAccess(boolean presentationCoherentAccess)
   {
      // Presentation only applies to publisher and subscriber.
      publisherQosMap.put(QOS_PRESENTATION_COHERENT_ACCESS, presentationCoherentAccess);
      subscriberQosMap.put(QOS_PRESENTATION_COHERENT_ACCESS, presentationCoherentAccess);
   }

   public boolean getPresentationCoherentAccess()
   {
      if ( !publisherQosMap.containsKey(QOS_PRESENTATION_COHERENT_ACCESS))
      {
         // default value for Presentation coherent access is false
         return false;
      }
      return (Boolean) publisherQosMap.get(QOS_PRESENTATION_COHERENT_ACCESS);
   }


   /*
    * Presentation Ordered Access
    */
   public void setPresentationOrderedAccess(boolean presentationOrderedAccess)
   {
      // Presentation only applies to publisher and subscriber.
      publisherQosMap.put(QOS_PRESENTATION_ORDERED_ACCESS, presentationOrderedAccess);
      subscriberQosMap.put(QOS_PRESENTATION_ORDERED_ACCESS, presentationOrderedAccess);
   }

   public boolean getPresentationOrderedAccess()
   {
      if ( !publisherQosMap.containsKey(QOS_PRESENTATION_ORDERED_ACCESS))
      {
         // default value for Presentation ordered access is false
         return false;
      }
      return (Boolean) publisherQosMap.get(QOS_PRESENTATION_ORDERED_ACCESS);
   }


   /*
    * Reader Data Lifecycle Autopurge Disposed Delay
    */
   public void setReaderDataLifecycleAutopurgeDisposedDelay(double disposedSamplesDelay)
   {
      // Convert the float to seconds and nanoseconds.
      int seconds = QosUtils.timeToSec(disposedSamplesDelay);
      int nanoseconds = QosUtils.timeToNanosec(disposedSamplesDelay);

      // Reader data lifecycle only applies to data reader.
      dataReaderQosMap.put(QOS_READER_DATA_LIFECYCLE_AUTOPURGE_DISPOSE_DELAY_SEC, seconds);
      dataReaderQosMap.put(QOS_READER_DATA_LIFECYCLE_AUTOPURGE_DISPOSE_DELAY_NSEC, nanoseconds);
   }

   public double getReaderDataLifecycleAutopurgeDisposedDelay()
   {
      if ( !dataReaderQosMap.containsKey(QOS_READER_DATA_LIFECYCLE_AUTOPURGE_DISPOSE_DELAY_SEC))
      {
         // default value of Reader Data Lifecycle autopurge_disposed_samples_delay is infinite
         return Integer.MAX_VALUE;
      }
      Object sec = dataReaderQosMap.get(QOS_READER_DATA_LIFECYCLE_AUTOPURGE_DISPOSE_DELAY_SEC);
      Object nanosec = dataReaderQosMap.get(QOS_READER_DATA_LIFECYCLE_AUTOPURGE_DISPOSE_DELAY_NSEC);

      return QosUtils.timeToDouble((Integer) sec, (Integer) nanosec);
   }


   /*
    * Reader Data Lifecycle Autopurge Nowriter Delay
    */
   public void setReaderDataLifecycleAutopurgeNowriterDelay(double nowriterSamplesDelay)
   {
      // Convert the float to seconds and nanoseconds.
      int seconds = QosUtils.timeToSec(nowriterSamplesDelay);
      int nanoseconds = QosUtils.timeToNanosec(nowriterSamplesDelay);

      // Reader data lifecycle only applies to data reader.
      dataReaderQosMap.put(QOS_READER_DATA_LIFECYCLE_AUTOPURGE_NOWRITER_DELAY_SEC, seconds);
      dataReaderQosMap.put(QOS_READER_DATA_LIFECYCLE_AUTOPURGE_NOWRITER_DELAY_NSEC, nanoseconds);
   }

   public double getReaderDataLifecycleAutopurgeNowriterDelay()
   {
      if ( !dataReaderQosMap.containsKey(QOS_READER_DATA_LIFECYCLE_AUTOPURGE_NOWRITER_DELAY_SEC))
      {
         // default value of Reader Data Lifecycle autopurge_nowriter_samples_delay is infinite
         return Integer.MAX_VALUE;
      }
      Object sec = dataReaderQosMap.get(QOS_READER_DATA_LIFECYCLE_AUTOPURGE_NOWRITER_DELAY_SEC);
      Object nanosec = dataReaderQosMap
            .get(QOS_READER_DATA_LIFECYCLE_AUTOPURGE_NOWRITER_DELAY_NSEC);

      return QosUtils.timeToDouble((Integer) sec, (Integer) nanosec);
   }


   /*
    * Reliability blocking time
    */
   public void setReliabilityBlockingTime(double blockingTime)
   {
      // Convert the float to seconds and nanoseconds.
      int seconds = QosUtils.timeToSec(blockingTime);
      int nanoseconds = QosUtils.timeToNanosec(blockingTime);

      // Reliability only applies to topic, data reader, and data writer.
      topicQosMap.put(QOS_RELIABILITY_BLOCKING_TIME_SEC, seconds);
      topicQosMap.put(QOS_RELIABILITY_BLOCKING_TIME_NSEC, nanoseconds);
      dataWriterQosMap.put(QOS_RELIABILITY_BLOCKING_TIME_SEC, seconds);
      dataWriterQosMap.put(QOS_RELIABILITY_BLOCKING_TIME_NSEC, nanoseconds);
      dataReaderQosMap.put(QOS_RELIABILITY_BLOCKING_TIME_SEC, seconds);
      dataReaderQosMap.put(QOS_RELIABILITY_BLOCKING_TIME_NSEC, nanoseconds);
   }

   public void setTopicReliabilityBlockingTime(double blockingTime)
   {
      // Convert the float to seconds and nanoseconds.
      int seconds = QosUtils.timeToSec(blockingTime);
      int nanoseconds = QosUtils.timeToNanosec(blockingTime);

      // Topic only
      topicQosMap.put(QOS_RELIABILITY_BLOCKING_TIME_SEC, seconds);
      topicQosMap.put(QOS_RELIABILITY_BLOCKING_TIME_NSEC, nanoseconds);
   }

   public double getReliabilityBlockingTime()
   {
      if ( !dataWriterQosMap.containsKey(QOS_RELIABILITY_BLOCKING_TIME_SEC))
      {
         // default value of Reliability max blocking time is 100ms
         return 0.1;
      }
      Object sec = dataWriterQosMap.get(QOS_RELIABILITY_BLOCKING_TIME_SEC);
      Object nanosec = dataWriterQosMap.get(QOS_RELIABILITY_BLOCKING_TIME_NSEC);

      return QosUtils.timeToDouble((Integer) sec, (Integer) nanosec);
   }


   /*
    * Reliability Kind
    */
   public void setReliabilityKind(String reliabilityKindString) throws DdsException
   {
      DDS.ReliabilityQosPolicyKind reliabilityKind = QosUtils
            .readReliabilityQosPolicyKind(reliabilityKindString);

      // Reliability only applies to topic, data reader, and data writer.
      topicQosMap.put(QOS_RELIABILITY_KIND, reliabilityKind);
      dataWriterQosMap.put(QOS_RELIABILITY_KIND, reliabilityKind);
      dataReaderQosMap.put(QOS_RELIABILITY_KIND, reliabilityKind);
   }

   public void setTopicReliabilityKind(String reliabilityKindString) throws DdsException
   {
      // Topic only
      topicQosMap.put(QOS_RELIABILITY_KIND,
            QosUtils.readReliabilityQosPolicyKind(reliabilityKindString));
   }

   public ReliabilityQosPolicyKind getReliabilityKind()
   {
      return (ReliabilityQosPolicyKind) dataWriterQosMap.get(QOS_RELIABILITY_KIND);
   }


   /*
    * Resource Limits Max Instances
    */
   public void setResourceLimitsMaxInstances(int maxInstances)
   {
      // Resource limits only applies to topic, data reader, and data writer.
      topicQosMap.put(QOS_RESOURCE_LIMITS_MAX_INSTANCES, maxInstances);
      dataWriterQosMap.put(QOS_RESOURCE_LIMITS_MAX_INSTANCES, maxInstances);
      dataReaderQosMap.put(QOS_RESOURCE_LIMITS_MAX_INSTANCES, maxInstances);
   }

   public void setTopicResourceLimitsMaxInstances(int maxInstances)
   {
      // Topic only
      topicQosMap.put(QOS_RESOURCE_LIMITS_MAX_INSTANCES, maxInstances);
   }

   public int getResourceLimitsMaxInstances()
   {
      if ( !dataWriterQosMap.containsKey(QOS_RESOURCE_LIMITS_MAX_INSTANCES))
      {
         // default value for Resource limits max instances is unlimited
         return Integer.MAX_VALUE;
      }
      return (Integer) dataWriterQosMap.get(QOS_RESOURCE_LIMITS_MAX_INSTANCES);
   }


   /*
    * Resource Limits Max Samples
    */
   public void setResourceLimitsMaxSamples(int maxSamples)
   {
      // Resource limits only applies to topic, data reader, and data writer.
      topicQosMap.put(QOS_RESOURCE_LIMITS_MAX_SAMPLES, maxSamples);
      dataWriterQosMap.put(QOS_RESOURCE_LIMITS_MAX_SAMPLES, maxSamples);
      dataReaderQosMap.put(QOS_RESOURCE_LIMITS_MAX_SAMPLES, maxSamples);
   }

   public void setTopicResourceLimitsMaxSamples(int maxSamples)
   {
      // Topic only
      topicQosMap.put(QOS_RESOURCE_LIMITS_MAX_SAMPLES, maxSamples);
   }


   public int getResourceLimitsMaxSamples()
   {
      if ( !dataWriterQosMap.containsKey(QOS_RESOURCE_LIMITS_MAX_SAMPLES))
      {
         // default value for Resource limits max samples is unlimited
         return Integer.MAX_VALUE;
      }
      return (Integer) dataWriterQosMap.get(QOS_RESOURCE_LIMITS_MAX_SAMPLES);
   }


   /*
    * Resource Limits Max Samples Per Instance
    */
   public void setResourceLimitsMaxSamplesPerInstance(int maxSamplesPerInstance)
   {
      // Resource limits only applies to topic, data reader, and data writer.
      topicQosMap.put(QOS_RESOURCE_LIMITS_MAX_SAMPLES_PER_INSTANCE, maxSamplesPerInstance);
      dataWriterQosMap.put(QOS_RESOURCE_LIMITS_MAX_SAMPLES_PER_INSTANCE, maxSamplesPerInstance);
      dataReaderQosMap.put(QOS_RESOURCE_LIMITS_MAX_SAMPLES_PER_INSTANCE, maxSamplesPerInstance);
   }

   public void setTopicResourceLimitsMaxSamplesPerInstance(int maxSamplesPerInstance)
   {
      // Topic only
      topicQosMap.put(QOS_RESOURCE_LIMITS_MAX_SAMPLES_PER_INSTANCE, maxSamplesPerInstance);
   }

   public int getResourceLimitsMaxSamplesPerInstance()
   {
      if ( !dataWriterQosMap.containsKey(QOS_RESOURCE_LIMITS_MAX_SAMPLES_PER_INSTANCE))
      {
         // default value for Resource limits max samples per instance is unlimited
         return Integer.MAX_VALUE;
      }
      return (Integer) dataWriterQosMap.get(QOS_RESOURCE_LIMITS_MAX_SAMPLES_PER_INSTANCE);
   }


   /*
    * Time Based Filter
    */
   public void setTimebasedFilter(double timebasedFilter)
   {
      // Convert the float to seconds and nanoseconds.
      int seconds = QosUtils.timeToSec(timebasedFilter);
      int nanoseconds = QosUtils.timeToNanosec(timebasedFilter);

      // Time based filter only applies to data reader.
      dataReaderQosMap.put(QOS_TIME_BASED_FILTER_SEC, seconds);
      dataReaderQosMap.put(QOS_TIME_BASED_FILTER_NSEC, nanoseconds);
   }

   public double getTimebasedFilter()
   {
      if ( !dataReaderQosMap.containsKey(QOS_TIME_BASED_FILTER_SEC))
      {
         // default value of Time based filter minimum separation is 0
         return 0;
      }
      Object sec = dataReaderQosMap.get(QOS_TIME_BASED_FILTER_SEC);
      Object nanosec = dataReaderQosMap.get(QOS_TIME_BASED_FILTER_NSEC);

      return QosUtils.timeToDouble((Integer) sec, (Integer) nanosec);
   }


   /*
    * Transport Priority
    */
   public void setTransportPriority(int priority)
   {
      // Transport priority only applies to topic and data writer.
      topicQosMap.put(QOS_TRANSPORT_PRIORITY, priority);
      dataWriterQosMap.put(QOS_TRANSPORT_PRIORITY, priority);
   }

   public void setTopicTransportPriority(int priority)
   {
      // Topic only
      topicQosMap.put(QOS_TRANSPORT_PRIORITY, priority);

   }

   public int getTransportPriority()
   {
      if ( !dataWriterQosMap.containsKey(QOS_TRANSPORT_PRIORITY))
      {
         // default value for Transport priority is 0
         return 0;
      }
      return (Integer) dataWriterQosMap.get(QOS_TRANSPORT_PRIORITY);
   }


   /*
    * Writer Data Lifecycle Autodispose
    */
   public void setWriterDataLifecycleAutodispose(boolean autodispose)
   {
      // Writer data lifecycle only applies to data writer.
      dataWriterQosMap.put(QOS_WRITER_DATA_LIFECYCLE_AUTODISPOSE, autodispose);
   }

   public boolean getWriterDataLifecycleAutodispose()
   {
      if ( !dataWriterQosMap.containsKey(QOS_WRITER_DATA_LIFECYCLE_AUTODISPOSE))
      {
         // default value for Writer Data Lifecycle autodispose is true
         return true;
      }
      return (Boolean) dataWriterQosMap.get(QOS_WRITER_DATA_LIFECYCLE_AUTODISPOSE);
   }


   /*
    * Group data
    */
   public void setGroupData(byte[] data)
   {
      // Group data only applies to publisher and subscriber.
      publisherQosMap.put(QOS_GROUP_DATA, data);
      subscriberQosMap.put(QOS_GROUP_DATA, data);
   }

   public byte[] getGroupData()
   {
      return (byte[]) publisherQosMap.get(QOS_GROUP_DATA);
   }


   /*
    * Topic data
    */
   public void setTopicData(byte[] data)
   {
      // Topic data only applies to topic.
      topicQosMap.put(QOS_TOPIC_DATA, data);
   }

   public byte[] getTopicData()
   {
      return (byte[]) topicQosMap.get(QOS_TOPIC_DATA);
   }


   /*
    * User data
    */
   public void setUserData(byte[] data)
   {
      // User data only applies to domain participant, data reader, and data writer.
      participantQosMap.put(QOS_USER_DATA, data);
      dataWriterQosMap.put(QOS_USER_DATA, data);
      dataReaderQosMap.put(QOS_USER_DATA, data);
   }

   public byte[] getUserData()
   {
      return (byte[]) dataWriterQosMap.get(QOS_USER_DATA);
   }


   /**
    * Get the map of extra QoS set on this endpoint for the DDS Publisher
    * 
    * @return a map of extra QoS for DDS Publisher
    */
   Map<String, Object> getPublisherQosMap()
   {
      return publisherQosMap;
   }

   /**
    * Get the map of extra QoS set on this endpoint for the DDS Subscriber
    * 
    * @return a map of extra QoS for DDS Subscriber
    */
   Map<String, Object> getSubscriberQosMap()
   {
      return subscriberQosMap;
   }

   /**
    * Get the map of extra QoS set on this endpoint for the DDS DataWriter
    * 
    * @return a map of extra QoS for DDS DataWriter
    */
   Map<String, Object> getDataWriterQosMap()
   {
      return dataWriterQosMap;
   }

   /**
    * Get the map of extra QoS set on this endpoint for the DDS DataReader
    * 
    * @return a map of extra QoS for DDS DataReader
    */
   Map<String, Object> getDataReaderQosMap()
   {
      return dataReaderQosMap;
   }


   @Override
   public boolean isSingleton()
   {
      return true;
   }

   @Override
   protected void doStart() throws Exception
   {
      LOG.debug("{} : doStart()", this);
      super.doStart();
      createParticipant();
      findOrCreateTopic();
   }

   @Override
   protected void doStop() throws Exception
   {
      LOG.debug("{} : doStop()", this);
      deleteTopic();
      deleteParticipant();
      super.doStop();
   }


   /**
    * Create the DDS Participant (if already created, do nothing).
    * 
    * @throws DdsException
    *            if a DDS error occurs.
    */
   private void createParticipant() throws DdsException
   {
      if (participant != null)
         return;

      // Get DomainParticipantFactory
      DomainParticipantFactory dpf = DomainParticipantFactory.get_instance();

      // Get default DomainParticipantQos
      DomainParticipantQosHolder participantQosHolder = new DomainParticipantQosHolder();
      int status = dpf.get_default_participant_qos(participantQosHolder);
      DdsErrorHandler.checkStatus(status, "Get default participant QoS");

      // Update DomainParticipantQos with endpoint options
      QosUtils.updateQos(participantQosHolder, participantQosMap);

      // Create DDS domain participant
      try
      {
         if (ddsComponent.getCreateParticipantMethod() == null)
         {
            throw new DdsException(
                  "Internal Error: failed to find appropriate create_participant() operation.");
         }

         // call appropriate create_participant operation
         // depending OpenSpiceDDS version
         if (ddsComponent.isDomainInt())
         {
            LOG.debug("{} : create_participant on domain {} (OSPL v6.x)", this, domainId);
            participant = (DomainParticipant) ddsComponent.getCreateParticipantMethod()
                  .invoke(dpf, domainId,
                        participantQosHolder.value, null,
                        DDS.STATUS_MASK_NONE.value);
         }
         else
         {
            // With OpenSpliceDDS v5.x use null as DomainId
            LOG.debug("create_participant on domain 'null' (OSPL v5.x)");
            LOG.warn("OpenSpliceDDS v5.x detected: Specified DomainID '" + domainId
                  + "' is ignored (domain found via $OSPL_URI will be used)");
            participant = (DomainParticipant) ddsComponent.getCreateParticipantMethod()
                  .invoke(dpf, null,
                        participantQosHolder.value, null,
                        DDS.STATUS_MASK_NONE.value);
         }
      }
      catch (Exception e)
      {
         // re-throw exception
         throw new DdsException(e);
      }
      DdsErrorHandler.checkHandle(participant, "Create participant");
   }

   /**
    * Delete the DDS Participant (if not created, do nothing).
    * 
    * @throws DdsException
    *            if a DDS error occurs.
    */
   private void deleteParticipant() throws DdsException
   {
      if (participant != null)
      {
         LOG.debug("{} : deleteParticipant", this);
         int status = DomainParticipantFactory.get_instance().delete_participant(participant);
         DdsErrorHandler.checkStatus(status, "Delete participant");
         participant = null;
      }
   }

   /**
    * Get the DDS Participant (throw a {@link RuntimeCamelException} if not created).
    * 
    * @return the DDS Participant
    */
   DomainParticipant getParticipant()
   {
      if (participant == null)
      {
         throw new RuntimeCamelException("Endpoint " + toString() + " is not started");
      }
      return participant;
   }


   /**
    * Try to find the DDS Topic if defined in the Domain, and create the DDS Topic if not found. If
    * contentFilter option is set, a ContentFilteredTopic is also created.
    * 
    * @return Topic the DDS Topic
    * @throws DdsException
    *            if a DDS error occurs
    */
   private Topic findOrCreateTopic() throws DdsException
   {
      try
      {
         LOG.debug("{} : Find or create Topic {}", this, topicName);

         // Get topic type name
         String typeSupportName = typeName + TYPE_SUPPORT_CLASS_SUFFIX;
         Class<?> typeSupport = getCamelContext().getClassResolver().resolveMandatoryClass(
               typeSupportName);
         Method getType = typeSupport.getMethod(GET_TYPE_METHOD, (Class[]) null);
         String typeName = (String) getType.invoke(typeSupport.newInstance(), (Object[]) null);

         // Register topic type
         LOG.debug("{} : Register TypeSupport: {}", this, typeSupportName);
         Class<?>[] arrRegisterTypeTypeParams =
         {
               DomainParticipant.class, String.class
         };
         Method registerType = typeSupport.getMethod(REGISTER_TYPE_METHOD,
               arrRegisterTypeTypeParams);
         Object[] arrRegisterTypeParams =
         {
               participant, typeName
         };
         Object statusObj = registerType.invoke(typeSupport.newInstance(), arrRegisterTypeParams);
         DdsErrorHandler.checkStatus( ((Integer) statusObj).intValue(),
               "Register type support for " + typeSupportName);

         // Try to find topic
         DDS.Duration_t duration = new DDS.Duration_t();
         duration.nanosec = 0;
         duration.sec = 0;
         ddsTopic = participant.find_topic(topicName, duration);

         if (ddsTopic == null)
         {
            // get default TopicQos
            TopicQosHolder topicQosHolder = new TopicQosHolder();
            int status = participant.get_default_topic_qos(topicQosHolder);
            DdsErrorHandler.checkStatus(status, "Get default topic QoS");

            // update TopicQos with endpoint options
            QosUtils.updateQos(topicQosHolder, topicQosMap);

            // Create topic
            LOG.debug("{} : Create topic {}", this, topicName);
            LOG.debug("{} : With type {}", this, typeName);
            ddsTopic = participant.create_topic(topicName, typeName, topicQosHolder.value, null, // listener
                  DDS.STATUS_MASK_NONE.value);
         }
         else
         {
            LOG.info("{} : Topic {} already exists, we use it.", this, topicName);
         }
         DdsErrorHandler.checkHandle(ddsTopic, "Create topic " + topicName);

         // Create filtered topic if required
         if (contentFilter != null)
         {
            int hash = contentFilter.hashCode();
            String filteredTopic = "filter" + hash + topicName;
            LOG.debug("{} : create_contentfilteredtopic {}", this, filteredTopic);
            contentFilteredTopic = participant.create_contentfilteredtopic(filteredTopic, ddsTopic,
                  contentFilter, new String[0]);
            DdsErrorHandler.checkHandle(contentFilteredTopic, "Create content filtered topic");
         }
      }
      catch (ClassNotFoundException e)
      {
         LOG.error("{} : Class not found.", this);
         throw new DdsException(e);
      }
      catch (IllegalAccessException e)
      {
         LOG.error("{} : Illegal access exception raised.", this);
         throw new DdsException(e);
      }
      catch (InstantiationException e)
      {
         LOG.error("{} : Instantiation exception raised.", this);
         throw new DdsException(e);
      }
      catch (SecurityException e)
      {
         LOG.error("{} : Security violation.", this);
         throw new DdsException(e);
      }
      catch (NoSuchMethodException e)
      {
         LOG.error("{} : No such method.", this);
         throw new DdsException(e);
      }
      catch (IllegalArgumentException e)
      {
         LOG.error("{} : Illegal argument.", this);
         throw new DdsException(e);
      }
      catch (InvocationTargetException e)
      {
         LOG.error("{} : Invocation target exception raised.", this);
         throw new DdsException(e);
      }
      return ddsTopic;
   }

   /**
    * Delete the DDS Topic (and the ContentFilteredTopic if it was created).
    * 
    * @throws DdsException
    *            if a DDS Error occurs
    */
   private void deleteTopic() throws DdsException
   {
      if (contentFilteredTopic != null)
      {
         LOG.debug("{} : delete_contentfilteredtopic", this);
         int status = participant.delete_contentfilteredtopic(contentFilteredTopic);
         DdsErrorHandler.checkStatus(status, "Delete content filtered topic");
         contentFilteredTopic = null;
      }

      if (ddsTopic != null)
      {
         LOG.debug("{} : delete_topic", this);
         int status = participant.delete_topic(ddsTopic);
         DdsErrorHandler.checkStatus(status, "Delete topic");
         ddsTopic = null;
      }
   }

   /**
    * Get the DDS Topic (throw a {@link RuntimeCamelException} if endpoint is not started).
    * 
    * @return the DDS Topic
    */
   public Topic getTopic()
   {
      if (ddsTopic == null)
      {
         throw new RuntimeCamelException("Endpoint " + toString() + " is not started");
      }
      return ddsTopic;
   }

   /**
    * Get the DDS ContentFilteredTopic or null if contentFilter option was not set (throw a
    * {@link RuntimeCamelException} if endpoint is not started).
    * 
    * @return the DDS ContentFilteredTopic
    */
   public ContentFilteredTopic getContentFilteredTopic()
   {
      if (ddsTopic == null)
      {
         throw new RuntimeCamelException("Endpoint " + toString() + " is not started");
      }
      return contentFilteredTopic;
   }


   @Override
   public Producer createProducer() throws Exception
   {
      return new DdsProducer(this);
   }

   @Override
   public Consumer createConsumer(Processor processor) throws Exception
   {
      return new DdsConsumer(this, processor);
   }

   @Override
   public PollingConsumer createPollingConsumer() throws Exception
   {
      return new DdsPollingConsumer(this);
   }


   /**
    * Create an exchange with sample and its SampleInfo and call the processor to forward the
    * message.
    * 
    * @param sample
    *           the DDS sample
    * @param info
    *           the associated SampleInfo
    */
   public Exchange createExchange(Object sample, SampleInfo info)
   {
      // create an new empty Exchange
      Exchange exchange = createExchange();

      // set DDS sample as IN body
      exchange.getIn().setBody(sample);

      // set full SampleInfo as IN header
      exchange.getIn().setHeader("DDS_SAMPLE_INFO", info);

      // if DISPOSED, set DDS_DISPOSE for possible consecutive publication
      if (info.instance_state == NOT_ALIVE_DISPOSED_INSTANCE_STATE.value)
      {
         if (info.valid_data && info.view_state == NEW_VIEW_STATE.value)
         {
            exchange.getIn().setHeader("DDS_DISPOSE", DdsDisposeHeader.WRITEDISPOSE);
         }
         else
         {
            exchange.getIn().setHeader("DDS_DISPOSE", DdsDisposeHeader.DISPOSE);
         }
      }

      return exchange;
   }

}
