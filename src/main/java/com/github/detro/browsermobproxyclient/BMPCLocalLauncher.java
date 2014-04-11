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

import com.github.detro.browsermobproxyclient.exceptions.BMPCLocalNotInstallerException;
import com.github.detro.browsermobproxyclient.exceptions.BMPCLocalStartStopException;
import com.github.detro.browsermobproxyclient.exceptions.BMPCUnexpectedErrorException;
import org.openqa.selenium.net.UrlChecker;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Utility class that takes care of Starting and Stopping a Local BrowserMob Proxy.
 *
 * "Local" means that it's located inside the Current User directory, inside
 * <code>.browsermob-proxy-client/browsermob-proxy-local</code>.
 *
 * A version of BrowserMob Proxy is contained within the resources of this
 * library. This class takes care of installing/uninstalling (zip/delete) it.
 */
public class BMPCLocalLauncher {

    private static final String USER_HOME_DIR = System.getProperty("user.home");
    private static final String BMPC_USER_DIR = USER_HOME_DIR + File.separator + ".browsermob-proxy-client";

    private static final String BMP_LOCAL_ZIP_RES = File.separator + "browsermob-proxy-local.zip";
    private static final String BMP_LOCAL_INSTALL_DIR = BMPC_USER_DIR + File.separator + "browsermob-proxy-local";
    private static final String BMP_LOCAL_LOG_FILE = BMPC_USER_DIR + File.separator + "browsermob-proxy-local.log";
    private static final String BMP_LOCAL_VERSION_FILE = BMP_LOCAL_INSTALL_DIR + File.separator + "VERSION.txt";
    private static final String BMP_LOCAL_EXEC_UNIX = BMP_LOCAL_INSTALL_DIR + File.separator + "bin" + File.separator + "browsermob-proxy";
    private static final String BMP_LOCAL_EXEC_WIN = BMP_LOCAL_INSTALL_DIR + File.separator + "bin" + File.separator + "browsermob-proxy.bat";

    private static final int BMP_LOCAL_DEFAULT_PORT = 8080;

    private static Process BMPProcess = null;
    private static int BMPPort = -1;

    /**
     * Start Local BrowserMob Proxy.
     * It will also install it if not installed yet.
     */
    public synchronized static void start() {
        start(BMP_LOCAL_DEFAULT_PORT);
    }

    /**
     * Start Local BrowserMob Proxy.
     * It will also install it if not installed yet.
     *
     * @param port Port to which BrowserMob Proxy needs to bind it's API
     */
    public synchronized static void start(int port) {
        // Ensure Local BrowserMob Proxy is installed
        if (!isInstalled()) {
            install();
        }

        BMPPort = port;

        // Start Local BrowserMob Proxy external process
        String failStartExceptionMsg = "Failed to start Local BrowserMob Proxy";
        try {
            BMPProcess = new ProcessBuilder(executablePerOS(),
                        "-port",
                        String.valueOf(BMPPort))
                    .redirectErrorStream(true)
                    .redirectOutput(new File(BMP_LOCAL_LOG_FILE))
                    .start();

            // Wait for Proxy to start accepting requests
            new UrlChecker().waitUntilAvailable(
                    20, SECONDS,
                    new URL("http://localhost:" + port + "/proxy"));

            // Throw exception if not running
            if (!isRunning()) {
                throw new BMPCLocalStartStopException(failStartExceptionMsg);
            }
        } catch (IOException | UrlChecker.TimeoutException e) {
            throw new BMPCLocalStartStopException(failStartExceptionMsg, e);
        }
    }

    /**
     * Check if Local BrowserMob Proxy is running.
     *
     * @return "true" if Local BrowserMob Proxy is running.
     */
    public synchronized static boolean isRunning() {
        if (null == BMPProcess) return false;

        try {
            BMPProcess.exitValue();
            return false;
        } catch(IllegalThreadStateException itse) {
            return true;
        }
    }

    /**
     * Stop Local BrowserMob proxy
     */
    public synchronized static void stop() {
        try {
            BMPProcess.destroy();
            BMPProcess.waitFor();
            BMPProcess = null;
            BMPPort = -1;
        } catch (InterruptedException ie) {
            throw new BMPCLocalStartStopException("Failed to stop Local BrowserMob Proxy", ie);
        }
    }

    /**
     * Returns the port on which Local BrowserMob Proxy is running.
     *
     * @return Port on which Local BrowserMob Proxy is running
     */
    public static int port() {
        return BMPPort;
    }

    /**
     * Is a Local instance of BrowserMob Proxy installed?
     *
     * @return Returns "true" if a Local BrowserMob Proxy is installed,
     *         "false" otherwise.
     */
    public static boolean isInstalled() {
        File installDir = new File(BMP_LOCAL_INSTALL_DIR);
        return installDir.exists() && installDir.isDirectory();
    }

    /**
     * Install Local BrowserMob Proxy.
     */
    public static void install() {
        InputStream is = BMPCLocalLauncher.class.getResourceAsStream(BMP_LOCAL_ZIP_RES);
        try {
            // Unzip BrowserMob Proxy contained in the project "/resources"
            unzip(is, BMPC_USER_DIR);

            // Set executable permissions on the BrowserMob Proxy lanching scripts
            new File(BMP_LOCAL_EXEC_UNIX).setExecutable(true);
            new File(BMP_LOCAL_EXEC_WIN).setExecutable(true);

            // Check there is an installed version
            installedVersion();
        } catch (BMPCLocalNotInstallerException | IOException e) {
            throw new BMPCUnexpectedErrorException("Installation failed", e);
        }
    }

    /**
     * Uninstall Local BrowserMob Proxy.
     */
    public static void uninstall() {
        if (isInstalled()) {
            File installDir = new File(BMP_LOCAL_INSTALL_DIR);
            delete(installDir);
        }
    }

    /**
     * Installed version of Local BrowserMob Proxy
     *
     * @return Version of Local BrowserMob Proxy, if installed.
     *         Throws @see BMPCLocalLauncherNotInstallerException if not installed.
     *         Please use BMPCLocalLauncher#isInstalled() to check first.
     */
    public static String installedVersion() {
        try {
            return Files.readAllLines(Paths.get(BMP_LOCAL_VERSION_FILE), Charset.forName("UTF-8")).get(0);
        } catch (IOException e) {
            throw new BMPCLocalNotInstallerException(
                    "Version file not found: " + BMP_LOCAL_VERSION_FILE);
        }
    }

    /**
     * Return the right executable script path, based on the OS.
     *
     * @return Right executable script path, based on the OS
     */
    private static String executablePerOS() {
        return System.getProperty("os.name").toLowerCase().contains("windows")
                ? BMP_LOCAL_EXEC_WIN
                : BMP_LOCAL_EXEC_UNIX;
    }

    /**
     * Unzip ZIP file (as InputStream) to a destination directory.
     *
     * @param zipFileInputStream Zip File InputStream
     * @param destinationDir Destination Directory
     */
    private static void unzip(InputStream zipFileInputStream, String destinationDir) throws IOException {
        // Create output directory if it doesn't exist
        File dir = new File(destinationDir);
        if (!dir.exists()) dir.mkdirs();

        // Buffer for read and write data to file
        byte[] buffer = new byte[1024];
        ZipInputStream zis = new ZipInputStream(zipFileInputStream);
        ZipEntry ze = zis.getNextEntry();
        while (ze != null) {
            String fileName = ze.getName();
            File newFile = new File(destinationDir + File.separator + fileName);

            // Check if it's a Directory entry
            if (ze.isDirectory()) {
                newFile.mkdirs();
            } else {
                // Save file to disk
                FileOutputStream fos = new FileOutputStream(newFile);
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
            }

            // Close current entry and move to next entry
            zis.closeEntry();
            ze = zis.getNextEntry();
        }

        // Close last entry
        zis.closeEntry();
        zis.close();
    }

    /**
     * Delete file, recursively if needed
     *
     * @param file File/Directory to delete
     */
    private static void delete(File file) {
        // Check if file is directory
        if (file.isDirectory()) {
            // Get all files in the folder
            File[] files = file.listFiles();

            // Delete each file in the folder
            for (int i = 0; i < files.length; ++i) {
                delete(files[i]);
            }

            // Delete the folder
            file.delete();
        } else {
            // Delete the file if it is not a folder
            file.delete();
        }
    }

}
