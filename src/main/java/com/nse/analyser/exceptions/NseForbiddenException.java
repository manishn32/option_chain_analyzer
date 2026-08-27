package com.nse.analyser.exceptions;

public class NseForbiddenException extends RuntimeException{
    public NseForbiddenException(String message, String responseMessage) {
        super(message+"\n"+responseMessage);
    }
}
