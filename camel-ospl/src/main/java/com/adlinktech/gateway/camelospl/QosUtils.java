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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.camel.RuntimeCamelException;

import DDS.DataReaderQosHolder;
import DDS.DataWriterQosHolder;
import DDS.DomainParticipantQosHolder;
import DDS.PublisherQosHolder;
import DDS.SubscriberQosHolder;
import DDS.TopicQosHolder;


/**
 * Utility class for DDS QoS
 */
public final class QosUtils
{

   /** Constant for nanoseconds in a second */
   private static final int NANOSECONDS_IN_A_SECOND = 1000000000;

   /*
    * Utility class with only static method.
    * Disable default constructor.
    */
   private QosUtils()
   {}


   /**
    * Get the seconds part from a time expressed as a double (with second as unit)
    * 
    * @param time the time
    * @return the seconds part of the time
    */
   public static int timeToSec(double time)
   {
      return (int) time;
   }

   /**
    * Get the nanoseconds part from a time expressed as a double (with second as unit)
    * 
    * @param time the time
    * @return the nanoseconds part of the time
    */
   public static int timeToNanosec(double time)
   {
      return (int) Math.round( (time - ((int) time)) * NANOSECONDS_IN_A_SECOND);
   }

   /**
    * Convert a time expressed with 2 int (seconds and nanoseconds)
    * to a double with second as unit
    * 
    * @param sec the seconds part of time
    * @param nanosec the nanoseconds part of time
    * @return the time as a double
    */
   public static double timeToDouble(int sec, int nanosec)
   {
      return ((double) sec) + ( ((double) nanosec) / NANOSECONDS_IN_A_SECOND);
   }


   /**
    * Read a DDS.DestinationOrderQosPolicyKind value as a String.
    * Accepted values are: "BY_RECEPTION_TIMESTAMP" or "BY_SOURCE_TIMESTAMP".
    * 
    * @param s the String to read
    * @return the DDS.DestinationOrderQosPolicyKind value
    * @throws DdsException if the String is not a DDS.DestinationOrderQosPolicyKind kind
    */
   public static DDS.DestinationOrderQosPolicyKind readDestinationOrderQosPolicyKind(String s)
      throws DdsException
   {
      if ("BY_RECEPTION_TIMESTAMP".equals(s))
      {
         return DDS.DestinationOrderQosPolicyKind.BY_RECEPTION_TIMESTAMP_DESTINATIONORDER_QOS;
      }
      else if ("BY_SOURCE_TIMESTAMP".equals(s))
      {
         return DDS.DestinationOrderQosPolicyKind.BY_SOURCE_TIMESTAMP_DESTINATIONORDER_QOS;
      }
      else
      {
         throw new DdsException("Invalid DestinationOrderQosPolicyKind string specified: " + s);
      }
   }

   /**
    * Read a DDS.DurabilityQosPolicyKind value as a String.
    * Accepted values are: "VOLATILE", "TRANSIENT", "TRANSIENT_LOCAL" or "PERSISTENT".
    * 
    * @param s the String to read
    * @return the DDS.DurabilityQosPolicyKind value
    * @throws DdsException if the String is not a DDS.DurabilityQosPolicyKind
    */
   public static DDS.DurabilityQosPolicyKind readDurabilityQosPolicyKind(String s)
      throws DdsException
   {
      if ("VOLATILE".equals(s))
      {
         return DDS.DurabilityQosPolicyKind.VOLATILE_DURABILITY_QOS;
      }
      else if ("TRANSIENT".equals(s))
      {
         return DDS.DurabilityQosPolicyKind.TRANSIENT_DURABILITY_QOS;
      }
      else if ("TRANSIENT_LOCAL".equals(s))
      {
         return DDS.DurabilityQosPolicyKind.TRANSIENT_LOCAL_DURABILITY_QOS;
      }
      else if ("PERSISTENT".equals(s))
      {
         return DDS.DurabilityQosPolicyKind.PERSISTENT_DURABILITY_QOS;
      }
      else
      {
         throw new DdsException("Invalid DurabilityQosPolicyKind string specified: " + s);
      }
   }

   /**
    * Read a DDS.HistoryQosPolicyKind value as a String.
    * Accepted values are: "KEEP_ALL" or "KEEP_LAST".
    * 
    * @param s the String to read
    * @return the DDS.HistoryQosPolicyKind value
    * @throws DdsException if the String is not a DDS.HistoryQosPolicyKind
    */
   public static DDS.HistoryQosPolicyKind readHistoryQosPolicyKind(String s)
      throws DdsException
   {
      if ("KEEP_ALL".equals(s))
      {
         return DDS.HistoryQosPolicyKind.KEEP_ALL_HISTORY_QOS;
      }
      else if ("KEEP_LAST".equals(s))
      {
         return DDS.HistoryQosPolicyKind.KEEP_LAST_HISTORY_QOS;
      }
      else
      {
         throw new DdsException(
               "Invalid HistoryQosPolicyKind string specified: " + s);
      }
   }

   /**
    * Read a DDS.LivelinessQosPolicyKind value as a String.
    * Accepted values are: "AUTOMATIC", "MANUAL_BY_PARTICPANT" or "MANUAL_BY_TOPIC".
    * 
    * @param s the String to read
    * @return the DDS.LivelinessQosPolicyKind value
    * @throws DdsException if the String is not a DDS.LivelinessQosPolicyKind
    */
   public static DDS.LivelinessQosPolicyKind readLivelinessQosPolicyKind(String s)
      throws DdsException
   {
      if ("AUTOMATIC".equals(s))
      {
         return DDS.LivelinessQosPolicyKind.AUTOMATIC_LIVELINESS_QOS;
      }
      else if ("MANUAL_BY_PARTICPANT".equals(s))
      {
         return DDS.LivelinessQosPolicyKind.MANUAL_BY_PARTICIPANT_LIVELINESS_QOS;
      }
      else if ("MANUAL_BY_TOPIC".equals(s))
      {
         return DDS.LivelinessQosPolicyKind.MANUAL_BY_TOPIC_LIVELINESS_QOS;
      }
      else
      {
         throw new DdsException("Invalid LivelinessQosPolicyKind string specified: " + s);
      }
   }

   /**
    * Read a DDS.OwnershipQosPolicyKind value as a String.
    * Accepted values are: "SHARED" or "EXCLUSIVE".
    * 
    * @param s the String to read
    * @return the DDS.OwnershipQosPolicyKind value
    * @throws DdsException if the String is not a DDS.OwnershipQosPolicyKind
    */
   public static DDS.OwnershipQosPolicyKind readOwnershipQosPolicyKind(String s)
      throws DdsException
   {
      if ("SHARED".equals(s))
      {
         return DDS.OwnershipQosPolicyKind.SHARED_OWNERSHIP_QOS;
      }
      else if ("EXCLUSIVE".equals(s))
      {
         return DDS.OwnershipQosPolicyKind.EXCLUSIVE_OWNERSHIP_QOS;
      }
      else
      {
         throw new DdsException(
               "Invalid OwnershipQosPolicyKind string specified: " + s);
      }
   }

   /**
    * Read a DDS.PresentationQosPolicyAccessScopeKind value as a String.
    * Accepted values are: "INSTANCE", "TOPIC" or "GROUP".
    * 
    * @param s the String to read
    * @return the DDS.PresentationQosPolicyAccessScopeKind value
    * @throws DdsException if the String is not a DDS.PresentationQosPolicyAccessScopeKind
    */
   public static DDS.PresentationQosPolicyAccessScopeKind readPresentationQosPolicyAccessScopeKind(
         String s)
      throws DdsException
   {
      if ("INSTANCE".equals(s))
      {
         return DDS.PresentationQosPolicyAccessScopeKind.INSTANCE_PRESENTATION_QOS;
      }
      else if ("TOPIC".equals(s))
      {
         return DDS.PresentationQosPolicyAccessScopeKind.TOPIC_PRESENTATION_QOS;
      }
      else if ("GROUP".equals(s))
      {
         return DDS.PresentationQosPolicyAccessScopeKind.GROUP_PRESENTATION_QOS;
      }
      else
      {
         throw new DdsException("Invalid PresentationQosPolicyAccessScopeKind string specified: "
               + s);
      }
   }

   /**
    * Read a DDS.ReliabilityQosPolicyKind value as a String.
    * Accepted values are: "RELIABLE" or "BEST_EFFORT".
    * 
    * @param s the String to read
    * @return the DDS.ReliabilityQosPolicyKind value
    * @throws DdsException if the String is not a DDS.ReliabilityQosPolicyKind
    */
   public static DDS.ReliabilityQosPolicyKind readReliabilityQosPolicyKind(String s)
      throws DdsException
   {
      if ("RELIABLE".equals(s))
      {
         return DDS.ReliabilityQosPolicyKind.RELIABLE_RELIABILITY_QOS;
      }
      else if ("BEST_EFFORT".equals(s))
      {
         return DDS.ReliabilityQosPolicyKind.BEST_EFFORT_RELIABILITY_QOS;
      }
      else
      {
         throw new DdsException(
               "Invalid ReliabilityQosPolicyKind string specified: " + s);
      }
   }


   /**
    * Update a DomainParticipantQos set with new QoS.
    * 
    * @param participantQosHolder the DomainParticipantQosHolder.
    * @param newQos the new QoS to be set.
    */
   public static void updateQos(DomainParticipantQosHolder participantQosHolder,
         Map<String, Object> newQos)
   {
      try
      {
         setNestedFields(participantQosHolder.value, newQos);
      }
      catch (Exception e)
      {
         throw new RuntimeCamelException(e);
      }
   }

   /**
    * Update a TopicQos set with new QoS.
    * 
    * @param topicQosHolder the TopicQosHolder.
    * @param newQos the new QoS to be set.
    */
   public static void updateQos(TopicQosHolder topicQosHolder,
         Map<String, Object> newQos)
   {
      try
      {
         setNestedFields(topicQosHolder.value, newQos);
      }
      catch (Exception e)
      {
         throw new RuntimeCamelException(e);
      }
   }

   /**
    * Update a PublisherQos set with new QoS.
    * 
    * @param publisherQosHolder the PublisherQosHolder.
    * @param newQos the new QoS to be set.
    */
   public static void updateQos(PublisherQosHolder publisherQosHolder,
         Map<String, Object> newQos)
   {
      try
      {
         setNestedFields(publisherQosHolder.value, newQos);
      }
      catch (Exception e)
      {
         throw new RuntimeCamelException(e);
      }
   }

   /**
    * Update a SubscriberQos set with new QoS.
    * 
    * @param subscriberQosHolder the SubscriberQosHolder.
    * @param newQos the new QoS to be set.
    */
   public static void updateQos(SubscriberQosHolder subscriberQosHolder,
         Map<String, Object> newQos)
   {
      try
      {
         setNestedFields(subscriberQosHolder.value, newQos);
      }
      catch (Exception e)
      {
         throw new RuntimeCamelException(e);
      }
   }

   /**
    * Update a DataReaderQos set with new QoS.
    * 
    * @param readerQosHolder the DataReaderQosHolder.
    * @param newQos the new QoS to be set.
    */
   public static void updateQos(DataReaderQosHolder readerQosHolder,
         Map<String, Object> newQos)
   {
      try
      {
         setNestedFields(readerQosHolder.value, newQos);
      }
      catch (Exception e)
      {
         throw new RuntimeCamelException(e);
      }
   }

   /**
    * Update a DataWriterQos set with new QoS.
    * 
    * @param writerQosHolder the DataWriterQosHolder.
    * @param newQos the new QoS to be set.
    */
   public static void updateQos(DataWriterQosHolder writerQosHolder,
         Map<String, Object> newQos)
   {
      try
      {
         setNestedFields(writerQosHolder.value, newQos);
      }
      catch (Exception e)
      {
         throw new RuntimeCamelException(e);
      }
   }


   /**
    * Set some nested fields values of an object instance, using Java reflection.
    * If some field in a path have null value, they will be instantiated
    * using the default constructor.
    * 
    * @param instance the object instance.
    * @param fieldsValues a map of values, where each key is a path to some field
    *           (e.g. "field1.field2.field3").
    */
   private static void setNestedFields(Object instance, Map<String, Object> fieldsValues)
      throws SecurityException, NoSuchFieldException,
      IllegalArgumentException, IllegalAccessException,
      NoSuchMethodException, InstantiationException,
      InvocationTargetException
   {
      for (Entry<String, Object> entry : fieldsValues.entrySet())
      {
         System.out.println("Set " + entry.getKey() + " : " + entry.getValue());
         setNestedField(instance, entry.getKey(), entry.getValue());
      }
   }


   /**
    * Set a nested field value of an object instance, using Java reflection.
    * If some field in the path have null value, they will be instantiated
    * using the default constructor.
    * 
    * @param instance the object instance.
    * @param path the path to some field (e.g. "field1.field2.field3").
    * @param value the value to be set.
    */
   private static void setNestedField(Object instance, String path, Object value)
      throws SecurityException, NoSuchFieldException,
      IllegalArgumentException, IllegalAccessException,
      NoSuchMethodException, InstantiationException,
      InvocationTargetException
   {
      if (instance == null)
         throw new NullPointerException("instance object is null");

      int idx = path.indexOf('.');
      if (idx == -1)
      {
         setField(instance, path, value);
      }
      else
      {
         Field f = getField(instance, path.substring(0, idx));
         Object fval = f.get(instance);
         if (fval == null)
         {
            fval = createDefaultValue(f.getType());
            f.set(instance, fval);
         }
         setNestedField(fval, path.substring(idx + 1), value);
      }
   }

   /**
    * Returns the {@link Field} object that reflects
    * the specified member field of the object.
    * 
    * @param obj the object.
    * @param name the field name.
    * @return the {@link Field} object.
    */
   private static Field getField(Object obj, String name)
      throws SecurityException, NoSuchFieldException
   {
      Class<?> c = obj.getClass();
      return c.getField(name);
   }

   /**
    * Sets the field with specified name of the specified object with the specified new value.
    * The new value is automatically unwrapped if the underlying field has a primitive type.
    * 
    * @param obj the object whose field should be modified.
    * @param name the field name.
    * @param value the value for the field of obj being modified.
    */
   private static void setField(Object obj, String name, Object value)
      throws SecurityException, NoSuchFieldException,
      IllegalArgumentException, IllegalAccessException
   {
      Field f = getField(obj, name);
      f.set(obj, value);
   }

   /**
    * Create a new instance of the specified class using the default constructor.
    * 
    * @param c the class to be instantiated.
    * @return a new instance of the class.
    */
   private static Object createDefaultValue(Class<?> c)
      throws SecurityException, NoSuchMethodException,
      IllegalArgumentException, InstantiationException,
      IllegalAccessException, InvocationTargetException
   {
      Constructor<?> ctr = c.getConstructor();
      return ctr.newInstance();
   }


}
