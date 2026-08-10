package com.talex.server.services.auth.impls;

import com.talex.server.entities.auth.Role;
import com.talex.server.repositories.auth.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoleService implements com.talex.server.services.auth.RoleService {
    private final RoleRepository roleRepository;

    @Transactional(readOnly = true)
    @Cacheable(value = "roles", key = "#code", cacheManager = "localCacheManager")
    @Override
    public Role findByCode(String code) {
        return roleRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("No role found with code " + code));
    }
}