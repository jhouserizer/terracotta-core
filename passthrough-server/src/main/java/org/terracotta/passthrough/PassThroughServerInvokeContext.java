/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terracotta.passthrough;

import org.terracotta.entity.ClientSourceId;
import org.terracotta.entity.InvokeContext;

public class PassThroughServerInvokeContext implements InvokeContext {
  private final long current;
  private final long oldest;
  private final ClientSourceId sourceId;
  private final int concurrencyKey;

  public PassThroughServerInvokeContext(ClientSourceId sourceId, int concurrencyKey, long current, long oldest) {
    this.sourceId = sourceId;
    this.current = current;
    this.oldest = oldest;
    this.concurrencyKey = concurrencyKey;
  }

  @Override
  public ClientSourceId getClientSource() {
    return sourceId;
  }

  @Override
  public long getCurrentTransactionId() {
    return current;
  }

  @Override
  public long getOldestTransactionId() {
    return oldest;
  }

  @Override
  public boolean isValidClientInformation() {
    return current > 0;
  }

  @Override
  public ClientSourceId makeClientSourceId(long l) {
    return new PassthroughClientSourceId(l);
  }

  @Override
  public int getConcurrencyKey() {
    return concurrencyKey;
  }

}
