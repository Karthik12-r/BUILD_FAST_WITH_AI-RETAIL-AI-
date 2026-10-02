package com.retailops.retailops_ai.repository;

import com.retailops.retailops_ai.entity.ManagerApproval;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ManagerApprovalRepository extends JpaRepository<ManagerApproval, String> {

	List<ManagerApproval> findByStatusIgnoreCase(String status);

	long countByStatusIgnoreCase(String status);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select approval from ManagerApproval approval where approval.actionId = :actionId")
	Optional<ManagerApproval> findByActionIdForUpdate(@Param("actionId") String actionId);
}