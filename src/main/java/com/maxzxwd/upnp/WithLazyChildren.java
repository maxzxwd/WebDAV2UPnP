package com.maxzxwd.upnp;

import com.maxzxwd.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

public final class WithLazyChildren<T> {

    @NotNull
    private final AtomicReference<CompletableFuture<List<WithLazyChildren<? extends UpnpEntry>>>> ref;

    @NotNull
    public final T obj;

    @Nullable
    private final URI contentDirectoryUri;

    @Nullable
    private final String parentObjectId;

    private WithLazyChildren(@NotNull T parent, @Nullable URI contentDirectoryUri, @Nullable String parentObjectId) {
        this.obj = parent;
        this.contentDirectoryUri = contentDirectoryUri;
        this.parentObjectId = parentObjectId;

        if (contentDirectoryUri == null) {
            ref = new AtomicReference<>(CompletableFuture.completedFuture(List.of()));
        } else {
            ref = new AtomicReference<>(null);
        }
    }

    @NotNull
    public CompletableFuture<List<WithLazyChildren<? extends UpnpEntry>>> traverse(int from, boolean lastRefresh,
                                                                                   @Nullable List<String> path) {
        return traverse(from, path == null ? 0 : path.size(), lastRefresh, path);
    }

    @NotNull
    public CompletableFuture<List<WithLazyChildren<? extends UpnpEntry>>> traverse(int from, int to, boolean lastRefresh,
                                                                                   @Nullable List<String> path) {

        from = Math.max(from, 0);

        if (path == null || to <= from) {
            return this.getChildren(true);
        }

        var current = getChildren(true);

        for (var i = from; i < to; i++) {
            var segment = path.get(i);

            var nextI = i + 1;
            current = current.thenCompose(children -> {

                var child = CollectionUtils.findFirst(children, c ->
                        (nextI == to || c.obj instanceof UpnpContainer) && c.obj.title.equals(segment)
                );

                if (child == null) {
                    return CompletableFuture.failedFuture(new NoSuchElementException("Segment not found: " + segment));
                } else if (child.obj instanceof UpnpItem) {
                    return CompletableFuture.completedFuture(List.of(child));
                } else {
                    return child.getChildren(!lastRefresh || nextI != to);
                }
            });
        }

        return current;
    }

    @NotNull
    public CompletableFuture<List<WithLazyChildren<? extends UpnpEntry>>> getChildren(boolean fromCache) {

        if (contentDirectoryUri == null) {
            return ref.get();
        }

        CompletableFuture<List<WithLazyChildren<? extends UpnpEntry>>> result;

        if (fromCache) {
            result = ref.get();

            if (result == null || result.isCompletedExceptionally()) {

                result = ref.updateAndGet(prev -> {
                    if (prev == null || prev.isCompletedExceptionally()) {
                        return UpnpUtils.browse(contentDirectoryUri, parentObjectId)
                                .thenApply(entries -> CollectionUtils.mapIntoList(entries, this::forEntry));
                    } else {
                        return prev;
                    }
                });
            }
        } else {
            result = ref.updateAndGet(_ -> UpnpUtils.browse(contentDirectoryUri, parentObjectId)
                    .thenApply(entries -> CollectionUtils.mapIntoList(entries, this::forEntry))
            );
        }

        return result;
    }

    @NotNull
    static WithLazyChildren<UpnpDevice> forDevice(@NotNull UpnpDevice device) {
        return new WithLazyChildren<>(device, device.contentDirectoryUri, null);
    }

    @NotNull
    private WithLazyChildren<? extends UpnpEntry> forEntry(@NotNull UpnpEntry entry) {

        switch (entry) {
            case UpnpItem item -> {
                return new WithLazyChildren<>(item, null,null);
            }
            case UpnpContainer container -> {
                return new WithLazyChildren<>(container, contentDirectoryUri, container.id);
            }
        }
    }
}
