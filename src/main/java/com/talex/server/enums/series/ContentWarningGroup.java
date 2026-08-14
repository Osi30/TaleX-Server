package com.talex.server.enums.series;

// Nhóm cảnh báo nội dung do Creator tự khai báo cho Series — gộp từ taxonomy L1 gốc của
// AWS Rekognition (xem ContentPipelineServiceImpl.mapParentLabelToGroup()). Dùng để quyết
// định: nếu Series đã khai đúng nhóm + ageRating=MATURE, vi phạm thuộc nhóm đó được coi là
// "đã cảnh báo trước", không cần đẩy Staff review nữa.
public enum ContentWarningGroup {
    SEXUAL_NUDITY,
    VIOLENCE_GORE,
    DRUGS_TOBACCO,
    ALCOHOL,
    GAMBLING,
    HATE_SYMBOLS,
    RUDE_GESTURES
}
