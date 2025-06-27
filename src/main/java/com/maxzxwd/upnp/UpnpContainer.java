package com.maxzxwd.upnp;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class UpnpContainer extends UpnpEntry {

    public static final UpnpContainer EMPTY_CONTAINER = new UpnpContainer("/", "");

    @NotNull
    public final String id;

    public UpnpContainer(@NotNull String title, @NotNull String id) {
        super(title);
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        var that = (UpnpContainer) o;
        return Objects.equals(title, that.title) && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, id);
    }

    @Override
    @NotNull
    public String toString() {
        return "UpnpContainer{" +
                "title='" + title + '\'' +
                ", id='" + id + '\'' +
                '}';
    }
}
