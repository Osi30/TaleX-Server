package com.talex.server.controllers.creator;

import com.talex.server.annotations.CurrentAccountId;
import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.requests.creator.CreatorRegisterDto;
import com.talex.server.dtos.requests.filters.CreatorFilterRequestDto;
import com.talex.server.dtos.responses.CreatorResponseDto;
import com.talex.server.records.CreatorVerificationStatus;
import com.talex.server.services.creator.ICreatorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/creators")
@RequiredArgsConstructor
@Tag(name = "Creators", description = "API quản lý creator, bao gồm đăng ký, truy vấn và cập nhật thông tin creator")
public class CreatorController {
    private final ICreatorService creatorService;

    @PostMapping
    @PreAuthorize("hasRole('VIEWER')")
    @Operation(
            summary = "Đăng ký nhà sáng tạo",
            description = "Đăng ký trở thành nhà sáng tạo cho tài khoản " +
                    ", thực hiện khi bấm vào Creator Studio lần đầu.")
    public ResponseEntity<BaseResponse> create(
            @CurrentAccountId UUID accountId,
            @Valid @RequestBody CreatorRegisterDto dto
    ) {
        dto.setAccountId(accountId);
        CreatorResponseDto resp = creatorService.createCreator(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.builder()
                        .code(201)
                        .message("Creator created")
                        .data(resp)
                        .build());
    }

    @PostMapping("/verification")
    @PreAuthorize("hasRole('VIEWER')")
    @Operation(
            summary = "Đăng ký xác thực",
            description = "Đăng ký xác thực để sử dụng các tính năng cao hơn" +
                    ", khi muốn bật kiếm tiền")
    public ResponseEntity<BaseResponse> verifyAccount(
            @CurrentAccountId UUID accountId,
            @Valid @RequestBody CreatorRegisterDto dto
    ) {
        dto.setAccountId(accountId);
        String resp = creatorService.verifyCreator(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.builder()
                        .code(201)
                        .message("Session id returned for verifying")
                        .data(resp)
                        .build());
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Lấy thông tin trạng thái xác thực",
            description = "Thông tin bao gồm trạng thái xác thực eKYC, term log, tax, payment profile")
    @GetMapping("/verification-status")
    public ResponseEntity<BaseResponse> getDetailedStatus(
            @CurrentAccountId UUID accountId
    ) {
        CreatorVerificationStatus status = creatorService.checkAndGetVerificationStatus(accountId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.builder()
                        .code(200)
                        .message("Creator verification status")
                        .data(status)
                        .build());
    }

    @GetMapping("/own")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Lấy creator của tài khoản",
            description = "Lấy thông tin creator liên kết với tài khoản đang đăng nhập, " +
                    "bao gồm thống kê tổng và trạng thái chấp nhận điều khoản mới nhất"
    )
    public ResponseEntity<BaseResponse> getAccountCreator(
            @CurrentAccountId UUID accountId
    ) {
        CreatorResponseDto resp = creatorService.getByAccount(accountId);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("OK")
                .data(resp)
                .build());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    @Operation(summary = "Lọc creator", description = "Lọc danh sách creator theo từ khóa và thời gian tạo/cập nhật.")
    public ResponseEntity<BaseResponse> filterCreators(
            @RequestParam(required = false) String searchKey,
            @RequestParam(required = false) String createdAtFrom,
            @RequestParam(required = false) String createdAtTo,
            @RequestParam(required = false) String updatedAtFrom,
            @RequestParam(required = false) String updatedAtTo,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        BasePageResponse<CreatorResponseDto> responsePage = creatorService.filterCreators(
                CreatorFilterRequestDto.builder()
                        .searchKey(searchKey)
                        .createdAtFrom(createdAtFrom)
                        .createdAtTo(createdAtTo)
                        .updatedAtFrom(updatedAtFrom)
                        .updatedAtTo(updatedAtTo)
                        .sortBy(sortBy)
                        .sortDirection(sortDirection)
                        .page(page)
                        .pageSize(pageSize)
                        .build());

        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Lấy danh sách creator thành công!")
                .data(responsePage)
                .build());
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Lấy creator theo ID",
            description = "Trả về thông tin creator theo ID, " +
                    "được sử dụng để xem thông tin public của creator"
    )
    public ResponseEntity<BaseResponse> getById(@PathVariable String id) {
        CreatorResponseDto resp = creatorService.getById(id);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("OK")
                .data(resp)
                .build());
    }
}
