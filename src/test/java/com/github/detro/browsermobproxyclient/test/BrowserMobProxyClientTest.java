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

import com.github.detro.browsermobproxyclient.BrowserMobProxyClient;
import com.google.gson.JsonObject;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class BrowserMobProxyClientTest {

    @Test
    public void shouldCreateProxyClient() {
        BrowserMobProxyClient client = new BrowserMobProxyClient("localhost", 8080);

        assertEquals(client.getHost(), "localhost");
        assertEquals(client.getPort(), 8080);
        assertTrue(client.getProxyPort() > 9090);
    }

    @Test
    public void shouldBeginNewHar() {
        BrowserMobProxyClient client = new BrowserMobProxyClient("localhost", 8080);

        // First time we begin new HAR, it returns null (no previous HAR)
        JsonObject emptyHar = client.beginNewHar("testPage1");
        assertNull(emptyHar);

        // Second time we begin a new HAR, check it returns the previous one
        JsonObject nonEmptyHar = client.beginNewHar();
        assertTrue(nonEmptyHar.isJsonObject());
        assertTrue(nonEmptyHar.has("log"));
        assertEquals(
                nonEmptyHar.getAsJsonObject("log").getAsJsonArray("pages").get(0).getAsJsonObject().getAsJsonPrimitive("id").getAsString(),
                "testPage1");

        // Third time we begin a new HAR, check it returns previous one that has a default "initialPageRef" value
        nonEmptyHar = client.beginNewHar();
        assertTrue(nonEmptyHar.isJsonObject());
        assertTrue(nonEmptyHar.has("log"));
        assertEquals(
                nonEmptyHar.getAsJsonObject("log").getAsJsonArray("pages").get(0).getAsJsonObject().getAsJsonPrimitive("id").getAsString(),
                "Page 1");
    }
}
