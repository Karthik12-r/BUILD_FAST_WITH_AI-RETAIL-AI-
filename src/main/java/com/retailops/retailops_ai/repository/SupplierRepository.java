package com.retailops.retailops_ai.repository;

import com.retailops.retailops_ai.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SupplierRepository extends JpaRepository<Supplier, String> {

	@Query("select supplier from Supplier supplier where supplier.productId = :productId order by supplier.supplierId")
	List<Supplier> findAllByProductId(String productId);
}