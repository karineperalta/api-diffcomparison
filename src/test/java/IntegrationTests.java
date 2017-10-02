import java.net.URI;
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

import org.apache.commons.lang3.RandomStringUtils;

import org.glassfish.jersey.servlet.ServletContainer;
import org.glassfish.jersey.test.ServletDeploymentContext;
import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory;
import org.glassfish.jersey.test.spi.TestContainerFactory;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;


import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.ext.Provider;

import static org.apache.commons.lang3.reflect.FieldUtils.writeField;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.junit.Assert.assertEquals;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.process.internal.RequestScoped;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.Test;



public class IntegrationTests extends JerseyTest {


    public ServletDeploymentContext configureDeployment() {
        return ServletDeploymentContext
                .forServlet(new ServletContainer(new ResourceConfig(DiffEndPoints.class))).build();
    }
    @Override
    protected TestContainerFactory getTestContainerFactory() {
        return new GrizzlyWebTestContainerFactory();
    }
    @Override
    protected URI getBaseUri() {
        return UriBuilder.fromUri(super.getBaseUri()).path("/diff/").build();
    }

    @Test
    public void createSameJobs_NoDiffsFound() throws InterruptedException, IllegalAccessException{

        // Same job to be diff-ed
        String input = "{\"base64Data\": \"a2FyaW52\"}";

        //JOBID
        String jobId = RandomStringUtils.random(8, "utf-8");
        Entity<String> entity = Entity.json(input);

        // Msg
        String expectedMsg = "Jobs are equals";

        // POST left
        Response responseLeft = target().path(jobId+"/left").request().accept("application/json")
                .post(entity);
        assertEquals(201, responseLeft.getStatus());

        // POST right
        Response responseRight = target().path(jobId+"/right").request().accept("application/json")
                .post(entity);
        assertEquals(201, responseRight.getStatus());

        // GET diff comparison
        Response diffComparison = target().path(jobId).request().accept("application/json")
                .get();
        assertEquals(200, diffComparison.getStatus());
    }

    @Test
    public void createDifferentJobs_differentSize() throws InterruptedException, IllegalAccessException{

        // job1
        String input1 = "{\"base64Data\": \"a2FyaW52\"}";

        // job2
        String input2 = "{\"base64Data\": \"a2FyaW5\"}";

        //JOBID
        String jobId = RandomStringUtils.random(8, "utf-8");

        Entity<String> entity1 = Entity.json(input1);
        Entity<String> entity2 = Entity.json(input2);

        // Msg
        String expectedMsg = "Jobs are not equals, neither regarding length";

        // POST left
        Response responseLeft = target().path(jobId+"/left").request().accept("application/json")
                .post(entity1);
        assertEquals(201, responseLeft.getStatus());

        // POST right
        Response responseRight = target().path(jobId+"/right").request().accept("application/json")
                .post(entity2);
        assertEquals(201, responseRight.getStatus());

        // GET diff comparison
        Response diffComparison = target().path(jobId).request().accept("application/json")
                .get();
        assertEquals(200, diffComparison.getStatus());

        // assert returned body content
        String diffString = diffComparison.readEntity(String.class);
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

        Entity<String> entity1 = Entity.json(input1);
        Entity<String> entity2 = Entity.json(input2);

        // Msg
        String expectedMsg = "Jobs have same length, but some diffs were found";

        // POST left
        Response responseLeft = target().path(jobId+"/left").request().accept("application/json")
                .post(entity1);
        assertEquals(201, responseLeft.getStatus());

        // POST right
        Response responseRight = target().path(jobId+"/right").request().accept("application/json")
                .post(entity2);
        assertEquals(201, responseRight.getStatus());

        // GET diff comparison
        Response diffComparison = target().path(jobId).request().accept("application/json")
                .get();
        assertEquals(200, diffComparison.getStatus());

        // assert returned body content
        String diffString = diffComparison.readEntity(String.class);
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

        Entity<String> entity1 = Entity.json(input1);

        // Msg
        String expectedMsg = "No content was uploaded to one of the endpoints yet. Please upload content to both endpoints and then, compare contents";

        // POST left
        Response responseLeft = target().path(jobId+"/left").request().accept("application/json")
                .post(entity1);
        assertEquals(201, responseLeft.getStatus());

        // GET diff comparison -- right endpoint is null
        Response diffComparison = target().path(jobId).request().accept("application/json")
                .get();
        assertEquals(200, diffComparison.getStatus());

        // assert returned body content
        String diffString = diffComparison.readEntity(String.class);
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

        Entity<String> entity1 = Entity.json(input1);

        // Msg
        String expectedMsg = "No content was uploaded to one of the endpoints yet. Please upload content to both endpoints and then, compare contents";

        // POST right
        Response responseRight = target().path(jobId+"/right").request().accept("application/json")
                .post(entity1);
        assertEquals(201, responseRight.getStatus());

        // GET diff comparison -- left endpoint is null
        Response diffComparison = target().path(jobId).request().accept("application/json")
                .get();
        assertEquals(200, diffComparison.getStatus());

        // assert returned body content
        String diffString = diffComparison.readEntity(String.class);
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
        Response responseLeft = target().path(jobId+"/left").request().accept("application/json")
                .post(null);
        assertEquals(400, responseLeft.getStatus());

        // assert returned body content
        String diffString = responseLeft.readEntity(String.class);
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
        Response responseRight = target().path(jobId+"/right").request().accept("application/json")
                .post(null);
        assertEquals(400, responseRight.getStatus());

        // assert returned body content
        String diffString = responseRight.readEntity(String.class);
        JSONObject jsonObj = new JSONObject(diffString);
        assertEquals(jobId, jsonObj.get("jobId").toString());
        assertEquals(expectedMsg, jsonObj.get("Message").toString());
    }

    @Test
    public void createJobMalformedBodyLeft_wrongJsonKey() throws InterruptedException, IllegalAccessException{

        // job1
        String input1 = "{\"wrongKey\": \"a2FyaW528796\"}";

        //JOBID
        String jobId = RandomStringUtils.random(8, "utf-8");

        Entity<String> entity1 = Entity.json(input1);

        // Msg
        String expectedMsg = "Body is missing or malformed";

        // POST left
        Response responseLeft = target().path(jobId+"/left").request().accept("application/json")
                .post(entity1);
        assertEquals(400, responseLeft.getStatus());

        // assert returned body content
        String diffString = responseLeft.readEntity(String.class);
        JSONObject jsonObj = new JSONObject(diffString);
        assertEquals(jobId, jsonObj.get("jobId").toString());
        assertEquals(expectedMsg, jsonObj.get("Message").toString());
    }

    @Test
    public void createJobMalformedBodyRight_wrongJsonKey() throws InterruptedException, IllegalAccessException{

        // job1
        String input1 = "{\"wrongKey\": \"a2FyaW528796\"}";

        //JOBID
        String jobId = RandomStringUtils.random(8, "utf-8");

        Entity<String> entity1 = Entity.json(input1);

        // Msg
        String expectedMsg = "Body is missing or malformed";

        // POST right
        Response responseRight = target().path(jobId+"/right").request().accept("application/json")
                .post(entity1);
        assertEquals(400, responseRight.getStatus());

        // assert returned body content
        String diffString = responseRight.readEntity(String.class);
        JSONObject jsonObj = new JSONObject(diffString);
        assertEquals(jobId, jsonObj.get("jobId").toString());
        assertEquals(expectedMsg, jsonObj.get("Message").toString());
    }

    @Test
    public void createJobMalformedBodyLeft_notbase64() throws InterruptedException, IllegalAccessException{

        // job1
        String input1 = "{\"base64Data\": \"karine.peralta\"}";

        //JOBID
        String jobId = RandomStringUtils.random(8, "utf-8");

        Entity<String> entity1 = Entity.json(input1);

        // Msg
        String expectedMsg = "An error happened when decoding body from base64";

        // POST left
        Response responseLeft = target().path(jobId+"/left").request().accept("application/json")
                .post(entity1);
        assertEquals(400, responseLeft.getStatus());

        // assert returned body content
        String diffString = responseLeft.readEntity(String.class);
        JSONObject jsonObj = new JSONObject(diffString);
        assertEquals(jobId, jsonObj.get("jobId").toString());
        assertEquals(expectedMsg, jsonObj.get("Message").toString());
    }

    @Test
    public void createJobMalformedBodyRight_notbase64() throws InterruptedException, IllegalAccessException{

        // job1
        String input1 = "{\"base64Data\": \"karine.peralta\"}";

        //JOBID
        String jobId = RandomStringUtils.random(8, "utf-8");

        Entity<String> entity1 = Entity.json(input1);

        // Msg
        String expectedMsg = "An error happened when decoding body from base64";

        // POST right
        Response responseRight = target().path(jobId+"/right").request().accept("application/json")
                .post(entity1);
        assertEquals(400, responseRight.getStatus());

        // assert returned body content
        String diffString = responseRight.readEntity(String.class);
        JSONObject jsonObj = new JSONObject(diffString);
        assertEquals(jobId, jsonObj.get("jobId").toString());
        assertEquals(expectedMsg, jsonObj.get("Message").toString());
    }

    @Test
    public void createJobMalformedBodyLeft_bodyPlaintext() throws InterruptedException, IllegalAccessException{

        // job1
        String input1 = "{base64Data: karine.peralta}";

        //JOBID
        String jobId = RandomStringUtils.random(8, "utf-8");

        Entity<String> entity1 = Entity.text(input1);

        // POST left
        Response responseLeft = target().path(jobId+"/left").request()
                .post(entity1);
        assertEquals(415, responseLeft.getStatus());
    }

    @Test
    public void createJobMalformedBodyRight_bodyPlaintext() throws InterruptedException, IllegalAccessException{

        // job1
        String input1 = "{base64Data: karine.peralta}";

        //JOBID
        String jobId = RandomStringUtils.random(8, "utf-8");

        Entity<String> entity1 = Entity.text(input1);

        // POST right
        Response responseRight = target().path(jobId+"/right").request()
                .post(entity1);
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

        Entity<String> entity1 = Entity.json(input1);
        Entity<String> entity2 = Entity.json(input2);

        // POST left
        Response responseLeft = target().path(jobId+"/left").queryParam("backgroundProcess", "true").request().accept("application/json")
                .post(entity1);
        assertEquals(201, responseLeft.getStatus());

        // POST right
        Response responseRight = target().path(jobId+"/right").request().accept("application/json")
                .post(entity2);
        assertEquals(201, responseRight.getStatus());
    }

    @Test
    public void createDifferentJobs_backgroundTrueRight() throws InterruptedException, IllegalAccessException{

        // job1
        String input1 = "{\"base64Data\": \"a2FyaW528796\"}";

        // job2
        String input2 = "{\"base64Data\": \"a2FyaW535455\"}";

        //JOBID
        String jobId = RandomStringUtils.random(8, "utf-8");

        Entity<String> entity1 = Entity.json(input1);
        Entity<String> entity2 = Entity.json(input2);

        // POST left
        Response responseLeft = target().path(jobId+"/left").request().accept("application/json")
                .post(entity1);
        assertEquals(201, responseLeft.getStatus());

        // POST right
        Response responseRight = target().path(jobId+"/right").queryParam("backgroundProcess", "true").request().accept("application/json")
                .post(entity2);
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

        Entity<String> entity1 = Entity.json(input1);

        // POST left
        Response responseLeft = target().path(jobId+"/left").queryParam("backgroundProcess", "true").request().accept("application/json")
                .post(entity1);
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

        Entity<String> entity1 = Entity.json(input1);
        Entity<String> entity2 = Entity.json(input2);

        // POST left
        Response responseLeft = target().path(jobId+"/left").request().accept("application/json")
                .post(entity1);
        assertEquals(201, responseLeft.getStatus());

        // POST right
        Response responseRight = target().path(jobId+"/right").request().accept("application/json")
                .post(entity2);
        assertEquals(201, responseRight.getStatus());

        // GET diff comparison -- request cachedDiff
        Response diffComparison = target().path(jobId).queryParam("cachedDiff", "true").request().accept("application/json")
                .get();
        assertEquals(200, diffComparison.getStatus());

        // assert returned body content
        String diffString = diffComparison.readEntity(String.class);
        JSONObject jsonObj = new JSONObject(diffString);
        assertEquals(jobId, jsonObj.get("jobId").toString());
        assertEquals(expectedMsg, jsonObj.get("Message").toString());
    }

    @Test
    public void deleteKeys() throws InterruptedException, IllegalAccessException{

        // job1
        String input1 = "{\"base64Data\": \"a2FyaW528796\"}";

        //JOBID
        String jobId = RandomStringUtils.random(8, "utf-8");

        // Msg
        String expectedMsg = "All keys were removed successfully";

        Entity<String> entity1 = Entity.json(input1);

        // POST left
        Response responseLeft = target().path(jobId+"/left").request().accept("application/json")
                .post(entity1);
        assertEquals(201, responseLeft.getStatus());

        // DELETE keys
        Response diffComparison = target().path(jobId).request().accept("application/json")
                .delete();
        assertEquals(200, diffComparison.getStatus());

        // assert returned body content
        String diffString = diffComparison.readEntity(String.class);
        JSONObject jsonObj = new JSONObject(diffString);
        assertEquals(jobId, jsonObj.get("jobId").toString());
        assertEquals(expectedMsg, jsonObj.get("Message").toString());
    }








}