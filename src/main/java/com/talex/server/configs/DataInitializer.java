package com.talex.server.configs;

import com.talex.server.entities.Role;
import com.talex.server.repositories.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;

    @Override
    public void run(String... args) {
        if (roleRepository.count() > 0) {
            log.info("Roles already seeded, skipping");
            return;
        }

        List<Role> roles = List.of(
                Role.builder().roleName("Viewer").code("VIEWER").build(),
                Role.builder().roleName("Creator").code("CREATOR").build(),
                Role.builder().roleName("Staff").code("STAFF").build(),
                Role.builder().roleName("Admin").code("ADMIN").build()
        );

        roleRepository.saveAll(roles);
        log.info("Seeded {} roles: VIEWER, CREATOR, STAFF, ADMIN", roles.size());
    }
}
