package com.github.detro.browsermobproxyclient;

import com.github.detro.browsermobproxyclient.exceptions.BMPCInvalidJsonException;
import com.github.detro.browsermobproxyclient.exceptions.BMPCUnableToParseJsonResponseException;
import com.github.detro.browsermobproxyclient.exceptions.BMPCUnexpectedErrorException;
import com.google.gson.*;
import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Set;

public class BMPCSimpleManager implements BMPCManager {

    private static final Gson GSON = new GsonBuilder()
            .serializeNulls()
            .create();

    private final CloseableHttpClient HTTPclient = HttpClients.createSystem();

    private final String APIHost;
    private final int APIPort;

    public BMPCSimpleManager(String apiHost, int apiPort) {
        this.APIHost = apiHost;
        this.APIPort = apiPort;

        // Validate server is up an running by doing a test-call.
        // The following will throw an exception in case the BrowserMob Proxy
        // isnt' there.
        getOpenProxies().size();
    }

    @Override
    public BMPCProxy createProxy() {
        return new BMPCProxy(APIHost, APIPort);
    }

    @Override
    public BMPCProxy createProxy(String upstreamProxyHostAndPort) {
        return new BMPCProxy(APIHost, APIPort, upstreamProxyHostAndPort);
    }

    @Override
    public Set<Integer> getOpenProxies() {
        Set<Integer> openProxiesPorts = new HashSet<Integer>();

        try {
            // Request list of Proxy currently running
            HttpGet request = new HttpGet(requestURIBuilder()
                    .setPath("/proxy")
                    .build());

            // Execute request
            CloseableHttpResponse response = HTTPclient.execute(request);

            // Check request was successful
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != 200) {
                throw new BMPCUnexpectedErrorException(String.format(
                        "Can't fetch list of Open Proxies from '%s:%d'." +
                                " Status code: %d",
                        APIHost, APIPort, statusCode));
            }

            // Parse JSON response and check it's valid
            String proxyListKey = "proxyList";
            JsonObject jsonResponse = httpResponseToJsonObject(response);
            if (null == jsonResponse || !jsonResponse.has(proxyListKey)) {
                throw new BMPCInvalidJsonException(String.format(
                        "JSON Response does not contain '%s'", proxyListKey));
            }

            // Create response with list of Open Proxies Ports
            JsonArray proxyList = jsonResponse.getAsJsonArray(proxyListKey);
            for (JsonElement proxy : proxyList) {
                openProxiesPorts.add(proxy.getAsJsonObject().getAsJsonPrimitive("port").getAsInt());
            }
        } catch (URISyntaxException |IOException e) {
            throw new BMPCUnexpectedErrorException(e);
        }

        return openProxiesPorts;
    }

    @Override
    public void closeAll() {
        BMPCProxy proxy;
        for (int openProxyPort : getOpenProxies()) {
            proxy = new BMPCProxy(APIHost, APIPort, openProxyPort);
            proxy.close();
        }
    }

    @Override
    public String getAPIHost() {
        return APIHost;
    }

    @Override
    public int getAPIPort() {
        return APIPort;
    }

    private URIBuilder requestURIBuilder() {
        return new URIBuilder()
                .setScheme("http")
                .setHost(APIHost)
                .setPort(APIPort);
    }

    private JsonObject httpResponseToJsonObject(HttpResponse response) {
        int statusCode = response.getStatusLine().getStatusCode();
        HttpEntity entity = response.getEntity();

        if (statusCode == 204) {
            // Request successful but the response has No Content
            return null;
        } else {
            try {
                // Workout the charset
                Charset charset = ContentType.getOrDefault(entity).getCharset();

                // De-serialize
                return GSON.fromJson(
                        new InputStreamReader(entity.getContent(), null != charset ? charset : Consts.UTF_8),
                        JsonObject.class);
            } catch (IOException e) {
                throw new BMPCUnableToParseJsonResponseException(e);
            }
        }
    }
}
