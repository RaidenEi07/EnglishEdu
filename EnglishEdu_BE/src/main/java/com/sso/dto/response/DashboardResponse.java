package com.sso.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class DashboardResponse {
    private long totalEnrolled;
    private long pendingCount;
    private long inProgress;
    private long completed;
}
