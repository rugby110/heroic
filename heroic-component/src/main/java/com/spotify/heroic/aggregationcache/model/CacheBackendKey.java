package com.spotify.heroic.aggregationcache.model;

import java.util.Map;

import lombok.Data;

import com.spotify.heroic.aggregation.Aggregation;
import com.spotify.heroic.filter.Filter;

@Data
public class CacheBackendKey {
    /**
     * Which filter was used to query the specified data.
     */
    private final Filter filter;

    /**
     * Which group this result belongs to.
     */
    private final Map<String, String> group;

    /**
     * Always includes sampling.
     */
    private final Aggregation aggregation;
}