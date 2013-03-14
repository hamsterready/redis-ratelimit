package com.sentaca.redis.ratelimit;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class BufferSizeService {

  private String namespace;
  private String action;
  private JedisPool pool;
  private SubjectKeyGenerator subjectKeyGenerator = new SubjectKeyGenerator();

  /**
   * 
   * @param pool
   *          -redis pool to be used
   * @param namespace
   *          - used to create redis key, might be null
   * @param action
   *          - required, used to create redis key
   */
  public BufferSizeService(JedisPool pool, String namespace, String action) {
    this.pool = pool;
    this.namespace = namespace;
    this.action = action;
  }

  /***
   * Gets the current buffer size for subject. Returns -1 if there is no buffer
   * for specific subject.
   * 
   * @param subject
   * @return
   */
  public int getCurrentBufferSize(String subject) {
    final String subjectKey = subjectKeyGenerator.getKeyForSubject(namespace, action, subject);
    final Jedis j = pool.getResource();

    try {
      String result = j.get(subjectKey);
      try {
        int buffer = Integer.valueOf(result);
        return buffer;
      } catch (Exception ex) {
        // invalid value, try to delete the key
        j.del(subjectKey);
      }
    } finally {
      pool.returnResource(j);
    }

    return -1;
  }

  /***
   * Sets the buffer size to the specific value.
   * 
   * @param subject
   * @param buffer
   */
  public void setCurrentBufferSize(String subject, int buffer) {
    final String subjectKey = subjectKeyGenerator.getKeyForSubject(namespace, action, subject);
    final Jedis j = pool.getResource();

    try {
      j.set(subjectKey, String.valueOf(buffer));
    } finally {
      pool.returnResource(j);
    }
  }

  /***
   * Increments the current buffer size by one.
   * 
   * @param subject
   * @return the value after increase
   */
  public long incrementCurrentBufferSize(String subject) {
    final String subjectKey = subjectKeyGenerator.getKeyForSubject(namespace, action, subject);
    final Jedis j = pool.getResource();

    try {
      return j.incr(subjectKey);
    } finally {
      pool.returnResource(j);
    }
  }

  /***
   * Decrements the current buffer size by one.
   * 
   * @param subject
   * @return the value after decrease
   */
  public long decrementCurrentBufferSize(String subject) {
    final String subjectKey = subjectKeyGenerator.getKeyForSubject(namespace, action, subject);
    final Jedis j = pool.getResource();

    try {
      return j.decr(subjectKey);
    } finally {
      pool.returnResource(j);
    }
  }
}
