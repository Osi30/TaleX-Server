package com.talex.server.services.impls;

import com.talex.server.entities.Role;
import com.talex.server.repositories.RoleRepository;
import com.talex.server.services.IRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoleService implements IRoleService {
    private final RoleRepository roleRepository;

    @Transactional(readOnly = true)
    @Cacheable(value = "roles", key = "#code", cacheManager = "localCacheManager")
    @Override
    public Role findByCode(String code) {
        return roleRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("No role found with code " + code));
    }
}