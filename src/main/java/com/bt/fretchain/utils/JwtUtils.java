package com.bt.fretchain.utils;


import io.jsonwebtoken.*;

import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.util.*;

public class JwtUtils {

    private static String secret = "aksjfqAFEIFJOIJRgworij2392FDSFwef92834WEFwe928374SWFew";
    private static Key hmacKey = new SecretKeySpec(Base64.getDecoder().decode(secret), SignatureAlgorithm.HS256.getJcaName());

    public static String createJWT(Map<String, Object> keyValuePair) {
        Calendar calendar = Calendar.getInstance();
        Date issueAt = calendar.getTime();
        calendar.add(Calendar.MINUTE, 43200);
        Date expiration = calendar.getTime();
        JwtBuilder jwtBuilder = Jwts.builder()
                .setIssuer("https://btEcommerce.in")
                .setSubject("Details")
                .setId(UUID.randomUUID().toString());
        for (String key : keyValuePair.keySet()) {
            jwtBuilder.claim(key, keyValuePair.get(key));
        }
        String jwtToken = jwtBuilder
                .setIssuedAt(issueAt)
                .setExpiration(expiration)
                .signWith(SignatureAlgorithm.HS256, hmacKey)
                .compact();
        return jwtToken;
    }

    public static Map<String, Object> parseJwt(String jwtString) {
        Jws<Claims> jwt = Jwts.parser()
                .setSigningKey(hmacKey)
                .parseClaimsJws(jwtString);
        Map<String, Object> keyValuePair = new LinkedHashMap<>();
        for (String key : jwt.getBody().keySet()) {
            keyValuePair.put(key, jwt.getBody().get(key));
        }
        return keyValuePair;
    }
}
