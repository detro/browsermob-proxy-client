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
import com.github.detro.browsermobproxyclient.BMPCManager;
import com.github.detro.browsermobproxyclient.exceptions.BMPCLocalNotInstalledException;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class BMPCLocalLauncherTest {

    public static final String INCLUDED_VERSION = "2.0-beta-9";

    @Test
    public void shouldBeAbleToInstallAndUninstall() {
        BMPCLocalLauncher.uninstall();
        assertFalse(BMPCLocalLauncher.isInstalled());
        BMPCLocalLauncher.install();
        assertTrue(BMPCLocalLauncher.isInstalled());
        BMPCLocalLauncher.uninstall();
        assertFalse(BMPCLocalLauncher.isInstalled());
        BMPCLocalLauncher.install();
        BMPCLocalLauncher.install();
        assertTrue(BMPCLocalLauncher.isInstalled());
        BMPCLocalLauncher.uninstall();
    }

    @Test
    public void shouldReportASpecificVersion() {
        BMPCLocalLauncher.install();
        assertTrue(BMPCLocalLauncher.isInstalled());
        assertEquals(BMPCLocalLauncher.installedVersion(), INCLUDED_VERSION);
        BMPCLocalLauncher.uninstall();
        assertFalse(BMPCLocalLauncher.isInstalled());
    }

    @Test(expectedExceptions = BMPCLocalNotInstalledException.class)
    public void shouldThrowIfVersionIsRequestedButLocalBrowserMobProxyNotInstalled() {
        BMPCLocalLauncher.uninstall();
        BMPCLocalLauncher.installedVersion();
    }

    @Test
    public void shouldStartAndStop() throws InterruptedException {
        BMPCLocalLauncher.install();

        assertFalse(BMPCLocalLauncher.isRunning());
        BMPCLocalLauncher.start();
        assertTrue(BMPCLocalLauncher.isRunning());

        Thread.currentThread().sleep(3000);

        BMPCLocalLauncher.stop();
        assertFalse(BMPCLocalLauncher.isRunning());

        BMPCLocalLauncher.uninstall();
    }

    @Test
    public void shouldSupportStartingMultipleTimesWithoutIssues() {
        BMPCLocalLauncher.start();
        BMPCLocalLauncher.start();
        BMPCLocalLauncher.start();
        BMPCLocalLauncher.start();
    }

    @Test
    public void shouldCreateABMPCManager() {
        BMPCManager man = BMPCLocalLauncher.createManager();
        int alreadyRunningProxies = man.getOpenProxies().size();

        // Create 5 proxies
        man.createProxy();
        man.createProxy();
        man.createProxy();
        man.createProxy();
        man.createProxy();

        assertEquals(man.getOpenProxies().size(), alreadyRunningProxies + 5);
        man.closeAll();
        BMPCLocalLauncher.stop();
        BMPCLocalLauncher.uninstall();
    }
}
