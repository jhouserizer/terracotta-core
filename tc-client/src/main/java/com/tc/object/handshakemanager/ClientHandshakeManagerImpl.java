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
package com.tc.object.handshakemanager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tc.net.ClientID;
import com.tc.object.msg.ClientHandshakeAckMessage;
import com.tc.object.msg.ClientHandshakeMessage;
import com.tc.object.msg.ClientHandshakeMessageFactory;
import com.tc.util.Assert;
import com.tc.util.Util;

import java.util.Objects;

/**
 * This class has been changed to be heavily synchronized. This is in attempt to 
 * address a rare bug where handshake is not initiated properly because the state 
 * of the handshake manager is not properly protected.  The problem is that callbacks 
 * are made from within synchronized blocks as well, also to insure state is preserved 
 * during the entire stretch of the call.  This can be dangerous as the callback sites may 
 * lead to synchronization of their own, making this code prone to deadlocks.
 * 
 * TODO:  Constrain callback code to include as little synchronization as is safe.
 * @author 
 */
public class ClientHandshakeManagerImpl implements ClientHandshakeManager {
  private enum State {
    PAUSED, STARTING, RUNNING
  }

  private static final Logger LOGGER = LoggerFactory.getLogger(ClientHandshakeManagerImpl.class);

  private final ClientHandshakeCallback callBacks;
  private final ClientHandshakeMessageFactory chmf;
  private final Logger logger;
  private final String clientVersion;
  private final String clientRevision;
  
  private final String uuid;
  private final String name;

  private State state;
  private volatile boolean disconnected;
  private volatile boolean wasConnected;
  private volatile boolean isShutdown = false;

  public ClientHandshakeManagerImpl(Logger logger, ClientHandshakeMessageFactory chmf,
                                    String uuid, String name, String clientVersion, String clientRevision,
                                    ClientHandshakeCallback entities) {
    this.logger = logger;
    this.chmf = chmf;
    this.uuid = uuid;
    this.name = name;
    this.clientVersion = clientVersion;
    this.clientRevision = clientRevision;
    this.callBacks = entities;
    this.state = State.PAUSED;
    this.disconnected = true;
    this.wasConnected = false;
    pauseCallbacks();
  }

  @Override
  public  void shutdown() {
    synchronized (this) {
      if (!isShutdown) {
        isShutdown = true;
        notifyAll();
      } else {
        return;
      }
    }
    shutdownCallbacks();
  }

  @Override
  public synchronized boolean isShutdown() {
    return isShutdown;
  }

  private boolean checkShutdown() {
    if (isShutdown) {
      this.logger.info("Drop handshaking due to client shutting down...");
    }
    return isShutdown;
  }

  private void initiateHandshake() {
    this.logger.debug("Initiating handshake...");
    ClientHandshakeMessage handshakeMessage;

    changeToStarting();
    handshakeMessage = this.chmf.newClientHandshakeMessage(this.uuid, this.name, this.clientVersion, this.clientRevision, this.wasConnected);
    if (handshakeMessage != null) {
      notifyCallbackOnHandshake(handshakeMessage);

      this.logger.debug("Sending handshake message");
      if (handshakeMessage.send() == null) {
        if (handshakeMessage.getChannel().isConnected()) {
          LOGGER.error("handshake not sent but channel is connected", new Exception("FATAL HANDSHAKE ERROR"));
        } else {
          LOGGER.info("handshake failed. channel not connected");
        }
      }
    }
  }
  
  @Override
  public void fireNodeError() {
    final String msg = "Reconnection was rejected from server. This client will never be able to join the cluster again.";
    logger.error(msg);
    LOGGER.error(msg);
  }

  @Override
  public synchronized void disconnected() {
    // We ignore the disconnected call if we are shutting down.
    if (!checkShutdown()) {
      boolean wasRunning = changeToPaused();
        
      if (wasRunning) {
      // A thread might be waiting for us to change whether or not we are disconnected.
        notifyAll();
        pauseCallbacks();
      }
    }
  }

  @Override
  public synchronized void connected() {
    this.logger.debug("Connected: Unpausing from " + this.state);
    if (this.state != State.PAUSED) {
      this.logger.warn("Ignoring unpause while " + this.state);
    } else if (!checkShutdown()) {
      // drop handshaking if shutting down
      initiateHandshake();
    }
  }

  @Override
  public void acknowledgeHandshake(ClientHandshakeAckMessage handshakeAck) {
    acknowledgeHandshake(handshakeAck.getThisNodeId(), handshakeAck.getAllNodes(),
        handshakeAck.getServerVersion());
  }

  protected synchronized void acknowledgeHandshake(ClientID thisNodeId, ClientID[] clusterMembers, String serverVersion) {
    this.logger.debug("Received Handshake ack");
    if (this.state != State.STARTING) {
      this.logger.warn("Ignoring handshake acknowledgement while " + this.state);
    } else {
      checkClientServerVersionCompatibility(serverVersion);

      changeToRunning();
      notifyAll();
      unpauseCallbacks();
    }
  }

  protected void checkClientServerVersionCompatibility(String serverVersion) {
    if (!Objects.equals(clientVersion, serverVersion)) {
      String message = String.format("Client version %s is different from server version %s.",
              clientVersion, serverVersion);
      logger.info(message);
    }
  }

  private void shutdownCallbacks() {
    // Now that the handshake manager has concluded that it is entering into a shutdown state, anyone else wishing to use it
    // needs to be notified that they cannot.
    this.callBacks.shutdown();
  }

  private void pauseCallbacks() {
    this.callBacks.pause();
  }

  private void notifyCallbackOnHandshake(ClientHandshakeMessage handshakeMessage) {
    this.callBacks.initializeHandshake(handshakeMessage);
  }

  private void unpauseCallbacks() {
    this.callBacks.unpause();
  }

  @Override
  public synchronized void waitForHandshake() {
    boolean isInterrupted = false;
    try {
      while (this.disconnected && !this.isShutdown) {
        try {
          wait();
        } catch (InterruptedException e) {
          this.logger.error("Interrupted while waiting for handshake");
          isInterrupted = true;
        }
      }
    } finally {
      Util.selfInterruptIfNeeded(isInterrupted);
    }
  }

  // returns true if PAUSED else return false if already PAUSED
  private boolean changeToPaused() {
    final State old = this.state;
    boolean didChangeToPaused = false;
    if (old != State.PAUSED) {
      this.state = State.PAUSED;

      this.logger.debug("Disconnected: Pausing from " + old + ". Disconnect count: " + this.disconnected);

      if (old == State.RUNNING) {
        didChangeToPaused = true;
        this.disconnected = true;
      }
    }
    return didChangeToPaused;
  }

  private void changeToStarting() {
    Assert.assertEquals(state, State.PAUSED);
    state = State.STARTING;
  }

  private void changeToRunning() {
    Assert.assertEquals(state, State.STARTING);
    state = State.RUNNING;

    this.disconnected = false;
    this.wasConnected = true;
  }
}
