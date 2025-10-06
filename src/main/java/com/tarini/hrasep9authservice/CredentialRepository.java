package com.tarini.hrasep9authservice;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface CredentialRepository extends MongoRepository<Credential, String>
{



}
