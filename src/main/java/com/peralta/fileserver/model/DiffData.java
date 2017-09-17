package com.peralta.fileserver.model;

/**
 * Class that specifies how Diff return must be formatted
 *
 * Provides Getters and Setters for the attributes, as well overrides the toString method to be returned on HTTP Response
 */
public class DiffData {

    /*
    * Diff Offset value
    */
    String offset;

    /*
    * Length of current different sequence
    */
    String length;

    /*
    * Constructor
    */
    public DiffData(String offset, String length) {
        this.offset = offset;
        this.length = length;
    }

    /*
    * Getter Offset
    */
    public String getOffset() {
        return offset;
    }

    /*
    * Setter Offset
    */
    public void setOffset(String offset) {
        this.offset = offset;
    }

    /*
    * Getter Length
    */
    public String getLength() {
        return length;
    }

    /*
    * Setter Length
    */
    public void setLength(String length) {
        this.length = length;
    }

    /**
    * Overrides toString operation to format object
    *
    * @return {@link String} formated object
    */
    @Override
    public String toString() {
        return "{\"offset\":\"" + offset + "\", \"length\":\"" + length + "\"}";
    }
}
