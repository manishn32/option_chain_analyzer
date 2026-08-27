package com.nse.analyser.services;

public interface NseRestClient {
    <T> T get(
            String url,
            Class<T> responseType);
}
