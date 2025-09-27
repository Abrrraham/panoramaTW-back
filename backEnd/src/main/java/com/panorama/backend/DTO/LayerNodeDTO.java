package com.panorama.backend.DTO;

import com.panorama.backend.DTO.base.BaseDTO;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @author: Steven Da
 * @date: 2024/09/22/10:19
 * @description:
 */
@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
public class LayerNodeDTO extends BaseDTO {
    private String category;
    @Builder.Default
    private List<LayerNodeDTO> children = new ArrayList<>();
}
