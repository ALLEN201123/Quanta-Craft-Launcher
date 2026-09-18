package com.qcl.launcher.utils;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.Base64;

/* loaded from: classes2.dex */
public final class KeyUtils {
    private KeyUtils() {
    }

    public static KeyPair generateKey() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(4096, new SecureRandom());
            return keyPairGenerator.genKeyPair();
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    public static String toPEMPublicKey(PublicKey publicKey) {
        return "-----BEGIN PUBLIC KEY-----\n" + Base64.getMimeEncoder(76, new byte[]{10}).encodeToString(publicKey.getEncoded()) + "\n-----END PUBLIC KEY-----\n";
    }
}
