package com.fintech.walletservice.config;

import com.fintech.walletservice.sharding.ShardManager;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class DataSourceConfig {

    @Bean
    @ConfigurationProperties("shard0.datasource")
    public DataSourceProperties shard0Properties() {
        return new DataSourceProperties();
    }

    @Bean
    @ConfigurationProperties("shard1.datasource")
    public DataSourceProperties shard1Properties() {
        return new DataSourceProperties();
    }

    @Bean
    @ConfigurationProperties("shard2.datasource")
    public DataSourceProperties shard2Properties() {
        return new DataSourceProperties();
    }

    @Bean
    @ConfigurationProperties("shard3.datasource")
    public DataSourceProperties shard3Properties() {
        return new DataSourceProperties();
    }

    @Bean
    public DataSource shard0() {
        return createDataSource(shard0Properties());
    }

    @Bean
    public DataSource shard1() {
        return createDataSource(shard1Properties());
    }

    @Bean
    public DataSource shard2() {
        return createDataSource(shard2Properties());
    }

    @Bean
    public DataSource shard3() {
        return createDataSource(shard3Properties());
    }

    private DataSource createDataSource(DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    @Bean
    @Primary
    public DataSource routingDataSource(
            @Qualifier("shard0") DataSource shard0,
            @Qualifier("shard1") DataSource shard1,
            @Qualifier("shard2") DataSource shard2,
            @Qualifier("shard3") DataSource shard3,
            ShardManager shardManager) {

        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put(0, shard0);
        targetDataSources.put(1, shard1);
        targetDataSources.put(2, shard2);
        targetDataSources.put(3, shard3);

        shardManager.setDefaultTargetDataSource(shard0);
        shardManager.setTargetDataSources(targetDataSources);

        return new LazyConnectionDataSourceProxy(shardManager);
    }
}