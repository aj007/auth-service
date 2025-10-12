package com.anupa.authservice;

import lombok.Data;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "credentials")
@Data
public class Credential
{
    @Id
    String phone;
    String password;
    String type; // CUSTOMER, VENDOR, DELIVERY_PARTNER
}
