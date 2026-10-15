package de.catma.ui.module.project.documentwizard;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.util.Set;

import org.junit.jupiter.api.Test;

class PublicUrlOpenerTest {

	@Test
	void rejectsInternalAddresses() throws IOException {
		String[] addresses = {
				"0.0.0.0", "0.1.2.3", "127.0.0.1", "10.1.2.3", "172.16.0.1", "192.168.1.1", "169.254.169.254", "100.64.0.1", "224.0.0.1",
				"::", "::1", "fe80::1", "fc00::1", "fd12:3456::1", "fec0::1", "ff02::1", "::ffff:127.0.0.1", "::ffff:10.0.0.1", "::127.0.0.1"
		};
		for (String address : addresses) {
			assertFalse(PublicUrlOpener.isPublic(InetAddress.getByName(address)), address);
		}
	}

	@Test
	void acceptsPublicAddresses() throws IOException {
		String[] addresses = {"8.8.8.8", "100.63.255.255", "100.128.0.1", "2001:4860:4860::8888", "::ffff:8.8.8.8"};
		for (String address : addresses) {
			assertTrue(PublicUrlOpener.isPublic(InetAddress.getByName(address)), address);
		}
	}

	@Test
	void checkRejectsNonHttpAndInternalUrls() throws IOException {
		String[] urls = {
				"file:///etc/passwd", "jar:file:///tmp/x.jar!/a", "ftp://8.8.8.8/x",
				"http://127.0.0.1/", "http://localhost:8080/", "https://[::1]/", "http://169.254.169.254/latest/meta-data/", "http://10.0.0.1/"
		};
		for (String url : urls) {
			assertThrows(IOException.class, () -> PublicUrlOpener.checkPublic(new URL(url), Set.of()), url);
		}
	}

	@Test
	void checkAcceptsPublicHttpAndHttps() throws IOException {
		PublicUrlOpener.checkPublic(new URL("http://8.8.8.8/doc.txt"), Set.of());
		PublicUrlOpener.checkPublic(new URL("HTTPS://8.8.8.8/doc.txt"), Set.of());
	}

	@Test
	void allowedInternalHostsSkipOnlyTheAddressCheck() throws IOException {
		Set<String> allowed = PublicUrlOpener.parseHostList(" Intranet.example , 10.0.0.1,,");
		assertEquals(Set.of("intranet.example", "10.0.0.1"), allowed);

		PublicUrlOpener.checkPublic(new URL("http://10.0.0.1/doc.txt"), allowed);
		PublicUrlOpener.checkPublic(new URL("https://INTRANET.example/doc.txt"), allowed);
		assertThrows(IOException.class, () -> PublicUrlOpener.checkPublic(new URL("http://10.0.0.2/"), allowed));
		assertThrows(IOException.class, () -> PublicUrlOpener.checkPublic(new URL("ftp://10.0.0.1/doc.txt"), allowed));
		assertEquals(Set.of(), PublicUrlOpener.parseHostList(null));
	}
}
