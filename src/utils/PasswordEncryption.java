package utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class PasswordEncryption {
    
    private static final String HASH_ALGORITHM = "SHA-256";
    private static final int SALT_LENGTH = 16;
    
    public static String encryptPassword(String password) {
        try {
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[SALT_LENGTH];
            random.nextBytes(salt);
            
            String saltedPassword = password + Base64.getEncoder().encodeToString(salt);
            MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hashedBytes = md.digest(saltedPassword.getBytes());
            
            String hash = Base64.getEncoder().encodeToString(hashedBytes);
            String saltString = Base64.getEncoder().encodeToString(salt);
            
            return saltString + ":" + hash;
            
        } catch (NoSuchAlgorithmException e) {
            System.err.println("❌ Encryption algorithm not available: " + e.getMessage());
            return simpleHash(password);
        }
    }
    
    public static boolean verifyPassword(String password, String encryptedPassword) {
        try {
            String[] parts = encryptedPassword.split(":");
            if (parts.length != 2) {
                return simpleHash(password).equals(encryptedPassword);
            }
            
            String saltString = parts[0];
            String storedHash = parts[1];
            
            byte[] salt = Base64.getDecoder().decode(saltString);
            String saltedPassword = password + Base64.getEncoder().encodeToString(salt);
            
            MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hashedBytes = md.digest(saltedPassword.getBytes());
            String hash = Base64.getEncoder().encodeToString(hashedBytes);
            
            return hash.equals(storedHash);
            
        } catch (Exception e) {
            System.err.println("❌ Password verification failed: " + e.getMessage());
            return false;
        }
    }
    
    private static String simpleHash(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = md.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hashedBytes);
        } catch (NoSuchAlgorithmException e) {
            System.err.println("❌ Hash algorithm not available: " + e.getMessage());
            return password;
        }
    }
    
    public static boolean isPasswordSecure(String password) {
        if (password == null || password.length() < 6) {
            return false;
        }
        
        boolean hasLetter = password.matches(".*[a-zA-Z].*");
        boolean hasNumber = password.matches(".*\\d.*");
        
        return hasLetter && hasNumber;
    }
}
