package com.peralta.fileserver.service;

import com.peralta.fileserver.model.DiffData;
import com.peralta.fileserver.model.DiffResponse;
import com.peralta.fileserver.persistence.RedisClient;
import redis.clients.jedis.exceptions.JedisConnectionException;

import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class that handles Diff operations and orchestrate other classes
 *
 * Controls access to Redis and Worker to perform Diff operation
 *
 */
public class DiffServer {

    /*
    * RedisClient instance
    */
    private RedisClient redisClient;

    /*
    * DiffWorker instance
    */
    private DiffWorker diffWorker;

    /*
    * Logger instance
    */
    private static final Logger LOGGER = Logger.getLogger(DiffServer.class.getName());


    /*
    * Constructor
    */
    public DiffServer() throws InterruptedException{

        try {
            //redisClient = new RedisClient();
            redisClient = new RedisClient("redis-17197.c8.us-east-1-2.ec2.cloud.redislabs.com", 17197);
            diffWorker = new DiffWorker();
        } catch (JedisConnectionException e) {
            String msg = "Error when connecting to Redis server";
            LOGGER.log(Level.SEVERE, msg, e);
            new InterruptedException();
        }
    }

    /**
     * Method to save pair <key, value> to Redis
     *
     * @param key Redis key that must be stored
     * @param value Redis value
     *
     * @throws InterruptedException In case an error happens when accessing Redis Server
     */
    public void saveToRedis(String key, String value) throws InterruptedException {
        try {
            redisClient.set(key, value);
        } catch (JedisConnectionException e) {
            String msg = "Error when connecting to Redis server";
            LOGGER.log(Level.SEVERE, msg, e);
            throw new InterruptedException();
        }
    }

    /**
     * Method to get the result of the Diff, stored on Redis under key '<jobId>.comparison'
     *
     * @param jobId The jobId where the Diff comparison is stored
     *
     * @throws InterruptedException In case an error happens when accessing Redis Server
     */
    public String getCachedDiff(String jobId) throws InterruptedException {
        try {
            return redisClient.get(jobId + ".comparison");
        } catch (JedisConnectionException e) {
            String msg = "Error when connecting to Redis server";
            LOGGER.log(Level.SEVERE, msg, e);
            throw new InterruptedException();
        }
    }

    /**
     * Method to clean Redis Cache for a jobId. Removes all keys under the pattern '<jobId>.*'
     *
     * @param jobId The jobId used as pattern key
     *
     * @throws InterruptedException In case an error happens when accessing Redis Server
     */
    public void deleteJobIdFromCache (String jobId) throws InterruptedException {
        try {
            Set<String> keys = redisClient.keys(jobId+".*");
            for (String key : keys) {
                redisClient.del(key);
            }
        } catch (JedisConnectionException e) {
            String msg = "Error when connecting to Redis server";
            LOGGER.log(Level.SEVERE, msg, e);
            throw new InterruptedException();
        }
    }

    /**
     * Method that starts Diff comparison for a job.
     * Controls if comparison is Sync (wait for the return) or if it must be performed in background, using flag backgroundProcess for that
     *
     * @param jobId The jobId where the Diff comparison is stored
     * @param backgroundProcess Flag that controls if Diff execution must be sync or must be performed in background, launching a Thread for that
     *
     * @return {@link DiffResponse} Object containing the result of the Diff process, if performed on sync mode.
     *
     * @throws InterruptedException In case an error happens when accessing Redis Server
     */
    public DiffResponse startDiffJobs(final String jobId, final boolean backgroundProcess) throws InterruptedException{

        try {
            // Load uploaded length from both endpoints
            String base64LengthLeft = redisClient.get(jobId + ".left.length");
            String base64LengthRight = redisClient.get(jobId + ".right.length");

            // Creates a DiffResponse object
            DiffResponse response = new DiffResponse();
            response.setJobId(jobId);

            // Checks if both endpoints were already provided (length != null)
            if(base64LengthLeft!=null && base64LengthRight!=null) {

                // if jobs don't have the same size, no need to load content
                if (!base64LengthLeft.equals(base64LengthRight)) {
                    response.setMessage("Jobs are not equals, neither regarding length");
                } else {
                    // Load content from both endpoints
                    String base64EncodedLeft = redisClient.get(jobId + ".left.encoded");
                    String base64EncodedRight = redisClient.get(jobId + ".right.encoded");

                    // Decode
                    byte[] base64decodedBytesLeft = Base64.getDecoder().decode(base64EncodedLeft);
                    byte[] base64decodedBytesRight = Base64.getDecoder().decode(base64EncodedRight);

                    // Compare if Arrays are equal
                    if (Arrays.equals(base64decodedBytesLeft, base64decodedBytesRight)) {
                        response.setMessage("Jobs are equals");
                    } else {

                        // Checks if Diff must be perfomed synchronously or in background (fire and forget proess)
                        if(backgroundProcess) {
                            final byte[] l = base64decodedBytesLeft;
                            final byte[] r = base64decodedBytesRight;
                            new Thread(new Runnable() {
                                public void run() {
                                    try {
                                        Map<Integer, Integer> map = diffWorker.runDiff(l, r);
                                        DiffResponse diffResponse = new DiffResponse();
                                        diffResponse.setJobId(jobId);
                                        diffResponse.setMessage("Jobs have same length, but some diffs were found");
                                        for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
                                            DiffData diffData = new DiffData(Integer.toString(entry.getKey()), Integer.toString(entry.getValue()));
                                            diffResponse.addToDiffDataList(diffData);
                                        }
                                        String s = diffResponse.toString();
                                        // Store Diff result for future requests
                                        redisClient.set(jobId + ".comparison", s);
                                    } catch (Exception e) {
                                        LOGGER.log(Level.SEVERE, "Error when running background task", e);
                                    }
                                }
                            }).start();
                            response.setMessage("Jobs have same length, but some diffs were found");
                        } else {
                            // Perform Diff synchronously
                            Map<Integer, Integer> map = diffWorker.runDiff(base64decodedBytesLeft, base64decodedBytesRight);
                            response.setMessage("Jobs have same length, but some diffs were found");
                            for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
                                DiffData diffData = new DiffData(Integer.toString(entry.getKey()), Integer.toString(entry.getValue()));
                                response.addToDiffDataList(diffData);
                            }
                            // Store Diff result for future requests
                            String s = response.toString();
                            redisClient.set(jobId + ".comparison", s);
                        }
                    }
                }
            } else {
                response.setMessage("No content was uploaded to one of the endpoints yet. Please upload content to both endpoints and then, compare contents");
            }
            return response;

        } catch (JedisConnectionException e) {
            String msg = "Error when connecting to Redis server";
            LOGGER.log(Level.SEVERE, msg, e);
            throw new InterruptedException();
        } catch (Exception e) {
            String msg = "Error when running Diff threads";
            LOGGER.log(Level.SEVERE, msg, e);
            throw new InterruptedException();
        }

    }

}
