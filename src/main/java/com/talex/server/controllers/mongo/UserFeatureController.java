package com.talex.server.controllers.mongo;

import com.talex.server.dtos.mongo.UserFeatureRequest;
import com.talex.server.entities.mongo.UserFeatureDocument;
import com.talex.server.services.mongo.IUserFeatureService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ua_parser.Client;
import ua_parser.Parser;

import java.util.Locale;

@RestController
@RequestMapping("/api/v1/mongo/features/user")
@RequiredArgsConstructor
public class UserFeatureController {
    private final IUserFeatureService featureService;
    private final Parser uaParser;

    @PostMapping("/{userId}")
    public ResponseEntity<UserFeatureDocument> saveUserFeatures(
            @PathVariable String userId,
            @RequestBody UserFeatureRequest request
    ) {
        UserFeatureDocument savedData = featureService.saveOrUpdateFeatures(userId, request);
        return ResponseEntity.ok(savedData);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserFeatureDocument> getUserFeatures(@PathVariable String userId) {
        return featureService.getFeaturesByUserId(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/test")
    public ResponseEntity<?> getAgent(@RequestHeader("User-Agent") String userAgentStr) {
        Client client = uaParser.parse(userAgentStr);
        String os = client.os.family;
        String device = client.device.family;

        if ("Other".equalsIgnoreCase(device)) {
            if ("Windows".equalsIgnoreCase(os) || "Mac OS X".equalsIgnoreCase(os) || "Linux".equalsIgnoreCase(os)) {
                device = "Desktop";
            }
        }

        return ResponseEntity.ok( String.format("Device: %s | OS: %s", device, os));
    }

    @RequestMapping(value = "/check-lang-2", method = RequestMethod.GET)
    public String getLanguage2(HttpServletRequest request) {
        Locale locale = request.getLocale();
        return "Ngôn ngữ ưu tiên: " + locale.toLanguageTag(); // VD: "vi-VN"
    }
}