package com.anjhana.repository;

import com.anjhana.model.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<InventoryItem, String> {

    // Pessimistic write lock — prevents concurrent over-reservation
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM InventoryItem i WHERE i.skuId = :skuId")
    Optional<InventoryItem> findBySkuIdWithLock(@Param("skuId") String skuId);
}
