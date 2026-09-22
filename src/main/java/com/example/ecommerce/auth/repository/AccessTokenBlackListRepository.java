package com.example.ecommerce.auth.repository;

import com.example.ecommerce.auth.entity.AccessTokenBlackList;
import org.springframework.data.repository.CrudRepository;

public interface AccessTokenBlackListRepository extends CrudRepository<AccessTokenBlackList, String> {
}
