package com.talex.server.configs;

import com.talex.server.entities.media.Copyright;
import com.talex.server.entities.Role;
import com.talex.server.repositories.CopyrightRepository;
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
    private final CopyrightRepository copyrightRepository;

    @Override
    public void run(String... args) {
//        seedRoles();
//        seedCopyrights();
    }

    private void seedRoles() {
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

    private void seedCopyrights() {
        if (copyrightRepository.count() > 0) {
            log.info("Copyrights already seeded, skipping");
            return;
        }

        List<Copyright> copyrights = List.of(
                makeCopyright("CC0", "Public Domain",
                        "No rights reserved. Anyone can use, modify, and distribute freely.",
                        "https://creativecommons.org/publicdomain/zero/1.0/",
                        "{\"canShare\":true,\"canModify\":true,\"canCommercialUse\":true}"),
                makeCopyright("CC_BY", "Creative Commons Attribution 4.0",
                        "Free to share and adapt with attribution to the original creator.",
                        "https://creativecommons.org/licenses/by/4.0/",
                        "{\"canShare\":true,\"canModify\":true,\"canCommercialUse\":true}"),
                makeCopyright("CC_BY_SA", "Creative Commons Attribution-ShareAlike 4.0",
                        "Free to share and adapt with attribution; derivatives must use the same license.",
                        "https://creativecommons.org/licenses/by-sa/4.0/",
                        "{\"canShare\":true,\"canModify\":true,\"canCommercialUse\":true}"),
                makeCopyright("CC_BY_NC", "Creative Commons Attribution-NonCommercial 4.0",
                        "Free to share and adapt with attribution; no commercial use permitted.",
                        "https://creativecommons.org/licenses/by-nc/4.0/",
                        "{\"canShare\":true,\"canModify\":true,\"canCommercialUse\":false}"),
                makeCopyright("STANDARD", "Standard Copyright",
                        "All rights reserved. No reuse without explicit permission from the copyright holder.",
                        null,
                        "{\"canShare\":false,\"canModify\":false,\"canCommercialUse\":false}")
        );

        copyrightRepository.saveAll(copyrights);
        log.info("Seeded {} copyright types: CC0, CC_BY, CC_BY_SA, CC_BY_NC, STANDARD", copyrights.size());
    }

    private Copyright makeCopyright(String code, String name, String description,
                                    String legalUrl, String permissions) {
        Copyright c = new Copyright();
        c.setCode(code);
        c.setName(name);
        c.setDescription(description);
        c.setLegalUrl(legalUrl);
        c.setIsActive(true);
        c.setPermissions(permissions);
        return c;
    }
}
