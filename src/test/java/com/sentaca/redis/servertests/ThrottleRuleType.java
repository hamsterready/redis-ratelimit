package com.sentaca.redis.servertests;

public enum ThrottleRuleType {
  BY_AVERAGE_TPS,
  BY_NO_EMTPY_BUCKETS_TPS,
  BY_PEAK_TPS,
  BY_LATEST_BUCKET_TPS
}
