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

import com.tc.bytes.TCByteBuffer;
import com.tc.bytes.TCByteBufferFactory;
import org.terracotta.entity.EntityMessage;

import com.tc.entity.VoltronEntityMessage;
import com.tc.net.ClientID;
import com.tc.object.EntityDescriptor;
import com.tc.object.tx.TransactionID;


/**
 *  This message is use to flush the deferred entity queue.  It is placed in 
 *  at the end of and exclusive entity message execution to flush the deferred queue.  
 *  It runs the entire pipeline but is never scheduled on the request processor by 
 *  ManagedEntityImpl.
 */
public class LocalPipelineFlushMessage implements VoltronEntityMessage, Runnable {
  private final EntityDescriptor  descriptor;
  private final boolean forDestroy;
  private final Runnable action;

  public LocalPipelineFlushMessage(EntityDescriptor descriptor, boolean forDestroy) {
    this.descriptor = descriptor;
    this.forDestroy = forDestroy;
    action = null;
  }
  
  public LocalPipelineFlushMessage(EntityDescriptor descriptor, Runnable action) {
    this.descriptor = descriptor;
    this.forDestroy = false;
    this.action = action;
  }
  
  @Override
  public void run() {
    if (action != null) {
      action.run();
    }
  }

  @Override
  public ClientID getSource() {
    return ClientID.NULL_ID;
  }

  @Override
  public TransactionID getTransactionID() {
    return TransactionID.NULL_ID;
  }

  @Override
  public EntityDescriptor getEntityDescriptor() {
    return descriptor;
  }

  @Override
  public boolean doesRequireReplication() {
    return false;
  }

  @Override
  public boolean doesRequestReceived() {
    return false;
  }

  @Override
  public boolean doesRequestRetired() {
    return false;
  }
  
  @Override
  public Type getVoltronType() {
    return (forDestroy) ? Type.LOCAL_ENTITY_GC : Type.LOCAL_PIPELINE_FLUSH;
  }

  @Override
  public TCByteBuffer getExtendedData() {
    return TCByteBufferFactory.getInstance(0);
  }

  @Override
  public TransactionID getOldestTransactionOnClient() {
    return TransactionID.NULL_ID;
  }

  @Override
  public EntityMessage getEntityMessage() {
    // No instance for this type.
    return null;
  }
}
