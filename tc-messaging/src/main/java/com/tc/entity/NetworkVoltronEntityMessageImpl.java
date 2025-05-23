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
package com.tc.entity;

import com.tc.bytes.TCByteBuffer;
import com.tc.bytes.TCByteBufferFactory;
import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.ClientID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCActionNetworkMessage;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.EntityDescriptor;
import com.tc.object.EntityID;
import com.tc.object.msg.DSOMessageBase;
import com.tc.object.session.SessionID;
import com.tc.object.tx.TransactionID;
import com.tc.util.Assert;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Set;

import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.EntityResponse;
import org.terracotta.entity.MessageCodec;
import org.terracotta.entity.MessageCodecException;


public class NetworkVoltronEntityMessageImpl extends DSOMessageBase implements NetworkVoltronEntityMessage {
  private ClientID clientID;
  private TransactionID transactionID;
  private EntityID eid;  // only used locally for exception handling on invokes DO NOT PUT OVER THE NETWORK
  private EntityDescriptor entityDescriptor;
  private Type type;
  private boolean requiresReplication;
  private TCByteBuffer extendedData;
  private TransactionID oldestTransactionPending;
  private MessageCodecSupplier supplier;
  private EntityMessage message;
  private Set<VoltronEntityMessage.Acks> requestedAcks;

  @Override
  public ClientID getSource() {
    Assert.assertNotNull(this.clientID);
    return this.clientID;
  }
  @Override
  public TransactionID getTransactionID() {
    Assert.assertNotNull(this.transactionID);
    return this.transactionID;
  }

  @Override
  public EntityID getEntityID() {
    return eid;  // only used locally for exception handling on invokes
  }

  @Override
  public EntityDescriptor getEntityDescriptor() {
    Assert.assertNotNull(this.entityDescriptor);
    return this.entityDescriptor;
  }

  @Override
  public boolean doesRequireReplication() {
    return this.requiresReplication;
  }
  
  @Override
  public boolean doesRequestReceived() {
    return this.requestedAcks.contains(Acks.RECEIVED);
  }
  
  @Override
  public boolean doesRequestRetired() {
    return this.requestedAcks.contains(Acks.RETIRED);
  }
  
  @Override
  public Type getVoltronType() {
    Assert.assertNotNull(this.type);
    return this.type;
  }
  
  @Override
  public TCByteBuffer getExtendedData() {
    Assert.assertNotNull(this.extendedData);
    return this.extendedData.duplicate();
  }

  @Override
  public TransactionID getOldestTransactionOnClient() {
    return this.oldestTransactionPending;
  }

  @Override
  public Set<Acks> getRequestedAcks() {
    return requestedAcks;
  }
  
  @Override
  public void setContents(ClientID clientID, TransactionID transactionID, EntityID eid, EntityDescriptor entityDescriptor, 
          Type type, boolean requiresReplication, TCByteBuffer extendedData, TransactionID oldestTransactionPending, Set<VoltronEntityMessage.Acks> acks) {
    // Make sure that this wasn't called twice.
    Assert.assertNull(this.type);
    Assert.assertNotNull(clientID);
    Assert.assertNotNull(transactionID);
    Assert.assertNotNull(eid);
    Assert.assertNotNull(entityDescriptor);
    Assert.assertNotNull(type);
    Assert.assertNotNull(extendedData);
    Assert.assertNotNull(oldestTransactionPending);
    Assert.assertNotNull(acks);
    if (type == Type.INVOKE_ACTION) {
      Assert.assertTrue(entityDescriptor.isIndexed());
    } else {
      Assert.assertFalse(entityDescriptor.isIndexed());
    }

    this.clientID = clientID;
    this.transactionID = transactionID;
    this.eid = eid;
    this.entityDescriptor = entityDescriptor;
    this.type = type;
    this.requiresReplication = requiresReplication;
    this.extendedData = extendedData.asReadOnlyBuffer();
    this.oldestTransactionPending = oldestTransactionPending;
    this.requestedAcks = acks;
  }

  @Override
  public void setMessageCodecSupplier(MessageCodecSupplier supplier) {
    this.supplier = supplier;
  }

  public NetworkVoltronEntityMessageImpl(SessionID sessionID, MessageMonitor monitor, TCByteBufferOutputStream out, MessageChannel channel, TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public NetworkVoltronEntityMessageImpl(SessionID sessionID, MessageMonitor monitor, MessageChannel channel, TCMessageHeader header, TCByteBufferInputStream data) {
    super(sessionID, monitor, channel, header, data);
  }

  @Override
  protected void dehydrateValues() {
    TCByteBufferOutputStream outputStream = getOutputStream();
    // We don't want to use the NVpair stuff:  it is horrendously complicated, doesn't work well with all types, and doesn't buy us anything.
    putNVPair((byte)0, (byte)0);
    
    this.clientID.serializeTo(outputStream);
    
    outputStream.writeLong(this.transactionID.toLong());
    
    this.entityDescriptor.serializeTo(outputStream);
    
    outputStream.writeInt(type.ordinal());
    
    outputStream.writeInt(extendedData.remaining());
    outputStream.write(extendedData.duplicate());
    
    outputStream.writeBoolean(requiresReplication);
    
    outputStream.writeLong(this.oldestTransactionPending.toLong());
    
    outputStream.writeByte(requestedAcks.size());
    for (VoltronEntityMessage.Acks ack : this.requestedAcks) {
      outputStream.writeByte(ack.ordinal());
    }
  }
  
  @Override
  protected boolean hydrateValue(byte name) throws IOException {
    Assert.assertTrue(0 == name);
    Assert.assertTrue(null == this.clientID);
    // Read our dummy byte.
    getByteValue();
    
    this.clientID = ClientID.readFrom(getInputStream());
    this.transactionID = new TransactionID(getLongValue());
    this.entityDescriptor = EntityDescriptor.readFrom(getInputStream());
    this.type = Type.values()[getIntValue()];
    this.extendedData = getByteBuffer();
    this.requiresReplication = getBooleanValue();
    this.oldestTransactionPending = new TransactionID(getLongValue());
    
    int ac = getByteValue();
    if (ac == 0) {
      this.requestedAcks = EnumSet.noneOf(Acks.class);
    } else {
      Acks first = Acks.values()[getByteValue()];
      if (ac > 1) {
        Acks[] rest = new Acks[ac - 1];
        for (int x=0;x < ac - 1;x++) {
          rest[x] = Acks.values()[getByteValue()];
        }
        this.requestedAcks = EnumSet.of(first, rest);
      } else {
        this.requestedAcks = EnumSet.of(first);
      }
    }

    try {
      if (this.type == Type.INVOKE_ACTION) {
        MessageCodec<? extends EntityMessage, ? extends EntityResponse> codec = supplier.getMessageCodec(this.entityDescriptor);
        this.message = codec.decodeMessage(TCByteBufferFactory.unwrap(extendedData));
      }
    } catch (MessageCodecException exception) {
/*  swallow it - this is an optimzation which does not handle the failure case.  
    If this invocation does not succeed, a later stage will try and decode the message 
    again.  When that fails the exception is handled and sent back to the client.
      */
    }
    
    return true;
  }

  @Override
  public EntityMessage getEntityMessage() {
    return this.message;
  }

  @Override
  public String toString() {
    return "NetworkVoltronEntityMessageImpl{" + "clientID=" + clientID + ", transactionID=" + transactionID + ", eid=" + eid + '}';
  }
  // for tests
  TCActionNetworkMessage getNetworkMessage() {
    return convertToNetworkMessage();
  }
}
