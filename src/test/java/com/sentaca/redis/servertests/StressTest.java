package com.sentaca.redis.servertests;

import junit.framework.TestCase;

import org.junit.Ignore;

/***
 * This test i susefull to see how the redis-ratelimit work. It requires a redis
 * server on the local machine running.
 * 
 * @author witek
 * 
 */
@Ignore("Integration test to see how redis-ratelimit works")
public class StressTest extends TestCase {

  public void test() {
    int allowedTps = 35;
    int bucketSpan = 100;
    int bucketInterval = 3;
    int tpsInterval = 10;
    TestServer server = new TestServer(allowedTps, bucketSpan, bucketInterval, tpsInterval, ThrottleRuleType.BY_AVERAGE_TPS);

    /* simulation time in seconds */
    double simulationTime = 15;

    /* Prepare the traffic generators */
    TrafficGenerator tg = new TrafficGenerator(30, server);
    TrafficGenerator tg2 = new TrafficGenerator(10, server);
    TrafficGenerator tg3 = new BurstTrafficGenerator(40, server); // 2 seconds
                                                                  // of idle, 1
                                                                  // second of
                                                                  // bursting
    /* start hiting the mock/test server */
    tg.start();
    tg2.start();
    tg3.start();

    try {
      Thread.sleep((int) (1000 * simulationTime));
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    /* stop hiting the server */
    tg.stop();
    tg2.stop();
    tg3.stop();

    System.out.println("Rejected hits (no threads available): " + server.getRejectedHits());
    System.out.println("Accepted hits                       : " + server.getAcceptedHits());
    System.out.println("Accepted hits average tps           : " + server.getAcceptedHits() / simulationTime);
    System.out.println("Accepted/rejected hits average tps  : " + (server.getAcceptedHits() + server.getRejectedHits()) / simulationTime);
  }
}
