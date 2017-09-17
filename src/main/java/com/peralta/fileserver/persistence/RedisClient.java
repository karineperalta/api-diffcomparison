package com.peralta.fileserver.persistence;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;
import java.util.Set;


/**
 * Class that handles requests to Redis Cache server
 *
 * Provides methods to access Redis Cache server
 * - Create Redis connection for localhost or remote server
 * - Implements set, get, keys and del operations
 */
public class RedisClient {

    /*
    * Redis Client implementation using Jedis lib
    */
    private Jedis jedisClient;

    /*
    * Redis server Hostname
    */
    private String redisHost;

    /*
    * Redis server Port
    */
    private int redisPort;

    /**
    * Class constructor for connecting to local Redis server
    */
    public RedisClient() throws JedisConnectionException{
        connect();
    }

    /**
    * Class constructor for connecting to remote Redis server
    *
    * @param redisHost Remote Redis hostname
    * @param redisPort Remote Redis port
    *
    * @throws JedisConnectionException In case an error happens when accessing Redis Server
    */
    public RedisClient(String redisHost, int redisPort) throws JedisConnectionException{
        this.redisHost = redisHost;
        this.redisPort = redisPort;
        connect();
    }

    /**
     * Connects to Redis server
     *
     * @throws JedisConnectionException In case an error happens when accessing Redis Server
     */
    public Jedis connect() throws JedisConnectionException{

        if (this.redisHost != null) {
            jedisClient = new Jedis(redisHost, redisPort);
        } else {
            jedisClient = new Jedis();
        }

        return jedisClient;
    }

    /**
     * Method to perform Redis set operation
     *
     * @param key Redis key that must be stored
     * @param value Redis value
     */
    public void set(String key, String value) {
        jedisClient.set(key, value);
    }

    /**
     * Method to perform Redis GET operation
     *
     * @param key Redis key that must be consulted
     *
     * @return {@link String} with the requested value
     */
    public String get(String key) {
        return jedisClient.get(key);
    }

    /**
     * Method to perform Redis KEYS operation
     *
     * @param key Redis key indicating pattern that must be looked for
     *
     * @return {@link Set} Set of {@link String} keys that match the requested pattern
     */
    public Set<String> keys(String key) {
        return jedisClient.keys(key);
    }

    /**
     * Method to perform Redis DEL operation
     *
     * @param key Redis key to be deleted
     */
    public void del(String key) {
        jedisClient.del(key);
    }
}
