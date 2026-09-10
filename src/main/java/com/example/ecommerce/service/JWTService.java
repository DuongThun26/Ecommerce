package com.example.ecommerce.service;

import com.example.ecommerce.dto.response.JWTInfor;
import com.example.ecommerce.entity.RedisToken;
import com.example.ecommerce.entity.UserEntity;
import com.example.ecommerce.repository.RedisTokenRepository;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JWTService {

    private final RedisTokenRepository redisTokenRepository;

    @Value("${secretkey}")
    private String secretKey;

    public String generateAccessToken(UserEntity user){
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);
        Date issueTime = new Date();
        Date expirationTime = Date.from(issueTime.toInstant().plus(15, ChronoUnit.MINUTES));
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .jwtID(UUID.randomUUID().toString())
                .subject(user.getUsername())
                .issueTime(issueTime)
                .expirationTime(expirationTime)
                .build();
        Payload payload = new Payload(claimsSet.toJSONObject());
        JWSObject jwsObject = new JWSObject(header, payload);
        try {
            jwsObject.sign(new MACSigner(secretKey));
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
        return jwsObject.serialize();
    }

    public String generateRefreshToken(UserEntity user){
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);
        Date issueTime = new Date();
        Date expirationTime = Date.from(issueTime.toInstant().plus(14, ChronoUnit.DAYS));
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .jwtID(UUID.randomUUID().toString())
                .subject(user.getUsername())
                .issueTime(issueTime)
                .expirationTime(expirationTime)
                .build();
        Payload payload = new Payload(claimsSet.toJSONObject());
        JWSObject jwsObject = new JWSObject(header, payload);
        try {
            jwsObject.sign(new MACSigner(secretKey));
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
        return jwsObject.serialize();
    }

    public boolean verifyToken(String token) throws ParseException, JOSEException {
        if(token == null || token.isEmpty()){
            return false;
        }
        SignedJWT signedJWT = SignedJWT.parse(token);
        Date expirationTime = signedJWT.getJWTClaimsSet().getExpirationTime();
        if(expirationTime.before(new Date())){
            return false;
        }
        String jwtId = signedJWT.getJWTClaimsSet().getJWTID();
        Optional<RedisToken> byId = redisTokenRepository.findById(jwtId);
        if(byId.isPresent()){
            throw new RuntimeException("Invalid token!");
        }
        return signedJWT.verify(new MACVerifier(secretKey));
    }

    public JWTInfor parse(String token) throws ParseException {
        JWTClaimsSet jwtClaimsSet = SignedJWT.parse(token).getJWTClaimsSet();
        String jwtId = jwtClaimsSet.getJWTID();
        Date issueTime = jwtClaimsSet.getIssueTime();
        Date expirationTime = jwtClaimsSet.getExpirationTime();
        return JWTInfor.builder()
                .id(jwtId)
                .issueTime(issueTime)
                .expirationTime(expirationTime)
                .build();
    }
}
