package com.nse.analyser.controller;

import com.nse.analyser.dtos.nseUnderLyingInfoDTOs.UnderLyingInfoResponse;
import com.nse.analyser.dtos.rest.OptionChainResponse;
import com.nse.analyser.enums.InstrumentType;
import com.nse.analyser.models.ChainSnapshot;
import com.nse.analyser.models.underlyingInfos.TradableFnO_Contracts;
import com.nse.analyser.services.impl.ModelService;
import com.nse.analyser.services.schedulers.NseDataRetrievalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ingestion/option-chain")
@RequiredArgsConstructor
public class OptionChainIngestionController {

    private final NseDataRetrievalService nseDataRetrievalService;

    private final ModelService modelService;

    @GetMapping("/indices")
    public ResponseEntity<OptionChainResponse> triggerIngestion(
            @RequestParam(required = false) String symbol, @RequestParam(required = false) String expiry) {

        List<ChainSnapshot> chainSnapshots = modelService.getChainSnapshots(InstrumentType.INDEX, symbol, expiry);
        return
                ResponseEntity.ok(
           new OptionChainResponse("SUCCESS","Ingestion Indices",symbol,chainSnapshots)
                );
    }

    @GetMapping("/equity")
    public ResponseEntity<OptionChainResponse> triggerIngestionForEquity(
            @RequestParam(required = false) String symbol, @RequestParam(required = false) String expiry) {

        List<ChainSnapshot> chainSnapshots = modelService.getChainSnapshots(InstrumentType.EQUITY, symbol, expiry);
        return
                ResponseEntity.ok(
                   new OptionChainResponse("SUCCESS","IngestStock",symbol,chainSnapshots)
                );
    }

    @GetMapping("/expiryDates")
    public ResponseEntity<OptionChainResponse> getExpires(
            @RequestParam(required = true) String symbol) {

        List<String> expires = nseDataRetrievalService.fetchExpirationDates(symbol);
        return
                ResponseEntity.ok(
                        new OptionChainResponse("SUCCESS","Fetch Expiry's",symbol,expires)

                );
    }

    @GetMapping("/strikePrices")
    public ResponseEntity<OptionChainResponse> getStrikePrices(
            @RequestParam(required = true) String symbol) {

        List<String> strikePrices = nseDataRetrievalService.fetchStrikePrices(symbol);
        return
                ResponseEntity.ok(
                        new OptionChainResponse("SUCCESS","Fetch Strikes",symbol,strikePrices)
                );
    }

    @GetMapping("/underlyingInfo")
    public ResponseEntity<OptionChainResponse> getFnoList() {
        TradableFnO_Contracts tradableFnOContracts = modelService.getTradableFnOContracts();
        return
                ResponseEntity.ok(
                        new OptionChainResponse("SUCCESS","Tradable FnO List","all",tradableFnOContracts)
                );
    }

    @GetMapping("/indicesAndExpiry")
    public ResponseEntity<OptionChainResponse> getIndicesAndExpiry() {
        Map<String, List<String>> stringListMap = modelService.mapIndicesAndExpiry();
        return
                ResponseEntity.ok(
                        new OptionChainResponse("SUCCESS","Indices and expiry","all indices",stringListMap)
                );
    }

    @GetMapping("/stocksAndExpiry")
    public ResponseEntity<OptionChainResponse> getSymbolAndExpiry() {
        Map<String, List<String>> stringListMap = modelService.mapSymbolAndExpiry();
        return
                ResponseEntity.ok(
                        new OptionChainResponse("SUCCESS","Stocks and expiry","all FnO stokcs",stringListMap)
                );
    }
}