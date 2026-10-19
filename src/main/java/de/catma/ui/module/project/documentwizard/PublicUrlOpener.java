package de.catma.ui.module.project.documentwizard;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URL;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import de.catma.properties.CATMAPropertyKey;

/**
 * Opens a URL that a user entered to import a document from.
 * <p>
 * The URL is fetched by the server, so without these checks a user could read files from the server (<code>file:</code> and other
 * protocols) or reach hosts that only the server can reach (internal services, cloud metadata endpoints, ...). Redirects are followed
 * here rather than by {@link HttpURLConnection}, so that every target is checked. Hosts listed in
 * {@link CATMAPropertyKey#DOCUMENT_URL_IMPORT_ALLOWED_INTERNAL_HOSTS} are exempt from the address check, but not from the protocol check.
 * <p>
 * Remaining gap: the host name is resolved again when connecting. With the JVM's default DNS cache, that second lookup returns the address
 * that was checked, so a DNS server that answers differently the second time (DNS rebinding) only gets past the address check if the cache
 * TTL (<code>networkaddress.cache.ttl</code>) is set to 0, or if connections go through an HTTP proxy, which resolves the name itself.
 */
class PublicUrlOpener {
	private static final int MAX_REDIRECTS = 5;
	private static final Set<Integer> REDIRECT_STATUS_CODES = Set.of(301, 302, 303, 307, 308);

	// the JDK's defaults are infinite, and the connection is opened on the UI thread, holding the user's session; the read timeout applies to each
	// read, so it also covers the download that follows, without limiting its total duration
	private static final int CONNECT_TIMEOUT_MILLIS = 10_000;
	private static final int READ_TIMEOUT_MILLIS = 30_000;

	/**
	 * @return a connection whose response is not a redirect, ready for {@link HttpURLConnection#getInputStream()}
	 * @throws IOException if the URL or a redirect target is not an http(s) URL with a public or allowed host, or on connection errors
	 */
	static HttpURLConnection open(URL url) throws IOException {
		Set<String> allowedInternalHosts = parseHostList(CATMAPropertyKey.DOCUMENT_URL_IMPORT_ALLOWED_INTERNAL_HOSTS.getValue());

		for (int redirects = 0; redirects <= MAX_REDIRECTS; redirects++) {
			checkPublic(url, allowedInternalHosts);

			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setInstanceFollowRedirects(false);
			connection.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
			connection.setReadTimeout(READ_TIMEOUT_MILLIS);

			if (!REDIRECT_STATUS_CODES.contains(connection.getResponseCode())) {
				return connection;
			}

			String location = connection.getHeaderField("Location");
			connection.disconnect();
			if (location == null) {
				throw new IOException(String.format("%s redirects without giving a location", url));
			}
			url = new URL(url, location);
		}

		throw new IOException(String.format("Too many redirects, gave up at %s", url));
	}

	static Set<String> parseHostList(String hostList) {
		if (hostList == null) {
			return Set.of();
		}
		return Arrays.stream(hostList.split(","))
				.map(host -> host.trim().toLowerCase(Locale.ROOT))
				.filter(host -> !host.isEmpty())
				.collect(Collectors.toSet());
	}

	static void checkPublic(URL url, Set<String> allowedInternalHosts) throws IOException {
		String protocol = url.getProtocol().toLowerCase();
		if (!protocol.equals("http") && !protocol.equals("https")) {
			throw new IOException(String.format("Only http and https URLs can be imported, not %s", url));
		}

		String host = url.getHost();
		if (host == null || host.isEmpty()) {
			throw new IOException(String.format("%s has no host", url));
		}

		if (allowedInternalHosts.contains(host.toLowerCase(Locale.ROOT))) {
			return;
		}

		for (InetAddress address : InetAddress.getAllByName(host)) {
			if (!isPublic(address)) {
				throw new IOException(String.format("%s does not resolve to a public address", host));
			}
		}
	}

	static boolean isPublic(InetAddress address) {
		if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
				|| address.isSiteLocalAddress() || address.isMulticastAddress()) {
			return false;
		}

		byte[] bytes = address.getAddress();

		if (address instanceof Inet4Address) {
			return bytes[0] != 0 // 0.0.0.0/8, "this network"
					&& !((bytes[0] & 0xff) == 100 && (bytes[1] & 0xc0) == 64); // 100.64.0.0/10, carrier-grade NAT
		}

		if (address instanceof Inet6Address) {
			if ((bytes[0] & 0xfe) == 0xfc) { // fc00::/7, unique local (isSiteLocalAddress only covers the deprecated fec0::/10)
				return false;
			}
			if (((Inet6Address) address).isIPv4CompatibleAddress()) { // ::a.b.c.d (IPv4-mapped ones already arrive as Inet4Address)
				try {
					return isPublic(InetAddress.getByAddress(new byte[] {bytes[12], bytes[13], bytes[14], bytes[15]}));
				}
				catch (IOException e) {
					return false;
				}
			}
		}

		return true;
	}
}
