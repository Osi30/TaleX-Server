package com.talex.server.utils;

import com.talex.server.annotations.ValidFile;
import com.talex.server.policies.FilePolicy;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.Collection;

public class FileValidator implements ConstraintValidator<ValidFile, Object> {
    private FilePolicy policy;

    @Override
    public void initialize(ValidFile constraintAnnotation) {
        this.policy = constraintAnnotation.policy();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        switch (value) {
            // Trường hợp 1: null/trống
            case null -> {
                return buildError(context, "Dữ liệu file không được để trống!");
            }


            // Trường hợp 2: 1 File đơn lẻ
            case MultipartFile multipartFile -> {
                return validateSingleFile(multipartFile, context);
            }


            // Trường hợp 3: Danh sách List<MultipartFile>
            case Collection<?> collection -> {
                if (collection.isEmpty()) {
                    return buildError(context, "Danh sách file không được để trống!");
                }
                for (Object obj : collection) {
                    if (obj instanceof MultipartFile) {
                        if (!validateSingleFile((MultipartFile) obj, context)) {
                            return false; // Dừng lại và trả lỗi ngay khi có 1 file lỗi
                        }
                    }
                }
                return true;
            }


            // Trường hợp 4: Mảng MultipartFile[]
            case MultipartFile[] array -> {
                if (array.length == 0) {
                    return buildError(context, "Danh sách file không được để trống!");
                }
                for (MultipartFile file : array) {
                    if (!validateSingleFile(file, context)) {
                        return false;
                    }
                }
                return true;
            }
            default -> {
            }
        }

        return false;
    }

    // Tách hàm xử lý validate cho từng file đơn lẻ
    private boolean validateSingleFile(MultipartFile file, ConstraintValidatorContext context) {
        if (file == null || file.isEmpty()) {
            return buildError(context, "Có file trống trong danh sách tải lên!");
        }

        // Kiểm tra dung lượng
        if (file.getSize() > policy.getMaxSizeBytes()) {
            String errorMsg = String.format("File '%s' vượt quá dung lượng! Đối với mục đích này, hệ thống chỉ cho phép tối đa %s.",
                    file.getOriginalFilename(), policy.getMaxSizeLabel());
            return buildError(context, errorMsg);
        }

        // Kiểm tra định dạng
        String contentType = file.getContentType();
        if (contentType == null || !Arrays.asList(policy.getAllowedContentTypes()).contains(contentType)) {
            String errorMsg = String.format("File '%s' định dạng không hợp lệ! Vui lòng chỉ upload: %s.",
                    file.getOriginalFilename(), policy.getAllowedExtensionsLabel());
            return buildError(context, errorMsg);
        }

        return true;
    }

    private boolean buildError(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
        return false;
    }
}