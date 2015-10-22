package com.github.rutledgepaulv.qbuilders.builders;

import com.github.rutledgepaulv.qbuilders.conditions.CompleteCondition;
import com.github.rutledgepaulv.qbuilders.conditions.PartialCondition;
import com.github.rutledgepaulv.qbuilders.nodes.*;
import com.github.rutledgepaulv.qbuilders.operators.ComparisonOperator;
import com.github.rutledgepaulv.qbuilders.properties.concrete.*;
import com.github.rutledgepaulv.qbuilders.properties.virtual.*;
import com.github.rutledgepaulv.qbuilders.utilities.ObjectUtils;
import com.github.rutledgepaulv.qbuilders.utilities.VarArgUtils;
import com.github.rutledgepaulv.qbuilders.visitors.NodeVisitor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("unchecked")
public class QBuilder<T extends QBuilder> implements PartialCondition<T> {

    protected LogicalNode root;
    protected LogicalNode current;

    public QBuilder() {
        root = new OrNode(null, new ArrayList<>());
        current = root;
    }

    public final BooleanProperty<T> bool(String field) {
        return new BooleanPropertyDelegate(field, self());
    }

    public final StringProperty<T> string(String field) {
        return new StringPropertyDelegate(field, self());
    }

    public final ShortProperty<T> shortNum(String field) {
        return new ShortPropertyDelegate(field, self());
    }

    public final IntegerProperty<T> intNum(String field) {
        return new IntegerPropertyDelegate(field, self());
    }

    public final LongProperty<T> longNum(String field) {
        return new LongPropertyDelegate(field, self());
    }

    public final FloatProperty<T> floatNum(String field) {
        return new FloatPropertyDelegate(field, self());
    }

    public final DoubleProperty<T> doubleNum(String field) {
        return new DoublePropertyDelegate(field, self());
    }

    @SafeVarargs
    public final CompleteCondition<T> and(CompleteCondition<T> c1, CompleteCondition<T> c2, CompleteCondition<T>... cn) {
        return and(VarArgUtils.combine(c1, c2, cn));
    }

    @SafeVarargs
    public final CompleteCondition<T> or(CompleteCondition<T> c1, CompleteCondition<T> c2, CompleteCondition<T>... cn) {
        return or(VarArgUtils.combine(c1, c2, cn));
    }

    public final CompleteCondition<T> and(List<CompleteCondition<T>> completeConditions) {
        return combine(completeConditions, AndNode.class);
    }

    public final CompleteCondition<T> or(List<CompleteCondition<T>> completeConditions) {
        return combine(completeConditions, OrNode.class);
    }


    private <S extends LogicalNode> CompleteCondition<T> combine(List<CompleteCondition<T>> conditions, Class<S> type) {

        List<AbstractNode> children = conditions.stream()
                .map(condition -> ((QBuilder<T>) condition).self().current)
                .collect(Collectors.toList());

        S node = ObjectUtils.init(type, self().current, children);
        self().current.getChildren().add(node);

        return new CompleteConditionDelegate(self());
    }

    private CompleteCondition<T> condition(String field, ComparisonOperator operator, Collection<?> values) {
        ComparisonNode node = new ComparisonNode(self().current);

        node.setField(field);
        node.setOperator(operator);
        node.setValues(values);

        self().current.getChildren().add(node);
        return new CompleteConditionDelegate(self());
    }

    protected T self() {
        return (T) this;
    }

    private abstract class Delegate extends QBuilder<T> {
        private T canonical;

        protected Delegate(T canonical) {
            this.canonical = canonical;
        }

        @Override
        protected final T self() {
            return canonical;
        }
    }

    private final class CompleteConditionDelegate extends Delegate implements CompleteCondition<T> {

        protected CompleteConditionDelegate(T canonical) {
            super(canonical);
        }

        public final T and() {
            LogicalNode current = self().current;
            List<AbstractNode> children = new ArrayList<>();
            children.add(current);
            AndNode node = new AndNode(current.getParent(), children);
            if (current == self().root) {
                self().root = node;
            }
            self().current = node;
            return self();
        }

        public final T or() {
            LogicalNode current = self().current;
            List<AbstractNode> children = new ArrayList<>();
            children.add(current);
            OrNode node = new OrNode(current.getParent(), children);
            if (current == self().root) {
                self().root = node;
            }
            self().current = node;
            return self();
        }

        public final <Q> Q query(NodeVisitor<Q> visitor) {
            return self().root.visit(visitor);
        }

    }

    private abstract class PropertyDelegate extends Delegate implements Property<T> {

        private String field;

        public PropertyDelegate(String field, T canonical) {
            super(canonical);
            this.field = field;
        }

        protected final String getField() {
            return field;
        }

    }

    private abstract class ExistentialPropertyDelegate extends PropertyDelegate implements ExistentialProperty<T> {

        protected ExistentialPropertyDelegate(String field, T canonical) {
            super(field, canonical);
        }

        public final CompleteCondition<T> exists() {
            return condition(getField(), ComparisonOperator.EX, Collections.singletonList(true));
        }

        public final CompleteCondition<T> doesNotExist() {
            return condition(getField(), ComparisonOperator.EX, Collections.singletonList(false));
        }

    }

    private abstract class EquitablePropertyDelegate<S> extends ExistentialPropertyDelegate
            implements EquitableProperty<T, S> {

        protected EquitablePropertyDelegate(String field, T canonical) {
            super(field, canonical);
        }

        public final CompleteCondition<T> eq(S value) {
            return condition(getField(), ComparisonOperator.EQ, Collections.singletonList(value));
        }

        public final CompleteCondition<T> ne(S value) {
            return condition(getField(), ComparisonOperator.NE, Collections.singletonList(value));
        }
    }

    private abstract class ListablePropertyDelegate<S> extends EquitablePropertyDelegate<S>
            implements ListableProperty<T, S> {

        protected ListablePropertyDelegate(String field, T canonical) {
            super(field, canonical);
        }

        public final CompleteCondition<T> in(S... values) {
            return condition(getField(), ComparisonOperator.IN, VarArgUtils.list(values));
        }

        public final CompleteCondition<T> in(Collection<S> values) {
            return condition(getField(), ComparisonOperator.IN, values);
        }

        public final CompleteCondition<T> nin(S... values) {
            return condition(getField(), ComparisonOperator.NIN, VarArgUtils.list(values));
        }

        public final CompleteCondition<T> nin(Collection<S> values) {
            return condition(getField(), ComparisonOperator.NIN, values);
        }
    }

    private abstract class NumberPropertyDelegate<S extends Number> extends ListablePropertyDelegate<S>
            implements NumberProperty<T, S> {

        protected NumberPropertyDelegate(String field, T canonical) {
            super(field, canonical);
        }

        public final CompleteCondition<T> gt(S number) {
            return condition(getField(), ComparisonOperator.GT, Collections.singletonList(number));
        }

        public final CompleteCondition<T> lt(S number) {
            return condition(getField(), ComparisonOperator.LT, Collections.singletonList(number));
        }

        public final CompleteCondition<T> gte(S number) {
            return condition(getField(), ComparisonOperator.GTE, Collections.singletonList(number));
        }

        public final CompleteCondition<T> lte(S number) {
            return condition(getField(), ComparisonOperator.LTE, Collections.singletonList(number));
        }

    }

    private final class BooleanPropertyDelegate extends ExistentialPropertyDelegate implements BooleanProperty<T> {

        protected BooleanPropertyDelegate(String field, T canonical) {
            super(field, canonical);
        }

        public final CompleteCondition<T> isTrue() {
            return condition(getField(), ComparisonOperator.EQ, Collections.singletonList(true));
        }

        public final CompleteCondition<T> isFalse() {
            return condition(getField(), ComparisonOperator.EQ, Collections.singletonList(false));
        }

    }

    private final class ShortPropertyDelegate extends NumberPropertyDelegate<Short> implements ShortProperty<T> {

        protected ShortPropertyDelegate(String field, T canonical) {
            super(field, canonical);
        }

    }

    private final class IntegerPropertyDelegate extends NumberPropertyDelegate<Integer> implements IntegerProperty<T> {

        protected IntegerPropertyDelegate(String field, T canonical) {
            super(field, canonical);
        }

    }

    private final class LongPropertyDelegate extends NumberPropertyDelegate<Long> implements LongProperty<T> {

        protected LongPropertyDelegate(String field, T canonical) {
            super(field, canonical);
        }

    }

    private final class FloatPropertyDelegate extends NumberPropertyDelegate<Float> implements FloatProperty<T> {

        protected FloatPropertyDelegate(String field, T canonical) {
            super(field, canonical);
        }

    }

    private final class DoublePropertyDelegate extends NumberPropertyDelegate<Double> implements DoubleProperty<T> {

        protected DoublePropertyDelegate(String field, T canonical) {
            super(field, canonical);
        }

    }

    private final class StringPropertyDelegate extends ListablePropertyDelegate<String> implements StringProperty<T> {

        protected StringPropertyDelegate(String field, T canonical) {
            super(field, canonical);
        }

        public final CompleteCondition<T> lexicallyAfter(String value) {
            return condition(getField(), ComparisonOperator.GT, Collections.singletonList(value));
        }

        public final CompleteCondition<T> lexicallyBefore(String value) {
            return condition(getField(), ComparisonOperator.LT, Collections.singletonList(value));
        }

    }

}
