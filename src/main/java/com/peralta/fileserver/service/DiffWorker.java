package com.peralta.fileserver.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Class that perform the Diff operation using multiple Threads for that
 *
 * The logic breaks the both received byte[] arrays into blocks, according to a selected ChunkSize
 * Each block is then verified position after position for a Thread
 * After all verifications were done, result is returned
 */
public class DiffWorker {

    /*
    * MAX number of thread used to perform Diff
    */
    private static final int MAX_THREADS = 5;

    /*
    * Predefined CHUNKSIZE
    */
    private static final int CHUNKSIZE = 32;

    /*
    * LOGGER instance
    */
    private static final Logger LOGGER = Logger.getLogger(DiffWorker.class.getName());

    /*
    * Executor to run tasks in a pool and organize execution
    */
    private ExecutorService executor;

    /*
    * Class constructor
    */
    public DiffWorker() {
        executor = Executors.newFixedThreadPool(MAX_THREADS);
    }

    /**
    * Method that divides both byte arrays in chunks and schedule the tasks to perform comparison
    * Blocks are organized according to an offset
    *
    * @param {@link byte[]} Containing binary data received on left endpoint
    * @param {@link byte[]} Containing binary data received on right endpoint
    *
    * @throws Exception if some error happens when processing information
    *
    * @return {@link Map<Integer, Integer>} Map containing offset as key, and length of diff-ed data from that position
    */
    public Map<Integer, Integer> runDiff(byte[] contentLeft, byte[] contentRight) throws Exception{

        byte[][] blocksLeft = new byte[(int)Math.ceil(contentLeft.length / (double)CHUNKSIZE)][CHUNKSIZE];
        byte[][] blocksRight = new byte[(int)Math.ceil(contentRight.length / (double)CHUNKSIZE)][CHUNKSIZE];
        Map<Integer, Integer> map = new TreeMap<Integer, Integer>();

        int start = 0;

        for(int i = 0; i < blocksLeft.length; i++) {
            blocksLeft[i] = Arrays.copyOfRange(contentLeft,start, start + CHUNKSIZE);
            blocksRight[i] = Arrays.copyOfRange(contentRight,start, start + CHUNKSIZE);
            start += CHUNKSIZE ;
        }

        LOGGER.log(Level.INFO, "Starting Diff...");

        List<Future<Map<Integer, Integer>>> futures = new ArrayList<Future<Map<Integer, Integer>>>();

        for (int blockId = 0; blockId < blocksLeft.length; blockId++) {

            byte[] blockLeft = blocksLeft[blockId];
            byte[] blockRight = blocksRight[blockId];

            Callable worker = new MyCallable(blockLeft, blockRight, blockId) {
            };
            final Future<Map<Integer, Integer>> future = executor.submit(worker);
            futures.add(future);
        }
        executor.shutdown();
        while (!executor.isTerminated()) {

        }
        for (Future<Map<Integer, Integer>> f : futures) {
            try {
                map.putAll(f.get());
            } catch (InterruptedException e) {} catch (ExecutionException ex) {
            }
        }
        LOGGER.log(Level.INFO, "Diff has finished...");
        return map;
    }

    /**
     * Class to run byte a byte comparison, in order to find if a binary block is equals to another one or not
     */
    public static class MyCallable implements Callable<Map<Integer, Integer>>  {

        /*
        * Block of bytes for the left content
        */
        private final byte[] blockLeft;

        /*
        * Block of bytes for the right content
        */
        private final byte[] blockRight;

        /*
        * Block id to calculate offset
        */
        private final int blockId;

        /*
        * Map containing processing result with offsets and diff-ed data lengt
        */
        private Map<Integer, Integer> map = new HashMap<Integer, Integer>();

        /*
        * Constructor
        */
        MyCallable(byte[] blockLeft, byte[] blockRight, int blockId) {
            this.blockLeft = blockLeft;
            this.blockRight = blockRight;
            this.blockId = blockId;
        }

        /*
        * Implements comparison between bytes and return a map with offset,length of diff-ed data
        */
        @Override
        public Map<Integer, Integer> call() {

            int length = blockLeft.length;
            int blockSequence = blockId * CHUNKSIZE;
            int lastDiff = 0;
            int count = 1;

            for (int index = 0; index < length; index++) {
                if (blockLeft[index] != blockRight[index]) {
                    if (index == lastDiff + count) {
                        count++;
                        map.put(lastDiff + blockSequence, count);
                    } else {
                        count = 1;
                        lastDiff = index;
                        map.put(index + blockSequence, count);
                    }
                } else {
                    count = 1;
                }
            }

            return map;
        }
    }


}
