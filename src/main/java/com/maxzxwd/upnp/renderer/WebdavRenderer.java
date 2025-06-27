package com.maxzxwd.upnp.renderer;

import com.maxzxwd.StringPathUtils;
import com.maxzxwd.upnp.UpnpContainer;
import com.maxzxwd.upnp.UpnpDevice;
import com.maxzxwd.upnp.UpnpItem;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;

public class WebdavRenderer extends Renderer {

    private static final byte[] START = "<?xml version='1.0' encoding='utf-8' ?><multistatus xmlns='DAV:'>"
            .getBytes(StandardCharsets.UTF_8);

    private static final byte[] END = "</multistatus>".getBytes(StandardCharsets.UTF_8);

    @NotNull
    @Override
    public byte[] renderStart() {
        return START;
    }

    @NotNull
    @Override
    public byte[] renderEnd() {
        return END;
    }

    @Override
    @NotNull
    public String render(@NotNull UpnpDevice device) {
        return renderContainer(device.name);
    }

    @Override
    @NotNull
    public String render(@NotNull UpnpItem upnpItem) {

        var parts = upnpItem.protocolInfo.split(":", 4);
        var mimeType = parts.length >= 3 ? parts[2] : "";

        return "<response><href>" + StringPathUtils.escapeSegment(upnpItem.title) +
                "</href><propstat><prop><getcontentlength>" + upnpItem.size + "</getcontentlength><getcontenttype>" +
                mimeType + "</getcontenttype></prop><status>HTTP/1.1 200 OK</status></propstat></response>";
    }

    @Override
    @NotNull
    public String render(@NotNull UpnpContainer upnpContainer) {
        return renderContainer(upnpContainer.title);
    }

    @NotNull
    private String renderContainer(@NotNull String title) {
        return "<response><href>" + StringPathUtils.escapeSegment(title) +
                "</href><propstat><prop><resourcetype><collection/></resourcetype></prop><status>HTTP/1.1 200 OK</status></propstat></response>";
    }
}
