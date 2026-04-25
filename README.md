# TaleX Server

TaleX là một nền tảng chuyên biệt để chia sẻ video ngắn và trung bình, tập trung vào cộng đồng sáng tạo nội dung kể chuyện bằng hình ảnh như Animated Webtoon và Visual Novel. Hệ thống cung cấp không gian cho các nhà sáng tạo tiếp cận khán giả và tích hợp các công cụ quản lý bản quyền nghiêm ngặt.

## 🚀 Tổng quan dự án

Nền tảng được thiết kế để giải quyết các vấn đề về phân tâm của người xem trên các mạng xã hội đại trà và hỗ trợ nghệ sĩ tối ưu hóa doanh thu.

- **Đối tượng**: Họa sĩ truyện tranh, nhóm lồng tiếng và khán giả yêu thích Webtoon/Anime.
- **Tính năng chính**: Trình phát video tối ưu chương hồi, hệ thống Content ID chống re-up, và mô hình kinh tế vi mô (Fast Pass, Donate).

## 🏗️ Kiến trúc hệ thống (3-Layer Architecture)

Mã nguồn được tổ chức theo cấu trúc chuẩn để đảm bảo khả năng bảo trì và mở rộng cho môi trường production:

```
src/main/java/com/talex/server/ 
├── configs/            # Cấu hình hệ thống (Security, Database, v.v.) 
├── controllers/        # (Presentation Layer): Tiếp nhận và xử lý HTTP requests 
├── dtos/               # Data Transfer Objects để trao đổi dữ liệu giữa các lớp 
├── entities/           # Định nghĩa các đối tượng dữ liệu (Database Entities) 
├── enums/              # Lưu trữ các hằng số định danh (Constants) 
├── exceptions/         # Xử lý và định nghĩa các lỗi tập trung 
├── mappers/impls/      # Chuyển đổi qua lại giữa Entities và DTOs 
├── repositories/       # (Data Access Layer): Tương tác trực tiếp với cơ sở dữ liệu 
├── services/impls/     # (Business Logic Layer): Thực hiện các nghiệp vụ chính của hệ thống 
├── utils/              # Các công cụ hỗ trợ dùng chung (Helper classes) 
└── TalexServerApplication.java # Điểm khởi đầu của ứng dụng Spring Boot 
```

## 🛠️ Metadata dự án

Các thông tin định danh quan trọng của dự án:

- **Group ID**: `com.talex`
- **Artifact ID**: `talex-server`
- **Package Name**: `com.talex.server`

## ⚖️ Bản quyền & Pháp lý

Dự án được bảo hộ bởi giấy phép Apache License 2.0, phù hợp cho mục đích thương mại và bảo vệ bằng sáng chế. Mọi hành vi sao chép trái phép nội dung trên nền tảng sẽ bị xử lý bởi hệ thống Watermark động và Content ID.

Copyright © 2026 Talex Team. All rights reserved.