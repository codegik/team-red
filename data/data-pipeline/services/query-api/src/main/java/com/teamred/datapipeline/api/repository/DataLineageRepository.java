package com.teamred.datapipeline.api.repository;

import com.teamred.datapipeline.api.entity.DataLineage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DataLineageRepository extends JpaRepository<DataLineage, String> {

    List<DataLineage> findBySaleId(String saleId);
}
