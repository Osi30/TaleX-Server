package com.talex.server.controllers;

import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.requests.CreatorRequestDto;
import com.talex.server.dtos.responses.CreatorResponseDto;
import com.talex.server.services.ICreatorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/creators")
@RequiredArgsConstructor
public class CreatorController {
    private final ICreatorService creatorService;

    @PostMapping
    public ResponseEntity<BaseResponse> create(@RequestBody CreatorRequestDto dto) {
        CreatorResponseDto resp = creatorService.createCreator(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.builder().code(201).message("Creator created").data(resp).build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse> getById(@PathVariable String id) {
        CreatorResponseDto resp = creatorService.getById(id);
        return ResponseEntity.ok(BaseResponse.builder().code(200).message("OK").data(resp).build());
    }

    @GetMapping
    public ResponseEntity<BaseResponse> list(@RequestParam Map<String, Object> params) {
        List<CreatorResponseDto> list = creatorService.listCreators(params);
        return ResponseEntity.ok(BaseResponse.builder().code(200).message("OK").data(list).build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<BaseResponse> update(@PathVariable String id, @RequestBody CreatorRequestDto dto) {
        CreatorResponseDto resp = creatorService.updateCreator(id, dto);
        return ResponseEntity.ok(BaseResponse.builder().code(200).message("Updated").data(resp).build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<BaseResponse> delete(@PathVariable String id) {
        creatorService.deleteCreator(id);
        return ResponseEntity.ok(BaseResponse.builder().code(200).message("Deleted").data(null).build());
    }
}
