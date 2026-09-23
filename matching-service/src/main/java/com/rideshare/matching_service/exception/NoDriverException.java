package com.rideshare.matching_service.exception;

public class NoDriverException extends RuntimeException{
    public NoDriverException(String message){
        super(message);
    }
}
