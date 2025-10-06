package com.tarini.hrasep9authservice;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "tokens")
@Data
public class Token {

    @Id
    String id;
    String phone;
    String status;
    Instant createdAt;
    Integer expiry; // seconds
}
