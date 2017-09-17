package com.peralta.fileserver;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.peralta.fileserver.model.DiffResponse;
import com.peralta.fileserver.service.DiffServer;
import org.json.JSONException;
import org.json.JSONObject;



/**
 * Class that handles request for a REST API that performs Diff between two files
 *
 * Context path is /v1/diff
 *
 * The provided endpoints allow users to
 * - Upload a base64 binary content to endpoint LEFT
 * - Upload a base64 binary content to endpoint RIGHT
 * - Compare both files
 *
 * Diff comparison may also be done in background, when uploading a content with backgroundProcess flag. Comparison will only be done if both 'sides' were provided
 *
 * If a previous comparison was already done, a cachedDiff flag is available, so user can load diff from cache instead of run Diff again *
 *
 * The service uses a Redis server as memory cache, so an endpoint to clean entries of a job is also provided
 */
@Path("diff")
public class DiffEndPoints {

    /*
    * LOGGER instance
    */
    private static final Logger LOGGER = Logger.getLogger(DiffEndPoints.class.getName());

    /*
    * DiffServer instance, which is the class that handles logic
    */
    private DiffServer diffServer;

    /*
    * Class constructor
    */
    public DiffEndPoints(){

        try {
            diffServer = new DiffServer();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    /**
     * Endpoint that handles HTTP POST requests for <jobid>/left' endpoint at the specified jobId resource. Receives a
     * JSON containing a base64 encoded binary data
     * and returns a JSON payload with job status
     *
     * Supports a Fire-and-Forget process using the flag backgroundProcess for that, returning a 201 HTTP code,
     * but starting a background Thread to perform Diff if both endpoints were already provided
     *
     * @param jobId The JobId to which content is being uploaded
     * @param backgroundProcess Flag that starts the Diff comparison in background if True (and if both endpoints already have the content)
     * @param body The JSON payload in the format {"base64Data":"<base64 encoded bynary data>"}
     *
     * @return {@link Response} Object containing result after processing request.
     */
    @POST
    @Path("{jobid}/left")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response ReceiveLeft(@PathParam("jobid") String jobId, @QueryParam("backgroundProcess") boolean backgroundProcess, String body) {

        try {
            JSONObject jsonObj = new JSONObject(body);
            String base64encodedString = jsonObj.get("base64Data").toString();

            // Decode base64 data to get bynary value
            byte[] base64decodedBytes = Base64.getDecoder().decode(base64encodedString);
            int length = base64decodedBytes.length;

            // Save encoded String and bynary data length to Redis cache under keys jobid.left.encoded and jobid.left.length
            diffServer.saveToRedis(jobId + ".left.encoded", base64encodedString);
            diffServer.saveToRedis(jobId + ".left.length", Integer.toString(length));

            // Starts Diff in background if flag backgroundProcess is true
            if (backgroundProcess == true) {
                diffServer.startDiffJobs(jobId, true);
            }

            // Handles return
            DiffResponse response = new DiffResponse();
            response.setJobId(jobId);
            response.setMessage("Content was uploaded to endpoint LEFT");
            return Response.status(201).entity(response.toString()).build();

        } catch (JSONException e) {
            String msg = "Body is missing or malformed";
            DiffResponse diffResponse = new DiffResponse();
            diffResponse.setJobId(jobId);
            diffResponse.setMessage(msg);
            LOGGER.log(Level.INFO, msg, e);
            return Response.status(400).entity(diffResponse.toString()).build();
        } catch (IllegalArgumentException e) {
            String msg = "An error happened when decoding body from base64";
            DiffResponse diffResponse = new DiffResponse();
            diffResponse.setJobId(jobId);
            diffResponse.setMessage(msg);
            LOGGER.log(Level.INFO, msg, e);
            return Response.status(400).entity(diffResponse.toString()).build();
        } catch (InterruptedException e) {
            String msg = "Server is unavailable. Please try again later";
            LOGGER.log(Level.SEVERE, msg, e);
            return Response.status(500).entity(msg).build();
        }


    }

    /**
     * Endpoint that handles HTTP POST requests for <jobid>/right' endpoint at the specified jobId resource. Receives a
     * JSON containing a base64 encoded binary data
     * and returns a JSON payload with job status
     *
     * Supports a Fire-and-Forget process using the flag backgroundProcess for that, returning a 201 HTTP code,
     * but starting a background Thread to perform Diff if both endpoints were already provided
     *
     * @param jobId The JobId to which content is being uploaded
     * @param backgroundProcess Flag that starts the Diff comparison in background if True (and if both endpoints already have the content
     * @param body The JSON payload in the format {"base64Data":"<base64 encoded bynary data>"}
     *
     * @return {@link Response} Object containing result after processing request.
     */
    @POST
    @Path("{jobid}/right")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response ReceiveRight(@PathParam("jobid") String jobId, @QueryParam("backgroundProcess") boolean backgroundProcess, String body) {

        try {
            JSONObject jsonObj = new JSONObject(body);
            String base64encodedString = jsonObj.get("base64Data").toString();

            // Decode base64 data to get bynary value
            byte[] base64decodedBytes = Base64.getDecoder().decode(base64encodedString);
            int length = base64decodedBytes.length;

            // Save encoded String and bynary data length to Redis cache under keys jobid.right.encoded and jobid.right.length
            diffServer.saveToRedis(jobId+".right.encoded", base64encodedString);
            diffServer.saveToRedis(jobId+".right.length", Integer.toString(length));

            // Starts Diff in background if flag backgroundProcess is true
            if(backgroundProcess == true) {
                diffServer.startDiffJobs(jobId, true);
            }

            // Handles return
            DiffResponse response = new DiffResponse();
            response.setJobId(jobId);
            response.setMessage("Content was uploaded to endpoint RIGHT");
            return Response.status(201).entity(response.toString()).build();
        } catch (JSONException e) {
            String msg = "Body is missing or malformed";
            DiffResponse diffResponse = new DiffResponse();
            diffResponse.setJobId(jobId);
            diffResponse.setMessage(msg);
            LOGGER.log(Level.INFO, msg, e);
            return Response.status(400).entity(diffResponse.toString()).build();
        } catch (IllegalArgumentException e) {
            String msg = "An error happened when decoding body from base64";
            DiffResponse diffResponse = new DiffResponse();
            diffResponse.setJobId(jobId);
            diffResponse.setMessage(msg);
            LOGGER.log(Level.INFO, msg, e);
            return Response.status(400).entity(diffResponse.toString()).build();
        } catch (InterruptedException e) {
            String msg = "Server is unavailable. Please try again later";
            LOGGER.log(Level.SEVERE, msg, e);
            return Response.status(500).entity(msg).build();
        }
    }

    /**
     * Endpoint that handles HTTP GET requests for starting Diff comparison for a specific <jobid>
     *
     * @param jobId The JobId which content must be compared
     * @param cachedDiff If this flag is True, server tries to load a previous Diff response from redis Cache before starting the Diff process
     *
     * @return {@link Response} Object containing result after processing request.
     */
    @GET
    @Path("{jobid}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response CompareJobs(@PathParam("jobid") String jobId, @QueryParam("cachedDiff") boolean cachedDiff) {

        String response = null;

        try {
            if(cachedDiff == true) {
                response = diffServer.getCachedDiff(jobId);
            }

            if(response == null) {
                DiffResponse diffResponse = diffServer.startDiffJobs(jobId, false);
                response = diffResponse.toString();
            }

            return Response.status(201).entity(response).build();

        } catch (InterruptedException e) {
            String msg = "Server is unavailable. Please try again later";
            LOGGER.log(Level.SEVERE, msg, e);
            return Response.status(500).entity(msg).build();
        }
    }

    /**
     * Endpoint that handles HTTP DELETE requests for cleaning the keys of a job from Redis cache
     *
     * @param jobId The JobId that must be cleaned from Redis cache
     *
     * @return {@link Response} Object containing result after processing request.
     */
    @DELETE
    @Path("{jobid}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response RemoveJobs(@PathParam("jobid") String jobId) {

        try {
            diffServer.deleteJobIdFromCache(jobId);
            DiffResponse diffResponse = new DiffResponse();
            diffResponse.setJobId(jobId);
            diffResponse.setMessage("All keys were removed successfully");
            return Response.status(201).entity(diffResponse.toString()).build();

        } catch (InterruptedException e) {
            String msg = "Server is unavailable. Please try again later";
            LOGGER.log(Level.SEVERE, msg, e);
            return Response.status(500).entity(msg).build();
        }
    }




}