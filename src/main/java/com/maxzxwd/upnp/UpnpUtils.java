package com.maxzxwd.upnp;

import com.maxzxwd.XmlUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public final class UpnpUtils {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .build();
    private static final DocumentBuilderFactory DOCUMENT_BUILDER_FACTORY = DocumentBuilderFactory.newInstance();

    static {
        DOCUMENT_BUILDER_FACTORY.setNamespaceAware(true);
    }

    private UpnpUtils() {}

    @Nullable
    public static UpnpDevice extractDevice(@Nullable HttpResponse<InputStream> response) {

        var xml = XmlUtils.safeParse(DOCUMENT_BUILDER_FACTORY, response == null ? null : response.body());
        var device = XmlUtils.getFirstElementByName(xml, "device");

        var friendlyName = XmlUtils.getTextContent(XmlUtils.getFirstElementByName(device, "friendlyName"));

        var serviceList = XmlUtils.getFirstElementByName(device, "serviceList");
        var serviceNodes = XmlUtils.getElementsByTagName(serviceList, "service");

        String url = null;

        for (var serviceNode : serviceNodes) {
            if (!(serviceNode instanceof Element service)) {
                continue;
            }

            var serviceId = XmlUtils.getTextContent(XmlUtils.getFirstElementByName(service, "serviceId"));

            if ("urn:upnp-org:serviceId:ContentDirectory".equals(serviceId)) {
                url = XmlUtils.getTextContent(XmlUtils.getFirstElementByName(service, "controlURL"));
                break;
            }
        }

        if (friendlyName != null && url != null && response != null) {
            return new UpnpDevice(friendlyName, response.request().uri().resolve(url));
        }

        return null;
    }

    @Nullable
    public static String extractUpnpResult(@Nullable HttpResponse<InputStream> response) {

        var xml = XmlUtils.safeParse(DOCUMENT_BUILDER_FACTORY, response == null ? null : response.body());
        var node = XmlUtils.getFirstChild(XmlUtils.getFirstChild(xml));

        if (node instanceof Element element) {
            return XmlUtils.getTextContent(XmlUtils.getFirstElementByName(element, "Result"));
        } else {
            return null;
        }
    }

    @NotNull
    public static List<UpnpEntry> extractUpnpEntries(@Nullable String response) {

        var xml = XmlUtils.safeParse(DOCUMENT_BUILDER_FACTORY, response);
        var firstChild = XmlUtils.getFirstChild(xml);
        var children = XmlUtils.getNodeList(firstChild);
        var entries = children.isEmpty() ? null : new ArrayList<UpnpEntry>(children.size());

        for (var node : children) {
            var nodeName = node.getNodeName();
            var isContainer = "container".equals(nodeName);

            if ((isContainer || "item".equals(nodeName)) && node instanceof Element element) {
                var title = XmlUtils.getTextContent(XmlUtils.getFirstElementByNameNS(element, "http://purl.org/dc/elements/1.1/", "title"));

                if (title != null) {
                    if (isContainer) {
                        var id = element.getAttribute("id");
                        var childCount = element.getAttribute("childCount");

                        if (!id.isEmpty() && !"0".equals(childCount)) {
                            entries.add(new UpnpContainer(
                                    title,
                                    id
                            ));
                        }
                    } else {
                        var res = XmlUtils.getFirstElementByName(element, "res");
                        var resTextContent = XmlUtils.getTextContent(res);

                        if (resTextContent != null) {
                            entries.add(new UpnpItem(
                                    title,
                                    resTextContent,
                                    res.getAttribute("size"),
                                    res.getAttribute("protocolInfo")
                            ));
                        }
                    }
                }
            }
        }

        return entries == null ? List.of() : Collections.unmodifiableList(entries);
    }

    @NotNull
    public static CompletableFuture<List<UpnpEntry>> browse(@NotNull URI contentDirectoryUri, @Nullable String objectId) {

        var soapBody = ("""
                <?xml version="1.0" encoding="utf-8"?>
                <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/"
                            s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
                  <s:Body>
                    <u:Browse xmlns:u="urn:schemas-upnp-org:service:ContentDirectory:1">
                      <ObjectID>
                """ + Objects.requireNonNullElse(objectId, "0") + """
                      </ObjectID>
                      <BrowseFlag>BrowseDirectChildren</BrowseFlag>
                      <Filter>*</Filter>
                      <StartingIndex>0</StartingIndex>
                      <RequestedCount>0</RequestedCount>
                      <SortCriteria></SortCriteria>
                    </u:Browse>
                  </s:Body>
                </s:Envelope>
                """).getBytes(StandardCharsets.UTF_8);

        var request = HttpRequest.newBuilder()
                .uri(contentDirectoryUri)
                .POST(HttpRequest.BodyPublishers.ofByteArray(soapBody))
                .headers(
                        "SOAPACTION", "\"urn:schemas-upnp-org:service:ContentDirectory:1#Browse\"",
                        "Content-Type", "text/xml; charset=\"utf-8\""
                ).build();

        return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
                .thenApply(UpnpUtils::extractUpnpResult)
                .thenApply(UpnpUtils::extractUpnpEntries);
    }
}
