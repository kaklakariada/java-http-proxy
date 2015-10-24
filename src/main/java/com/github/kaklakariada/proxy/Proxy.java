package com.github.kaklakariada.proxy;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.littleshoot.proxy.HttpProxyServer;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Proxy {

    private final static Logger LOG = LoggerFactory.getLogger(Proxy.class);

    public static void main(String[] args) {
        LOG.info("Starting...");
        final InetSocketAddress listeningAddress = getInterfaceAddress("eth0", 8888);
        final InetSocketAddress outgoingAddress = getInterfaceAddress("wlan0", 0);

        final HttpProxyServer server = DefaultHttpProxyServer.bootstrap() //
                .withAddress(listeningAddress) //
                .withNetworkInterface(outgoingAddress) //
                .start();
    }

    private static InetSocketAddress getInterfaceAddress(String interfaceName, int port) {
        try {
            final NetworkInterface networkInterface = NetworkInterface.getByName(interfaceName);
            if (networkInterface == null) {
                throw new RuntimeException("Interface " + interfaceName + " not found");
            }
            final List<InetAddress> addresses = Collections.list(networkInterface.getInetAddresses());
            LOG.debug("Found {} addresses for interface {}: {}", addresses.size(), networkInterface.getDisplayName(),
                    addresses);
            final List<InetAddress> usableAddresses = addresses.stream() //
                    .filter(addr -> !addr.isAnyLocalAddress()) //
                    .filter(addr -> !addr.isLinkLocalAddress()) //
                    .filter(addr -> addr.isSiteLocalAddress()) //
                    .filter(addr -> !addr.isMulticastAddress()) //
                    .filter(addr -> !addr.isLoopbackAddress()) //
                    .filter(addr -> addr instanceof Inet4Address) //
                    .collect(Collectors.toList());
            LOG.debug("Found {} usable addresses for interface {}: {}", usableAddresses.size(),
                    networkInterface.getDisplayName(), usableAddresses);
            if (usableAddresses.isEmpty()) {
                throw new RuntimeException("No usable addresses found for " + interfaceName);
            }

            return new InetSocketAddress(usableAddresses.get(0), port);
        } catch (final SocketException e) {
            throw new RuntimeException("Error getting interface " + interfaceName, e);
        }
    }
}
