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

import DDS.RETCODE_NO_DATA;
import DDS.RETCODE_OK;


/**
 * This class is the error handler used to check DDS returned
 * statuses and handles.
 */
public final class DdsErrorHandler
{

   /**
    * The number of possible return codes.
    */
   public static final int NR_ERROR_CODES = 13;

   /**
    * Array to hold the names for all ReturnCodes.
    */
   public static String[] retCodeName = new String[NR_ERROR_CODES];

   /**
    * Initialise the return code array.
    */
   static
   {
      retCodeName[0] = new String("DDS_RETCODE_OK");
      retCodeName[1] = new String("DDS_RETCODE_ERROR");
      retCodeName[2] = new String("DDS_RETCODE_UNSUPPORTED");
      retCodeName[3] = new String("DDS_RETCODE_BAD_PARAMETER");
      retCodeName[4] = new String("DDS_RETCODE_PRECONDITION_NOT_MET");
      retCodeName[5] = new String("DDS_RETCODE_OUT_OF_RESOURCES");
      retCodeName[6] = new String("DDS_RETCODE_NOT_ENABLED");
      retCodeName[7] = new String("DDS_RETCODE_IMMUTABLE_POLICY");
      retCodeName[8] = new String("DDS_RETCODE_INCONSISTENT_POLICY");
      retCodeName[9] = new String("DDS_RETCODE_ALREADY_DELETED");
      retCodeName[10] = new String("DDS_RETCODE_TIMEOUT");
      retCodeName[11] = new String("DDS_RETCODE_NO_DATA");
      retCodeName[12] = new String("DDS_RETCODE_ILLEGAL_OPERATION");
   }

   /*
    * Utility class with only static method.
    * Disable default constructor.
    */
   private DdsErrorHandler()
   {}

   /**
    * Returns the name of an error code.
    */
   public static String getErrorName(int status)
   {
      return retCodeName[status];
   }

   /**
    * Check the return status for errors. If there is an error, then terminate.
    */
   public static synchronized void checkStatus(int status, String info) throws DdsException
   {
      if (status != RETCODE_OK.value && status != RETCODE_NO_DATA.value)
      {
         System.out.println("status = " + retCodeName[status]);
         throw new DdsException(info + "(reason: " + getErrorMessage() + ")", status);
      }
   }

   /**
    * Check whether a valid handle has been returned. If not, then terminate.
    */
   public static synchronized void checkHandle(Object handle, String info)
      throws DdsException
   {
      if (handle == null)
      {
         throw new DdsException(info + "(reason: " + getErrorMessage() + ")");
      }
   }

   /**
    * Get an error message from DDS.ErrorInfo.
    * 
    * @return the error message
    */
   private static String getErrorMessage()
   {
      DDS.ErrorInfo error = new DDS.ErrorInfo();
      if (error.update() == DDS.RETCODE_NO_DATA.value)
      {
         return "Unkown!! Check ospl-error.log file for more info.";
      }
      else
      {
         DDS.StringHolder message = new DDS.StringHolder();
         String result = "";

         error.get_message(message);
         result += message.value + " - ";
         error.get_source_line(message);
         result += message.value;

         return result;
      }
   }

}
