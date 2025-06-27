package com.maxzxwd.upnp.renderer;

import com.maxzxwd.upnp.*;
import org.jetbrains.annotations.NotNull;

public abstract class Renderer {

    @NotNull
    public abstract byte[] renderStart();

    @NotNull
    public abstract byte[] renderEnd();

    @NotNull
    public abstract String render(@NotNull UpnpDevice device);

    @NotNull
    public abstract String render(@NotNull UpnpItem upnpItem);

    @NotNull
    public abstract String render(@NotNull UpnpContainer upnpContainer);

    @NotNull
    public final String render(@NotNull UpnpEntry upnpEntry) {
        switch (upnpEntry) {
            case UpnpItem upnpItem -> {
                return render(upnpItem);
            }
            case UpnpContainer upnpContainer -> {
                return render(upnpContainer);
            }
        }
    }
}
