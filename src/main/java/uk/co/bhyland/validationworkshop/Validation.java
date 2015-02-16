package uk.co.bhyland.validationworkshop;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * The Validation class represents a disjoint union (aka sum type) of either
 *      a (non-empty) list of error values of type E, or
 *      a success value of type T
 *
 * The internal implementation should not be observable to client code except via the Validation public api.
 */
public abstract class Validation<E, T> {

    /*
    Implement the following to make the tests pass.
    Feel free to add to or change anything about the implementation, so long as the tests still pass and the intent of the class remains.
    */
    
    private Validation() {}

    private static class Success<E, T> extends Validation<E, T>
    {
        private final T value;

        public Success(T value)
        {
            this.value = value;
        }
        
        @Override
        public <U> U fold(Function<List<E>, U> ifFailure, Function<T, U> ifSuccess)
        {
            return ifSuccess.apply(value);
        }

        @Override
        public String toString()
        {
            return "Success [value=" + value + "]";
        }
    }
    
    private static class Failure<E, T> extends Validation<E, T>
    {
        private final List<E> failures;

        public Failure(List<E> failures)
        {
            this.failures = failures;
        }
        
        @Override
        public <U> U fold(Function<List<E>, U> ifFailure, Function<T, U> ifSuccess)
        {
            return ifFailure.apply(failures);
        }

        @Override
        public String toString()
        {
            return "Failure [failures=" + failures + "]";
        }
    }

    /**
     * Produce a Validation representing the success value of type T.
     */
    public static <E, T> Validation<E, T> success(final T value) {
        return new Success<E, T>(value);
    }

    /**
     * Produce a Validation representing the list of error values of type E.
     */
    @SafeVarargs
    public static <E, T> Validation<E, T> failure(final E value, final E... values) {
        return new Failure<>(makeNonEmptyList(value, values));
    }
    
    private static <E, T> Validation<E, T> failure(List<E> errors) {
        return new Failure<>(errors);
    }

    private <U> Validation<E, U> makeFailure(List<E> errors)
    {
        return Validation.<E, U>failure(errors);
    }

    private <U> Validation<E, U> makeSuccess(U value)
    {
        return success(value);
    }
    
    /**
     * Convenience to test what this Validation represents while ignoring its contained value(s).
     */
    public final boolean isSuccess()
    {
        return fold(es -> false, s -> true);
    }

    /**
     * Convenience to test what this Validation represents while ignoring its contained value(s).
     */
    public final boolean isFailure()
    {
        return !isSuccess();
    }

    // You may find this utility function useful
    @SafeVarargs
    protected static <A> List<A> makeNonEmptyList(final A a, final A... others) {
        final List<A> list = new ArrayList<>();
        list.add(a);
        list.addAll(Arrays.asList(others));
        return list;
    }

    // You may find this utility function useful
    protected static <A> A getHeadOfList(final List<A> nonEmptyList) {
        return nonEmptyList.get(0);
    }

    // You may find this utility function useful
    protected static <A> A[] getTailOfList(final List<A> nonEmptyList)
    {
        @SuppressWarnings("unchecked")
        final A[] tail = (A[]) new Object[nonEmptyList.size() - 1];
        for (int i = 1; i < nonEmptyList.size(); i++)
        {
            tail[i - 1] = nonEmptyList.get(i);
        }
        return tail;
    }

    // You may find this utility function useful
    @SafeVarargs
    protected static <A> List<A> combineLists(final List<A> first, final List<A> second, final List<A>... rest)
    {
        final List<A> combined = new ArrayList<>();
        combined.addAll(first);
        combined.addAll(second);

        for (List<A> as : rest)
        {
            combined.addAll(as);
        }

        return combined;
    }

    /**
     * Return the success value of this Validation, if it represents a success; otherwise, return the supplied default value.
     * The given supplier should only be invoked if its return value is required.
     */
    public final T getOrElse(final Supplier<T> defaultValue)
    {
        return fold(
            es -> defaultValue.get(),
            s -> s
        );
    }

    /**
     * Return this Validation, if it represents a success; otherwise, return the supplied default Validation.
     * The given supplier should only be invoked if its return value is required.
     */
    public final Validation<E, T> orElse(Supplier<Validation<E, T>> defaultValue)
    {
        return fold(
            es -> defaultValue.get(),
            s -> this
        );
    }

    /**
     * Run exactly one of the two provided functions, depending on what this Validation represents.
     * Return the result of the selected function.
     *
     * Hint for later tasks:
     *      Many things can be implemented in terms of fold.
     *      What procedural code structure is represented by fold?
     */

    public abstract <U> U fold(final Function<List<E>, U> ifFailure, final Function<T, U> ifSuccess);

    @SafeVarargs
    public static <E> void ifFailure(
        final Consumer<List<E>> ifFailure, 
        Validation<E, ?> validation,
        Validation<E, ?>... moreValidations)
    {
        validation.ifFailure(ifFailure);
        
        for (Validation<E, ?> v : moreValidations)
        {
            v.ifFailure(ifFailure);
        }
    }
    
    @SafeVarargs
    public static <E> List<E> reduceErrors(
        List<E> allErrors, 
        Validation<E, ?> validation,
        Validation<E, ?>... moreValidations)
    {
        ifFailure((es) -> allErrors.addAll(es), validation, moreValidations);
        return allErrors;
    }

    /**
     * Convenience to perform an effect using the success value if this Validation represents a success.
     * The consumer should not be invoked if this Validation represents a failure.
     *
     * Not tested: implementing this is optional.
     */
    public final void ifSuccess(final Consumer<T> ifSuccess)
    {
        fold(
            es -> null,
            s -> { ifSuccess.accept(s); return null; }
        );
    }

    /**
     * Convenience to perform an effect using the failure values if this Validation represents a failure.
     * The consumer should not be invoked if this Validation represents a success.
     *
     * Not tested: implementing this is optional.
     */
    public final void ifFailure(final Consumer<List<E>> ifFailure)
    {
        fold(
            es -> { ifFailure.accept(es); return null; },
            s -> null
        );
    }

    /**
     * If this Validation represents a success, return a Validation containing the success value transformed by the given function;
     * otherwise, return a Validation containing the error values.
     * The given function should only be invoked if its return value is required.
     */
    public <U> Validation<E, U> map(final Function<T, U> f) {
        
        return fold(
            es -> this.<U>makeFailure(es),
            t -> makeSuccess(f.apply(t))
        );
    }

    /**
     * If this Validation represents a success, return the success value transformed to a Validation by the given function;
     * otherwise, return a Validation containing the error values.
     * The given function should only be invoked if its return value is required.
     */
    public <U> Validation<E, U> flatMap(final Function<T, Validation<E, U>> f) {
        
        return fold(
            es -> this.<U>makeFailure(es),
            t -> f.apply(t)
        );
    }

    /**
     * Specialisation of List.map() that transforms the given list of inputs into a list of Validations by applying the given function.
     * Unfortunately, List.map() doesn't actually exist, although Stream.map() does.
     * You won't miss much if you skip this, but it is tested and is used in some of the example code.
     */
    public static <E, A, B> List<Validation<E, B>> mapInputs(final List<A> inputs, final Function<A, Validation<E, B>> f) {
        return inputs.stream().map(f).collect(Collectors.toList());
    }

    /**
     * Specialisation of List.flatMap() that transforms the given list of Validations into a new list of Validations by applying the given function.
     * Unfortunately, List.flatMap() doesn't actually exist, although Stream.flatMap() does.
     * You won't miss much if you skip this, but it is tested and is used in some of the example code.
     */
    public static <E, A, B> List<Validation<E, B>> flatMapInputs(final List<Validation<E, A>> inputs, final Function<A, Validation<E, B>> f) {        
        return inputs.stream().map((v) -> v.flatMap(f)).collect(Collectors.toList());
    }

    /**
     * If both this Validation and the given Validation represent successes,
     * return a Validation containing the success value of this Validation transformed by the success value of the given Validation;
     * otherwise, return a Validation containing the all the available error values, and do not invoke the function that might be contained in the argument.
     *
     * Hint for later tasks:
     *      Many things can be implemented in terms of fold and apply.
     *      Consider making Validation.apply() final before continuing, as this should help make its structure clear.
     */
    public final <U> Validation<E, U> apply(final Validation<E, Function<T, U>> functionValidation) {
        return fold(
            errors -> functionValidation.fold(
                errors2 -> this.<U>makeFailure(combineLists(errors, errors2)),
                func -> this.<U>makeFailure(errors)
            ),
            value -> functionValidation.fold(
                errors -> this.<U>makeFailure(errors),
                func -> makeSuccess(func.apply(value))
            ));
    }

    /**
     * Transform the given list of Validations into a Validation of a List.
     * If any of the given Validations represented error values, then the resulting Validation contains all available errors;
     * otherwise, it contains a List of all available success values.
     * The given list should be considered in order.
     */
    public static <E, T> Validation<E, List<T>> sequence(final List<Validation<E, T>> validations) {
        
        List<E> errors = new ArrayList<>();
        List<T> values = new ArrayList<>();
        
        validations.forEach(validation ->
        {
            validation.ifSuccess(t -> values.add(t));
            validation.ifFailure(es -> errors.addAll(es));
        });
        
        if (errors.isEmpty())
        {
            return success(values);
        }
        else
        {
            return failure(errors);
        }
    }

    /**
     * Transform the given list of Validations into a Validation of a List.
     * If any of the given Validations represented error values, then the resulting Validation contains all available errors;
     * otherwise, it contains a List of all available success values, each transformed by the given function.
     * The given list should be considered in order.
     * Once it can be determined that the return value will represent a failure, no further invocations of the given function should occur.
     */
    public static <E, T, R> Validation<E, List<R>> traverse(final List<Validation<E, T>> validations, final Function<T, R> f) {
        
        Validation<E, List<T>> sequence = sequence(validations);
        
        return sequence.fold(
            es -> Validation.<E,List<R>>failure(es),
            l -> Validation.<E,List<R>>success(l.stream().map(f).collect(Collectors.toList()))
        );
    }

    /**
     * If any of the given Validations represented error values, then the resulting Validation contains all available errors;
     * otherwise, it contains the result of transforming the success values of the given Validations with the given curried function.
     */
    public static <E, A, B, R> Validation<E, R> map2(final Validation<E, A> va, final Validation<E, B> vb,
                                                     final Function<A, Function<B, R>> f) {
        
        return va.fold(
            errors -> Validation.<E,R>failure(reduceErrors(new ArrayList<>(), va, vb)),
            a -> vb.map(f.apply(a))
        );
    }

    /**
     * If any of the given Validations represented error values, then the resulting Validation contains all available errors;
     * otherwise, it contains the result of transforming the success values of the given Validations with the given curried function.
     */
    public static <E, A, B, C, R> Validation<E, R> map3(final Validation<E, A> va, final Validation<E, B> vb, final Validation<E, C> vc,
                                                        final Function<A, Function<B, Function<C, R>>> f) {
        return va.fold(
            errors -> Validation.<E,R>failure(reduceErrors(new ArrayList<>(), va, vb, vc)),
            a -> map2(vb, vc, f.apply(a))
        );
    }

    /**
     * If any of the given Validations represented error values, then the resulting Validation contains all available errors;
     * otherwise, it contains the result of transforming the success values of the given Validations with the given curried function.
     *
     * Not tested: implementing this is optional.
     */
    public static <E, A, B, C, D, R> Validation<E, R> map4(final Validation<E, A> va, final Validation<E, B> vb, final Validation<E, C> vc, final Validation<E, D> vd,
                                                           final Function<A, Function<B, Function<C, Function<D, R>>>> f) {
        return va.fold(
            errors -> Validation.<E,R>failure(reduceErrors(new ArrayList<>(), va, vb, vc, vd)),
            a -> map3(vb, vc, vd, f.apply(a))
        );
    }
}
