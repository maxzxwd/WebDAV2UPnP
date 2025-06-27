package com.maxzxwd;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

public final class CollectionUtils {

    private CollectionUtils() {}

    @NotNull
    public static <T, R> List<R> mapIntoList(
            @Nullable
            Collection<? extends T> source,

            @NotNull
            Function<? super T, ? extends R> mapper
    ) {
        var newSize = source == null ? 0 : source.size();
        return newSize == 0
                ? List.of()
                : Collections.unmodifiableList(mapInto(source, mapper, new ArrayList<R>(newSize)));
    }

    @NotNull
    public static <T, R, C extends Collection<R>> C mapInto(
            @Nullable Iterable<? extends T> source,
            @NotNull Function<? super T, ? extends R> mapper,
            @NotNull C destination
    ) {
        if (source != null) {
            for (T item : source) {
                destination.add(mapper.apply(item));
            }
        }
        return destination;
    }

    @Nullable
    public static <T> T findFirst(
            @Nullable Iterable<? extends T> source,
            @NotNull Predicate<? super T> predicate
    ) {
        if (source != null) {
            for (T item : source) {
                if (predicate.test(item)) {
                    return item;
                }
            }
        }

        return null;
    }
}
