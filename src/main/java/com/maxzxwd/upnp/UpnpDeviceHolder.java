package com.maxzxwd.upnp;

import com.maxzxwd.SsdpUtils;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public final class UpnpDeviceHolder {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .build();

    private static final AtomicReference<Flux<WithLazyChildren<UpnpDevice>>> ref = new AtomicReference<>(null);

    private UpnpDeviceHolder() {}

    @NotNull
    public static Flux<HttpResponse<InputStream>> loadRaw() {

        return SsdpUtils.discover()
                .flatMap(uri -> {
                    var request = HttpRequest.newBuilder()
                            .uri(uri)
                            .GET()
                            .build();

                    return Mono.fromFuture(HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream()));
                })
                .filter(rs -> rs.statusCode() == 200);
    }

    @NotNull
    public static Flux<WithLazyChildren<UpnpDevice>> getList() {
        return getList(true);
    }

    @NotNull
    public static Flux<WithLazyChildren<UpnpDevice>> getList(boolean fromCache) {

        Flux<WithLazyChildren<UpnpDevice>> result;

        if (fromCache) {
            result = ref.get();

            if (result == null) {

                result = ref.updateAndGet(prev -> Objects.requireNonNullElseGet(
                        prev,
                        () -> loadRaw().mapNotNull(UpnpUtils::extractDevice).map(WithLazyChildren::forDevice).cache())
                );
            }
        } else {
            result = ref.updateAndGet(_ -> loadRaw().mapNotNull(UpnpUtils::extractDevice).map(WithLazyChildren::forDevice).cache());
        }

        return result;
    }
}
