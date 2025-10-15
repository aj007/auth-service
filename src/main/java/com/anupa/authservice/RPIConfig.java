package com.anupa.authservice;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.netflix.eureka.EurekaDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Configuration
public class RPIConfig {

    @Autowired
    EurekaDiscoveryClient discoveryClient;

    //@Value("${profile-service.hostname}") //will be picked up according to the profile
    //String hostname;

    //@Value("${profile-service.portnumber}") //will be picked up according to the profile
    //String portnumber;

    @Bean
    @Scope("prototype")
    public WebClient profileGetWebClient(WebClient.Builder webClientBuilder) // 1. [DEFAULT] SERVICE DISCOVERY VIA K8S
    {
        List<ServiceInstance> instances = discoveryClient.getInstances("customer-profile-service");
//        //No load balancing algorithm is used here, so we are just taking the first instance
//        // you can use load balancing algorithm like round robin or random if you want
        String hostname = instances.get(0).getHost();
        String portnumber = String.valueOf(instances.get(0).getPort());

        return webClientBuilder // hardcoded hostname and portnumber for the auth-service
                .baseUrl(String.format("http://%s:%s/api/v1/internal/profile/get/", hostname, portnumber))
                .filter(new LoggingWebClientFilter())
                .build();
    }

    @Bean
    @Scope("prototype")
    public WebClient profileDeleteWebClient(WebClient.Builder webClientBuilder) //2. [AUXILIARY] SERVICE DISCOVERY VIA EUREKA
    {
        List<ServiceInstance> instances = discoveryClient.getInstances("profile-service");
        //No load balancing algorithm is used here, so we are just taking the first instance
        // you can use load balancing algorithm like round robin or random if you want
        String eurekahostname = instances.get(0).getHost();
        String eurekaport = String.valueOf(instances.get(0).getPort());

        return webClientBuilder // hardcoded hostname and portnumber for the auth-service
                .baseUrl(String.format("http://%s:%s/api/v1/internal/profile/delete/", eurekahostname, eurekaport))
                .filter(new LoggingWebClientFilter())
                .build();
    }

}
