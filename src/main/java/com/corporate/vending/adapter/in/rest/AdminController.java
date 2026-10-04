package com.corporate.vending.adapter.in.rest;

import com.corporate.vending.application.dto.RestockRequest;
import com.corporate.vending.application.dto.SlotResponse;
import com.corporate.vending.application.dto.SlotStatusRequest;
import com.corporate.vending.application.service.InventoryAdminService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/machines/{machineId}/slots/{slotNo}")
@RequiredArgsConstructor
@Tag(name = "Admin")
public class AdminController {

    private final InventoryAdminService inventoryAdminService;

    @PostMapping("/restock")
    public SlotResponse restock(@PathVariable UUID machineId, @PathVariable String slotNo,
                                @Valid @RequestBody RestockRequest request) {
        return inventoryAdminService.restock(machineId, slotNo, request.quantity());
    }

    @PatchMapping("/status")
    public SlotResponse setStatus(@PathVariable UUID machineId, @PathVariable String slotNo,
                                  @Valid @RequestBody SlotStatusRequest request) {
        return inventoryAdminService.setStatus(machineId, slotNo, request.status());
    }
}
