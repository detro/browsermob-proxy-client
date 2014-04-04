/*
This file is part of the BrowserMob Proxy Client project by Ivan De Marino (http://ivandemarino.me).

Copyright (c) 2014, Ivan De Marino (http://ivandemarino.me)
All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice,
      this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright notice,
      this list of conditions and the following disclaimer in the documentation
      and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.github.detro.browsermobproxyclient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.apache.http.*;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.openqa.selenium.Proxy;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Client API for a running BrowserMob Proxy.
 *
 * It uses the REST API provided by BrowserMob Proxy to create a new HTTP Proxy.
 * Maps all the REST API to a usable client object that internally only
 * deals with HTTP REST-ful calls.
 *
 * IMPORTANT: the REST API runs on a specific port (provided at Construction time), but
 * all the instances of this class will then be assigned a second Port towards which
 * a Driver/Browser will be pointed.
 * This means that a single BrowserMob Proxy instance listens to N+1 ports:
 * <ul>
 *     <li>1 port for the REST API</li>
 *     <li>N for N Proxy</li>
 * </ul>
 * This is so that every Driver can have an isolated Proxy on it's own.
 *
 * Starting and stopping BrowserMob Proxy REST API needs to be done in another place:
 * this class assumes that such REST API is up and running and consumes it.
 */
public class BrowserMobProxyClient {

    private static final Gson GSON = new GsonBuilder()
            .serializeNulls()
            .create();

    private final CloseableHttpClient HTTPclient = HttpClients.createSystem();

    private final String bmpHost;
    private final int bmpPort;
    private final int bmpProxyPort;

    /**
     * Create a BrowserMob Proxy Client
     *
     * TODO Support for 'port' and 'bindAddress' parameters when creating a new Proxy Client
     *
     * @param host Host were BrowserMob Proxy is running
     * @param port Port were BrowserMob Proxy REST API is listening
     */
    public BrowserMobProxyClient(String host, int port) {
        bmpHost = host;
        bmpPort = port;

        try {
            // Request BMP to create a new Proxy
            HttpPost createProxyPOST = new HttpPost(defaultURIBuilder()
                    .setPath("/proxy")
                    .build());

            // Execute request
            CloseableHttpResponse createProxyPOSTResponse = HTTPclient.execute(createProxyPOST);
            assert(createProxyPOSTResponse.getStatusLine().getStatusCode() == 200);

            // Parse response into JSON
            JsonObject createProxyResponseJson = httpResponseToJsonObject(createProxyPOSTResponse);
            createProxyPOSTResponse.close();
            assert(null != createProxyResponseJson);
            assert(createProxyResponseJson.isJsonObject());

            // Store newly created Proxy Port
            bmpProxyPort = createProxyResponseJson.getAsJsonPrimitive("port").getAsInt();
        } catch (URISyntaxException|IOException e) {
            throw new RuntimeException(String.format("Unable to connect to BMP Proxy at '%s:%s'", bmpHost, bmpPort), e);
        }
    }

    /**
     * Releases the Proxy Client resources.
     * After this call the Proxy Client will not exist anymore and will not be usable.
     */
    public void release() {
        try {
            HTTPclient.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        release();
    }

    /**
     * Returns the Proxy this client wraps, in form of a Selenium Proxy configuration object.
     *
     * @return Selenium Proxy configuration object
     */
    public Proxy getSeleniumProxy() {
        Proxy seleniumProxyConfig = new Proxy();

        seleniumProxyConfig.setHttpProxy(String.format("http://%s:%s", bmpHost, bmpProxyPort));

        return seleniumProxyConfig;
    }

    /**
     * Host on which BrowserMob Proxy is running.
     * @return Host on which the proxy is running
     */
    public String getHost() {
        return bmpHost;
    }

    /**
     * Port on which BrowserMob Proxy is listening.
     * This is NOT the port of the proxy itself, but of the REST API.
     *
     * @return Port on which the proxy REST API are running
     */
    public int getPort() {
        return bmpPort;
    }

    /**
     * Port on which the Proxy is listening.
     * This is where the driver will connect.
     *
     * @return Port on which the proxy is listening
     */
    public int getProxyPort() {
        return bmpProxyPort;
    }

    public JsonObject beginNewHar() {
        return beginNewHar(null, false, false, false);
    }

    public JsonObject beginNewHar(String initialPageRef) {
        return beginNewHar(initialPageRef, false, false, false);
    }

    public JsonObject beginNewHar(String initialPageRef, boolean captureHeaders) {
         return beginNewHar(initialPageRef, captureHeaders, false, false);
    }

    public JsonObject beginNewHar(String initialPageRef, boolean captureHeaders, boolean captureContent, boolean captureBinaryContent) {
        try {
            // Request BMP to create a new HAR for this Proxy
            HttpPut createHARPUT = new HttpPut(defaultURIBuilder()
                    .setPath(proxyURIPath() + "/har")
                    .build());

            // Add form parameters to the request
            applyFormParamsToHttpRequest(createHARPUT,
                    new BasicNameValuePair("initialPageRef", initialPageRef),
                    new BasicNameValuePair("captureHeaders", Boolean.toString(captureHeaders)),
                    new BasicNameValuePair("captureContent", Boolean.toString(captureContent)),
                    new BasicNameValuePair("captureBinaryContent", Boolean.toString(captureBinaryContent)));

            // Execute request
            CloseableHttpResponse createHARPUTResponse = HTTPclient.execute(createHARPUT);
            assert(createHARPUTResponse.getStatusLine().getStatusCode() == 200);

            // Parse response into JSON
            JsonObject createHARResponseJson = httpResponseToJsonObject(createHARPUTResponse);
            createHARPUTResponse.close();

            return createHARResponseJson;
        } catch (URISyntaxException|IOException e) {
            throw new RuntimeException(String.format("Unable to begin new HAR collection"), e);
        }
    }

    private JsonObject httpResponseToJsonObject(HttpResponse response) {
        StatusLine statusLine = response.getStatusLine();
        HttpEntity entity = response.getEntity();

        // Workout if we got back a good response
        if (statusLine.getStatusCode() >= 300) {
            throw new RuntimeException("Unexpected HTTP Status Code: " + statusLine.getStatusCode());
        }

        if (statusLine.getStatusCode() == 204) {
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
                throw new RuntimeException(e);
            }
        }
    }

    private void applyFormParamsToHttpRequest(HttpEntityEnclosingRequestBase httpReq, NameValuePair ... pairs) {
        // Filter out null-value Pairs
        List<NameValuePair> formParams = new ArrayList<NameValuePair>();
        for (NameValuePair pair : pairs) {
            if (pair.getValue() != null) {
                formParams.add(pair);
            }
        }

        // Encode as entity and set
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formParams, Consts.UTF_8);
        httpReq.setEntity(entity);
    }

    private URIBuilder defaultURIBuilder() {
        return new URIBuilder()
                .setScheme("http")
                .setHost(bmpHost)
                .setPort(bmpPort);
    }

    private String proxyURIPath() {
        return String.format("/proxy/%d", bmpProxyPort);
    }

    // TODO maybe a Builder pattern would work better here
//    public void nextPage(String pageRef);
//
//
//    public JsonObject getHar();

    // TODO more API
    // - whitelist PUT/DELETE
    // - blacklist PUT/DELETE
    // - bandwidth limit PUT
    // - HTTP headers POST
    // - host/DNS override POST
    // - basic auth POST
    // - wait PUT
    // - timeouts PUT
    // - url rewrite PUT/DELETE
    // - retry count PUT
    // - DNS cache flush DELETE
}
