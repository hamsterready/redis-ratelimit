package com.sentaca.redis.servertests;

/***
 * Generates the fragment-burst traffic to the server. The traffic is generated
 * by repeating the following phases: <li>two seconds of no traffic</li> <li>one
 * second of constant traffic with the specific TPS rate</li>
 * 
 * <p>
 * 
 * @author witek
 * 
 */
public class BurstTrafficGenerator extends TrafficGenerator {

  private State state = State.WAITING;
  private long burstingStartTime;

  private enum State {
    WAITING,
    BURSTING
  }

  public BurstTrafficGenerator(double tps, TestServer server) {
    super(tps, server);
  }

  public void start() {
    Thread thread = new Thread(new Runnable() {

      @Override
      public void run() {
        try {
          while (!stopRequested) {
            switch (state) {
            case WAITING:
              // sleep for 2 seconds
              Thread.sleep(2000);
              state = State.BURSTING;
              burstingStartTime = System.currentTimeMillis();
              break;
            case BURSTING:
              // burst for 1 second
              long millis = (long) (1000 * random.expRandom(tps));
              Thread.sleep(millis);
              server.hit();
              if (System.currentTimeMillis() - burstingStartTime >= 1000) {
                state = State.WAITING;
              }
              break;
            }
          }
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    });
    thread.start();
  }
}
