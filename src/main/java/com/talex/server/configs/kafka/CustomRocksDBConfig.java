package com.talex.server.configs.kafka;

import org.apache.kafka.streams.state.RocksDBConfigSetter;
import org.rocksdb.BlockBasedTableConfig;
import org.rocksdb.LRUCache;
import org.rocksdb.Options;

import java.util.Map;

public class CustomRocksDBConfig implements RocksDBConfigSetter {
    @Override
    public void setConfig(String storeName, Options options, Map<String, Object> configs) {
        BlockBasedTableConfig tableConfig = new BlockBasedTableConfig();

        // Giới hạn Block Cache của RocksDB chỉ được ăn tối đa 16MB RAM
        long cacheSizeInBytes = 50 * 1024 * 1024L;
        tableConfig.setBlockCache(new LRUCache(cacheSizeInBytes));

        options.setTableFormatConfig(tableConfig);
        // Giới hạn kích thước ghi tối đa
        options.setMaxWriteBufferNumber(2);
        options.setWriteBufferSize(8 * 1024 * 1024L); // 8MB
    }

    @Override
    public void close(String storeName, Options options) {
    }
}