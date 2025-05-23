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
package com.tc.object.msg;

import com.tc.entity.ResendVoltronEntityMessage;
import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.TCSocketAddress;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCActionNetworkMessage;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.session.SessionID;
import com.tc.util.Assert;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.Comparator;


public class ClientHandshakeMessageImpl extends DSOMessageBase implements ClientHandshakeMessage {
  private static final byte   RECONNECT        = 1;
  private static final byte   CLIENT_VERSION           = 2;
  private static final byte   UNUSED_2        = 3;
  private static final byte   LOCAL_TIME_MILLS         = 4;
  private static final byte   RECONNECT_REFERENCES     = 5;
  private static final byte   RESEND_MESSAGES          = 6;
  private static final byte   CLIENT_PID               = 7;
  private static final byte   CLIENT_UUID              = 8;
  private static final byte   CLIENT_NAME              = 9;
  private static final byte   CLIENT_ADDRESS           = 10;
  private static final byte   CLIENT_REVISION          = 11;

  private long                currentLocalTimeMills    = System.currentTimeMillis();
  private String              uuid                     = com.tc.util.UUID.NULL_ID.toString();
  private String              name                     = "";
  private String              clientVersion            = "";
  private String              clientRevision           = "";
  private String              clientAddress            = ""; 
  private int                 pid                      = -1;
  private boolean             reconnect                = false;
  private final Set<ClientEntityReferenceContext> reconnectReferences = new HashSet<ClientEntityReferenceContext>();
  private final Set<ResendVoltronEntityMessage> resendMessages = new TreeSet<ResendVoltronEntityMessage>(new Comparator<ResendVoltronEntityMessage>() {
    @Override
    public int compare(ResendVoltronEntityMessage first, ResendVoltronEntityMessage second) {
      return first.getTransactionID().compareTo(second.getTransactionID());
    }
  });

  public ClientHandshakeMessageImpl(SessionID sessionID, MessageMonitor monitor, TCByteBufferOutputStream out,
                                    MessageChannel channel, TCMessageType messageType) {
    super(sessionID, monitor, out, channel, messageType);
    // if this is on the server, it will be replaced by the dehydrate
    clientAddress = TCSocketAddress.getStringForm(channel.getLocalAddress());
  }

  public ClientHandshakeMessageImpl(SessionID sessionID, MessageMonitor monitor, MessageChannel channel,
                                    TCMessageHeader header, TCByteBufferInputStream data) {
    super(sessionID, monitor, channel, header, data);
    // if this is on the server, it will be replaced by the dehydrate
    clientAddress = TCSocketAddress.getStringForm(channel.getLocalAddress());
  }

  @Override
  public void setReconnect(boolean isReconnect) {
    this.reconnect = isReconnect;
  }

  @Override
  public boolean isReconnect() {
    return this.reconnect;
  }

  @Override
  public String getClientVersion() {
    return this.clientVersion;
  }

  @Override
  public String getClientRevision() {
    return this.clientRevision;
  }

  @Override
  public void setClientPID(int pid) {
    this.pid = pid;
  }

  @Override
  public int getClientPID() {
    return pid;
  }

  @Override
  public void setUUID(String uuid) {
    this.uuid = uuid;
  }

  @Override
  public String getUUID() {
    return this.uuid;
  }

  @Override
  public String getName() {
    return this.name;
  }

  @Override
  public void setName(String name) {
    this.name = name;
  }

  @Override
  public void setClientVersion(String version) {
    this.clientVersion = version;
  }

  @Override
  public void setClientRevision(String revision) {
    this.clientRevision = revision;
  }

  @Override
  public long getLocalTimeMills() {
    return this.currentLocalTimeMills;
  }

  @Override
  public String getClientAddress() {
    return this.clientAddress;
  }

  @Override
  protected void dehydrateValues() {
    putNVPair(RECONNECT, reconnect);  // unused but keep for compatibility
    putNVPair(UNUSED_2, false);  // unused but keep for compatibility
    putNVPair(CLIENT_UUID, this.uuid);
    putNVPair(CLIENT_NAME, this.name);
    putNVPair(CLIENT_VERSION, this.clientVersion);
    putNVPair(CLIENT_PID, this.pid);
    putNVPair(LOCAL_TIME_MILLS, this.currentLocalTimeMills);
    for (final ClientEntityReferenceContext referenceContext : this.reconnectReferences) {
      putNVPair(RECONNECT_REFERENCES, referenceContext);
    }
    for (final ResendVoltronEntityMessage resendMessage : this.resendMessages) {
      putNVPair(RESEND_MESSAGES, resendMessage);
    }
    putNVPair(CLIENT_ADDRESS, this.clientAddress);
    putNVPair(CLIENT_REVISION, this.clientRevision);
  }

  @Override
  protected boolean hydrateValue(byte name) throws IOException {
    switch (name) {
      case RECONNECT:
        this.reconnect = getBooleanValue();
        return true;
      case UNUSED_2:
        getBooleanValue();  // unused but keep for compatibility
        return true;
      case CLIENT_VERSION:
        this.clientVersion = getStringValue();
        return true;
      case LOCAL_TIME_MILLS:
        this.currentLocalTimeMills = getLongValue();
        return true;
      case RECONNECT_REFERENCES:
        this.reconnectReferences.add(getObject(new ClientEntityReferenceContext()));
        return true;
      case RESEND_MESSAGES:
        this.resendMessages.add(getObject(new ResendVoltronEntityMessage()));
        return true;
      case CLIENT_PID:
        this.pid = getIntValue();
        return true;
      case CLIENT_UUID:
        this.uuid = getStringValue();
        return true;
      case CLIENT_NAME:
        this.name = getStringValue();
        return true;
      case CLIENT_ADDRESS:
        this.clientAddress = getStringValue();
        return true;
      case CLIENT_REVISION:
        this.clientRevision = getStringValue();
        return true;
      default:
        return false;
    }
  }

  @Override
  public void addReconnectReference(ClientEntityReferenceContext context) {
    boolean newAddition = this.reconnectReferences.add(context);
    Assert.assertTrue(newAddition);
  }

  @Override
  public Collection<ClientEntityReferenceContext> getReconnectReferences() {
    return this.reconnectReferences;
  }

  @Override
  public void addResendMessage(ResendVoltronEntityMessage message) {
    boolean newAddition = this.resendMessages.add(message);
    Assert.assertTrue(newAddition);
  }

  @Override
  public Collection<ResendVoltronEntityMessage> getResendMessages() {
    return this.resendMessages;
  }
    
  // for tests
  TCActionNetworkMessage getNetworkMessage() {
    return convertToNetworkMessage();
  }
}
