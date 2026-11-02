package com.bt.fretchain.primary.dto;

import com.bt.fretchain.primary.pojo.enums.DriverAssignmentStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

public class DriverAssignmentDTO {

    @Getter
    @Setter
    public static class AssignRequest {
        private String driverUuid;
    }


    @Getter
    @Setter
    @Builder
    public static class AssignResponse {
        private String assignmentUuid;
        private String orderUuid;
        private String orderNumber;
        private String driverUuid;
        private String driverName;
        private DriverAssignmentStatus assignmentStatus;
    }


    @Getter
    @Setter
    @Builder
    public static class PendingResponse {
        private String assignmentUuid;
        private String orderUuid;
        private String orderNumber;
        private String assignmentStatus;
    }


    @Getter
    @Setter
    public static class RejectRequest {
        private String reason;
    }
}