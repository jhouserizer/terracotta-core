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
package com.tc.async.impl;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import org.terracotta.tripwire.TripwireFactory;

/**
 */
public class MonitoringEventCreator<EC> implements EventCreator<EC> {
  private final String name;
  private final EventCreator<EC> next;
  private static final ThreadLocal<PipelineMonitor> CURRENT = new ThreadLocal<>();
  private final LongAdder queueTime = new LongAdder();
  private final LongAdder runTime = new LongAdder();
  private final LongAdder queued = new LongAdder();
  private static PipelineMonitoringConsumer pipelineConsumer;

  public MonitoringEventCreator(String name, EventCreator<EC> next) {
    this.name = name;
    this.next = next;
  }
  
  public static void setPipelineMonitor(PipelineMonitoringConsumer consumer) {
    pipelineConsumer = consumer;
  }
  
  public static void finish() {
    if (pipelineConsumer != null) {
      PipelineMonitor mon = CURRENT.get();
      if (mon != null) {
        mon.close();
        CURRENT.remove();
        pipelineConsumer.record(mon);
      }
    }
  }
  
  public static void start() {
    if (pipelineConsumer != null) {
      CURRENT.set(new PipelineMonitor());
    }    
  }
  
  @Override
  public Event createEvent(EC event) {
    MonitorStats stats = new MonitorStats(name, event.toString());
    PipelineMonitor running = CURRENT.get();
    if (running != null) {
      running.action(name, PipelineMonitor.Type.ENQUEUE, event);
    }
    Event nextEvent = next.createEvent(event);
    if (nextEvent != null) {
      return () -> {
        if (running != null) {
          CURRENT.set(running.action(name, PipelineMonitor.Type.RUN, event));
        }
        stats.run();
        nextEvent.call();
        stats.end();
        addStats(stats);
        if (running != null) {
          CURRENT.remove();
          running.action(name, PipelineMonitor.Type.END, event);
        }
      };
    } else {
      if (running != null) {
        running.action(name, PipelineMonitor.Type.RUN, event);
        running.action(name, PipelineMonitor.Type.END, event);
      }
      return null;
    }
  }

  private void addStats(MonitorStats stats) {
    runTime.add(stats.runTime());
    queueTime.add(stats.queueTime());
    queued.increment();
  }

  public Map<String, ?> getState() {
    Map<String, Object> stats = new LinkedHashMap<>();
    stats.put("queueTime", queueTime);
    stats.put("runTime", runTime);
    stats.put("count", queued);
    long count = queued.sum();
    if (count > 0) {
      stats.put("average queue time", TimeUnit.NANOSECONDS.toNanos(queueTime.sum()/count));
      stats.put("average run time", TimeUnit.NANOSECONDS.toNanos(runTime.sum()/count));
    }
    return stats;
  }

  private static class MonitorStats<EC> {
    private long queue = 0;
    private long run = 0;
    private long end = 0;
    private final org.terracotta.tripwire.Event event;
    
    public MonitorStats(String name, String debugging) {
      queue();
      event = TripwireFactory.createStageEvent(name, debugging);
    }
    
    void run() {
      run = System.nanoTime();
      event.begin();
    }
    
    final void queue() {
      queue = System.nanoTime();
    }
    
    void end() {
      end = System.nanoTime();
      event.end();
      event.commit();
    }
    
    long queueTime() {
      return run - queue;
    }
    
    long runTime() {
      return end - run;
    }
  }
}
