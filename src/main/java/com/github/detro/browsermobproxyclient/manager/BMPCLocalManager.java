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

package com.github.detro.browsermobproxyclient.manager;

import com.github.detro.browsermobproxyclient.BMPCProxy;
import com.github.detro.browsermobproxyclient.exceptions.BMPCLocalStartStopException;
import org.openqa.selenium.net.PortProber;
import org.openqa.selenium.net.UrlChecker;

import java.io.*;
import java.net.ServerSocket;
import java.net.URL;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class BMPCLocalManager implements BMPCManager {

    public static final String DEFAULT_HOST = "localhost";
    private final int port;
    private final String logPath;
    private Process process = null;
    private InputStream stdout = null;
    private Thread jvmShotdownHook;

    private final BMPCDefaultManager defaultManager;

    public BMPCLocalManager(String executablePath, String logPath) {
        this(executablePath, logPath, PortProber.findFreePort());
    }

    public BMPCLocalManager(String executablePath, final String logPath, int port) {
        this.port = port;
        this.logPath = logPath + "." + this.port;

        final String failStartExceptionMsg = String.format(
                "Failed to start Local BrowserMob Proxy on port '%d'", this.port);

        // Ensure nothing is running on the given Port at the moment
        if (!isPortFree(this.port)) {
            throw new BMPCLocalStartStopException(failStartExceptionMsg);
        }

        // Start Local BrowserMob Proxy external process
        try {
            process = new ProcessBuilder(executablePath,
                    "-port",
                    String.valueOf(this.port))
                    .redirectErrorStream(true)
                    .start();
            stdout = process.getInputStream();

            // Wait for Proxy to start accepting requests
            new UrlChecker().waitUntilAvailable(
                    20, TimeUnit.SECONDS,
                    new URL("http://localhost:" + this.port + "/proxy"));

            // Throw exception if not running
            if (!isRunning()) {
                throw new BMPCLocalStartStopException(failStartExceptionMsg);
            }
        } catch (Exception e) {
            throw new BMPCLocalStartStopException(failStartExceptionMsg, e);
        }

        // Launch a Thread to output Local BrowserMob Proxy output to file
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Prepare input and output
                    String lineSeparator = System.getProperty("line.separator");
                    BufferedReader reader = new BufferedReader(new InputStreamReader(stdout));
                    BufferedWriter writer = new BufferedWriter(new FileWriter(BMPCLocalManager.this.logPath, true));

                    // Mark beginning of this Log
                    writer.write("*** Local BrowserMob Proxy (port " +
                            BMPCLocalManager.this.port +
                            ") STARTED ***" + lineSeparator);

                    while(BMPCLocalManager.this.isRunning()) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            writer.write(line + lineSeparator);
                            writer.flush();
                        }
                        Thread.currentThread().sleep(250);
                    }

                    reader.close();
                    writer.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();

        // Register JVM Shutdown Hook to ensure we stop the
        // Local BrowserMob Proxy if client code doesn't
        jvmShotdownHook = new Thread(new Runnable() {
            @Override
            public void run() {
                BMPCLocalManager.this.stop();
            }
        });
        enableShutdownWithJVM();

        // Initialize internal Default Manager
        defaultManager = new BMPCDefaultManager(DEFAULT_HOST, this.port);
    }

    /**
     * Enables automatic shutdown when JVM shuts-down (gracefully).
     *
     * This is enabled by default.
     * NOTE: The hooks to the JVM Shutdown don't work in case of "non-graceful" shutdown.
     */
    public synchronized void enableShutdownWithJVM() {
        Runtime.getRuntime().addShutdownHook(jvmShotdownHook);
    }

    /**
     * Disables automatic shutdown when JVM shuts-down (gracefully).
     */
    public synchronized void disableShutdownWithJVM() {
        Runtime.getRuntime().removeShutdownHook(jvmShotdownHook);
    }

    /**
     * Is the automatic shutdown when JVM shuts-down enabled?
     *
     * @return Is the automatic shutdown when JVM shuts-down enabled?
     */
    public synchronized boolean isShutdownWithJVMEnabled() {
        try {
            // If the following doesn't throw an exception,
            // than it was not already registered.
            enableShutdownWithJVM();
            disableShutdownWithJVM();
            return false;
        } catch (IllegalArgumentException iae) {
            return true;
        }
    }

    /**
     * Returns path to the Log written by this Manager.
     *
     * @return String representing path to the log file.
     */
    public String getLogPath() {
        return logPath;
    }

    /**
     * Check if Local BrowserMob Proxy is running.
     *
     * @return "true" if Local BrowserMob Proxy is running.
     */
    public synchronized boolean isRunning() {
        if (null == process) return false;

        try {
            process.exitValue();
            return false;
        } catch(IllegalThreadStateException itse) {
            return true;
        }
    }

    /**
     * Stop Local BrowserMob proxy
     */
    public synchronized void stop() {
        if (isRunning()) {
            try {
                process.destroy();
                process.waitFor();
                process = null;
                stdout = null;
            } catch (Exception ie) {
                throw new BMPCLocalStartStopException(
                        String.format("Failed to stop Local BrowserMob Proxy on port '%d'", port),
                        ie);
            }
        }
    }

    @Override
    public BMPCProxy createProxy() {
        return defaultManager.createProxy();
    }

    @Override
    public BMPCProxy createProxy(String upstreamProxyHostAndPort) {
        return defaultManager.createProxy(upstreamProxyHostAndPort);
    }

    @Override
    public Set<Integer> getOpenProxies() {
        return defaultManager.getOpenProxies();
    }

    @Override
    public void closeAll() {
        defaultManager.closeAll();
    }

    @Override
    public String getAPIHost() {
        return defaultManager.getAPIHost();
    }

    @Override
    public int getAPIPort() {
        return defaultManager.getAPIPort();
    }

    private boolean isPortFree(int port) {
        ServerSocket socket = null;
        try {
            socket = new ServerSocket(port);
            return true;
        } catch (IOException e) {
            /* ignore */
        } finally {
            if (socket != null) {
                try { socket.close(); } catch (IOException ioe) { /* ignore */ }
            }
        }

        return false;
    }
}
