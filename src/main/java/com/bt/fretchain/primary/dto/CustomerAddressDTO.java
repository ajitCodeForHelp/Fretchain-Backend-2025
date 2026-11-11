package com.bt.fretchain.primary.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

/** The customer's address book (home, work, other places they order to). */
public class CustomerAddressDTO {

    @Getter
    @Setter
    public static class SaveOrUpdate {
        /** HOME, OFFICE or OTHER (free text up to 50 chars is accepted). */
        @Size(max = 50)
        private String addressType;

        /** A name for the place, e.g. "Mum's house". */
        @Size(max = 100)
        private String addressName;

        @Size(max = 100)
        private String contactName;

        @Size(max = 20)
        private String contactNumber;

        @NotBlank
        @Size(max = 500)
        private String fullAddress;

        @Size(max = 255)
        private String landmark;

        @NotNull
        @DecimalMin("-90.0")
        @DecimalMax("90.0")
        private Double latitude;

        @NotNull
        @DecimalMin("-180.0")
        @DecimalMax("180.0")
        private Double longitude;

        private Boolean defaultAddress = false;
    }

    @Getter
    @Setter
    public static class Detail {
        private String uuid;
        private String addressType;
        private String addressName;
        private String contactName;
        private String contactNumber;
        private String fullAddress;
        private String landmark;
        private Double latitude;
        private Double longitude;
        private Boolean defaultAddress;
    }
}
