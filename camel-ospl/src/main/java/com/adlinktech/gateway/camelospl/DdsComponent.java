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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.ResolveEndpointFailedException;
import org.apache.camel.impl.DefaultComponent;
import org.apache.camel.util.ObjectHelper;
import org.slf4j.Logger;

import com.adlinktech.gateway.camelospl.utils.Loggers;

import DDS.DomainParticipantFactory;


/**
 * DDS Camel component.
 */
public class DdsComponent
   extends DefaultComponent
{

   // Logger
   private static final Logger LOG = Loggers.LOGGER_ENDPOINT;

   // The OpenSplice Method that shall be used to create a Participant
   // (this method differs depending the OpenSplice version)
   private Method createParticipantMethod = null;

   // True if OpenSplice requires the domainId to be an int (as in v6.x+)
   // False otherwise (as in v5.x)
   private boolean isDomainInt = true;

   // List of the PublicationHandles, each created by a local DataWriter.
   // This list is used to implement the ignoreLocalPublishers option.
   private Set<Long> localPublicationHandles;


   // URI must match <topicName>:<domainId>/<typeName> where:
   // - <topicName> is an name containing alphanum or '-' or '_' characters,
   // but starting with a letter
   // - <domainId> is an integer
   // - <typeName> is a java class name (full scope)
   private static final String URI_PATTERN = "(\\p{Alpha}[\\p{Alnum}-_]*)[:](\\d+)[/](\\p{Alpha}[\\p{Alnum}_.]*)?";

   /**
    * Default constructor.
    */
   public DdsComponent()
   {
      super();
      localPublicationHandles = new HashSet<Long>();
   }

   /**
    * Constructor with an attached CamelContext.
    * 
    * @param context the CamelContext to attach to the DdsComponent.
    */
   public DdsComponent(CamelContext context)
   {
      super(context);
      findOpenSpliceCreateParticipantMethod();
      localPublicationHandles = new HashSet<Long>();
   }


   @Override
   public void setCamelContext(CamelContext context)
   {
      super.setCamelContext(context);
      // if CamelContext was set after calling the default constructor,
      // the createParticipantMethod was not set. Find it and set it now.
      findOpenSpliceCreateParticipantMethod();
   }

   /**
    * Find by reflection the DDS.DomainParticipantFactory.create_operation() defined by OpenSplice.
    * The signature of this operation is different between OpenSplice v5.x and v6.x+.
    * WARNING: this operation requires the CamelContext to be set. Therefore, it cannot be called
    * in the default constructor...
    */
   private synchronized void findOpenSpliceCreateParticipantMethod()
   {
      if (createParticipantMethod == null)
      {
         ObjectHelper.notNull(getCamelContext(), "camelContext");
         try
         {
            @SuppressWarnings("unchecked")
            Class<DomainParticipantFactory> c = (Class<DomainParticipantFactory>)
                  getCamelContext().getClassResolver().resolveMandatoryClass(
                        "DDS.DomainParticipantFactory");

            try
            {
               // Try OSPL V6.x (Domain = int)
               createParticipantMethod = c.getMethod("create_participant", int.class,
                     DDS.DomainParticipantQos.class,
                     DDS.DomainParticipantListener.class, int.class);
            }
            catch (SecurityException e)
            {
               // Should not happen
               throw new RuntimeException(
                     "Failed to find method DDS.DomainParticipantFactory.create_participant()", e);
            }
            catch (NoSuchMethodException e)
            {
               // bad luck...
               try
               {
                  // Try OSPL V5.x (Domain = String)
                  createParticipantMethod = c.getMethod("create_participant", String.class,
                        DDS.DomainParticipantQos.class,
                        DDS.DomainParticipantListener.class, int.class);
                  isDomainInt = false;
               }
               catch (SecurityException e1)
               {
                  // Should not happen
                  throw new RuntimeException(
                        "Failed to find method DDS.DomainParticipantFactory.create_participant()",
                        e1);
               }
               catch (NoSuchMethodException e1)
               {
                  // Should not happen
                  throw new RuntimeException(
                        "Failed to find method DDS.DomainParticipantFactory.create_participant()",
                        e1);
               }
            }
         }
         catch (ClassNotFoundException e)
         {
            // Should not happen
            throw new RuntimeException("Failed to find class DDS.DomainParticipantFactory", e);
         }
      }
   }

   /**
    * Return the OpenSplice's {@link DomainParticipantFactory} Method
    * to be used for the creation of a Participant.
    * 
    * @return the OpenSplice Method creating a Participant
    */
   protected Method getCreateParticipantMethod()
   {
      return createParticipantMethod;
   }

   /**
    * Return true if OpenSplice requires the DDS DomainId to be an int,
    * false if it requires to be a String.
    * 
    * @return true if OpenSplice requires the DDS DomainId to be an int.
    */
   protected boolean isDomainInt()
   {
      return isDomainInt;
   }

   protected void validateURI(String uri, String path, Map<String, Object> parameters)
   {
      super.validateURI(uri, path, parameters);

      if ( !path.matches(URI_PATTERN))
         throw new ResolveEndpointFailedException(uri, "Invalid uri syntax for a 'dds' endpoint: "
               + "it doesn't match expected pattern: dds:<TopicName>:<DomainID>/[<TypeName>]");
   }


   protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters)
      throws Exception
   {
      LOG.info("Create DDS endpoint: {}", uri);

      // Parse the remaining part of the URI which should have the topic name,
      // the domain, and the data class name, e.g., Chat:5/Chat.ChatMessage.
      String[] splitStrings = remaining.split("/");
      DdsEndpoint endpoint = null;
      String split[] = splitStrings[0].split(":");
      String topicName = split[0];
      int domainId = Integer.valueOf(split[1]);
      String typeName = null;
      if (splitStrings.length == 1)
      {
         LOG.debug("create a CamelDdsEndpoint for topic:domain = '{}'", splitStrings[0]);
         endpoint = new com.adlinktech.gateway.camelospl.camelmessage.CamelDdsEndpoint(topicName,
               domainId, uri, this);
      }
      else
      {
         typeName = splitStrings[1];
         LOG.debug("create a DdsEndpoint for topic:domain = '{}' and type {}", splitStrings[0],
               typeName);
         endpoint = new DdsEndpoint(topicName, domainId, typeName, uri, this);
      }
      return endpoint;
   }

   /**
    * Add the PublicationHandle of a local (i.e. same process) DataWriter
    * 
    * @param publicationHandle the DataWriter's PublicationHandle
    */
   protected void addLocalPublicationHandle(long publicationHandle)
   {
      localPublicationHandles.add(publicationHandle);
   }

   /**
    * Remove the PublicationHandle of a local (i.e. same process) DataWriter
    * 
    * @param publicationHandle the DataWriter's PublicationHandle
    */
   protected void removeLocalPublicationHandle(long publicationHandle)
   {
      localPublicationHandles.remove(publicationHandle);
   }

   /**
    * Return true if the PublicationHandle corresponds to a local (i.e. same process) DataWriter.
    * 
    * @param publicationHandle the PublicationHandle to test.
    * @return true if it corresponds to a local DataWriter.
    */
   protected boolean isPublicationHandleLocal(long publicationHandle)
   {
      return localPublicationHandles.contains(publicationHandle);
   }

}
