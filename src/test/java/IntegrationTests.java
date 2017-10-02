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


}