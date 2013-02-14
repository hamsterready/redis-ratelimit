package com.sentaca.redis.ratelimit;

import java.util.HashMap;
import java.util.Map;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.mockito.Mockito.*;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Transaction;
import junit.framework.TestCase;

public class RateLimitServiceTest extends TestCase {

  @Mock
  private JedisPool pool;
  @Mock
  private Jedis jedis;
  @Mock
  private Transaction tx;

  private RateLimitService service;

  private Map<String, Integer> data;

  @Override
  protected void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    service = new RateLimitService(pool, "access", 10);
    when(pool.getResource()).thenReturn(jedis);
    when(jedis.multi()).thenReturn(tx);
    data = new HashMap<String, Integer>();
    when(tx.hincrBy(eq("access:127.0.0.1"), anyString(), eq(1l))).then(new Answer<Void>() {

      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        String bucket = (String) invocation.getArguments()[1];
        if (!data.containsKey(bucket)) {
          data.put(bucket, 0);
        }
        data.put(bucket, data.get(bucket) + 1);
        return null;
      }
    });

    when(tx.hdel(eq("access:127.0.0.1"), anyString())).then(new Answer<Void>() {

      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        String bucket = (String) invocation.getArguments()[1];
        data.remove(bucket);
        return null;
      }
    });
    when(tx.hget(eq("access:127.0.0.1"), anyString())).then(new Answer<Void>() {

      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        String bucket = (String) invocation.getArguments()[1];
        // return data.get(bucket);
        return null;
      }
    });
  }

  public void testGetBucket() throws Exception {
    assertEquals(112, service.getBucket(712233));
  }

  public void testAdd() throws Exception {
    service.add(712233, "127.0.0.1");
    verify(tx).hincrBy("access:127.0.0.1", "112", 1);
    for (int i = 0; i < 300 - 10; i++) {
      verify(tx).hdel("access:127.0.0.1", String.valueOf((i + 112 + 1) % 300));
    }
    verify(tx).expire("access:127.0.0.1", 299);
    verify(tx).exec();

    verifyNoMoreInteractions(tx);
  }

  public void testCountOneBucket() throws Exception {
    service.add(712233, "127.0.0.1");
    service.count(712233 + 1000, "127.0.0.1");

    verify(tx).hincrBy("access:127.0.0.1", "112", 1);
    verify(tx).hget("access:127.0.0.1", "112");

    assertEquals(1, data.size());
    assertEquals(1, (int) data.get("112"));
  }

  public void testCountOneBucketSameTime() throws Exception {
    service.add(712233, "127.0.0.1");
    service.count(712233, "127.0.0.1");

    verify(tx).hincrBy("access:127.0.0.1", "112", 1);
    verify(tx).hget("access:127.0.0.1", "111");

    assertEquals(1, data.size());
    assertEquals(1, (int) data.get("112"));
  }

  public void testCountMultiBuckets() throws Exception {
    service.add(712233, "127.0.0.1");
    service.add(712233, "127.0.0.1");
    service.add(712233, "127.0.0.1");

    service.add(712233 + 1000, "127.0.0.1");
    service.add(712233 + 1000, "127.0.0.1");

    service.add(712233 + 2000, "127.0.0.1");
    service.add(712233 + 2000, "127.0.0.1");
    service.add(712233 + 2000, "127.0.0.1");
    service.add(712233 + 2000, "127.0.0.1");

    service.count(712233 + 3000, "127.0.0.1");

    verify(tx, times(3)).hincrBy("access:127.0.0.1", "112", 1);
    verify(tx, times(2)).hincrBy("access:127.0.0.1", "113", 1);
    verify(tx, times(4)).hincrBy("access:127.0.0.1", "114", 1);

    verify(tx).hget("access:127.0.0.1", "112");
    verify(tx).hget("access:127.0.0.1", "113");
    verify(tx).hget("access:127.0.0.1", "114");

    assertEquals(3, data.size());
    assertEquals(3, (int) data.get("112"));
    assertEquals(2, (int) data.get("113"));
    assertEquals(4, (int) data.get("114"));

  }

}
