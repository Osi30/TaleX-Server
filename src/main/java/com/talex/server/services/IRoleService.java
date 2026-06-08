package com.talex.server.services;

import com.talex.server.entities.Role;

public interface IRoleService {
    Role findByCode(String code);
}
