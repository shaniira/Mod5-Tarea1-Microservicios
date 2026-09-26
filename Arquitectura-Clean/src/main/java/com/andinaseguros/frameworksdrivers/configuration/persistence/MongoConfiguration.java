package com.andinaseguros.frameworksdrivers.configuration.persistence;

import com.andinaseguros.interfaceadapters.out.persistence.mongodb.repository.SpringDataClienteMongoRepository;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(basePackageClasses = SpringDataClienteMongoRepository.class)
public class MongoConfiguration {}
