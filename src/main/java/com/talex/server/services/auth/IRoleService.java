package com.talex.server.services.auth;

import com.talex.server.entities.auth.Role;

public interface IRoleService {
    Role findByCode(String code);
}
