package com.talex.server.controllers;

import com.talex.server.annotations.CurrentAccountId;
import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.requests.CreatorIdentityRequestDto;
import com.talex.server.dtos.responses.CreatorIdentityResponseDto;
import com.talex.server.services.ICreatorIdentityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/creators/identities")
@RequiredArgsConstructor
public class CreatorIdentityController {
    private final ICreatorIdentityService creatorIdentityService;

    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse> getById(@PathVariable String id) {
        CreatorIdentityResponseDto resp = creatorIdentityService.getById(id);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("OK")
                .data(resp)
                .build());
    }

    @GetMapping("/own")
    public ResponseEntity<BaseResponse> getAccountCreatorIdentity(
            @CurrentAccountId UUID accountId
    ) {
        CreatorIdentityResponseDto resp = creatorIdentityService
                .getByAccountId(accountId.toString());
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("OK")
                .data(resp)
                .build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<BaseResponse> update(
            @PathVariable String id,
            @RequestBody CreatorIdentityRequestDto dto
    ) {
        CreatorIdentityResponseDto resp = creatorIdentityService.update(id, dto);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Updated")
                .data(resp)
                .build());
    }

    // Cần thiết kế lại để đảm bảo đáp ứng pháp lý
    // Hiện đang xóa cứng
    @DeleteMapping("/{id}")
    public ResponseEntity<BaseResponse> delete(@PathVariable String id) {
        creatorIdentityService.delete(id);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Deleted")
                .data(null)
                .build());
    }
}
