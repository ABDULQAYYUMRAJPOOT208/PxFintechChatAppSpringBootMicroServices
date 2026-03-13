package com.fintech.walletservice.sharding;

import com.google.common.hash.Hashing;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;

@Component
public class ConsistentHashing<T> {

    private final int numberOfShards;
    private final int virtualNodes;
    private final SortedMap<Long, T> circle = new TreeMap<>();

    public ConsistentHashing(
            @Value("${sharding.number-of-shards:4}") int numberOfShards,
            @Value("${sharding.virtual-nodes:100}") int virtualNodes) {
        this.numberOfShards = numberOfShards;
        this.virtualNodes = virtualNodes;
    }

    public void addShard(T shard) {
        for (int i = 0; i < virtualNodes; i++) {
            long hash = hash(shard.toString() + i);
            circle.put(hash, shard);
        }
    }

    public void addShards(Collection<T> shards) {
        for (T shard : shards) {
            addShard(shard);
        }
    }

    public T getShard(String key) {
        if (circle.isEmpty()) {
            return null;
        }

        long hash = hash(key);
        if (!circle.containsKey(hash)) {
            SortedMap<Long, T> tailMap = circle.tailMap(hash);
            hash = tailMap.isEmpty() ? circle.firstKey() : tailMap.firstKey();
        }

        return circle.get(hash);
    }

    public T getShard(UUID userId) {
        return getShard(userId.toString());
    }

    public int getShardIndex(UUID userId) {
        long hash = hash(userId.toString());
        return (int) (hash % numberOfShards);
    }

    private long hash(String key) {
        return Hashing.murmur3_128().hashString(key, StandardCharsets.UTF_8).asLong();
    }
}