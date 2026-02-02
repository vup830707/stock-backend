package com.example.stock_backend.service;

import com.example.stock_backend.model.Company;
import com.example.stock_backend.repository.CompanyRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CompanyInitializer implements CommandLineRunner {

    private final CompanyRepository companyRepository;

    public CompanyInitializer(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        // 如果已經有資料就略過
        if (companyRepository.count() > 0) {
            System.out.println("Company collection 已存在資料，略過初始化");
            return;
        }

        List<Company> taiwan50 = List.of(
                new Company("2330", "台積電"),
                new Company("2317", "鴻海"),
                new Company("2412", "中華電"),
                new Company("6505", "台塑化"),
                new Company("1301", "台塑"),
                new Company("1303", "南亞"),
                new Company("1326", "台化"),
                new Company("2308", "台達電"),
                new Company("2881", "富邦金"),
                new Company("2882", "國泰金"),
                new Company("2883", "開發金"),
                new Company("2884", "玉山金"),
                new Company("2885", "元大金"),
                new Company("2886", "兆豐金"),
                new Company("2887", "台新金"),
                new Company("2888", "新光金"),
                new Company("2889", "國票金"),
                new Company("2890", "永豐金"),
                new Company("2891", "中信金"),
                new Company("2892", "第一金"),
                new Company("2897", "王道銀行"),   // ← 修正
                new Company("2303", "聯電"),
                new Company("2002", "中鋼"),
                new Company("1101", "台泥"),
                new Company("1216", "統一"),
                new Company("1304", "台聚"),       // ← 修正
                new Company("1324", "地球"),       // ← 修正
                new Company("1402", "遠東新"),
                new Company("2006", "東和鋼鐵"),
                new Company("2201", "裕隆"),
                new Company("2301", "光寶科"),
                new Company("2324", "仁寶"),
                new Company("2337", "旺宏"),
                new Company("2357", "華碩"),
                new Company("2382", "廣達"),
                new Company("2408", "南亞科"),
                new Company("2603", "長榮"),
                new Company("2609", "陽明"),
                new Company("2801", "彰銀"),
                new Company("2823", "中壽"),
                new Company("2880", "華南金"),
                new Company("9910", "豐泰")
        );

        companyRepository.saveAll(taiwan50);
        System.out.println("已初始化台灣50公司資料到 company collection");
    }
}
