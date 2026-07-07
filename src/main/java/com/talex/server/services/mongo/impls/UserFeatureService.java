package com.talex.server.services.mongo.impls;

import com.talex.server.dtos.mongo.UserFeatureRequest;
import com.talex.server.entities.mongo.UserFeatureDocument;
import com.talex.server.repositories.mongo.UserFeatureRepository;
import com.talex.server.services.mongo.IUserFeatureService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserFeatureService implements IUserFeatureService {
    private final UserFeatureRepository featureRepository;

    @Override
    public UserFeatureDocument saveOrUpdateFeatures(String userId, UserFeatureRequest incoming) {
        // Nếu chưa có document dưới DB thì khởi tạo thực thể mới
        UserFeatureDocument existing = featureRepository.findByAccountId(userId)
                .orElseGet(() -> {
                    UserFeatureDocument newDoc = new UserFeatureDocument();
                    newDoc.setAccountId(userId);
                    return newDoc;
                });

        // Đảm bảo các List nội bộ không bị null phòng hờ lỗi mapping dữ liệu cũ
        if (existing.getDeviceTypes() == null) existing.setDeviceTypes(new ArrayList<>());
        if (existing.getOs() == null) existing.setOs(new ArrayList<>());

        // 1. Tích lũy Device Type (Nếu chưa tồn tại trong List thì mới add vào)
        if (incoming.getDeviceType() != null && !incoming.getDeviceType().isBlank()) {
            if (!existing.getDeviceTypes().contains(incoming.getDeviceType())) {
                existing.getDeviceTypes().add(incoming.getDeviceType());
            }
        }

        // 2. Tích lũy OS (Nếu chưa tồn tại trong List thì mới add vào)
        if (incoming.getOs() != null && !incoming.getOs().isBlank()) {
            if (!existing.getOs().contains(incoming.getOs())) {
                existing.getOs().add(incoming.getOs());
            }
        }

        // 3. Cập nhật Địa lý (Chỉ cập nhật khi trường request có dữ liệu)
        if (incoming.getLanguage() != null) existing.setLanguage(incoming.getLanguage());
        if (incoming.getTimezone() != null) existing.setTimezone(incoming.getTimezone());

        // 4. Cập nhật Demographic / Profile (Chỉ cập nhật khi trường request có dữ liệu)
        if (incoming.getAccountAge() != null) existing.setAccountAge(incoming.getAccountAge());
        if (incoming.getCreatorTier() != null) existing.setCreatorTier(incoming.getCreatorTier());
        if (incoming.getGender() != null) existing.setGender(incoming.getGender());
        if (incoming.getAge() != null) existing.setAge(incoming.getAge());

        // 5. Các trường Write-once (Chỉ ghi nhận ở lần đầu tiên hoặc khi DB trống)
        if (existing.getRegisterBy() == null && incoming.getRegisterBy() != null) {
            existing.setRegisterBy(incoming.getRegisterBy());
        }
        if (existing.getOnboardingMovieGenres().isEmpty() && incoming.getOnboardingMovieGenres() != null) {
            existing.setOnboardingMovieGenres(incoming.getOnboardingMovieGenres());
        }
        if (existing.getOnboardingComicGenres().isEmpty() && incoming.getOnboardingComicGenres() != null) {
            existing.setOnboardingComicGenres(incoming.getOnboardingComicGenres());
        }

        // Thực hiện lưu trữ/cập nhật xuống MongoDB
        return featureRepository.save(existing);
    }

    @Override
    public Optional<UserFeatureDocument> getFeaturesByUserId(String userId) {
        return featureRepository.findByAccountId(userId);
    }
}
