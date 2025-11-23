package com.panorama.backend.DTO.system;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MenuButtonDTO {
    private String code;
    private String desc;
}
