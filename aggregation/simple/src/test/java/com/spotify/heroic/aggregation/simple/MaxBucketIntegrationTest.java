package com.spotify.heroic.aggregation.simple;

import java.util.Collection;
import java.util.function.DoubleBinaryOperator;

import com.google.common.collect.ImmutableList;
import com.spotify.heroic.aggregation.DoubleBucket;
import com.spotify.heroic.metric.Point;

public class MaxBucketIntegrationTest extends ValueBucketIntegrationTest {
    public MaxBucketIntegrationTest() {
        super(Double.NEGATIVE_INFINITY, new DoubleBinaryOperator() {
            @Override
            public double applyAsDouble(double left, double right) {
                return Math.max(left, right);
            }
        });
    }

    @Override
    public Collection<DoubleBucket<Point>> buckets() {
        return ImmutableList.<DoubleBucket<Point>> of(new MaxBucket(0l), new StripedMaxBucket(0l));
    }
}