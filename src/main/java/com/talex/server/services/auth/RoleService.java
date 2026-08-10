package com.talex.server.services.auth;

import com.talex.server.entities.auth.Role;

public interface RoleService {
    Role findByCode(String code);
}
