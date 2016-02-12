package com.hiinoono;

/**
 *
 * @author Lyle T Harris
 */
public enum PropertyKey {

    AES_KEY("AES_KEY"),
    NODE_ID_PROPERTY("NodeId");

    private final String value;


    PropertyKey(String val) {
        this.value = val;
    }


    public String value() {
        return value;
    }


    public static PropertyKey fromValue(String v) {
        return valueOf(v);
    }


}
