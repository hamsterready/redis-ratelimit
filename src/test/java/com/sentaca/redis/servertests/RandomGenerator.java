package com.sentaca.redis.servertests;

import java.util.Calendar;
import java.util.Random;

public class RandomGenerator {

  private static Random random = new Random(Calendar.getInstance().getTimeInMillis());

  /***
   * Returns the pseudo generated value described by "expotential distribution" (http://en.wikipedia.org/wiki/Exponential_distribution).
   * 
   * @param lambda
   *          number of events per second
   * @return
   */
  public double expRandom(double lambda) {
    double t = random.nextDouble();
    double x = -Math.log(t) / lambda;

    return x;
  }
}
