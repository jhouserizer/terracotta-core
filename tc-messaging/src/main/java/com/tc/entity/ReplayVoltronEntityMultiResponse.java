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

import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.NetworkRecall;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.net.protocol.tcm.UnknownNameException;
import com.tc.object.ClientInstanceID;
import com.tc.object.session.SessionID;
import com.tc.object.tx.TransactionID;
import java.io.IOException;



public abstract class ReplayVoltronEntityMultiResponse implements VoltronEntityMultiResponse {

  @Override
  public boolean addReceived(TransactionID tid) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public boolean addRetired(TransactionID tid) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public boolean addResult(TransactionID tid, byte[] result) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public boolean addResultAndRetire(TransactionID tid, byte[] result) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public boolean addServerMessage(ClientInstanceID cid, byte[] message) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public boolean addServerMessage(TransactionID cid, byte[] message) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public boolean addStats(TransactionID cid, long[] timings) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void stopAdding() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public TCMessageType getMessageType() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public void hydrate() throws IOException, UnknownNameException {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public NetworkRecall send() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public MessageChannel getChannel() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public NodeID getSourceNodeID() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public NodeID getDestinationNodeID() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public SessionID getLocalSessionID() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public boolean startAdding() {
    throw new UnsupportedOperationException("Not supported yet.");
  }
  
}
