package com.nse.analyser.services;

import org.springframework.web.client.RestClient;

public interface NseSessionManager {
    void initializeSession();
    RestClient getClient();
    void refreshSession();
    boolean expired();

}
