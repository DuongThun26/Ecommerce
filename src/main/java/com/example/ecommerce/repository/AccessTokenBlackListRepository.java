package com.example.ecommerce.repository;

import com.example.ecommerce.entity.AccessTokenBlackList;
import org.springframework.data.repository.CrudRepository;

public interface AccessTokenBlackListRepository extends CrudRepository<AccessTokenBlackList, String> {
}
