package com.example.ecommerce.auth.repository;

import com.example.ecommerce.auth.entity.RefreshTokenWhiteList;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefreshTokenWhiteListRepository extends CrudRepository<RefreshTokenWhiteList, String> {
}
