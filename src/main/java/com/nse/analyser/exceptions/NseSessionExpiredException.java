package com.nse.analyser.exceptions;

public class NseSessionExpiredException
        extends RuntimeException {

    public NseSessionExpiredException() {
        super("NSE session expired");
    }
}