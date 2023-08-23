package com.example.Final_Project_9team.global.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.sql.Date;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JwtTokenUtils {
    private final Key signingKey;
    private final JwtParser jwtParser;
    private final int accessExpirationTime;

    public JwtTokenUtils(
            @Value("${jwt.secret}") String jwtSecret, @Value("${jwt.accessExpirationTime}") int accessExpirationTime)
    {
        this.signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        this.jwtParser = Jwts.parserBuilder().setSigningKey(this.signingKey).build();
        this.accessExpirationTime = accessExpirationTime;
    }

    public String generateToken(UserDetails userDetails) {
        log.info("\"{}\" jwt 발급", userDetails.getUsername());

        // collection으로 반환되는 authorities를 문자열로 변환해 저장
        String authorities = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).collect(Collectors.joining(","));
        Claims jwtClaims = Jwts.claims()
                .setSubject(userDetails.getUsername())
                .setIssuedAt(Date.from(Instant.now()))
                .setExpiration(Date.from(Instant.now().plusSeconds(accessExpirationTime)));

        return Jwts.builder()
                .setClaims(jwtClaims)
                .claim("authorities", authorities)
                .signWith(signingKey)
                .compact();
    }

    public boolean validate(String token) {
        log.info("jwt validate check");
        try {
            jwtParser.parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            log.warn("유효하지 않은 jwt: {}", e.getClass());
            return false;
        }
    }

    public Claims parseClaims(String token) {
        log.info("jwt parsing : {}", jwtParser.parseClaimsJws(token).getBody());
        return jwtParser
                .parseClaimsJws(token)
                .getBody();
    }

    // 문자열로 저장된 authorities를 다시 Collection으로 변환
    public Collection<? extends GrantedAuthority> getAuthFromClaims(Claims claims){

    String authoritiesString = (String) claims.get("authorities"); // authorities 정보 가져오기

    return Arrays.stream(authoritiesString.split(","))
            .map(SimpleGrantedAuthority::new)
            .collect(Collectors.toList());
    }

}
