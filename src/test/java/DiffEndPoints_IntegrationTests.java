import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
//import javax.json.JsonException;
import com.peralta.fileserver.DiffEndPoints;
import com.peralta.fileserver.model.DiffData;
import com.peralta.fileserver.model.DiffResponse;
import com.peralta.fileserver.persistence.RedisClient;
import com.peralta.fileserver.service.DiffServer;
import com.sun.jersey.api.NotFoundException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.core.ClassNamesResourceConfig;
import com.sun.jersey.spi.container.servlet.WebComponent;
import com.sun.jersey.spi.inject.SingletonTypeInjectableProvider;
import com.sun.jersey.test.framework.WebAppDescriptor;
import com.sun.jersey.test.framework.spi.container.TestContainerFactory;
import com.sun.jersey.test.framework.spi.container.grizzly2.web.GrizzlyWebTestContainerFactory;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.tools.ant.types.Assertions;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.test.framework.AppDescriptor;
import com.sun.jersey.test.framework.JerseyTest;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;

import static org.apache.commons.lang3.reflect.FieldUtils.writeField;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class DiffEndPoints_IntegrationTests extends JerseyTest {


    @Override
    public WebAppDescriptor configure() {
        return new WebAppDescriptor.Builder()
                .initParam(WebComponent.RESOURCE_CONFIG_CLASS,
                        ClassNamesResourceConfig.class.getName())
                .initParam(
                        ClassNamesResourceConfig.PROPERTY_CLASSNAMES,
                        DiffEndPoints.class.getName()).build();

    }

    @Override
    public TestContainerFactory getTestContainerFactory() {
        return new GrizzlyWebTestContainerFactory();
    }


    @Test
    public void createSameJobs_NoDiffsFound() throws InterruptedException, IllegalAccessException{

        // Same job to be diff-ed
        String input = "{\"base64Data\": \"a2FyaW52\"}";

        //JOBID
        String jobId = RandomStringUtils.random(8, "utf-8");

        // Msg
        String expectedMsg = "Jobs are equals";

        // POST left
        ClientResponse responseLeft = resource().path("diff/"+jobId+"/left").accept("application/json")
                .type("application/json").post(ClientResponse.class, input);
        assertEquals(201, responseLeft.getStatus());

        // POST right
        ClientResponse responseRight = resource().path("diff/"+jobId+"/right").accept("application/json")
                .type("application/json").post(ClientResponse.class, input);
        assertEquals(201, responseRight.getStatus());

        // GET diff comparison
        ClientResponse diffComparison = resource().path("diff/"+jobId)
                .type("application/json").get(ClientResponse.class);
        assertEquals(200, diffComparison.getStatus());

        // assert returned body content
        String diffString = diffComparison.getEntity(String.class);
        JSONObject jsonObj = new JSONObject(diffString);
        assertEquals(jobId, jsonObj.get("jobId").toString());
        assertEquals(expectedMsg, jsonObj.get("Message").toString());
    }

    @Test
    public void createDifferentJobs_differentSize() throws InterruptedException, IllegalAccessException{

        // job1
        String input1 = "{\"base64Data\": \"a2FyaW52\"}";

        // job2
        String input2 = "{\"base64Data\": \"a2FyaW5\"}";

        //JOBID
        String jobId = RandomStringUtils.random(8, "utf-8");

        // Msg
        String expectedMsg = "Jobs are not equals, neither regarding length";

        // POST left
        ClientResponse responseLeft = resource().path("diff/"+jobId+"/left").accept("application/json")
                .type("application/json").post(ClientResponse.class, input1);
        assertEquals(201, responseLeft.getStatus());

        // POST right
        ClientResponse responseRight = resource().path("diff/"+jobId+"/right").accept("application/json")
                .type("application/json").post(ClientResponse.class, input2);
        assertEquals(201, responseRight.getStatus());

        // GET diff comparison
        ClientResponse diffComparison = resource().path("diff/"+jobId)
                .type("application/json").get(ClientResponse.class);
        assertEquals(200, diffComparison.getStatus());

        // assert returned body content
        String diffString = diffComparison.getEntity(String.class);
        JSONObject jsonObj = new JSONObject(diffString);
        assertEquals(jobId, jsonObj.get("jobId").toString());
        assertEquals(expectedMsg, jsonObj.get("Message").toString());
    }

    @Test
    public void createDifferentJobs_sameSize() throws InterruptedException, IllegalAccessException{

        // job1
        String input1 = "{\"base64Data\": \"a2FyaW528796\"}";

        // job2
        String input2 = "{\"base64Data\": \"a2FyaW535455\"}";

        //JOBID
        String jobId = RandomStringUtils.random(8, "utf-8");

        // Msg
        String expectedMsg = "Jobs have same length, but some diffs were found";

        // POST left
        ClientResponse responseLeft = resource().path("diff/"+jobId+"/left").accept("application/json")
                .type("application/json").post(ClientResponse.class, input1);
        assertEquals(201, responseLeft.getStatus());

        // POST right
        ClientResponse responseRight = resource().path("diff/"+jobId+"/right").accept("application/json")
                .type("application/json").post(ClientResponse.class, input2);
        assertEquals(201, responseRight.getStatus());

        // GET diff comparison
        ClientResponse diffComparison = resource().path("diff/"+jobId)
                .type("application/json").get(ClientResponse.class);
        assertEquals(200, diffComparison.getStatus());

        // assert returned body content
        String diffString = diffComparison.getEntity(String.class);
        JSONObject jsonObj = new JSONObject(diffString);
        assertEquals(jobId, jsonObj.get("jobId").toString());
        assertEquals(expectedMsg, jsonObj.get("Message").toString());
    }

    @Test
    public void tryToCompareWhenMissingRightSide() throws InterruptedException, IllegalAccessException{

        // job1
        String input1 = "{\"base64Data\": \"a2FyaW528796\"}";

        //JOBID
        String jobId = RandomStringUtils.random(8, "utf-8");

        // Msg
        String expectedMsg = "No content was uploaded to one of the endpoints yet. Please upload content to both endpoints and then, compare contents";

        // POST left
        ClientResponse responseLeft = resource().path("diff/"+jobId+"/left").accept("application/json")
                .type("application/json").post(ClientResponse.class, input1);
        assertEquals(201, responseLeft.getStatus());

        // GET diff comparison -- right endpoint is null
        ClientResponse diffComparison = resource().path("diff/"+jobId)
                .type("application/json").get(ClientResponse.class);
        assertEquals(200, diffComparison.getStatus());

        // assert returned body content
        String diffString = diffComparison.getEntity(String.class);
        JSONObject jsonObj = new JSONObject(diffString);
        assertEquals(jobId, jsonObj.get("jobId").toString());
        assertEquals(expectedMsg, jsonObj.get("Message").toString());
    }

    @Test
    public void tryToCompareWhenMissingLeftSide() throws InterruptedException, IllegalAccessException{

        // job1
        String input1 = "{\"base64Data\": \"a2FyaW528796\"}";

        //JOBID
        String jobId = RandomStringUtils.random(8, "utf-8");

        // Msg
        String expectedMsg = "No content was uploaded to one of the endpoints yet. Please upload content to both endpoints and then, compare contents";

        // POST right
        ClientResponse responseRight = resource().path("diff/"+jobId+"/right").accept("application/json")
                .type("application/json").post(ClientResponse.class, input1);
        assertEquals(201, responseRight.getStatus());

        // GET diff comparison -- right endpoint is null
        ClientResponse diffComparison = resource().path("diff/"+jobId)
                .type("application/json").get(ClientResponse.class);
        assertEquals(200, diffComparison.getStatus());

        // assert returned body content
        String diffString = diffComparison.getEntity(String.class);
        JSONObject jsonObj = new JSONObject(diffString);
        assertEquals(jobId, jsonObj.get("jobId").toString());
        assertEquals(expectedMsg, jsonObj.get("Message").toString());
    }

    @Test
    public void createJobMissingBodyLeft() throws InterruptedException, IllegalAccessException{

        //JOBID
        String jobId = RandomStringUtils.random(8, "utf-8");

        // Msg
        String expectedMsg = "Body is missing or malformed";

        // POST left
        ClientResponse responseLeft = resource().path("diff/"+jobId+"/left").accept("application/json")
                .type("application/json").post(ClientResponse.class);
        assertEquals(400, responseLeft.getStatus());

        // assert returned body content
        String diffString = responseLeft.getEntity(String.class);
        JSONObject jsonObj = new JSONObject(diffString);
        assertEquals(jobId, jsonObj.get("jobId").toString());
        assertEquals(expectedMsg, jsonObj.get("Message").toString());
    }

    @Test
    public void createJobMissingBodyRight() throws InterruptedException, IllegalAccessException{

        //JOBID
        String jobId = RandomStringUtils.random(8, "utf-8");

        // Msg
        String expectedMsg = "Body is missing or malformed";

        // POST right
        ClientResponse responseRight = resource().path("diff/"+jobId+"/right").accept("application/json")
                .type("application/json").post(ClientResponse.class);
        assertEquals(400, responseRight.getStatus());

        // assert returned body content
        String diffString = responseRight.getEntity(String.class);
        JSONObject jsonObj = new JSONObject(diffString);
        assertEquals(jobId, jsonObj.get("jobId").toString());
        assertEquals(expectedMsg, jsonObj.get("Message").toString());
    }

    @Test
    public void createJobMalformedBodyLeft_wrongJsonKey() throws InterruptedException, IllegalAccessException{

        //JOBID
        String jobId = RandomStringUtils.random(8, "utf-8");

        // job1
        String input1 = "{\"wrongKey\": \"a2FyaW528796\"}";

        // Msg
        String expectedMsg = "Body is missing or malformed";

        // POST left
        ClientResponse responseLeft = resource().path("diff/"+jobId+"/left").accept("application/json")
                .type("application/json").post(ClientResponse.class, input1);
        assertEquals(400, responseLeft.getStatus());

        // assert returned body content
        String diffString = responseLeft.getEntity(String.class);
        JSONObject jsonObj = new JSONObject(diffString);
        assertEquals(jobId, jsonObj.get("jobId").toString());
        assertEquals(expectedMsg, jsonObj.get("Message").toString());
    }

    @Test
    public void createJobMalformedBodyRight_wrongJsonKey() throws InterruptedException, IllegalAccessException{

        //JOBID
        String jobId = RandomStringUtils.random(8, "utf-8");

        // job1
        String input1 = "{\"wrongKey\": \"a2FyaW528796\"}";

        // Msg
        String expectedMsg = "Body is missing or malformed";

        // POST right
        ClientResponse responseRight = resource().path("diff/"+jobId+"/right").accept("application/json")
                .type("application/json").post(ClientResponse.class, input1);
        assertEquals(400, responseRight.getStatus());

        // assert returned body content
        String diffString = responseRight.getEntity(String.class);
        JSONObject jsonObj = new JSONObject(diffString);
        assertEquals(jobId, jsonObj.get("jobId").toString());
        assertEquals(expectedMsg, jsonObj.get("Message").toString());
    }

    @Test
    public void createJobMalformedBodyLeft_notbase64() throws InterruptedException, IllegalAccessException{

        //JOBID
        String jobId = RandomStringUtils.random(8, "utf-8");

        // job1
        String input1 = "{\"base64Data\": \"karine.peralta\"}";

        // Msg
        String expectedMsg = "An error happened when decoding body from base64";

        // POST left
        ClientResponse responseLeft = resource().path("diff/"+jobId+"/left").accept("application/json")
                .type("application/json").post(ClientResponse.class, input1);
        assertEquals(400, responseLeft.getStatus());

        // assert returned body content
        String diffString = responseLeft.getEntity(String.class);
        JSONObject jsonObj = new JSONObject(diffString);
        assertEquals(jobId, jsonObj.get("jobId").toString());
        assertEquals(expectedMsg, jsonObj.get("Message").toString());
    }

    @Test
    public void createJobMalformedBodyRight_notbase64() throws InterruptedException, IllegalAccessException, IllegalArgumentException{

        //JOBID
        String jobId = RandomStringUtils.random(8, "utf-8");

        // job1
        String input1 = "{\"base64Data\": \"karine.peralta\"}";

        // Msg
        String expectedMsg = "An error happened when decoding body from base64";

        // POST right
        ClientResponse responseRight = resource().path("diff/"+jobId+"/right").accept("application/json")
                .type("application/json").post(ClientResponse.class, input1);
        assertEquals(400, responseRight.getStatus());

        // assert returned body content
        String diffString = responseRight.getEntity(String.class);
        JSONObject jsonObj = new JSONObject(diffString);
        assertEquals(jobId, jsonObj.get("jobId").toString());
        assertEquals(expectedMsg, jsonObj.get("Message").toString());
    }

    @Test
    public void createJobMalformedBodyLeft_bodyPlaintext() throws InterruptedException, IllegalAccessException{

        //JOBID
        String jobId = RandomStringUtils.random(8, "utf-8");

        // job1
        String input1 = "{base64Data: karine.peralta}";

        // POST left
        ClientResponse responseLeft = resource().path("diff/"+jobId+"/left")
                .post(ClientResponse.class, input1);
        assertEquals(415, responseLeft.getStatus());
    }

    @Test
    public void createJobMalformedBodyRight_bodyPlainText() throws InterruptedException, IllegalAccessException, IllegalArgumentException{

        //JOBID
        String jobId = RandomStringUtils.random(8, "utf-8");

        // job1
        String input1 = "{base64Data: karine.peralta}";

        // POST Right
        ClientResponse responseRight = resource().path("diff/"+jobId+"/right")
                .post(ClientResponse.class, input1);
        assertEquals(415, responseRight.getStatus());
    }

    /*********** Extra tests ***********/

    /*
    * We can't evaluate backgroundProcess using these tests, however this test evaluates if flow doesn't break
    */
    @Test
    public void createDifferentJobs_backgroundTrueLeft() throws InterruptedException, IllegalAccessException{

        // job1
        String input1 = "{\"base64Data\": \"a2FyaW528796\"}";

        // job2
        String input2 = "{\"base64Data\": \"a2FyaW535455\"}";

        //JOBID
        String jobId = RandomStringUtils.random(8, "utf-8");

        // POST left
        ClientResponse responseLeft = resource().path("diff/"+jobId+"/left").accept("application/json")
                .type("application/json").post(ClientResponse.class, input1);
        assertEquals(201, responseLeft.getStatus());

        // POST right backgroundProcess = true
        ClientResponse responseRight = resource().path("diff/"+jobId+"/right/").queryParam("backgroundProcess", "true").accept("application/json")
                .type("application/json").post(ClientResponse.class, input2);
        assertEquals(201, responseRight.getStatus());
    }

    /*
    * We can't evaluate backgroundProcess using these tests, however this test evaluates if flow doesn't break
    */
    @Test
    public void createDifferentJobs_backgroundTrue_onlyLeftSubmitted() throws InterruptedException, IllegalAccessException{

        // job1
        String input1 = "{\"base64Data\": \"a2FyaW528796\"}";

        //JOBID
        String jobId = RandomStringUtils.random(8, "utf-8");

        // POST left  backgroundProcess = true
        ClientResponse responseLeft = resource().path("diff/"+jobId+"/left/").queryParam("backgroundProcess", "true").accept("application/json")
                .type("application/json").post(ClientResponse.class, input1);
        assertEquals(201, responseLeft.getStatus());
    }

    /*
    * We can't evaluate cachedDiff Result using these tests, however this test evaluates if flow doesn't break
    */
    @Test
    public void createDifferentJobs_cachedDiffTrue() throws InterruptedException, IllegalAccessException{

        // job1
        String input1 = "{\"base64Data\": \"a2FyaW528796\"}";

        // job2
        String input2 = "{\"base64Data\": \"a2FyaW535455\"}";

        //JOBID
        String jobId = RandomStringUtils.random(8, "utf-8");

        // Msg
        String expectedMsg = "Jobs have same length, but some diffs were found";

        // POST left
        ClientResponse responseLeft = resource().path("diff/"+jobId+"/left").accept("application/json")
                .type("application/json").post(ClientResponse.class, input1);
        assertEquals(201, responseLeft.getStatus());

        // POST right backgroundProcess = true
        ClientResponse responseRight = resource().path("diff/"+jobId+"/right").accept("application/json")
                .type("application/json").post(ClientResponse.class, input2);
        assertEquals(201, responseRight.getStatus());

        // GET diff comparison -- request cachedDiff
        ClientResponse diffComparison = resource().path("diff/"+jobId).queryParam("cachedDiff", "true")
                .type("application/json").get(ClientResponse.class);
        assertEquals(200, diffComparison.getStatus());

        // assert returned body content
        String diffString = diffComparison.getEntity(String.class);
        JSONObject jsonObj = new JSONObject(diffString);
        assertEquals(jobId, jsonObj.get("jobId").toString());
        assertEquals(expectedMsg, jsonObj.get("Message").toString());
    }

    /*
    * We can't evaluate cachedDiff Result using these tests, however this test evaluates if flow doesn't break
    */
    @Test
    public void deleteKeys() throws InterruptedException, IllegalAccessException{

        // job1
        String input1 = "{\"base64Data\": \"a2FyaW528796\"}";

        //JOBID
        String jobId = RandomStringUtils.random(8, "utf-8");

        //Message
        String expectedMsg = "All keys were removed successfully";

        // POST left
        ClientResponse responseLeft = resource().path("diff/"+jobId+"/left").accept("application/json")
                .type("application/json").post(ClientResponse.class, input1);
        assertEquals(201, responseLeft.getStatus());

        // DELETE keys
        ClientResponse diffComparison = resource().path("diff/"+jobId)
                .type("application/json").delete(ClientResponse.class);
        assertEquals(200, diffComparison.getStatus());

        // assert returned body content
        String diffString = diffComparison.getEntity(String.class);
        JSONObject jsonObj = new JSONObject(diffString);
        assertEquals(jobId, jsonObj.get("jobId").toString());
        assertEquals(expectedMsg, jsonObj.get("Message").toString());
    }



}
