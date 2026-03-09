package by.bsuir.fp.controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class NbrbRateDto {
    @JsonProperty("Cur_ID")
    private Integer curId;

    @JsonProperty("Date")
    private String date;

    @JsonProperty("Cur_Abbreviation")
    private String curAbbreviation;

    @JsonProperty("Cur_Scale")
    private Integer curScale;

    @JsonProperty("Cur_Name")
    private String curName;

    @JsonProperty("Cur_OfficialRate")
    private BigDecimal curOfficialRate;
}