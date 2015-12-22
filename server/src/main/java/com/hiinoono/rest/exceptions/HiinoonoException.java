package com.hiinoono.rest.exceptions;

/**
 * Base Class of custom Exceptions to go along with HiinoonoExceptionMapper.
 *
 * @author Lyle T Harris
 */
public class HiinoonoException extends RuntimeException {

    public HiinoonoException() {
    }


    public HiinoonoException(String message) {
        super(message);
    }


}
