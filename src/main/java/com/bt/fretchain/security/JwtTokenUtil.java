package com.bt.fretchain.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtTokenUtil implements Serializable {

    private static final long serialVersionUID = -3776792039227742546L;

    private long jwtTokenValidity = 604800; // in Seconds // > 604800 > 7 Days

    // @Value("${helpall.jwt.secret}")
    private String secret = "06Z++5lK0edmx1Uz845xga/4BFHo2HWClWT1ulXHH808goW9+D87uydk0tpQs1ByhE4jOuHBukmbfKQeIH4dXmtqZEH4aui2dtth0Z6bR9iQatv/RgrFx5VeJr4pntrfa5SjVHRuOCYmiEYIGApp+sqAlB759b/bDe0bYwI8DFaesnnkJ/xODJOjpyk5W/91QaiJcjH0Xm5dgPR7r5xg/2S1909EqQrl0hPowA==";

    public String getUsernameFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    public Date getIssuedAtDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getIssuedAt);
    }

    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }
    public String validateToken(String authToken){
        String[] s = authToken.split(" ");
        if(s.length > 1){ authToken = s[1];
        } else { authToken = s[0]; }
        Claims claims = getAllClaimsFromToken(authToken);
        return (String) claims.get("username");
    }

    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parser().setSigningKey(secret.getBytes(Charset.forName("UTF-8")))
                .parseClaimsJws(token).getBody();
    }

    public Object getValueFromClaimsKey(String token, String key) {
        Claims claims = Jwts.parser().setSigningKey(secret.getBytes(Charset.forName("UTF-8")))
                .parseClaimsJws(token).getBody();
        if (claims.containsKey(key)) {
            return claims.get(key);
        }
        return null;
    }

    private Boolean isTokenExpired(String token) {
        final Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }

    public String generateToken(Map<String, Object> claims, UserDetails userDetails) {
        if (claims == null) {
            claims = new HashMap<>();
        }
        return doGenerateToken(claims, userDetails.getUsername());
    }

    private String doGenerateToken(Map<String, Object> claims, String subject) {
        return Jwts.builder().setClaims(claims).setSubject(subject).setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + jwtTokenValidity * 1000))
                .signWith(SignatureAlgorithm.HS512, secret.getBytes(Charset.forName("UTF-8")))
                .compact();
    }

    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = getUsernameFromToken(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }
}
