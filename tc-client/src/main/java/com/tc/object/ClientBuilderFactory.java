/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.object;

import com.tc.util.ManagedServiceLoader;

import java.util.Properties;

public interface ClientBuilderFactory {

  static <T> T get(Class<T> type) {

    T finalFactory = null;

    for (T factory : ManagedServiceLoader.loadServices(type, ClientBuilderFactory.class.getClassLoader())) {
      if (finalFactory == null) {
        finalFactory = factory;
      } else {
        throw new RuntimeException("Found multiple implementations of " + type.getName());
      }
    }
    
    return finalFactory;
  }

  ClientBuilder create(Properties connectionProperties);
}
