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
package com.tc.objectserver.entity;

import com.tc.net.ClientID;
import com.tc.object.EntityDescriptor;
import com.tc.object.FetchID;
import java.util.List;
import java.util.Set;


/**
 * This interface tracks the connections between clients and entities, created by a fetch and destroyed by a release.
 * Note that the same client can refer to a specific entity multiple times, which each connection possessing a distinct
 * life cycle.
 */
  public interface ClientEntityStateManager {
  /**
   * Adds a reference from clientID to the entity described by entityDescriptor.
   * 
   * @param clientID The client.
   * @param entityDescriptor The entity.
   * @return 
   */
  public boolean addReference(ClientDescriptorImpl clientID, FetchID entityDescriptor);

  /**
   * Removes a reference from clientID to the entity described by entityDescriptor.NOTE:  This will assert if an attempt is made to remove a reference which was never added.
   * 
   * @param clientID The client.
   * @return 
   */
  public boolean removeReference(ClientDescriptorImpl clientID);

  /**
   * Verifies that no clients have a reference to the entity described by entityDescriptor.
   * NOTE:  This will assert if there are any remaining references.
   * 
   * @param entityDescriptor The entity.
   * @return true if there are no references
   */
  public boolean verifyNoEntityReferences(FetchID entityDescriptor);
  
  /**
   * Verifies that the client has no references in the system
   * 
   * 
   * @param client The client.
   * @return true if there are no references
   */
  public boolean verifyNoClientReferences(ClientID client);  
  
  public List<EntityDescriptor> clientDisconnectedFromEntity(ClientID client, FetchID entity);
  
  public List<FetchID> clientDisconnected(ClientID client);
  
  public Set<ClientID> clearClientReferences();
}
