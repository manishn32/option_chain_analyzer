package com.nse.analyser.dtos.nseUnderLyingInfoDTOs;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UnderLyingResponseData {

        @JsonProperty("IndexList")
        private List<UnderlyingItem> indexList;

        @JsonProperty("UnderlyingList")
        private List<UnderlyingItem> underlyingList;
    }
