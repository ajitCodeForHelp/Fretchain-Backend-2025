package com.bt.fretchain.utils;

public class PublicPrivateKeyEncryptionDecryption {

//    public static String getEncrypted(String publicKey, String data) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidKeySpecException, IllegalBlockSizeException, BadPaddingException {
//        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
//        PublicKey pk = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(publicKey.getBytes())));
//        cipher.init(Cipher.ENCRYPT_MODE, pk);
//        byte[] encryptedBytes = cipher.doFinal(data.getBytes());
//        return new String(Base64.getEncoder().encode(encryptedBytes));
//    }
//
//    public static String getDecrypted(String privateKey, String data) throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
//        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
//        PrivateKey pk = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKey.getBytes())));
//        cipher.init(Cipher.DECRYPT_MODE, pk);
//        byte[] encryptedBytes = cipher.doFinal(Base64.getDecoder().decode(data.getBytes()));
//        return new String(encryptedBytes);
//    }

}
