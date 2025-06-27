package com.maxzxwd.upnp.renderer;

import com.maxzxwd.upnp.UpnpContainer;
import com.maxzxwd.upnp.UpnpDevice;
import com.maxzxwd.upnp.UpnpItem;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class HtmlRenderer extends Renderer {

    private static final byte[] START_END = new byte[0];

    @NotNull
    @Override
    public byte[] renderStart() {
        return START_END;
    }

    @NotNull
    @Override
    public byte[] renderEnd() {
        return START_END;
    }

    @Override
    @NotNull
    public String render(@NotNull UpnpDevice device) {

        var href = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(device.contentDirectoryUri.toString().getBytes(StandardCharsets.UTF_8)) + "/0";
        return "<a href='" + href + "'>\uD83D\uDDA7 " + device.name + "</a><br/>";
    }

    @Override
    @NotNull
    public String render(@NotNull UpnpItem upnpItem) {
        return "<a href='" + upnpItem.url + "' target='_blank' rel='noopener noreferrer'>" + upnpItem.title + "</a><br/>";
    }

    @Override
    @NotNull
    public String render(@NotNull UpnpContainer upnpContainer) {
        return "<a href='" + upnpContainer.id + "'>\uD83D\uDCC1 " + upnpContainer.title + "</a><br/>";
    }
}
