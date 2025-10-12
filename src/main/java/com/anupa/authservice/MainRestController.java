package com.anupa.authservice;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("api/v1")
public class MainRestController
{
    private static final Logger logger = LoggerFactory.getLogger(MainRestController.class);
    @Autowired
    CredentialRepository credentialRepository;
    @Autowired
    TokenRepository tokenRepository;
    @Autowired
    Producer producer;
    @Autowired
    TokenService tokenService;
    @Autowired
    private ProfileService profileService;

    @Autowired
    io.opentelemetry.api.trace.Tracer tracer;

    @PostMapping("signup") // Unsecured Endpoint need Username and Password
    ResponseEntity<?> signup(@RequestBody Credential credential,
                             AuthEvent authEvent) throws JsonProcessingException {
        logger.info("Request Received for Fresh Signup for Principal: "+credential.getPhone());
        // validation of credential before persisting is required
        // 1. username should not already exist
        // 2. password should be strong
        Credential savedCredential =  credentialRepository.save(credential);
        logger.info("Credential Saved for Principal: "+credential.getPhone());

        authEvent.setPrincipal(savedCredential.getPhone());
        authEvent.setType("CREATE");
        authEvent.setAction("SIGNUP");
        authEvent.setStatus("SUCCESS");
        authEvent.setTraceid(io.opentelemetry.api.trace.Span.current().getSpanContext().getTraceId());
        producer.pubAuthEvent_1(authEvent); // MESSAGE PUBLISHED TO KAFKA TOPIC AUTH-EVENTS
        return  ResponseEntity.ok("NEW SIGNUP SUCCESSFUL FOR PRINCIPAL: "+credential.getPhone());
    }

    @PostMapping("login") // Unsecured Endpoint need Username and Password
    ResponseEntity<?> login(@RequestBody CredentialDTO credentialDTO,
                            HttpServletResponse response,
                            AuthEvent authEvent
                            ) throws JsonProcessingException {
         Optional<Credential> fetchedCredential =  credentialRepository.findById(credentialDTO.getPhone());

         if(fetchedCredential.isPresent())
         {
             if(fetchedCredential.get().getPassword().equals(credentialDTO.getPassword()))
             {
                 Token token = tokenService.generateToken(credentialDTO.getPhone());
                 Cookie authCookie = new Cookie("AUTH-TOKEN", token.getId());
                 response.addCookie(authCookie);

                 authEvent.setPrincipal(fetchedCredential.get().getPhone());
                 authEvent.setType("QUERY");
                 authEvent.setAction("LOGIN");
                 authEvent.setStatus("SUCCESS");
                 authEvent.setTraceid(io.opentelemetry.api.trace.Span.current().getSpanContext().getTraceId());
                 producer.pubAuthEvent_1(authEvent); // MESSAGE PUBLISHED TO KAFKA TOPIC AUTH-EVENTS
                 return ResponseEntity.ok("LOGIN SUCCESSFUL FOR PRINCIPAL: "+credentialDTO.getPhone());
             }
             else
             {

                 authEvent.setPrincipal(fetchedCredential.get().getPhone());
                 authEvent.setType("QUERY");
                 authEvent.setAction("LOGIN");
                 authEvent.setStatus("FAILED");
                 authEvent.setTraceid(io.opentelemetry.api.trace.Span.current().getSpanContext().getTraceId());
                 producer.pubAuthEvent_1(authEvent); // MESSAGE PUBLISHED TO KAFKA TOPIC AUTH-EVENTS
                 return   ResponseEntity.ok("LOGIN FAILED | INCORRECT PASSWORD");
             }
         }
         else
         {
             authEvent.setPrincipal(fetchedCredential.get().getPhone());
             authEvent.setType("QUERY");
             authEvent.setAction("LOGIN");
             authEvent.setStatus("FAILED");
             authEvent.setTraceid(io.opentelemetry.api.trace.Span.current().getSpanContext().getTraceId());
             producer.pubAuthEvent_1(authEvent); // MESSAGE PUBLISHED TO KAFKA TOPIC AUTH-EVENTS
             return  ResponseEntity.ok("LOGIN FAILED | PRINCIPAL NOT FOUND");
         }

    }

    @GetMapping("greet")
    ResponseEntity<?> greet(HttpServletRequest request) throws JsonProcessingException {
        List<Cookie> cookies = new ArrayList<>();

        if(!(request.getCookies() == null))
        {
            cookies = List.of(request.getCookies());
        }

        Optional<Cookie> authcookie =  cookies.stream().filter(cookie -> cookie.getName().equals("AUTH-TOKEN")).findFirst();
        String principal = null;

        if(authcookie.isPresent())
        {
            String authToken = authcookie.get().getValue();
            Optional<String> fetchedPrincipal =  Optional.ofNullable(tokenService.validateToken(authToken));

            if(fetchedPrincipal.isEmpty())
            {
                return ResponseEntity.ok("TRY TO LOGIN AGAIN");
            }
            else
            {
                logger.info("Principal from TokenService: {}", fetchedPrincipal.get());

                //fetch user's profile from profile-service
                // if profile is not present -> ask the user to update his profile
                // if profile is present -> greet the user with his name

                Optional<Profile> retrievedProfile =  profileService.getProfile(fetchedPrincipal.get());

                return retrievedProfile.map(profile -> ResponseEntity.ok("Hello, " + profile.getFirstname())).orElseGet(() -> ResponseEntity.ok("Please Update Your Profile"));


            }

        }

        return ResponseEntity.ok("TRY TO LOGIN AGAIN");

    }

    @GetMapping("validate")
    ResponseEntity<?> validate(@RequestHeader("Authorization") String authToken
                               ) throws JsonProcessingException
    {
        logger.info("Request Received for Validating Auth Token: {}", authToken);
        return ResponseEntity.ok(tokenService.validateToken(authToken)); // NULL IF INVALID
    }

    @GetMapping("logout")
    ResponseEntity<?> logout(HttpServletRequest request,
                             AuthEvent authEvent) throws JsonProcessingException
    {
        List<Cookie> cookies = new ArrayList<>();

        if(!(request.getCookies() == null))
        {
            cookies = List.of(request.getCookies());
        }

        Optional<Cookie> authcookie =  cookies.stream().filter(cookie -> cookie.getName().equals("AUTH-TOKEN")).findFirst();

        if(authcookie.isPresent())
        {
            tokenService.invalidate(authcookie.get().getValue());
            return  ResponseEntity.ok("LOGOUT SUCCESSFUL");
        }
        else
        {
            return ResponseEntity.badRequest().body("NO VALID AUTH-TOKEN FOUND");
        }

    }


}
