import com.peralta.fileserver.model.DiffResponse;
import com.peralta.fileserver.persistence.RedisClient;
import com.peralta.fileserver.service.DiffServer;
import com.peralta.fileserver.service.DiffWorker;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mockito.*;
import org.mockito.Mock;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static com.sun.scenario.Settings.set;
import static org.apache.commons.lang3.reflect.FieldUtils.writeField;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
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


public class DiffWorkerTest {

    @InjectMocks
    private DiffWorker diffWorker;

    private byte[] createDifferentSet(){
        byte[] array = new byte[140];
        new Random().nextBytes(array);
        return array;
    }

    @Before
    public void setUp() throws IllegalAccessException, InterruptedException, IOException {
        diffWorker = new DiffWorker();
    }

    @Test
    public void testRunDiffMethod_differentSet() throws Exception{
        Map<Integer, Integer> map = diffWorker.runDiff(createDifferentSet(), createDifferentSet());
        assertTrue(map.size() > 0);
    }

    @Test
    public void testRunDiffMethod_sameSet() throws Exception{
        byte[] testBytes = createDifferentSet();
        Map<Integer, Integer> map = diffWorker.runDiff(testBytes, testBytes);
        assertEquals(0, map.size());
    }








}