package com.retailops.retailops_ai.repository;
import com.retailops.retailops_ai.entity.SalesData;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SalesDataRepository extends JpaRepository<SalesData,Long>{

	@Query(value = """
			select latest.id, latest.date, latest.store_id, latest.product_id, latest.category, latest.region,
			       latest.inventory_level, latest.units_sold, latest.units_ordered, latest.price, latest.discount,
			       latest.promotion, latest.demand, latest.weather_condition, latest.seasonality,
			       latest.epidemic, latest.competitor_pricing
			from (
				select sale.*,
				       row_number() over (
				           partition by sale.store_id, sale.product_id
				           order by sale.date desc, sale.id desc
				       ) as row_num
				from sales_data sale
			) latest
			where latest.row_num = 1
			order by latest.store_id, latest.product_id
			""", nativeQuery = true)
	List<SalesData> findLatestByStoreAndProduct();

	Optional<SalesData> findFirstByStoreIdAndProductIdOrderByDateDescIdDesc(String storeId, String productId);
}
