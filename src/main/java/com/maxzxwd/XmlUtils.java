package com.maxzxwd;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.io.StringReader;
import java.util.*;

public final class XmlUtils {

    private XmlUtils() {}

    @Nullable
    public static Document safeParse(
            @Nullable DocumentBuilderFactory documentBuilderFactory,
            @Nullable String str
    ) {
        if (documentBuilderFactory == null || str == null) {
            return null;
        } else {
            return safeParse(documentBuilderFactory, new InputSource(new StringReader(str)));
        }
    }

    @Nullable
    public static Document safeParse(
            @Nullable DocumentBuilderFactory documentBuilderFactory,
            @Nullable InputStream is
    ) {
        if (documentBuilderFactory == null || is == null) {
            return null;
        } else {
            return safeParse(documentBuilderFactory, new InputSource(is));
        }
    }

    @Nullable
    public static Document safeParse(
            @Nullable DocumentBuilderFactory documentBuilderFactory,
            @Nullable InputSource is
    ) {

        if (documentBuilderFactory != null && is != null) {
            try {
                return documentBuilderFactory.newDocumentBuilder().parse(is);
            } catch (Throwable ignored) {}
        }

        return null;
    }

    @Nullable
    public static String getTextContent(@Nullable Node node) {
        return node == null ? null : node.getTextContent();
    }

    @Nullable
    public static Node getFirstChild(@Nullable Node node) {
        return node == null ? null : node.getFirstChild();
    }

    @NotNull
    public static Collection<Node> getElementsByTagName(@Nullable Document document, @Nullable String name) {

        if (document == null || name == null) {
            return List.of();
        } else {
            return new NodeListCollection(document.getElementsByTagName(name));
        }
    }

    @NotNull
    public static Collection<Node> getElementsByTagName(@Nullable Element element, @Nullable String name) {

        if (element == null || name == null) {
            return List.of();
        } else {
            return new NodeListCollection(element.getElementsByTagName(name));
        }
    }

    @NotNull
    public static Collection<Node> getElementsByTagNameNs(
            @Nullable Element element,
            @NotNull String ns,
            @Nullable String name
    ) {
        if (element == null || name == null) {
            return List.of();
        } else {
            return new NodeListCollection(element.getElementsByTagNameNS(ns, name));
        }
    }

    @NotNull
    public static Collection<Node> getNodeList(@Nullable Node node) {
        return node == null ? List.of() : new NodeListCollection(node.getChildNodes());
    }

    @Nullable
    public static Element getFirstElementByName(@Nullable Document base, @Nullable String name) {
        return getFirstElement(getElementsByTagName(base, name));
    }

    @Nullable
    public static Element getFirstElementByName(@Nullable Element base, @Nullable String name) {
        return getFirstElement(getElementsByTagName(base, name));
    }

    @Nullable
    public static Element getFirstElementByNameNS(@Nullable Element base, @NotNull String ns, @Nullable String name) {
        return getFirstElement(getElementsByTagNameNs(base, ns, name));
    }

    @Nullable
    public static Element getFirstElement(@Nullable Collection<Node> nodes) {

        var size = nodes == null ? 0 : nodes.size();
        var node = size > 0 ? nodes.iterator().next() : null;

        return node instanceof Element element ? element : null;
    }

    public static class NodeListCollection extends AbstractCollection<Node> {

        @NotNull
        private final NodeList nodeList;
        private final int length;

        private NodeListCollection(@NotNull NodeList nodeList) {
            this.nodeList = nodeList;
            length = nodeList.getLength();
        }

        @Override
        public int size() {
            return length;
        }

        @Override
        public boolean isEmpty() {
            return length == 0;
        }

        @NotNull
        @Override
        public Iterator<Node> iterator() {
            return new Iterator<>() {
                private int index = 0;

                @Override
                public boolean hasNext() {
                    return index < length;
                }

                @Override
                public Node next() {
                    return nodeList.item(index++);
                }
            };
        }
    }
}
