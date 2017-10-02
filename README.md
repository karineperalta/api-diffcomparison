# Diff REST API
This repository contains the implementation of a REST API to perform Diff on base64 encoded binary data.

## Overview

The contents to be compared must be sent to two different endpoints:
- /v1/diff/\<jobid>/left
- /v1/diff/\<jobid>/right

After both files were successfully sent to the endpoints, a third endpoint must be called in order to start the Diff 
process:

- /v1/diff/\<jobid>

The result is a JSON Response indicating the positions where different data was found, and the length of it

## Endpoints

The Diff REST API has three main endpoints:

### Upload content to LEFT endpoint

This call is used to send data to LEFT endpoint.

- POST /v1/diff/\<jobid>/left

Where \<jobid> is any string used to identify the job
 
This call requests a JSON body, containing one attribute `base64Data` holding a base64 encoded binary data that must be
evaluated.  
 
```
  JSON Body:
  {
      "base64Data": "<base64EncodedBynaryData"
  }
   ```

Expected response code: 201

```
  JSON Returned Body:
  {
      "jobId": "<jobId>",
      "Message": "<message informing that job was received",
      "Diff":[]
  }
```


### Upload content to RIGHT endpoint

This call behaves the same as the previous request, but is used to send data to RIGHT endpoint.

- POST /v1/diff/\<jobid>/right

Where \<jobid> is any string used to identify the job
 
This call requests a JSON body, containing one attribute `base64Data` holding a base64 encoded binary data that must be 
evaluated.
  
```
  JSON Body:
  {
      "base64Data": "<base64EncodedBynaryData"
  }
```

Expected response code: 201

```
  JSON Returned Body:
  {
      "jobId": "<jobId>",
      "Message": "<message informing that job was received",
      "Diff":[]
  }
```

### Start Diff Processing

This call is used to start the Diff comparison between the data provided on the two endpoints previously explained.

- GET /v1/diff/\<jobid>

Where \<jobid> is any string used to identify the job
 
Returned body is a JSON informing the offset positions where diffs were found, as well as the length of the sequence 
from that point that differs between both endpoints. 
 
Expected response code: 200
 
```
  JSON Returned Body:
  {
      "jobId": "<jobId>",
      "Message": "<message informing the status of the comparison",
      "Diff":[{
        "offset":"<offset position wheere diff was found",
        "length":"length of the diff-ed sequence found"
      },
      {
              "offset":"<offset position wheere diff was found",
              "length":"length of the diff-ed sequence found"
      }]
  }
```

Where "Diff" is a list of diff-ed offsets and lengths. 


## Persistence 

A Redis server is being used to store the uploaded content, as well as other information considered relevant, 
like the last Diff execution of a job. This approach was used because Redis is a very good cache solution, with 
 good performance and low effort to setup. The code is already pointing to this remote server, that is on Redis Lab 
 infrastructure.

Connection info:
- Host: "redis-17197.c8.us-east-1-2.ec2.cloud.redislabs.com"
- Port: 17197


## Extra Functionalities

In order to improve this REST API, the following functionalities were implemented:

- Background Processing:

    The traditional workflow of this API is:
    - Upload content to left endpoint
    - Upload content to right endpoint
    - Perform a request to start Diff
    
    This requires the user to perform 3 requests, and still wait for the Diff process to finish before getting the response from the third call.
    In order to improve this process, both upload endpoints support a `backgroundProcess` flag, informed as a Query parameter. 
    If the user performs the upload with this flag set to True, the API checks if content for both endpoints were already provided. 
    If yes, the API then starts the Diff process in background, using a Fire-and-Forget approach - a Response is sent to the user, 
    but the API remains processing the Diff. After process is finished, the result is stored on Redis server using the key `jobid.comparison`.
     
    The URL to use this functionality is
    
    ```
         POST /v1/diff/<jobid>/left/?backgroundProcess=true
         POST /v1/diff/<jobid>/right/?backgroundProcess=true
    ```
     
- Cached Diff Result:

    Everytime a Diff is performed, the REST API caches the result. This happens for both execution modes: Traditional or in Background.
    
    In order to have a better performance, when an user requests a Diff comparison, he may inform the flag `cachedDiff` as a Query parameter.
    Is this flag is informed, the REST API will first check if there is a previous diff saved, like the one performed in background, for example.
    If there is a cached Diff Result, the API will use this result instead of start a new Diff process. If there is no cached Diff Result, 
    the API will follows its traditional workflow and do the comparison.
     
    The URL to use this functionality is
        
     ```
          GET /v1/diff/<jobid>/?cachedDiff=true
     ```
     
- Clean jobid cache from Redis

    Depending on the number of jobs created, the amount of data stored in Redis may increase fast. To help reduce costs, the REST API provides 
    an endpoint to clean all Redis keys related to a jobid. 
    
    The URL to use this functionality is

     ```
          DELETE /v1/diff/<jobid>
     ```

## Tests

All tests (unit and integration) are under /test/java folder. Unit Tests were implemented using Mockito Framework and 
Integration Tests were implemented using JerseyTest 2 Framework.
 
## Running Code with IntelliJ

Requirements:
- IntelliJ IDEA (tested on version 2017.1.5)
- A localhost Redis Server installed. Otherwise comment the following line on DiffServer.java `redisClient = new RedisClient();` 
 and uncomment line `redisClient = new RedisClient("redis-17197.c8.us-east-1-2.ec2.cloud.redislabs.com", 17197);`
- Maven 
 
Starting App: 
  
1. Clone code using `git clone https://github.com/karineperalta/api-diffcomparison.git`
2. Using a prompt, open the folder where code was cloned and run `mvn clean install`
3. Open the project using IntelliJ. Do not import anything automatically
4. Right-click on the project name > Open Module Settings
5. On the Facets menu, click on '+' button and select Web. When a dialog appears, select fileServer
6. A message informing that 'Web Facet resources are not included on the Artifact' will be presented. 
Click on the 'Create Artifact' button
7. On the right-side of the screen, on the Available Elements panel, select all libraries under fileserver item. 
Click with the right button on the selected list and select option 'Put into /WEB-INF/lib'
8. Select the project name and click on the Run menu. Choose 'Edit Configurations' item
9. Click on the '+' button and select 'Tomcat Server' > 'Local'
10. Open the Deployment Tab > click on the '+' button > Artifact. The fileserver artifact will be automatically selected.
11. Click on the OK button
12. Click on the Run button (the button with a green arrow)
13. Perform requests to http://localhost:<port>/v1/diff... according to your needs :)  

