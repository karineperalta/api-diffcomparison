import com.peralta.fileserver.model.DiffResponse;
import com.peralta.fileserver.persistence.RedisClient;
import com.peralta.fileserver.service.DiffServer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mockito.*;
import org.mockito.Mock;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static com.sun.scenario.Settings.set;
import static org.apache.commons.lang3.reflect.FieldUtils.writeField;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
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


public class DiffServerTest {

    @InjectMocks
    private DiffServer diffServer;



    @Mock
    private RedisClient redisClient;



    @Before
    public void setUp() throws IllegalAccessException, InterruptedException, IOException {
        //jedisClient = mock(Jedis.class);
        redisClient = mock(RedisClient.class);
        diffServer = new DiffServer();
        writeField(diffServer, "redisClient", redisClient, true);


    }

    @Test
    public void testSaveToRedisMethod() throws InterruptedException{
        doNothing().when(redisClient).set(isA(String.class), isA(String.class));
        diffServer.saveToRedis("jobid", "value");
        verify(redisClient, times(1)).set(isA(String.class), isA(String.class));
    }

    @Test (expected = InterruptedException.class)
    public void testSaveToRedisMethodThrowException() throws InterruptedException{
        doThrow(JedisConnectionException.class).when(redisClient).set(isA(String.class), isA(String.class));
        diffServer.saveToRedis("jobid", "value");
    }

    @Test
    public void testGetCachedDiffMethod() throws InterruptedException{
        when(redisClient.get(isA(String.class))).thenReturn("value");
        String resp = diffServer.getCachedDiff("jobid");
        assertEquals("value", resp);
        verify(redisClient, times(1)).get(isA(String.class));
    }

    @Test (expected = InterruptedException.class)
    public void testGetCachedDiffMethodThrowException() throws InterruptedException{
        doThrow(JedisConnectionException.class).when(redisClient).get(isA(String.class));
        diffServer.getCachedDiff("jobid");
    }

    @Test
    public void testDeleteJobIdFromCacheMethod() throws InterruptedException{
        Set<String> mockedSet = new HashSet<String>();
        mockedSet.add("jobid1");
        mockedSet.add("jobid2");
        when(redisClient.keys(isA(String.class))).thenReturn(mockedSet);
        diffServer.deleteJobIdFromCache("jobid");
        verify(redisClient, times(2)).del(isA(String.class));
    }

    @Test (expected = InterruptedException.class)
    public void testDeleteJobIdFromCacheMethodThrowException() throws InterruptedException{
        when(redisClient.keys(isA(String.class))).thenThrow(JedisConnectionException.class);
        diffServer.deleteJobIdFromCache("jobid");
    }

    @Test
    public void testStartDiffJobsMethod_sameSizeSameJobs() throws InterruptedException{

        when(redisClient.get(isA(String.class))).thenReturn("5", "5", "a2FyaW51", "a2FyaW51");
        DiffResponse diffResponse = diffServer.startDiffJobs("jobid", false);
        assertEquals("Jobs are equals", diffResponse.getMessage());
    }

    @Test
    public void testStartDiffJobsMethod_differentSize() throws InterruptedException{

        when(redisClient.get(isA(String.class))).thenReturn("5", "6");
        DiffResponse diffResponse = diffServer.startDiffJobs("jobid", false);
        assertEquals("Jobs are not equals, neither regarding length", diffResponse.getMessage());
    }

    @Test
    public void testStartDiffJobsMethod_sameSizeDifferentJobs() throws InterruptedException{

        when(redisClient.get(isA(String.class))).thenReturn("5", "5", "a2FyaW51", "a2FyaW52");
        DiffResponse diffResponse = diffServer.startDiffJobs("jobid", false);
        assertEquals("Jobs have same length, but some diffs were found", diffResponse.getMessage());
        assertEquals(1, diffResponse.getDiffDataList().size());
    }

    @Test
    public void testStartDiffJobsMethod_sameSizeDifferentJobs_backgroundTrue() throws InterruptedException{

        when(redisClient.get(isA(String.class))).thenReturn("5", "5", "a2FyaW51", "a2FyaW52");
        DiffResponse diffResponse = diffServer.startDiffJobs("jobid", true);
        assertEquals("Jobs have same length, but some diffs were found", diffResponse.getMessage());
    }

    @Test
    public void testStartDiffJobsMethod_missingJob() throws InterruptedException{

        when(redisClient.get(isA(String.class))).thenReturn("5", null);
        DiffResponse diffResponse = diffServer.startDiffJobs("jobid", false);
        assertEquals("No content was uploaded to one of the endpoints yet. Please upload content to both endpoints and then, compare contents", diffResponse.getMessage());
    }

    @Test (expected = InterruptedException.class)
    public void testStartDiffJobsMethodThrowsJedisException() throws InterruptedException {
        when(redisClient.get(isA(String.class))).thenThrow(JedisConnectionException.class);
        diffServer.startDiffJobs("jobid", false);
    }

    @Test (expected = InterruptedException.class)
    public void testStartDiffJobsMethodThrowsException() throws InterruptedException {
        when(redisClient.get(isA(String.class))).thenThrow(Exception.class);
        diffServer.startDiffJobs("jobid", false);
    }
















}