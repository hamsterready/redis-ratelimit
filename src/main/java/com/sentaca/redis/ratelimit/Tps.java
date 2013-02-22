package com.sentaca.redis.ratelimit;

public class Tps {
  private double tps;
  private double tpsNoEmptyBuckets;
  private double peakTps;
  private double latestBucketTps;
  private CountResult counters;

  /***
   * Creates the object that contains calculated tps rates.
   * 
   * @param tps
   *          average tps in the meassure window
   * @param tpsNoEmptyBuckets
   *          tps calculated on non-empty buckets in the meassure window
   * @param peakTps
   *          peak tps in the meassure window
   * @param latestBucketTps
   *          tps calculate only in the current bucket
   * @param counters
   *          counters that were used to calculate the tps rates
   */
  public Tps(double tps, double tpsNoEmptyBuckets, double peakTps, double latestBucketTps, CountResult counters) {
    this.tps = tps;
    this.tpsNoEmptyBuckets = tpsNoEmptyBuckets;
    this.peakTps = peakTps;
    this.latestBucketTps = latestBucketTps;
    this.counters = counters;
  }

  /***
   * Gets the tps accross the tps window.
   * 
   * @return
   */
  public double getTps() {
    return tps;
  }

  /***
   * Gets the tps accross the tps window but not include the emtpy buckets. This
   * value may be greater that tps returned by {@link #getTps}.
   * 
   * @return
   */
  public double getTpsNoEmptyBuckets() {
    return tpsNoEmptyBuckets;
  }

  /***
   * Gets the maximal tps observed for one bucket in the tps window.
   * 
   * @return
   */
  public double getPeakTps() {
    return peakTps;
  }

  /***
   * Gets the current tps (from current bucket).
   * 
   * @return
   */
  public double getLatestBucketTps() {
    return latestBucketTps;
  }

  /***
   * Gets the counters that were used to calculate the tps rates.
   * 
   * @return
   */
  public CountResult getCounters() {
    return counters;
  }
}
