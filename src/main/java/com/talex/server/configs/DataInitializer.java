package com.talex.server.configs;

import com.talex.server.entities.auth.Role;
import com.talex.server.entities.media.Copyright;
import com.talex.server.entities.creator.CreatorTier;
import com.talex.server.entities.series.Category;
import com.talex.server.entities.series.Tag;
import com.talex.server.entities.term.TermsVersion;
import com.talex.server.enums.TermsType;
import com.talex.server.repositories.auth.RoleRepository;
import com.talex.server.repositories.creator.CreatorTierRepository;
import com.talex.server.repositories.media.CopyrightRepository;
import com.talex.server.repositories.series.CategoryRepository;
import com.talex.server.repositories.series.TagRepository;
import com.talex.server.repositories.term.TermsVersionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {
    private final CopyrightRepository copyrightRepository;
    private final RoleRepository roleRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final TermsVersionRepository termsVersionRepository;
    private final CreatorTierRepository creatorTierRepository;

    @Override
    public void run(String... args) {
        seedRoles();
        seedCopyrights();
        seedCategories();
        seedTags();
        seedTerms();
        seedCreatorTiers();
    }

    // Không có seeder nào cho roles trước đây — DB mới (docker volume mới, máy dev mới)
    // không có sẵn 4 role cố định mà AuthServiceImpl/AdminAccountServiceImpl/AccountWorker
    // tra bằng code ("VIEWER", "CREATOR", "STAFF", "ADMIN"), khiến RoleService.findByCode()
    // ném RuntimeException ngay lần đăng ký/đăng nhập đầu tiên.
    private void seedRoles() {
        if (roleRepository.count() > 0) {
            log.info("Roles already seeded, skipping");
            return;
        }

        List<Role> roles = List.of(
                makeRole("VIEWER", "Viewer"),
                makeRole("CREATOR", "Creator"),
                makeRole("STAFF", "Staff"),
                makeRole("ADMIN", "Admin")
        );

        roleRepository.saveAll(roles);
        log.info("Seeded {} roles: VIEWER, CREATOR, STAFF, ADMIN", roles.size());
    }

    private Role makeRole(String code, String roleName) {
        Role role = new Role();
        role.setCode(code);
        role.setRoleName(roleName);
        role.setIsActive(true);
        return role;
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

    private void seedCategories() {
        if (categoryRepository.count() > 0) {
            log.info("Categories already seeded, skipping");
            return;
        }

        List<Category> categories = List.of(
                makeCategory("Manga", "manga", "Truyện tranh Nhật Bản"),
                makeCategory("Manhwa", "manhwa", "Truyện tranh Hàn Quốc"),
                makeCategory("Manhua", "manhua", "Truyện tranh Trung Quốc"),
                makeCategory("Anime", "anime", "Video hoạt hình chuyển thể")
        );

        categoryRepository.saveAll(categories);
        log.info("Seeded {} categories: Manga, Manhwa, Manhua, Anime", categories.size());
    }

    private Category makeCategory(String name, String slug, String description) {
        Category category = new Category();
        category.setCategoryName(name);
        category.setSlug(slug);
        category.setDescription(description);
        return category;
    }

    private void seedTags() {
        if (tagRepository.count() > 0) {
            log.info("Tags already seeded, skipping");
            return;
        }

        List<Tag> tags = List.of(
                makeTag("Action", "action"),
                makeTag("Romance", "romance"),
                makeTag("Comedy", "comedy"),
                makeTag("Fantasy", "fantasy"),
                makeTag("Isekai", "isekai")
        );

        tagRepository.saveAll(tags);
        log.info("Seeded {} tags: Action, Romance, Comedy, Fantasy, Isekai", tags.size());
    }

    private Tag makeTag(String name, String slug) {
        Tag tag = new Tag();
        tag.setTagName(name);
        tag.setSlug(slug);
        return tag;
    }

    // CreatorService.acceptCreatorTerms() gọi termsVersionService.getActiveByType(CREATOR)
    // — nếu không có bản active nào, hàm đó NÉM EXCEPTION (ACTIVE_VERSION_NOT_FOUND) ngay
    // khi creator xác nhận điều khoản, giống hệt lớp bug thiếu seed Role trước đó.
    private void seedTerms() {
        if (termsVersionRepository.count() > 0) {
            log.info("Terms versions already seeded, skipping");
            return;
        }

        List<TermsVersion> terms = List.of(
                makeTerms(TermsType.CREATOR, "1.0", "Điều khoản dành cho Creator",
                        "Creator đồng ý tuân thủ quy định nội dung, bản quyền và chính sách chia sẻ doanh thu của TaleX."),
                makeTerms(TermsType.CREATOR_VERIFYING_PROCESS, "1.0", "Điều khoản xác minh danh tính Creator",
                        "Creator đồng ý cung cấp thông tin eKYC chính xác để xác minh danh tính trước khi được duyệt."),
                makeTerms(TermsType.CREATOR_ENABLE_MONETIZATION, "1.0", "Điều khoản bật kiếm tiền cho Creator",
                        "Creator đồng ý điều kiện chia sẻ doanh thu, thuế và phương thức thanh toán khi bật tính năng kiếm tiền."),
                makeTerms(TermsType.GENERAL_TOS, "1.0", "Điều khoản sử dụng chung",
                        "Điều khoản sử dụng áp dụng cho toàn bộ người dùng nền tảng TaleX.")
        );

        termsVersionRepository.saveAll(terms);
        log.info("Seeded {} terms versions: CREATOR, CREATOR_VERIFYING_PROCESS, CREATOR_ENABLE_MONETIZATION, GENERAL_TOS",
                terms.size());
    }

    private TermsVersion makeTerms(TermsType type, String version, String title, String content) {
        TermsVersion terms = new TermsVersion();
        terms.setType(type);
        terms.setVersion(version);
        terms.setTitle(title);
        terms.setContent(content);
        terms.setIsActive(true);
        return terms;
    }

    // CreatorMapperImpl.toResponseDto() luôn gọi getCurrentEligibleTier(), hàm này fallback
    // getDefaultTier() khi creator chưa đạt tier nào — getDefaultTier() NÉM EXCEPTION 404
    // (CreatorTierErrorCode.NOT_FOUND) nếu không có tier nào isDefault=true. Không seed cái
    // này thì NGAY LẦN TẠO CREATOR ĐẦU TIÊN đã lỗi 404, giống hệt lớp bug thiếu seed Role.
    private void seedCreatorTiers() {
        if (creatorTierRepository.count() > 0) {
            log.info("Creator tiers already seeded, skipping");
            return;
        }

        List<CreatorTier> tiers = List.of(
                makeCreatorTier("Người Mới", 0, 0L, 0L, 0.0, 0.5, 0.6, true),
                makeCreatorTier("Đồng", 1, 1_000L, 50_000L, 500.0, 0.55, 0.65, false),
                makeCreatorTier("Bạc", 2, 10_000L, 500_000L, 5_000.0, 0.6, 0.7, false),
                makeCreatorTier("Vàng", 3, 50_000L, 2_000_000L, 20_000.0, 0.65, 0.75, false)
        );

        creatorTierRepository.saveAll(tiers);
        log.info("Seeded {} creator tiers: Người Mới (default), Đồng, Bạc, Vàng", tiers.size());
    }

    private CreatorTier makeCreatorTier(String name, Integer level, Long minFollowers, Long minViews,
                                         Double minWatchTime, Double premiumShare, Double directShare,
                                         boolean isDefault) {
        return CreatorTier.builder()
                .tierName(name)
                .tierLevel(level)
                .minFollowerRequired(minFollowers)
                .minViewsRequired(minViews)
                .minWatchTimeRequired(minWatchTime)
                .premiumFundShareRatio(premiumShare)
                .directPurchaseShareRatio(directShare)
                .isDefault(isDefault)
                .build();
    }
}
