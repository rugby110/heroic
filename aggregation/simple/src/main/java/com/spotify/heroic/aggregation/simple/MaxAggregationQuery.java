package com.spotify.heroic.aggregation.simple;

import lombok.Data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.spotify.heroic.aggregation.AggregationQuery;

@Data
public class MaxAggregationQuery implements AggregationQuery<MaxAggregation> {
    private final AggregationSamplingQuery sampling;

    @Override
    public MaxAggregation build() {
        return new MaxAggregation(sampling.build());
    }

    @JsonCreator
    public MaxAggregationQuery(@JsonProperty("sampling") AggregationSamplingQuery sampling) {
        this.sampling = Optional.fromNullable(sampling).or(AggregationSamplingQuery.DEFAULT_SUPPLIER);
    }
}