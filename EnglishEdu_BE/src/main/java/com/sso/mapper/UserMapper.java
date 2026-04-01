package com.sso.mapper;

import com.sso.dto.response.UserResponse;
import com.sso.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserResponse toResponse(User user);
}
