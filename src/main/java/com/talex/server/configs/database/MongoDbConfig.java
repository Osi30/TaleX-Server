package com.talex.server.configs.database;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(basePackages = "com.talex.server.repositories.mongo")
public class MongoDbConfig {

}