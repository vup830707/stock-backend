package com.example.stock_backend.repository;

import com.example.stock_backend.model.Company;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface CompanyRepository extends MongoRepository<Company, String> {
    Optional<Company> findByStockNo(String stockNo);
}
