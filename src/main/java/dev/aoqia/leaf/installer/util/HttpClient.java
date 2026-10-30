// spotless:off

/*
 * Copyright 2026 aoqia
 *
 * This file was originally released under LICENSE_UPSTREAM (see below for the original header) and has been relicensed
 * to GNU's AGPLv3 license, with additional terms in the LICENSE_EXCEPTION.md file.
 *
 * All modifications made to this file after the original version were created separate to the original copyright.
 * These modifications are provided under the AGPLv3 license (including exception terms). The portions of this file that
 * were not modified remain subject to the original copyright notice and license, and the original LICENSE is kept in
 * LICENSE_UPSTREAM located alongside LICENSE.
 *
 * The complete record of changes to this file is publicly available in the project's Git history, which anyone may view
 * at any time for any reason.
 *
 * Copyright (c) 2016-2025 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// spotless:on

package dev.aoqia.leaf.installer.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class HttpClient {
    // When we successfully connect to a proxy, we store it here so that we can try it first for subsequent requests.
    private static volatile Proxy lastSuccessfulProxy;

    private static final List<ProxySupplier> PROXIES = Arrays.asList(
        // Direct connect without proxy
        uri -> Collections.singletonList(Proxy.NO_PROXY),
        // Common Java proxy system properties and system configured proxies See: sun.net.spi.DefaultProxySelector
        uri -> ProxySelector.getDefault().select(uri),
        // CURL environment variables
        HttpClient::getEnvironmentProxies);

    private static final int HTTP_TIMEOUT_MS = 8000;

    private HttpClient() {
    }

    public static void downloadFile(URL url, Path path) throws IOException {
        try {
            tryWithProxies(url, (Handler<Void>) in -> {
                Files.createDirectories(path.getParent());
                Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);
                return null;
            });
        } catch (Throwable t) {
            try {
                Files.deleteIfExists(path);
            } catch (Throwable t2) {
                t.addSuppressed(t2);
            }

            throw t;
        }
    }

    public static String readString(URL url) throws IOException {
        return tryWithProxies(url, Utils::readString);
    }

    // Returns the list of proxies set via environment variables.
    // This reads the de-facto standard environment variables used by CURL, see https://superuser.com/a/1166790
    private static List<Proxy> getEnvironmentProxies(URI uri) {
        ArrayList<Proxy> proxies = new ArrayList<>();

        String httpProxy = System.getenv("HTTP_PROXY");

        if (httpProxy == null) {
            // CURL only supports the lowercase environment variable
            httpProxy = System.getenv("http_proxy");
        }

        if (httpProxy != null) {
            try {
                proxies.add(parseProxy(httpProxy));
            } catch (URISyntaxException e) {
                System.err.println("Invalid HTTP_PROXY environment variable: " + httpProxy);
            }
        }

        String httpsProxy = System.getenv("HTTPS_PROXY");

        if (httpsProxy != null) {
            try {
                proxies.add(parseProxy(httpsProxy));
            } catch (URISyntaxException e) {
                System.err.println("Invalid HTTPS_PROXY environment variable: " + httpsProxy);
            }
        }

        return proxies;
    }

    private static InputStream openUrl(URL url, Proxy proxy) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection(proxy);

        conn.setConnectTimeout(HTTP_TIMEOUT_MS);
        conn.setReadTimeout(HTTP_TIMEOUT_MS);
        conn.connect();

        int responseCode = conn.getResponseCode();
        if (responseCode < 200 || responseCode >= 300) {
            throw new IOException("HTTP request to " + url + " failed: " + responseCode);
        }

        return conn.getInputStream();
    }

    private static Proxy parseProxy(String str) throws URISyntaxException {
        URI uri = new URI(str);
        String host = uri.getHost();
        // Default to port 80 if not specified
        int port = uri.getPort() == -1 ? 80 : uri.getPort();
        return new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, port));
    }

    private static <T> T tryWithProxies(URL url, Handler<T> handler) throws IOException {
        URI uri;

        try {
            uri = url.toURI();
        } catch (URISyntaxException e) {
            throw new IOException(e.getMessage(), e);
        }

        Set<Proxy> attemptedProxies = new HashSet<>();
        IOException exception = null;

        // try lastSuccessfulProxy first, if available
        if (lastSuccessfulProxy != null) {
            attemptedProxies.add(lastSuccessfulProxy);

            try (InputStream is = openUrl(url, lastSuccessfulProxy)) {
                return handler.read(is);
            } catch (IOException e) {
                // failed, remove priority for the specific proxy
                HttpClient.lastSuccessfulProxy = null;
                exception = new IOException(String.format("Request to %s failed: %s", uri, e.getMessage()), e);
            }
        }

        // try all other proxies

        for (ProxySupplier proxySupplier : PROXIES) {
            List<Proxy> proxies = proxySupplier.getProxies(uri);

            // Try each proxy in the list
            for (Proxy proxy : proxies) {
                if (proxy == null) {
                    continue;
                }

                // Don't attempt to use a proxy that we have already tried
                if (!attemptedProxies.add(proxy)) {
                    continue;
                }

                try {
                    T value;

                    try (InputStream is = openUrl(url, proxy)) {
                        value = handler.read(is);
                    }

                    // Store the last used proxy so we can try it first next time
                    HttpClient.lastSuccessfulProxy = proxy;

                    return value;
                } catch (IOException e) {
                    IOException ioe = new IOException(
                        String.format("Request to %s using %s failed: %s", uri, proxy, e.getMessage()), e);

                    if (exception == null) {
                        exception = ioe;
                    } else {
                        exception.addSuppressed(ioe);
                    }
                }
            }
        }

        if (exception != null) {
            throw exception;
        } else {
            // Should never happen, as we always try to connect directly first
            throw new IllegalStateException("Did not attempt http connection");
        }
    }

    private interface Handler<T> {
        T read(InputStream in) throws IOException;
    }

    private interface ProxySupplier {
        // Returns a list of proxies for the given URI, or a null list if no proxy should be used.
        // A null proxy entry in the list is skipped.
        List<Proxy> getProxies(URI uri) throws IOException;
    }
}
