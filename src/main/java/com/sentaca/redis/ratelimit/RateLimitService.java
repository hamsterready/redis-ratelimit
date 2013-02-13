package com.sentaca.redis.ratelimit;

import java.util.Date;
import java.util.List;
import java.util.Set;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Transaction;

/**
 * Java port of <a href=
 * 'https://github.com/chriso/redback/blob/master/lib/advanced_structures/RateLimi
 * t . j s ' > r e d b a c k RateLimit</a> nodejs module.
 * 
 * 
 * @author hamster
 * 
 */
public class RateLimitService {

  private static final int DEFAULT_SUBJECT_EXPIRY = 1200;
  private static final int DEFAULT_BUCKET_SPAN = 600;
  private static final int DEFAULT_BUCKET_INTERVAL = 5;
  private String namespace;
  private String action;
  private int bucketInterval;
  private int bucketSpan;
  private int subjectExpiry;
  private JedisPool pool;
  private int bucketCount;

  /**
   * <pre>
   *  Options:
   *    `bucket_interval` - default is 5 seconds
   *    `bucket_span`     - default is 10 minutes
   *    `subject_expiry`  - default is 20 minutes
   * </pre>
   * 
   * @param namespace
   *          optional, might be null
   * @param action
   */
  public RateLimitService(JedisPool pool, String namespace, String action, int bucketSpan, int bucketInterval, int subjectExpiry) {
    this.pool = pool;
    this.namespace = namespace;
    this.action = action;
    this.bucketInterval = bucketInterval;
    this.bucketSpan = bucketSpan;
    this.bucketCount = Math.round(this.bucketSpan / this.bucketInterval);
    this.subjectExpiry = subjectExpiry;
  }

  /**
   * 
   * @param action
   * @see #RateLimitService(String, String)
   */
  public RateLimitService(JedisPool pool, String action) {
    this(pool, null, action);
  }

  /**
   * 
   * @param namespace
   * @param action
   * 
   * @see #RateLimitService(String, String, int, int, int)
   */
  public RateLimitService(JedisPool pool, String namespace, String action) {
    this(pool, namespace, action, DEFAULT_BUCKET_INTERVAL, DEFAULT_BUCKET_SPAN, DEFAULT_SUBJECT_EXPIRY);
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
    final String subjectKey = getKeyForSubject(subject);
    final Jedis j = pool.getResource();
    try {
      Transaction m = j.multi();

      // Increment the current bucket
      m.hincrBy(subjectKey, s(bucket), 1);

      // Clear the buckets ahead
      for (int i = 1; i < 6; i++) {
        m.hdel(subjectKey, s((bucket + i) % this.bucketCount));
        // m.hdel(subjectKey, s((bucket + 2) % this.bucketCount));
      }

      // Renew the key TTL
      m.expire(subjectKey, this.subjectExpiry);

      m.exec();
      // TODO handle m.exec() results
    } finally {
      pool.returnResource(j);
    }
  }

  public int count(long time, String subject, int interval) {
    final Jedis j = pool.getResource();
    try {
      final Transaction m = j.multi();
      final String subjectKey = getKeyForSubject(subject);

      int bucket = getBucket(time);
      int count = (int) Math.floor(interval / bucketInterval);
      m.hget(subjectKey, s(bucket));
      while (count-- != 0) {
        m.hget(subjectKey, s((--bucket + bucketCount) % bucketCount));
      }
      List<Object> result = m.exec();
      int sum = 0;
      for (Object object : result) {
        if (object != null) {
          sum += i(object);
        }
      }
      return sum;
    } finally {
      pool.returnResource(j);
    }

  }

  private int i(Object i) {
    return Integer.parseInt((String) i);
  }

  private String s(int i) {
    return String.valueOf(i);
  }

  private String getKeyForSubject(String subject) {
    if (namespace == null) {
      return action + ":" + subject;
    }
    return namespace + ":" + action + ":" + subject;
  }
}
