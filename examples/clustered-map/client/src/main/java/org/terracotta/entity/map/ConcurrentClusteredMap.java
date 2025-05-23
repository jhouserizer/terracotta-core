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
package org.terracotta.entity.map;


import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;
import org.terracotta.connection.entity.Entity;

public interface ConcurrentClusteredMap<K, V> extends ConcurrentMap<K, V>, Entity {
  long VERSION = 1;

  /**
   * Records the key and value classes to enable optimizations.
   *
   * @param keyClass the key class
   * @param valueClass the value class
   */
  void setTypes(Class<K> keyClass, Class<V> valueClass);
  
  Future<?> insert(K key, V value);
}
