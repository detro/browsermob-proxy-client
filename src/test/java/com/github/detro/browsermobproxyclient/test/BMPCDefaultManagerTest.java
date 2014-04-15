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

import com.github.detro.browsermobproxyclient.BMPCDefaultManager;
import com.github.detro.browsermobproxyclient.BMPCLocalLauncher;
import com.github.detro.browsermobproxyclient.BMPCManager;
import com.github.detro.browsermobproxyclient.BMPCProxy;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class BMPCDefaultManagerTest {

    public static String BMOB_API_HOST;
    public static int BMOB_API_PORT;

    @BeforeClass
    public void startLocalBMP() {
        BMPCLocalLauncher.install();
        BMPCLocalLauncher.start();
        BMOB_API_HOST = BMPCLocalLauncher.host();
        BMOB_API_PORT = BMPCLocalLauncher.port();
    }

    @Test
    public void shouldCreateSimpleManager() {
        BMPCManager manager = new BMPCDefaultManager(BMOB_API_HOST, BMOB_API_PORT);

        assertNotNull(manager);
        assertEquals(manager.getAPIHost(), BMOB_API_HOST);
        assertEquals(manager.getAPIPort(), BMOB_API_PORT);
    }

    @Test
    public void shouldCreateProxy() {
        BMPCManager manager = new BMPCDefaultManager(BMOB_API_HOST, BMOB_API_PORT);

        BMPCProxy proxy = manager.createProxy();

        assertNotNull(proxy);
        assertTrue(proxy.getProxyPort() > 9090);
        proxy.close();
    }

    @Test
    public void shouldReturnListOfOpenProxies() {
        BMPCManager manager = new BMPCDefaultManager(BMOB_API_HOST, BMOB_API_PORT);

        int initialProxiesCount = manager.getOpenProxies().size();
        manager.createProxy();
        manager.createProxy();
        manager.createProxy();
        manager.createProxy();
        manager.createProxy();

        assertEquals(manager.getOpenProxies().size(), initialProxiesCount + 5);

        manager.closeAll();
        assertEquals(manager.getOpenProxies().size(), 0);
    }

    @Test
    public void shouldCreateProxyThatGoesThroughUpstreamProxy() {
        BMPCManager manager = new BMPCDefaultManager(BMOB_API_HOST, BMOB_API_PORT);

        BMPCProxy upstreamProxy = manager.createProxy();
        BMPCProxy proxy = manager.createProxy(upstreamProxy.asHostAndPort());

        proxy.newHar();
        proxy.har();
        proxy.close();
        upstreamProxy.close();
    }
}
