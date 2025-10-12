package com.anupa.authservice;


import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.Random;

@Service
public class TokenService
{
    @Autowired
    Producer producer;
    private static final Logger logger = LoggerFactory.getLogger(TokenService.class);

    TokenRepository tokenRepository;

    public TokenService(TokenRepository tokenRepository)
    {
        this.tokenRepository = tokenRepository; // Constructor Dependency Injection
    }

    String validateToken(String tokenid) throws JsonProcessingException {
        Token token = validate(tokenid);


        if(token == null)
        {
            logger.info("Invalid token");

            return null;
        }
        else
        {
            if(token.getStatus().equals("ACTIVE"))
            {
                logger.info("Token is ACTIVE");

                AuthEvent authEvent = new AuthEvent();

                authEvent.setPrincipal(token.getPhone());
                authEvent.setType("QUERY");
                authEvent.setAction("VALIDATE");
                authEvent.setStatus("SUCCESS");
                authEvent.setTraceid(io.opentelemetry.api.trace.Span.current().getSpanContext().getTraceId());
                producer.pubAuthEvent_1(authEvent); // MESSAGE PUBLISHED TO KAFKA TOPIC AUTH-EVENTS
                return token.getPhone();
            }
            else
            {
                logger.info("Token is INACTIVE");
                AuthEvent authEvent = new AuthEvent();

                authEvent.setPrincipal(token.getPhone());
                authEvent.setType("QUERY");
                authEvent.setAction("VALIDATE");
                authEvent.setStatus("FAILED");
                authEvent.setTraceid(io.opentelemetry.api.trace.Span.current().getSpanContext().getTraceId());
                producer.pubAuthEvent_1(authEvent); // MESSAGE PUBLISHED TO KAFKA TOPIC AUTH-EVENTS
                return null;
            }
        }
    }

    Token generateToken(String principal)
    {
        Token token = new Token();
        token.setId(String.valueOf(new Random().nextInt(1000000)));
        token.setPhone(principal);
        token.setStatus("ACTIVE");
        token.setCreatedAt(Instant.now());
        token.setExpiry(600);
        tokenRepository.save(token);
        return  token;
    }

    Token validate(String tokenid)
    {
        Optional<Token> token =  tokenRepository.findById(tokenid);
        return token.orElse(null);
    }

    void invalidate(String tokenid) throws JsonProcessingException {
        Optional<Token> token =  tokenRepository.findById(tokenid);

        if(token.isPresent())
        {
            if(token.get().getStatus().equals("ACTIVE"))
            {
                token.get().setStatus("INACTIVE");
                AuthEvent authEvent = new AuthEvent();
                authEvent.setPrincipal(token.get().getPhone());
                authEvent.setType("QUERY");
                authEvent.setAction("LOGOUT");
                authEvent.setStatus("SUCCESS");
                authEvent.setTraceid(io.opentelemetry.api.trace.Span.current().getSpanContext().getTraceId());
                producer.pubAuthEvent_1(authEvent); // MESSAGE PUBLISHED TO KAFKA TOPIC AUTH-EVENTS
                tokenRepository.save(token.get());
            }
        }
    }
}
