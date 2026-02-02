package com.example.stock_backend.controller;

import com.example.stock_backend.model.Company;
import com.example.stock_backend.repository.CompanyRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/company")
@CrossOrigin(origins = "*")
public class CompanyController {

    private final CompanyRepository repo;

    public CompanyController(CompanyRepository repo) {
        this.repo = repo;
    }

    // 取得所有公司
    @GetMapping("/all")
    public List<Company> getAllCompanies() {
        return repo.findAll();
    }
}
