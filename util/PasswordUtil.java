package util;

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

    /*
     * Generate Salt for password generator
     */
    public String generateSalt(int length) {
        return RandomStringUtils.randomAlphanumeric(length);
    }

    /*
     * Generate Hash from password and salt value
     */
    public String generateHash(String password, String salt) {
        return Sha512DigestUtils.shaHex(password+salt);
    }

    /*
     * Generate random password for reset password function
     */
    public String generateRandomPassword() {
        return RandomStringUtils.randomAlphanumeric(LENGTH_OF_PASSWORD);
    }
}
