package com.example.ecommerce.mail.mapper;

import com.example.ecommerce.mail.dto.response.EmailLogResponse;
import com.example.ecommerce.mail.entity.EmailLogEntity;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface EmailLogMapper {

    EmailLogResponse toResponse(EmailLogEntity entity);

    List<EmailLogResponse> toListResponse(List<EmailLogEntity> entities);
}
