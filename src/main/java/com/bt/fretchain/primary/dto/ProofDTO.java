package com.bt.fretchain.primary.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ProofDTO {

    /** What the driver sends when he saves the check at the pickup or at a drop. */
    @Getter
    @Setter
    public static class Save {
        private Boolean goodsMatch;
        private Boolean quantityConfirmed;
        @Size(max = 200)
        private String actualQuantity;
        private Boolean conditionOk;
        @Size(max = 2000)
        private String discrepancies;
        @Size(max = 10)
        private List<String> photoUrls = new ArrayList<>();
        /** SVG markup of the signature drawn on the phone. */
        @Size(max = 300000)
        private String signatureSvg;
        @Size(max = 150)
        private String signerName;
        private Double latitude;
        private Double longitude;
    }

    @Getter
    @Setter
    public static class View {
        private String stage;
        private Integer dropSequence;
        private String dropName;
        private Boolean goodsMatch;
        private Boolean quantityConfirmed;
        private String expectedQuantity;
        private String actualQuantity;
        private Boolean conditionOk;
        private String discrepancies;
        private List<String> photoUrls = new ArrayList<>();
        private String signatureSvg;
        private String signerName;
        private LocalDateTime signedAt;
        private LocalDateTime capturedAt;
        private String driverName;
        private Double latitude;
        private Double longitude;
        /** The record has everything the step needs. */
        private boolean complete;
    }
}
