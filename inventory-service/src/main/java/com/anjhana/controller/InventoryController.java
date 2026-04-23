package com.anjhana.controller;

import com.anjhana.model.InventoryItem;
import com.anjhana.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/{skuId}")
    public ResponseEntity<InventoryItem> getStock(@PathVariable String skuId) {
        return ResponseEntity.ok(inventoryService.getStock(skuId));
    }

    @PostMapping("/{skuId}/add")
    public ResponseEntity<InventoryItem> addStock(
            @PathVariable(name="skuId") String skuId,
            @RequestParam(name="productName") String productName,
            @RequestParam(name="quantity") int quantity) {
        return ResponseEntity.ok(inventoryService.addStock(skuId, productName, quantity));
    }
}
