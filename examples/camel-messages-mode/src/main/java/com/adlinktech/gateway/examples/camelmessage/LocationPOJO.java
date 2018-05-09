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
package com.adlinktech.gateway.examples.camelmessage;

import java.io.Serializable;

// Example of Serializable class that can be used as exchanged data through DDS.
public class LocationPOJO implements Serializable {

    //The name of a City
    private String city;

    //The zip code of the city defined above
    private int zip;

    public LocationPOJO(String c, int z) {
        city = c;
        zip = z;
    }

    public String getCity() {
        return city;
    }

    public int getZip() {
        return zip;
    }


}
