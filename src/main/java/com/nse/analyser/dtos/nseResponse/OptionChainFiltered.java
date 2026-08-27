package com.nse.analyser.dtos.nseResponse;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OptionChainFiltered {

    private List<OptionStrikeData> data;

    private Activity CE;

    private Activity PE;

}
