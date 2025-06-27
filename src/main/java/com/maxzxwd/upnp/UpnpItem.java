package com.maxzxwd.upnp;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class UpnpItem extends UpnpEntry {

    @NotNull
    public final String url;

    @NotNull
    public final String size;

    @NotNull
    public final String protocolInfo;

    public UpnpItem(@NotNull String title, @NotNull String url, @NotNull String size, @NotNull String protocolInfo) {
        super(title);
        this.url = url;
        this.size = size;
        this.protocolInfo = protocolInfo;
    }

    @Override
    public boolean equals(Object o) {

        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        var upnpItem = (UpnpItem) o;
        return Objects.equals(title, upnpItem.title) && Objects.equals(url, upnpItem.url) &&
                Objects.equals(size, upnpItem.size) && Objects.equals(protocolInfo, upnpItem.protocolInfo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, url, size, protocolInfo);
    }

    @Override
    @NotNull
    public String toString() {
        return "UpnpItem{" +
                "title='" + title + '\'' +
                ", url='" + url + '\'' +
                ", size='" + size + '\'' +
                ", protocolInfo='" + protocolInfo + '\'' +
                '}';
    }
}
