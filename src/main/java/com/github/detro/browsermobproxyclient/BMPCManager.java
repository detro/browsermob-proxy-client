package com.github.detro.browsermobproxyclient;

import java.util.Set;

public interface BMPCManager {

    public BMPCProxy createProxy();

    public BMPCProxy createProxy(String upstreamProxyHostAndPort);

    public Set<Integer> getOpenProxies();

    public void closeAll();

    public String getAPIHost();

    public int getAPIPort();
}
