/*
 * Copyright (c) Microsoft. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for full license information.
 */
package com.microsoft.azure.eventhubs;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class SharedAccessSignatureTokenProvider {
    final String keyName;
    final String sharedAccessKey;
    final String sharedAccessSignature;

    SharedAccessSignatureTokenProvider(
            final String keyName,
            final String sharedAccessKey) {
        this.keyName = keyName;
        this.sharedAccessKey = sharedAccessKey;
        this.sharedAccessSignature = null;
    }

    public SharedAccessSignatureTokenProvider(final String sharedAccessSignature) {
        this.keyName = null;
        this.sharedAccessKey = null;
        this.sharedAccessSignature = sharedAccessSignature;
    }

    public String getToken(final String resource, final Duration tokenTimeToLive) throws IOException, InvalidKeyException, NoSuchAlgorithmException {
        return this.sharedAccessSignature == null
                ? generateSharedAccessSignature(this.keyName, this.sharedAccessKey, resource, tokenTimeToLive)
                : this.sharedAccessSignature;
    }

    public static String generateSharedAccessSignature(
            final String keyName,
            final String sharedAccessKey,
            final String resource,
            final Duration tokenTimeToLive)
            throws IOException, NoSuchAlgorithmException, InvalidKeyException {
        if (StringUtil.isNullOrWhiteSpace(keyName)) {
            throw new IllegalArgumentException("keyName cannot be empty");
        }

        if (StringUtil.isNullOrWhiteSpace(sharedAccessKey)) {
            throw new IllegalArgumentException("sharedAccessKey cannot be empty");
        }

        if (StringUtil.isNullOrWhiteSpace(resource)) {
            throw new IllegalArgumentException("resource cannot be empty");
        }

        if (tokenTimeToLive.isZero() || tokenTimeToLive.isNegative()) {
            throw new IllegalArgumentException("tokenTimeToLive has to positive and in the order-of seconds");
        }

        final String utf8Encoding = StandardCharsets.UTF_8.name();
        String expiresOn = Long.toString(Instant.now().getEpochSecond() + tokenTimeToLive.getSeconds());
        String audienceUri = URLEncoder.encode(resource, utf8Encoding);
        String secretToSign = audienceUri + "\n" + expiresOn;

        final String hashAlgorithm = "HMACSHA256";
        Mac hmac = Mac.getInstance(hashAlgorithm);
        byte[] sasKeyBytes = sharedAccessKey.getBytes(utf8Encoding);
        SecretKeySpec finalKey = new SecretKeySpec(sasKeyBytes, hashAlgorithm);
        hmac.init(finalKey);
        byte[] signatureBytes = hmac.doFinal(secretToSign.getBytes(utf8Encoding));
        String signature = Base64.getEncoder().encodeToString(signatureBytes);

        return String.format(Locale.US, "SharedAccessSignature sr=%s&sig=%s&se=%s&skn=%s",
                audienceUri,
                URLEncoder.encode(signature, utf8Encoding),
                URLEncoder.encode(expiresOn, utf8Encoding),
                URLEncoder.encode(keyName, utf8Encoding));
    }
}
