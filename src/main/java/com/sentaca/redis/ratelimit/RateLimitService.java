package com.sentaca.redis.ratelimit;

import java.util.Date;
import java.util.List;
import java.util.Set;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Transaction;

/**
 * Java port of <a href= 'http://bit.ly/YaGa0m'>redback RateLimit</a> nodejs
 * module with some minor modifications.
 * 
 * @author hamster
 * 
 */
public class RateLimitService {

  private static final int DEFAULT_BUCKET_SPAN = 300; // in seconds
  private static final int DEFAULT_BUCKET_INTERVAL = 1; // in seconds
  private String namespace;
  private String action;
  private int bucketInterval;
  private int bucketSpan;
  private int subjectExpiry;
  private JedisPool pool;
  private int bucketCount;
  private int tpsInterval;
  private int bucketsUsedForTps;
  private int bucketToClear;

  private SubjectKeyGenerator subjectKeyGenerator = new SubjectKeyGenerator();

  /**
   * 
   * @param pool
   *          -redis pool to be used
   * @param namespace
   *          - used to create redis key, might be null
   * @param action
   *          - required, used to create redis key
   * @param bucketSpan
   *          {@link #DEFAULT_BUCKET_SPAN}
   * @param bucketInterval
   *          {@value #DEFAULT_BUCKET_INTERVAL}
   * @param tpsInterval
   *          - time window (span) in seconds where the TPS value is calculated
   */
  public RateLimitService(JedisPool pool, String namespace, String action, int bucketSpan, int bucketInterval, int tpsInterval) {
    this.pool = pool;
    this.namespace = namespace;
    this.action = action;
    this.bucketInterval = bucketInterval;
    this.bucketSpan = bucketSpan;
    this.tpsInterval = tpsInterval;
    this.bucketCount = Math.round(this.bucketSpan / this.bucketInterval);
    this.subjectExpiry = bucketSpan - 1;
    this.bucketsUsedForTps = Math.round(tpsInterval / bucketInterval);
    this.bucketToClear = bucketCount - bucketsUsedForTps;

  }

  /**
   * @see #RateLimitService(JedisPool, String, String, int, int, int)
   * @param pool
   * @param action
   * @param tpsInterval
   *          - time window (span) in seconds where the TPS value is calculated
   */
  public RateLimitService(JedisPool pool, String action, int tpsInterval) {
    this(pool, null, action, tpsInterval);
  }

  /**
   * @see #RateLimitService(JedisPool, String, String, int, int, int)
   * @param pool
   * @param namespace
   * @param action
   * @param tpsInterval
   *          - time window (span) in seconds where the TPS value is calculated
   */
  public RateLimitService(JedisPool pool, String namespace, String action, int tpsInterval) {
    this(pool, namespace, action, DEFAULT_BUCKET_SPAN, DEFAULT_BUCKET_INTERVAL, tpsInterval);
  }

  public void addAll(long time, Set<String> subjects) {
    for (String subject : subjects) {
      add(time, subject);
    }
  }

  public int getBucket(long time) {
    return (int) (Math.floor((time / 1000) % bucketSpan) / bucketInterval);
  }

  public void add(long time, String subject) {
    final int bucket = getBucket(time);
    final String subjectKey = subjectKeyGenerator.getKeyForSubject(namespace, action, subject);
    final Jedis j = pool.getResource();
    try {
      Transaction m = j.multi();

      // Increment the current bucket
      m.hincrBy(subjectKey, s(bucket), 1);

      // Clear the buckets ahead
      for (int i = 1; i < bucketToClear + 1; i++) {
        m.hdel(subjectKey, s((bucket + i) % this.bucketCount));
      }

      // Renew the key TTL
      m.expire(subjectKey, this.subjectExpiry);

      m.exec();
      // TODO handle m.exec() results
    } finally {
      pool.returnResource(j);
    }
  }

  public CountResult count(long time, String subject) {
    final Jedis j = pool.getResource();
    try {
      final Transaction m = j.multi();
      final String subjectKey = subjectKeyGenerator.getKeyForSubject(namespace, action, subject);

      final int currentBucket = getBucket(time);
      int bucket = currentBucket;
      int count = (int) Math.floor(tpsInterval / bucketInterval);
      while (count-- != 0) {
        m.hget(subjectKey, s((bucket + bucketCount) % bucketCount));
        bucket--;
      }

      // Clear the buckets ahead
      for (int i = 1; i < bucketToClear + 1; i++) {
        m.hdel(subjectKey, s((currentBucket + i) % this.bucketCount));
      }
      List<Object> result = m.exec();
      int sum = 0;
      int numberOfEmtpyBuckets = 0;
      int maxCount = 0;
      int latestBucketCount = 0;
      int i = 0;
      for (Object object : result) {

        if (i++ < Math.floor(tpsInterval / bucketInterval)) {
          if (object != null) {
            int c = i(object);
            sum += c;
            if (c == 0) {
              numberOfEmtpyBuckets++;
            }
            if (c > maxCount) {
              maxCount = c;
            }
            if (i == 1) {
              latestBucketCount = c;
            }
          } else {
            numberOfEmtpyBuckets++;
          }
        }
      }
      return new CountResult(sum, numberOfEmtpyBuckets, maxCount, latestBucketCount);
    } finally {
      pool.returnResource(j);
    }
  }

  public Tps tps(String subject) {
    CountResult cr = count(new Date().getTime(), subject);
    double tps = (double) cr.getCount() / (double) tpsInterval;
    double tpsNoEmptyBuckets = 0;

    double factor = (double) (tpsInterval - cr.getNumberOfEmptyBuckets() * this.bucketInterval);
    if (factor != 0) {
      tpsNoEmptyBuckets = cr.getCount() / factor;
    }

    double peakTps = ((double) cr.getPeakCount()) / ((double) this.bucketInterval);
    double latestBucketTps = ((double) cr.getLatestBucketCount()) / ((double) this.bucketInterval);

    return new Tps(tps, tpsNoEmptyBuckets, peakTps, latestBucketTps, cr);
  }

  private int i(Object i) {
    return Integer.parseInt((String) i);
  }

  private String s(int i) {
    return String.valueOf(i);
  }
}
