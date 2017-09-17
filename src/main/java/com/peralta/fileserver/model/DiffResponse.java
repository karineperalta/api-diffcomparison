package com.peralta.fileserver.model;

import com.peralta.fileserver.model.DiffData;

import java.util.ArrayList;
import java.util.List;

/**
 * Class that specifies how Diff Responses must be formatted
 *
 * Provides Getters and Setters for the attributes, as well overrides the toString method to be returned on HTTP Response
 */
public class DiffResponse {

    /*
    * Diff JobId value
    */
    String jobId;

    /*
    * Diff Status Message
    */
    String message;

    /*
    * List with Diff Result after processing
    */
    List<DiffData> diffDataList = new ArrayList<DiffData>();

    /*
    * Class constructor
    */
    public DiffResponse() {

    }

    /*
    * Getter JobId
    */
    public String getJobId() {
        return jobId;
    }

    /*
    * Setter JobId
    */
    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    /*
    * Getter Message
    */
    public String getMessage() {
        return message;
    }

    /*
    * Setter Message
    */
    public void setMessage(String message) {
        this.message = message;
    }

    /*
    * Getter diffDataList
    */
    public List<DiffData> getDiffDataList() {
        return diffDataList;
    }

    /*
    * Setter diffDataList
    */
    public void setDiffDataList(List<DiffData> diffDataList) {
        this.diffDataList = diffDataList;
    }

    /*
    * Add diffData result to list
    */
    public void addToDiffDataList(DiffData diffData) {
        this.diffDataList.add(diffData);
    }

    /**
     * Overrides toString operation to format object
     *
     * @return {@link String} formated object
     */
    @Override
    public String toString() {

        String resp = "{\"jobId\":\"" + jobId + "\", \"Message\":\"" + message + "\"";

        if(getDiffDataList() != null) {
            resp += ", \"Diff\":" +  getDiffDataList().toString()  +   "}";
        } else {
            resp += "}";
        }

        return resp;
    }
}
