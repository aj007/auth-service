package com.anupa.authservice;

import lombok.Data;

@Data
public class AuthEvent {

    String principal;
    String type; // CREATE / UPDATE / DELETE / QUERY
    String action;
    String status; // SUCCESS / FAILURE
    String traceid;

}
