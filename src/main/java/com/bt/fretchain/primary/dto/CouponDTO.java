package com.bt.fretchain.primary.dto;

import com.bt.fretchain.primary.pojo.enums.CouponDiscountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class CouponDTO {

    @Getter
    @Setter
    public static class SaveOrUpdate {

        @NotBlank(message = "Coupon code is required")
        private String couponCode;

        @NotBlank(message = "English title is required")
        private String titleEn;

        @NotBlank(message = "French title is required")
        private String titleFr;

        private String descriptionEn;

        private String descriptionFr;

        @NotNull(message = "Discount type is required")
        private CouponDiscountType discountType;

        @NotNull(message = "Discount value is required")
        private BigDecimal discountValue;

        private BigDecimal maxDiscountAmount;

        private BigDecimal minimumOrderAmount;

        private Integer usageLimit;

        private Integer perCustomerLimit;

        private LocalDateTime startDate;

        private LocalDateTime endDate;
    }

    @Getter
    @Setter
    public static class Detail extends BasicDto {

        private String couponCode;

        private String titleEn;

        private String titleFr;

        private String descriptionEn;

        private String descriptionFr;

        private CouponDiscountType discountType;

        private BigDecimal discountValue;

        private BigDecimal maxDiscountAmount;

        private BigDecimal minimumOrderAmount;

        private Integer usageLimit;

        private Integer usedCount;

        private Integer perCustomerLimit;

        private LocalDateTime startDate;

        private LocalDateTime endDate;
    }

    @Getter
    @Setter
    public static class ListOrSearchData extends BasePageRequest {

        private Filters filters;

        private DateFilter dateFilter;

        private SortRequest sort;
    }

    @Getter
    @Setter
    public static class Filters {

        private Boolean active;

        private CouponDiscountType discountType;
    }

    @Getter
    @Setter
    public static class DateFilter {

        private LocalDate from;

        private LocalDate to;
    }

    @Getter
    @Setter
    public static class SortRequest {

        private String field;

        private String direction;
    }
}