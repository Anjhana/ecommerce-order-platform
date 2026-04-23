package com.anjhana.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "inventory_items")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InventoryItem {

    @Id
    private String skuId;

    @Column(nullable = false)
    private String productName;

    @Column(nullable = false)
    private Integer availableQuantity;

    @Column(nullable = false)
    private Integer reservedQuantity;

    public boolean canReserve(int qty) {
        return availableQuantity - reservedQuantity >= qty;
    }

    public void reserve(int qty) {
        if (!canReserve(qty)) throw new IllegalStateException("Insufficient stock for SKU: " + skuId);
        this.reservedQuantity += qty;
    }

    public void release(int qty) {
        this.reservedQuantity = Math.max(0, this.reservedQuantity - qty);
    }

    public int getAvailableStock() {
        return availableQuantity - reservedQuantity;
    }
}
