package com.maxzxwd;

import com.maxzxwd.upnp.UpnpContainer;
import com.maxzxwd.upnp.UpnpDeviceHolder;
import com.maxzxwd.upnp.UpnpItem;
import com.maxzxwd.upnp.UpnpUtils;
import com.maxzxwd.upnp.renderer.HtmlRenderer;
import com.maxzxwd.upnp.renderer.Renderer;
import com.maxzxwd.upnp.renderer.WebdavRenderer;
import io.javalin.Javalin;
import io.javalin.http.*;
import io.javalin.router.JavalinDefaultRouting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class Main {

    private static final String TEXT_XML_UTF8 = "text/xml; charset=\"utf-8\"";
    private static final String TEXT_HTML_UTF8 = "text/html; charset=\"utf-8\"";
    private static final Renderer HTML_RENDERER = new HtmlRenderer();
    private static final Renderer WEBDAV_RENDERER = new WebdavRenderer();

    public static void main(@NotNull String... args) {
        Javalin.create(config -> {
            config.showJavalinBanner = false;
            config.router.mount(Main::configureRouter);
        }).start(57416);
    }

    @Nullable
    private static URI decodeUriFromBase64(@NotNull String base64) {

        try {
            return new URI(new String(Base64.getUrlDecoder().decode(base64), StandardCharsets.UTF_8));
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void writeUtf8RawResponse(@NotNull Context ctx, @NotNull String text, boolean flush) {
        writeRawResponse(ctx, text.getBytes(StandardCharsets.UTF_8), flush, false);
    }

    private static void writeRawResponse(@NotNull Context ctx, @NotNull byte[] bytes, boolean flush, boolean close) {
        try {
            ctx.res().getOutputStream().write(bytes);

            if (flush || close) {
                ctx.res().flushBuffer();
            }

            if (close) {
                ctx.res().getOutputStream().close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    private static CompletableFuture<?> handleWebDavRoot(@NotNull Context ctx) {

        ctx.status(HttpStatus.MULTI_STATUS).contentType(TEXT_XML_UTF8);
        writeRawResponse(ctx, WEBDAV_RENDERER.renderStart(), false, false);

        return UpnpDeviceHolder.getList(false)
                .map(device -> WEBDAV_RENDERER.render(device.obj))
                .doOnNext(s -> writeUtf8RawResponse(ctx, s, true))
                .count()
                .subscribeOn(Schedulers.boundedElastic())
                .toFuture()
                .thenAccept(count -> {
                    if (count == 0) {
                        writeUtf8RawResponse(ctx, WEBDAV_RENDERER.render(UpnpContainer.EMPTY_CONTAINER), false);
                    }
                    writeRawResponse(ctx, WEBDAV_RENDERER.renderEnd(), true, true);
                });
    }

    @NotNull
    private static CompletableFuture<Void> handleWebDavPath(@NotNull Context ctx, @NotNull List<String> path) {

        var deviceName = path.getFirst();

        return UpnpDeviceHolder.getList()
                .filter(di -> deviceName.equals(di.obj.name))
                .next()
                .subscribeOn(Schedulers.boundedElastic())
                .toFuture()
                .thenCompose(device -> {
                    if (device == null) {
                        ctx.status(HttpStatus.NOT_FOUND);
                        return CompletableFuture.completedFuture(List.of());
                    } else {
                        ctx.status(HttpStatus.MULTI_STATUS).contentType(TEXT_XML_UTF8);
                        writeRawResponse(ctx, WEBDAV_RENDERER.renderStart(), false, false);
                        return device.traverse(1, true, path);
                    }
                })
                .thenAccept(es -> {
                    if (es != null) {

                        if (es.isEmpty()) {
                            writeUtf8RawResponse(ctx, WEBDAV_RENDERER.render(UpnpContainer.EMPTY_CONTAINER), false);
                        } else {
                            for (var e : es) {
                                writeUtf8RawResponse(ctx, WEBDAV_RENDERER.render(e.obj), true);
                            }
                        }

                        writeRawResponse(ctx, WEBDAV_RENDERER.renderEnd(), true, true);
                    }
                });
    }

    @NotNull
    private static CompletableFuture<Void> handleWebDavGet(@NotNull Context ctx) {

        var path = StringPathUtils.extractUnescapedSegments(ctx.path());
        var deviceName = path.getFirst();
        var lastPathSegment = path.getLast();

        return UpnpDeviceHolder.getList()
                .filter(di -> deviceName.equals(di.obj.name))
                .next()
                .subscribeOn(Schedulers.boundedElastic())
                .toFuture()
                .thenCompose(device -> {
                    if (device == null) {
                        ctx.status(HttpStatus.NOT_FOUND);
                        return CompletableFuture.completedFuture(List.of());
                    } else {
                        return device.traverse(1, path.size() - 1, false, path);
                    }
                }).thenAccept(entries -> {
                    for (var entry : entries) {
                        if (entry.obj instanceof UpnpItem i && i.title.equals(lastPathSegment)) {
                            ctx.redirect(i.url, HttpStatus.MOVED_PERMANENTLY);
                            break;
                        }
                    }
                });
    }

    private static void configureRouter(@NotNull JavalinDefaultRouting router) {

        router.before(ctx -> {

            // hacky PROPFIND method support
            var method = ctx.header(Header.X_HTTP_METHOD_OVERRIDE);
            method = method == null ? ctx.req().getMethod() : method;

            if ("PROPFIND".equalsIgnoreCase(method)) {

                if ("1".equals(ctx.header("depth"))) {

                    ctx.future(() -> {
                        var path = StringPathUtils.extractUnescapedSegments(ctx.path());
                        return path.isEmpty() ? handleWebDavRoot(ctx) : handleWebDavPath(ctx, path);
                    });
                } else {
                    writeRawResponse(ctx, "Need depth header = 1".getBytes(StandardCharsets.US_ASCII), true, true);
                    ctx.status(HttpStatus.BAD_REQUEST);
                }
            }

        }).get("/", ctx -> ctx.future(() -> UpnpDeviceHolder.loadRaw()
                .mapNotNull(UpnpUtils::extractDevice)
                .map(HTML_RENDERER::render)
                .doOnNext(s -> {
                    ctx.status(HttpStatus.OK).contentType(TEXT_HTML_UTF8);
                    writeUtf8RawResponse(ctx, s, true);
                })
                .count()
                .subscribeOn(Schedulers.boundedElastic())
                .toFuture()

        )).get("/<contentDirectory>/<objectId>", ctx -> ctx.future(() -> {

            var contentDirectoryUrl = decodeUriFromBase64(ctx.pathParam("contentDirectory"));

            if (contentDirectoryUrl == null) {
                return handleWebDavGet(ctx);
            } else {
                return UpnpUtils.browse(contentDirectoryUrl, ctx.pathParam("objectId"))
                        .thenAccept(entries -> {

                            ctx.status(HttpStatus.OK).contentType(TEXT_HTML_UTF8);

                            for (var entry : entries) {
                                writeUtf8RawResponse(ctx, HTML_RENDERER.render(entry), false);
                            }
                        });
            }
        }));
    }
}