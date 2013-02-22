package com.sentaca.redis.servertests;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import com.sentaca.redis.ratelimit.RateLimitService;
import com.sentaca.redis.ratelimit.Tps;

/***
 * Integration test server that simulates accepting the hits from external
 * source.
 * 
 * @author witek
 * 
 */
public class TestServer {

  private JedisPool jedisPool;
  private int allowedTPS = 35;
  private RateLimitService rateService;
  private int tpsInterval;
  private int hitNumber = 0;
  private int sleepCount = 0;
  private int rejectedHits = 0;
  private int acceptedHits = 0;

  private final int maxThreads = 10;
  private int currentThreads = 0;
  private Object lock = new Object();

  public TestServer(int allowedTps, int bucketSpan, int bucketInterval, int tpsInterval) {
    jedisPool = new JedisPool("localhost");
    Jedis j = jedisPool.getResource();
    j.flushDB();
    jedisPool.returnResource(j);

    this.allowedTPS = allowedTps;
    this.tpsInterval = tpsInterval;

    rateService = new RateLimitService(jedisPool, null, "access", bucketSpan, bucketInterval, tpsInterval);

    System.out.println("    time \t hit \t tps \t tps-no-empty-buckets \t tps-peak \t tps-latest-bucket \t empty-buckets \t hit-waiting");
  }

  public void hit() {
    Thread thread = getThread();
    if (thread != null) {
      thread.start();
    } else {
      rejectedHits++;
    }
  }

  public int getSleepCount() {
    return sleepCount;
  }

  public int getRejectedHits() {
    return rejectedHits;
  }

  public int getAcceptedHits() {
    return acceptedHits;
  }

  private boolean _hit() throws InterruptedException {
    hitNumber++;
    acceptedHits++;

    final int hit = hitNumber;

    Tps tps = rateService.tps("192.168.1.1");
    
    
    while (tps.getTps() > allowedTPS) {
      long sleep = (long) (tpsInterval * (tps.getTps() - allowedTPS) * 1000 / allowedTPS);
      printMessage(hit, tps, sleep);
      
      sleepCount++;
      Thread.sleep(sleep);

      tps = rateService.tps("192.168.1.1");
    }

    rateService.add(new Date().getTime(), "192.168.1.1");

    printMessage(hit, tps, 0);

    return true;
  }

  private synchronized Thread getThread() {

    synchronized (lock) {
      if (currentThreads < maxThreads) {
        currentThreads++;

        final Thread thread = new Thread(new Runnable() {

          @Override
          public void run() {
            try {
              _hit();
            } catch (InterruptedException e) {
              // TODO Auto-generated catch block
              e.printStackTrace();
            } finally {
              returnThread();
            }
          }
        });

        return thread;
      }

      return null;
    }
  }

  private synchronized void returnThread() {
    synchronized (lock) {
      currentThreads--;
    }
  }
  
  private void printMessage(int hit, Tps tps, long sleepTime){
    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
    DecimalFormat df = new DecimalFormat("0.0");
    Calendar date = Calendar.getInstance();
    
    boolean hitWaiting = (sleepTime == 0);
    
    System.out.println(sdf.format(date.getTime()) + "\t " + hit + "\t " + df.format(tps.getTps()) + "\t\t" + df.format(tps.getTpsNoEmptyBuckets()) + "\t\t    "
        + df.format(tps.getPeakTps()) + "                " + df.format(tps.getLatestBucketTps()) + "\t\t       "
        + df.format(tps.getCounters().getNumberOfEmptyBuckets()) + "\t     " + hitWaiting + "\t" + sleepTime + "\t" + currentThreads);
  }
}
