package com.example.licenseplate.service.cache;

import com.example.licenseplate.dto.response.ApplicationDto;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class ApplicationCacheService {

    private final Map<CacheKey, CacheEntry<List<ApplicationDto>>> cache = new ConcurrentHashMap<>();
    private static final long TTL_MINUTES = 5;

    public List<ApplicationDto> get(String status, String region) {
        CacheKey key = new CacheKey(status, region);
        CacheEntry<List<ApplicationDto>> entry = cache.get(key);

        if (entry != null && !entry.isExpired()) {
            List<ApplicationDto> data = entry.getData();
            log.info("Cache HIT for key: {}, data size: {}", key, data != null ? data.size() : 0);
            return data;
        }

        if (entry != null) {
            log.info("Cache expired for key: {}", key);
            cache.remove(key);
        } else {
            log.info("Cache MISS for key: {}", key);
        }

        return Collections.emptyList();
    }

    public void put(String status, String region, List<ApplicationDto> data) {
        if (data == null || data.isEmpty()) {
            return;
        }

        CacheKey key = new CacheKey(status, region);
        cache.put(key, new CacheEntry<>(data));
        log.info("Cache updated for key: {}, data size: {}", key, data.size());
    }

    public void invalidate() {
        cache.clear();
        log.info("Cache fully invalidated.");
    }

    public void invalidateByRegion(String region) {
        cache.keySet().removeIf(key -> key.getRegion().equals(region));
        log.info("Cache invalidated for region: {}", region);
    }

    public void invalidateByStatus(String status) {
        cache.keySet().removeIf(key -> key.getStatus().equals(status));
        log.info("Cache invalidated for status: {}", status);
    }

    private static class CacheKey {
        private final String status;
        private final String region;

        public CacheKey(String status, String region) {
            this.status = status;
            this.region = region;
        }

        public String getStatus() {
            return status;
        }
        public String getRegion() {
            return region;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            CacheKey cacheKey = (CacheKey) o;
            return Objects.equals(status, cacheKey.status) &&
                Objects.equals(region, cacheKey.region);
        }

        @Override
        public int hashCode() {
            return Objects.hash(status, region);
        }

        @Override
        public String toString() {
            return "CacheKey{status='" + status + "', region='" + region + "'}";
        }
    }

    private static class CacheEntry<T> {
        @Getter
        private final T data;
        private final LocalDateTime timestamp;

        public CacheEntry(T data) {
            this.data = data;
            this.timestamp = LocalDateTime.now();
        }

        public boolean isExpired() {
            return timestamp.plusMinutes(TTL_MINUTES).isBefore(LocalDateTime.now());
        }
    }
}