import com.peralta.fileserver.model.DiffResponse;
import com.peralta.fileserver.persistence.RedisClient;
import com.peralta.fileserver.service.DiffServer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mockito.*;
import org.mockito.Mock;
import org.mockito.exceptions.base.MockitoException;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static com.sun.scenario.Settings.set;
import static org.apache.commons.lang3.reflect.FieldUtils.writeField;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.isA;
import static org.mockito.Matchers.isNotNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;



public class RedisClientTest {

    @InjectMocks
    private RedisClient redisClient;

    @Mock
    private Jedis jedisClient;

    @Before
    public void setUp() throws IllegalAccessException, InterruptedException, IOException {
        jedisClient = mock(Jedis.class);
        redisClient = new RedisClient();
        writeField(redisClient, "jedisClient", jedisClient, true);
    }

    @Test
    public void testSetMethod() throws JedisConnectionException {

        when(jedisClient.set(isA(String.class), isA(String.class))).thenReturn("val");
        redisClient.set("jobid", "value");
        verify(jedisClient, times(1)).set(isA(String.class), isA(String.class));

    }

    @Test
    public void testGetMethod() throws JedisConnectionException{

        when(jedisClient.get(isA(String.class))).thenReturn("value");
        String ret = redisClient.get("jobid");
        assertEquals("value", ret);
    }

    @Test
    public void testKeysMethod() throws JedisConnectionException{

        Set<String> mockedSet = new HashSet<String>();
        mockedSet.add("jobid1");
        mockedSet.add("jobid2");

        when(jedisClient.keys(isA(String.class))).thenReturn(mockedSet);
        Set<String> ret = redisClient.keys("jobid");
        assertEquals(2, ret.size());
    }

    @Test
    public void testDelMethod() throws JedisConnectionException {

        when(jedisClient.del(isA(String.class))).thenReturn(anyLong());
        redisClient.del("jobid");
        verify(jedisClient, times(1)).del(isA(String.class));

    }





}