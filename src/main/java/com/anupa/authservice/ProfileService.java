package com.anupa.authservice;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.security.Principal;
import java.util.Optional;

@Service
public class ProfileService {

    ApplicationContext applicationContext;
    public ProfileService(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    Optional<Profile> getProfile(String principal)
    {
        // send the request to profile service using webflux webclient to fetch user's profile using his phone number
        WebClient profileGetWebClient = (WebClient) applicationContext.getBean("profileGetWebClient");

        return Optional.ofNullable( profileGetWebClient.get()
                .uri(principal)
                .retrieve()
                .bodyToMono(Profile.class)
                .block());
    }



}
