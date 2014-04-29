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

import java.util.Set;

public interface BMPCManager {

    /**
     * Create a new BMPCProxy Instance
     *
     * @return New BMPCProxy Instance
     */
    public BMPCProxy createProxy();

    /**
     * Create a new BMPCProxy Instance
     *
     * @param upstreamProxyHostAndPort Bind the new Proxy instance to
     *                                 pass through an Upstream Proxy
     * @return New BMPCProxy Instance
     */
    public BMPCProxy createProxy(String upstreamProxyHostAndPort);

    /**
     * Returns list of Proxies currently open (i.e. list of the ports they listen on)
     *
     * @return List of Proxies currently open (i.e. list of the ports they listen on)
     */
    public Set<Integer> getOpenProxies();

    /**
     * Close all Proxies.
     *
     * This will not close the BrowserMob Proxy itself.
     */
    public void closeAll();

    /**
     * Returns host where BrowserMob Proxy is running.
     *
     * @return Host where BrowserMob Proxy is running.
     */
    public String getAPIHost();

    /**
     * Returns port where BrowserMob Proxy is listening.
     *
     * @return Port where BrowserMob Proxy is listening.
     */
    public int getAPIPort();
}
