package com.nse.analyser.services.schedulers;

import com.nse.analyser.services.NseSessionManager;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SessionRefreshScheduler {

    private final NseSessionManager sessionManager;

    //Refreshed cookies
//    @Scheduled(fixedDelay = 15 * 60 * 1000)
    public void refresh() {

        sessionManager.refreshSession();
    }
}