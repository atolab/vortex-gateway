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
package com.adlinktech.gateway.camelospl.camelmessage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.activation.DataHandler;
import javax.mail.util.ByteArrayDataSource;

import org.apache.camel.Message;

import com.adlinktech.gateway.camelospl.DdsException;

import CamelDDS.CamelAttachment;
import CamelDDS.CamelHeader;
import CamelDDS.CamelMessage;
import CamelDDS.SerializedPOJO;


/**
 * Utility class for {@link CamelMessage}.
 */
public final class CamelMessageUtils
{

   /*
    * Utility class with only static method.
    * Disable default constructor.
    */
   private CamelMessageUtils()
   {}

   /**
    * Create a {@link CamelMessage} object from a Camel Message object.
    * 
    * @param m the Camel' Message object to be transformed as a CamelMessage
    * @param target the CamelMessage's target
    * @return a new CamelMessage
    * @throws DdsException if an error occurs
    */
   public static CamelMessage createCamelMessage(Message m, String target) throws DdsException
   {
      // create a new CamelMessage
      CamelMessage msg = new CamelMessage();

      // copy messageId
      msg.message_id = m.getMessageId();

      // copy Message's body (serializing it)
      msg.body = getSerializedObject((Serializable) m.getBody());

      // set target
      if (target != null)
      {
         msg.target = target;
      }
      else
      {
         target = "";
      }

      // copy Message's headers (serializing each one)
      Map<String, Object> headers = m.getHeaders();
      CamelHeader[] hdrs = new CamelHeader[headers.size()];
      int x = 0;
      for (Entry<String, Object> entry : headers.entrySet())
      {
         hdrs[x++] = getCamelHeader(entry.getKey(), entry.getValue());
      }
      msg.headers = hdrs;

      // copy Message's attachments (serializing each one)
      Map<String, DataHandler> atts = m.getAttachments();
      CamelAttachment[] ats = new CamelAttachment[atts.size()];
      x = 0;
      for (Entry<String, DataHandler> entry : atts.entrySet())
      {
         ats[x++] = getCamelAttachment(entry.getKey(), entry.getValue());
      }
      msg.attachments = ats;

      return msg;
   }

   /**
    * Extract the content of a {@link CamelMessage} into a Camel Message object.
    * 
    * @param msg the CamelMessage to extract from.
    * @param result the Message to fill
    * @throws DdsException if an erro occurs
    */
   public static void extractCamelMessage(CamelMessage msg, Message result) throws DdsException
   {
      // extract messageId
      result.setMessageId(msg.message_id);

      // extract body
      result.setBody(extractBody(msg));

      // extract headers
      result.setHeaders(extractHeadersFromMessage(msg));

      // extract attachments
      result.setAttachments(extractAttachmentsFromMessage(msg));

   }


   private static SerializedPOJO getSerializedObject(Serializable object)
      throws DdsException
   {
      String className = object.getClass().getName();
      byte[] byteArray = serialize(object);
      return new SerializedPOJO(className, byteArray);
   }

   private static Object extractBody(CamelMessage msg)
      throws DdsException
   {
      Object ret = null;
      SerializedPOJO p = msg.body;
      if (p.serialized_object != null)
      {
         ret = deserialize(p.serialized_object);
      }
      return ret;
   }


   private static CamelHeader getCamelHeader(String key, Object obj)
      throws DdsException
   {
      SerializedPOJO serializedHeader = getSerializedObject((Serializable) obj);
      return new CamelHeader(key, serializedHeader);
   }

   private static Map<String, Object> extractHeadersFromMessage(CamelMessage msg)
      throws DdsException
   {
      Map<String, Object> headerMap = new HashMap<String, Object>();
      CamelHeader[] hdrs = msg.headers;
      String key = null;
      Object body = null;
      for (CamelHeader hdr : hdrs)
      {
         key = hdr.name;
         body = deserialize(hdr.value.serialized_object);
         headerMap.put(key, body);
      }
      return headerMap;
   }


   private static CamelAttachment getCamelAttachment(String key, DataHandler handler)
      throws DdsException
   {
      try
      {
         InputStream is = handler.getInputStream();
         long length = is.available();
         byte[] bytes = new byte[(int) length];
         is.read(bytes);
         String mimetype = handler.getContentType();
         return new CamelAttachment(key, mimetype, bytes);
      }
      catch (IOException e)
      {
         throw new DdsException(e);
      }
   }

   private static Map<String, DataHandler> extractAttachmentsFromMessage(CamelMessage msg)
   {
      Map<String, DataHandler> attMap = new HashMap<String, DataHandler>();
      CamelAttachment[] atts = msg.attachments;
      String key = null;
      DataHandler handler = null;
      for (CamelAttachment att : atts)
      {
         key = att.id;
         // Support only File attachments
         byte[] bytes = att.content;
         ByteArrayDataSource s = new ByteArrayDataSource(bytes,
               att.mime_type);
         handler = new DataHandler(s);
         attMap.put(key, handler);
      }
      return attMap;
   }


   private static Object deserialize(byte[] buf)
      throws DdsException
   {
      Object ret = null;
      try
      {
         ByteArrayInputStream b = new ByteArrayInputStream(buf);
         ObjectInputStream ostr = new ObjectInputStream(b);
         ret = ostr.readObject();
         ostr.close();
         b.close();
      }
      catch (IOException e)
      {
         throw new DdsException(e);
      }
      catch (ClassNotFoundException e)
      {
         throw new DdsException(e);
      }
      return ret;
   }

   private static byte[] serialize(Serializable obj)
      throws DdsException
   {
      byte[] data = null;
      try
      {
         ByteArrayOutputStream bos = new ByteArrayOutputStream();
         ObjectOutputStream oos = new ObjectOutputStream(bos);
         oos.writeObject(obj);
         oos.flush();
         oos.close();
         bos.close();
         data = bos.toByteArray();
      }
      catch (IOException e)
      {
         throw new DdsException(e);
      }
      return data;
   }


}
