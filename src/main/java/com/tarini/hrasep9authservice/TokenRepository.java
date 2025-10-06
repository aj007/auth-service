package com.tarini.hrasep9authservice;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface TokenRepository extends MongoRepository<Token, String>
{



}
