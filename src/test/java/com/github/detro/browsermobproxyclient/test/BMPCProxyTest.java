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

package com.github.detro.browsermobproxyclient.test;

import com.github.detro.browsermobproxyclient.BMPCLocalLauncher;
import com.github.detro.browsermobproxyclient.BMPCProxy;
import com.github.detro.browsermobproxyclient.exceptions.BMPCUnableToConnectException;
import com.google.gson.JsonObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class BMPCProxyTest {

    public static final String BMOB_API_HOST = "localhost";
    public static int BMOB_API_PORT;

    @BeforeClass
    public void startLocalBMP() {
        BMPCLocalLauncher.install();
        BMPCLocalLauncher.start();
        BMOB_API_PORT = BMPCLocalLauncher.port();
    }

    @AfterClass
    public void stopLocalBMP() {
        BMPCLocalLauncher.stop();
        BMPCLocalLauncher.uninstall();
    }

    @Test(expectedExceptions = BMPCUnableToConnectException.class)
    public void shouldFaileIfHostOrPortAreWrong() {
        new BMPCProxy("does.not.exist", 88888);
    }

    @Test
    public void shouldCreateProxyClient() {
        BMPCProxy proxy = new BMPCProxy(BMOB_API_HOST, BMOB_API_PORT);

        assertEquals(proxy.getAPIHost(), BMOB_API_HOST);
        assertEquals(proxy.getAPIPort(), BMOB_API_PORT);
        assertTrue(proxy.getProxyPort() > 9090);

        proxy.close();
    }

    @Test
    public void shouldBeginNewHar() {
        BMPCProxy proxy = new BMPCProxy(BMOB_API_HOST, BMOB_API_PORT);

        // First time we begin new HAR, it returns null (no previous HAR)
        JsonObject emptyHar = proxy.newHar("testPage1");
        assertNull(emptyHar);

        // Second time we begin a new HAR, check it returns the previous one
        JsonObject nonEmptyHar = proxy.newHar();
        assertTrue(nonEmptyHar.isJsonObject());
        assertTrue(nonEmptyHar.has("log"));
        assertEquals(
                nonEmptyHar.getAsJsonObject("log").getAsJsonArray("pages").get(0).getAsJsonObject().getAsJsonPrimitive("id").getAsString(),
                "testPage1");

        // Third time we begin a new HAR, check it returns previous one that has a default "initialPageRef" value
        nonEmptyHar = proxy.newHar();
        assertTrue(nonEmptyHar.isJsonObject());
        assertTrue(nonEmptyHar.has("log"));
        assertEquals(
                nonEmptyHar.getAsJsonObject("log").getAsJsonArray("pages").get(0).getAsJsonObject().getAsJsonPrimitive("id").getAsString(),
                "Page 1");

        proxy.close();
    }

    @Test
    public void shouldBeginNewPage() {
        BMPCProxy proxy = new BMPCProxy(BMOB_API_HOST, BMOB_API_PORT);

        String myPage1ref = "my page 1";
        String myPage2ref = "my page 2";
        String defaultPage3ref = "Page 3";

        proxy.newHar(myPage1ref);
        proxy.newPage(myPage2ref);
        proxy.newPage();
        JsonObject finalHar = proxy.newHar();

        assertNotNull(finalHar);
        assertTrue(finalHar.isJsonObject());
        assertTrue(finalHar.has("log"));
        assertEquals(
                finalHar.getAsJsonObject("log").getAsJsonArray("pages").get(0).getAsJsonObject().getAsJsonPrimitive("id").getAsString(),
                myPage1ref);
        assertEquals(
                finalHar.getAsJsonObject("log").getAsJsonArray("pages").get(1).getAsJsonObject().getAsJsonPrimitive("id").getAsString(),
                myPage2ref);
        assertEquals(
                finalHar.getAsJsonObject("log").getAsJsonArray("pages").get(2).getAsJsonObject().getAsJsonPrimitive("id").getAsString(),
                defaultPage3ref);

        proxy.close();
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void shouldClose() {
        BMPCProxy proxy = new BMPCProxy(BMOB_API_HOST, BMOB_API_PORT);

        String myPage1ref = "my page 1";
        proxy.newHar(myPage1ref);
        proxy.close();
        proxy.close(); //< a second call will throw an exception
    }

    @Test
    public void shouldReturnHar() {
        BMPCProxy proxy = new BMPCProxy(BMOB_API_HOST, BMOB_API_PORT);

        String myPage1ref = "my page 1";
        proxy.newHar(myPage1ref);

        JsonObject har = proxy.har();
        assertNotNull(har);
        assertTrue(har.isJsonObject());
        assertTrue(har.has("log"));

        proxy.close();
    }

    @Test
    public void shouldReturnAsSeleniumProxy() {
        BMPCProxy proxy = new BMPCProxy(BMOB_API_HOST, BMOB_API_PORT);

        assertEquals(proxy.getAPIHost(), BMOB_API_HOST);
        int port = proxy.getProxyPort();

        assertEquals(proxy.asSeleniumProxy().getHttpProxy(), "http://" + BMOB_API_HOST + ":" + port);

        proxy.close();
    }

    @Test
    public void shouldGoThroughAnUpstreamProxy() {
        // Note how cool this is: the same BMob proxy creates the 2 proxies and one is upstream to the other!
        BMPCProxy upstreamProxy = new BMPCProxy(BMOB_API_HOST, BMOB_API_PORT);
        BMPCProxy proxy = new BMPCProxy(BMOB_API_HOST, BMOB_API_PORT, upstreamProxy.asHostAndPort());

        proxy.newHar();
        proxy.har();
        proxy.close();
        upstreamProxy.close();
    }
}
