package com.sentaca.redis.ratelimit;

public class SubjectKeyGenerator {
  public String getKeyForSubject(String namespace, String action, String subject) {
    if (namespace == null) {
      return action + ":" + subject;
    }
    return namespace + ":" + action + ":" + subject;
  }
}
