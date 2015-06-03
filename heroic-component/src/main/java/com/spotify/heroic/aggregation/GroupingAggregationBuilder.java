package com.spotify.heroic.aggregation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;

import com.spotify.heroic.grammar.AggregationValue;
import com.spotify.heroic.grammar.ListValue;
import com.spotify.heroic.grammar.Value;

@RequiredArgsConstructor
public abstract class GroupingAggregationBuilder<T> implements AggregationBuilder<T> {
    private final AggregationFactory factory;

    @Override
    public T build(List<Value> args, Map<String, Value> keywords) {
        final List<String> over;
        final Aggregation each;

        if (args.size() > 0) {
            over = convertOver(args.get(0));
        } else {
            over = convertOver(keywords.get("over"));
        }

        if (args.size() > 1) {
            each = convertEach(args.subList(1, args.size()));
        } else {
            each = new ChainAggregation(flatten(keywords.get("each")));
        }

        return build(over, each);
    }

    protected abstract T build(List<String> over, Aggregation each);

    private List<String> convertOver(Value value) {
        if (value == null)
            return null;

        final ListValue list = value.cast(ListValue.class);

        final List<String> over = new ArrayList<>();

        for (final Value v : list.getList()) {
            over.add(v.cast(String.class));
        }

        return over;
    }

    private Aggregation convertEach(List<Value> values) {
        final List<Aggregation> aggregations = new ArrayList<>();

        for (final Value v : values) {
            aggregations.addAll(flatten(v));
        }

        return new ChainAggregation(aggregations);
    }

    private List<Aggregation> flatten(Value v) {
        final List<Aggregation> aggregations = new ArrayList<>();

        if (v == null)
            return aggregations;

        if (v instanceof ListValue) {
            for (final Value item : ((ListValue) v).getList()) {
                aggregations.addAll(flatten(item));
            }
        } else {
            final AggregationValue a = v.cast(AggregationValue.class);
            aggregations.add(factory.build(a.getName(), a.getArguments(), a.getKeywordArguments()));
        }

        return aggregations;
    }
}