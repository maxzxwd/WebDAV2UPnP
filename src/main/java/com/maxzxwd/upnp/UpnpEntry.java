package com.maxzxwd.upnp;

import org.jetbrains.annotations.NotNull;

public sealed abstract class UpnpEntry permits UpnpContainer, UpnpItem {

    @NotNull
    public final String title;

    UpnpEntry(@NotNull String title) {
        this.title = title;
    }
}
