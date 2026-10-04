package com.corporate.vending.adapter.in.rest;

import com.corporate.vending.application.dto.SlotResponse;
import com.corporate.vending.application.service.InventoryAdminService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/machines")
@RequiredArgsConstructor
@Tag(name = "Catalog")
public class CatalogController {

    private final InventoryAdminService inventoryAdminService;

    @GetMapping("/{machineId}/slots")
    public List<SlotResponse> slots(@PathVariable UUID machineId) {
        return inventoryAdminService.listSlots(machineId);
    }
}
