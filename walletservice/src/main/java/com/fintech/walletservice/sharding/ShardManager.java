package com.fintech.walletservice.sharding;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class ShardManager extends AbstractRoutingDataSource {

    private final ConsistentHashing<Integer> consistentHashing;
    private final Map<Integer, DataSource> shardDataSources;
    private static final ThreadLocal<String> currentShardKey = new ThreadLocal<>();

    public ShardManager(
            @Qualifier("shard0") DataSource shard0,
            @Qualifier("shard1") DataSource shard1,
            @Qualifier("shard2") DataSource shard2,
            @Qualifier("shard3") DataSource shard3,
            ConsistentHashing<Integer> consistentHashing) {

        this.consistentHashing = consistentHashing;
        this.shardDataSources = new HashMap<>();

        shardDataSources.put(0, shard0);
        shardDataSources.put(1, shard1);
        shardDataSources.put(2, shard2);
        shardDataSources.put(3, shard3);

        consistentHashing.addShards(shardDataSources.keySet());

        Map<Object, Object> targetDataSources = new HashMap<>();
        shardDataSources.forEach(targetDataSources::put);

        setDefaultTargetDataSource(shard0);
        setTargetDataSources(targetDataSources);
    }

    public static void setShardKey(UUID userId) {
        currentShardKey.set(userId.toString());
    }

    public static void setShardKey(String key) {
        currentShardKey.set(key);
    }

    public static void clearShardKey() {
        currentShardKey.remove();
    }

    @Override
    protected Object determineCurrentLookupKey() {
        String key = currentShardKey.get();
        if (key != null) {
            Integer shardIndex = consistentHashing.getShardIndex(UUID.fromString(key));
            return shardIndex;
        }
        return null;
    }
}