package com.panorama.backend.controller.admin;

import com.panorama.backend.DTO.admin.LayerListResponse;
import com.panorama.backend.DTO.admin.LayerSummaryDTO;
import com.panorama.backend.service.admin.AdminLayerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminLayerController.class)
class AdminLayerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminLayerService adminLayerService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void listLayersReturnsOk() throws Exception {
        LayerSummaryDTO item = LayerSummaryDTO.builder()
                .id("layer-1")
                .layerName("roads")
                .tableName("roads")
                .category("vector")
                .usage(Map.of("status", "READY"))
                .build();
        LayerListResponse response = LayerListResponse.builder()
                .items(List.of(item))
                .page(1)
                .size(20)
                .total(1)
                .build();

        when(adminLayerService.listLayers(1, 20, null, null, null)).thenReturn(response);

        mockMvc.perform(get("/api/v0/admin/layers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.items[0].id").value("layer-1"));
    }
}
