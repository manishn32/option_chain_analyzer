package com.nse.analyser.models;

import com.nse.analyser.enums.InstrumentType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document
public class SymbolsAndExpiry {
    private LocalDate date;

    private InstrumentType instrumentType;
    private Map<String, List<String>> symbolsAndExpiry;
}
