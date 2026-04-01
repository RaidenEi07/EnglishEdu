package com.sso.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class LevelResponse {
    private Long id;
    private String name;
    private String slug;
    private Integer sortOrder;
}
