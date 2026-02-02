package com.example.stock_backend.config;

import com.mongodb.client.MongoClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

@Configuration
public class MongoTemplateConfig {

    @Bean
    public MongoTemplate mongoTemplate() {
        // 直接指定要使用的資料庫 stockDB
        return new MongoTemplate(MongoClients.create("mongodb://mongo:27017"), "stockDB");
    }
}
