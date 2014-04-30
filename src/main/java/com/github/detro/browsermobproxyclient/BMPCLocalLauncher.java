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

import com.github.detro.browsermobproxyclient.exceptions.BMPCLocalNotInstalledException;
import com.github.detro.browsermobproxyclient.exceptions.BMPCUnexpectedErrorException;
import com.github.detro.browsermobproxyclient.manager.BMPCLocalManager;
import org.openqa.selenium.net.PortProber;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Utility class that takes care of Installing/Uninstalling Local Browser Mob Proxy as well as Starting and Stopping it.
 * <p/>
 *
 * "Local" means that it's located inside the Current User directory, inside
 * <code>.browsermob-proxy-client/browsermob-proxy-local</code>.
 * <p/>
 *
 * Using the "Local installation", this class can launch multiple "Managers".
 * Precisely, the various running BrowserMob Proxies are represented by
 * {@code BMPCLocalManager}.
 * <p/>
 *
 * Those instances are started when created, but it's left to the client
 * code to deal with cleanup.
 * <p/>
 *
 * A version of BrowserMob Proxy is contained within the resources of this
 * library. This class takes care of installing/uninstalling (zip/delete) it.
 */
public class BMPCLocalLauncher {

    private static final String USER_HOME_DIR = System.getProperty("user.home");
    private static final String BMPC_USER_DIR = USER_HOME_DIR + File.separator + ".browsermob-proxy-client";

    private static final String BMP_LOCAL_ZIP_RES = "/browsermob-proxy-local.zip";
    private static final String BMP_LOCAL_INSTALL_DIR = BMPC_USER_DIR + File.separator + "browsermob-proxy-local";
    private static final String BMP_LOCAL_LOG_FILE = BMPC_USER_DIR + File.separator + "browsermob-proxy-local.log";
    private static final String BMP_LOCAL_VERSION_FILE = BMP_LOCAL_INSTALL_DIR + File.separator + "VERSION.txt";
    private static final String BMP_LOCAL_EXEC_UNIX = BMP_LOCAL_INSTALL_DIR + File.separator + "bin" + File.separator + "browsermob-proxy";
    private static final String BMP_LOCAL_EXEC_WIN = BMP_LOCAL_INSTALL_DIR + File.separator + "bin" + File.separator + "browsermob-proxy.bat";

    public static final int BMP_LOCAL_DEFAULT_PORT = 8080;

    /**
     * See {@link com.github.detro.browsermobproxyclient.BMPCLocalLauncher#launch(int)}
     */
    public static BMPCLocalManager launchOnRandomPort() {
        return launch(PortProber.findFreePort());
    }

    /**
     * See {@link com.github.detro.browsermobproxyclient.BMPCLocalLauncher#launch(int)}
     */
    public static BMPCLocalManager launchOnDefaultPort() {
        return launch(BMP_LOCAL_DEFAULT_PORT);
    }

    /**
     * Launch Local BrowserMob Proxy and return a BMPCManager to handle it.
     *
     * It will also install it if not installed yet.
     *
     * @param port Port to bind Local BrowserMob Proxy to
     * @return Instance of BMPCManager
     */
    public static BMPCLocalManager launch(int port) {
        install();
        return new BMPCLocalManager(executablePerOS(), BMP_LOCAL_LOG_FILE, port);
    }

    /**
     * Is a Local instance of BrowserMob Proxy installed?
     *
     * @return Returns "true" if a Local BrowserMob Proxy is installed,
     *         "false" otherwise.
     */
    public synchronized static boolean isInstalled() {
        try {
            installedVersion();
            return true;
        } catch (BMPCLocalNotInstalledException nie) {
            return false;
        }
    }

    /**
     * Install Local BrowserMob Proxy.
     */
    public synchronized static void install() {
        if (!isInstalled()) {
            InputStream is = BMPCLocalLauncher.class.getResourceAsStream(BMP_LOCAL_ZIP_RES);
            try {
                // Unzip BrowserMob Proxy contained in the project "/resources"
                unzip(is, BMPC_USER_DIR);

                // Set executable permissions on the BrowserMob Proxy lanching scripts
                new File(BMP_LOCAL_EXEC_UNIX).setExecutable(true);
                new File(BMP_LOCAL_EXEC_WIN).setExecutable(true);

                // Check there is an installed version
                installedVersion();
            } catch (Exception e) {
                throw new BMPCUnexpectedErrorException("Installation failed", e);
            }
        }
    }

    /**
     * Uninstall Local BrowserMob Proxy.
     */
    public synchronized static void uninstall() {
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
    public synchronized static String installedVersion() {
        BufferedReader versionReader = null;
        try {
            versionReader = new BufferedReader(new FileReader(BMP_LOCAL_VERSION_FILE));

            // Read version and verify it's there
            String version = versionReader.readLine();
            if (null == version) throw new Exception();

            return version;
        } catch(Exception e) {
            throw new BMPCLocalNotInstalledException(
                    "Version file not found: " + BMP_LOCAL_VERSION_FILE);
        } finally {
            try {
                if (null != versionReader) versionReader.close();
            } catch (IOException e) {
                // Do nothing here
            }
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
    private synchronized static void unzip(InputStream zipFileInputStream, String destinationDir) throws IOException {
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
    private synchronized static void delete(File file) {
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
