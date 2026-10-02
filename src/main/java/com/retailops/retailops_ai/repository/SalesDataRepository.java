package com.retailops.retailops_ai.repository;
import com.retailops.retailops_ai.entity.SalesData;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SalesDataRepository extends JpaRepository<SalesData,Long>{

	@Query("""
			select sale from SalesData sale
			where not exists (
				select newer.id from SalesData newer
				where newer.storeId = sale.storeId
				  and newer.productId = sale.productId
				  and (newer.date > sale.date or (newer.date = sale.date and newer.id > sale.id))
			)
			order by sale.storeId, sale.productId
			""")
	List<SalesData> findLatestByStoreAndProduct();
}
