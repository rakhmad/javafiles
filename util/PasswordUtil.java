package com.pusilkom.artajasa.billing.util;

import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.token.Sha512DigestUtils;

import java.security.MessageDigest;

public class PasswordUtil {

    private Logger logger;

    private MessageDigest digest;
    
    final int LENGTH_OF_PASSWORD = 10;

    public PasswordUtil() {
        logger = LoggerFactory.getLogger(PasswordUtil.class);
    }
    public String generateSalt(int length) {
        return RandomStringUtils.randomAlphanumeric(length);
    }

    public String generateHash(String password, String salt) {
        return Sha512DigestUtils.shaHex(password+salt);
    }

    public String generateRandomPassword() {
        return RandomStringUtils.randomAlphanumeric(LENGTH_OF_PASSWORD);
    }
}
