package com.sentaca.redis.ratelimit;

public class CountResult {
  private int count;
  private int peakCount;
  private int numberOfEmptyBuckets;
  private int latestBucketCount;

  /***
   * Constructs the count result across the buckets.
   * 
   * @param count
   *          sum of counts for all buckets
   * @param numberOfEmptyBuckets
   *          number of buckets where count is zero
   * @param peakCount
   *          max count found in the buckets
   * @param latestBucketCount
   *          the count in the latest bucket
   */
  public CountResult(int count, int numberOfEmptyBuckets, int peakCount, int latestBucketCount) {
    this.count = count;
    this.numberOfEmptyBuckets = numberOfEmptyBuckets;
    this.peakCount = peakCount;
    this.latestBucketCount = latestBucketCount;
  }

  /***
   * Gets the sum of counts accross all buckets in the tps interval window.
   * 
   * @return
   */
  public int getCount() {
    return count;
  }

  /***
   * Gets numbor of empty buckets.
   * 
   * @return
   */
  public int getNumberOfEmptyBuckets() {
    return numberOfEmptyBuckets;
  }

  /***
   * Gets the max count found in the bucket.
   * 
   * @return
   */
  public int getPeakCount() {
    return peakCount;
  }

  /***
   * Get the count in the current bucket.
   * 
   * @return
   */
  public int getLatestBucketCount() {
    return latestBucketCount;
  }
}
