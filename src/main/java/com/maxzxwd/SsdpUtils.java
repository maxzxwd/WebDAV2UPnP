package com.maxzxwd;

import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.regex.Pattern;

public final class SsdpUtils {

    private SsdpUtils() {}

    private static final String SSDP_ADDRESS = "239.255.255.250";
    private static final int SSDP_PORT = 1900;
    private static final Pattern LOCATION_PATTERN = Pattern.compile("^location:\\s*(.*)$", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
    private static final byte[] M_SEARCH_DATA = ("M-SEARCH * HTTP/1.1\r\n" +
            "HOST: " + SSDP_ADDRESS + ":" + SSDP_PORT + "\r\n" +
            "MAN: \"ssdp:discover\"\r\n" +
            "MX: 3\r\n" +
            "ST: urn:schemas-upnp-org:device:MediaServer:1\r\n" +
            "\r\n").getBytes(StandardCharsets.UTF_8);

    @NotNull
    public static Flux<URI> discover() {

        return Flux.<URI>create(emitter -> {

            try (var socket = new DatagramSocket()) {

                socket.setSoTimeout(500);
                var ssdpInetAddress = InetAddress.getByName(SSDP_ADDRESS);
                socket.send(new DatagramPacket(M_SEARCH_DATA, M_SEARCH_DATA.length, ssdpInetAddress, SSDP_PORT));

                while (!emitter.isCancelled()) {
                    try {
                        var buf = new byte[1024];
                        var response = new DatagramPacket(buf, buf.length);
                        socket.receive(response);

                        var received = new String(response.getData(), 0, response.getLength(), StandardCharsets.UTF_8);
                        var matcher = LOCATION_PATTERN.matcher(received);

                        if (matcher.find()) {
                            emitter.next(URI.create(matcher.group(1).trim()));
                        }
                    } catch (Throwable ignored) {}
                }

                emitter.complete();
            } catch (IOException e) {
                emitter.error(e);
            }
        }, FluxSink.OverflowStrategy.BUFFER).take(Duration.ofSeconds(5));
    }
}
