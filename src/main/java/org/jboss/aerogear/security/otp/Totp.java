/**
 * JBoss, Home of Professional Open Source
 * Copyright Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.aerogear.security.otp;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.aerogear.security.otp.api.Clock;
import org.jboss.aerogear.security.otp.api.Digits;
import org.jboss.aerogear.security.otp.api.Hash;
import org.jboss.aerogear.security.otp.api.Hmac;

public class Totp {

    private final String secret;
    private final Clock clock;
    private static final int DELAY_WINDOW = 1;

    /**
     * Initialize an OTP instance with the shared secret generated on Registration process
     *
     * @param secret Shared secret
     */
    public Totp(String secret) {
        this.secret = secret;
        clock = new Clock();
    }

    /**
     * Initialize an OTP instance with the shared secret generated on Registration process
     *
     * @param secret Shared secret
     * @param clock  Clock responsible for retrieve the current interval
     */
    public Totp(String secret, Clock clock) {
        this.secret = secret;
        this.clock = clock;
    }

    /**
     * Prover - To be used only on the client side
     * Retrieves the encoded URI to generated the QRCode required by Google Authenticator
     *
     * @param name Account name
     * @return Encoded URI
     */
    public String uri(String name) {
        try {
            return String.format("otpauth://totp/%s?secret=%s", URLEncoder.encode(name, "UTF-8"), secret);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    /**
     * Retrieves the current OTP
     *
     * @return OTP
     */
    public String now() {
        return leftPadding(hash(secret, clock.getCurrentInterval()));
    }

    /**
     * Verifier - To be used only on the server side
     * <p/>
     * Taken from Google Authenticator with small modifications from
     * {@see <a href="http://code.google.com/p/google-authenticator/source/browse/src/com/google/android/apps/authenticator/PasscodeGenerator.java?repo=android#212">PasscodeGenerator.java</a>}
     * <p/>
     * Verify a timeout code. The timeout code will be valid for a time
     * determined by the interval period and the number of adjacent intervals
     * checked.
     *
     * @param otp Timeout code
     * @return True if the timeout code is valid
     *         <p/>
     *         Author: sweis@google.com (Steve Weis)
     */
    public boolean verify(String otp) {
    	try {
	        long code = Long.parseLong(otp);
	        long currentInterval = clock.getCurrentInterval();
	
	        int pastResponse = Math.max(DELAY_WINDOW, 0);
	
	        for (int i = pastResponse; i >= 0; --i) {
	            int candidate = generate(this.secret, currentInterval - i);
	            if (candidate == code) {
	                return true;
	            }
	        }
    	}
    	catch (NumberFormatException nfe) {
    		Logger.getLogger(getClass().getName()).log(Level.WARNING, "invalid otp", nfe);
    	}
        return false;
    }

    private int generate(String secret, long interval) {
        return hash(secret, interval);
    }

    private int hash(String secret, long interval) {
        byte[] hash = new byte[0];
        try {
            //Base32 encoding is just a requirement for google authenticator. We can remove it on the next releases.
//            hash = new Hmac(Hash.SHA1, Base32.decode(secret), interval).digest();
            hash = new Hmac(Hash.SHA1, secret.getBytes("UTF-8"), interval).digest();
           
        } catch (Exception e) {
           throw new RuntimeException(e);
        }
        return bytesToInt(hash);
    }

    private int bytesToInt(byte[] hash) {
        // put selected bytes into result int
        int offset = hash[hash.length - 1] & 0xf;

        int binary = ((hash[offset] & 0x7f) << 24) |
                ((hash[offset + 1] & 0xff) << 16) |
                ((hash[offset + 2] & 0xff) << 8) |
                (hash[offset + 3] & 0xff);

        return binary % Digits.SIX.getValue();
    }

    private String leftPadding(int otp) {
        return String.format("%06d", otp);
    }

}
