package com.talex.server.services.payout.impls;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.talex.server.dtos.payout.response.BatchPayoutDataResponseDto;
import com.talex.server.dtos.payout.request.BatchPayoutRequestDto;
import com.talex.server.dtos.payout.response.PayOSResponseDto;
import com.talex.server.dtos.payout.response.PayoutAccountBalanceResponseDto;
import com.talex.server.services.payout.PayoutService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayoutServiceImpl implements PayoutService {
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${payos.client-id}")
    private String clientId;

    @Value("${payos.api-key}")
    private String apiKey;

    @Value("${payos.checksum-key}")
    private String checksumKey;

    @Value("${payos.payout-url:https://api-merchant.payos.vn/v2/payouts}")
    private String payoutUrl;

    @Value("${payos.payout-account-balance-url:https://api-merchant.payos.vn/v1/payouts-account/balance}")
    private String accountBalanceUrl;

    @Override
    public PayoutAccountBalanceResponseDto getAccountBalance() {
        try {
            // 1. Khởi tạo Header theo yêu cầu của PayOS (x-client-id & x-api-key)
            HttpHeaders headers = new HttpHeaders();
            headers.set("x-client-id", clientId);
            headers.set("x-api-key", apiKey);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            // 2. Gọi GET Request tới PayOS
            ResponseEntity<PayOSResponseDto<PayoutAccountBalanceResponseDto>> response = restTemplate.exchange(
                    accountBalanceUrl,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<PayOSResponseDto<PayoutAccountBalanceResponseDto>>() {}
            );

            PayOSResponseDto<PayoutAccountBalanceResponseDto> body = response.getBody();

            // 3. Kiểm tra kết quả trả về
            if (body != null && "00".equals(body.getCode())) {
                return body.getData();
            }

            String errorMsg = (body != null) ? body.getDesc() : "Không nhận được phản hồi từ PayOS";
            log.error("Lỗi lấy thông tin số dư tài khoản chi: code={}, desc={}",
                    body != null ? body.getCode() : "N/A", errorMsg);
            log.error("apikey={}, clientid={}",
                    apiKey, clientId);
            throw new RuntimeException("Lấy số dư tài khoản chi thất bại: " + errorMsg);

        } catch (Exception e) {
            log.error("Lỗi kết nối API lấy số dư tài khoản chi PayOS: {}", e.getMessage(), e);
            throw new RuntimeException("Không thể lấy thông tin số dư tài khoản chi: " + e.getMessage());
        }
    }

    @Override
    public BatchPayoutDataResponseDto createBatchPayout(BatchPayoutRequestDto requestDto) {
        try {
            // 1. Chuyển DTO sang JSON String để gửi Request Body
            String requestBodyJson = objectMapper.writeValueAsString(requestDto);

            // 2. Tạo x-idempotency-key chống trùng lặp
            String idempotencyKey = UUID.randomUUID().toString();

            // 3. Tính toán chữ ký Signature CHUẨN theo thuật toán PayOS
            String signature = calculatePayOSSignature(requestDto, checksumKey);

            // 4. Khởi tạo Header
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-client-id", clientId);
            headers.set("x-api-key", apiKey);
            headers.set("x-idempotency-key", idempotencyKey);
            headers.set("x-signature", signature);

            HttpEntity<String> entity = new HttpEntity<>(requestBodyJson, headers);

            // 5. Gọi API PayOS
            ResponseEntity<PayOSResponseDto<BatchPayoutDataResponseDto>> response = restTemplate.exchange(
                    payoutUrl,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<PayOSResponseDto<BatchPayoutDataResponseDto>>() {}
            );

            PayOSResponseDto<BatchPayoutDataResponseDto> body = response.getBody();

            if (body != null && "00".equals(body.getCode())) {
                return body.getData();
            }

            String errorMsg = (body != null) ? body.getDesc() : "Không nhận được phản hồi từ PayOS";
            log.error("PayOS Payout Failed: code={}, desc={}", body != null ? body.getCode() : "N/A", errorMsg);
            throw new RuntimeException("Tạo lệnh chi thất bại: " + errorMsg);

        } catch (Exception e) {
            log.error("Lỗi khi gửi lệnh chi hàng loạt tới PayOS: {}", e.getMessage(), e);
            throw new RuntimeException("Không thể thực hiện chi hộ qua PayOS: " + e.getMessage());
        }
    }

    /**
     * Tái hiện chính xác hàm createSignature & deepSortObj của PayOS
     */
    private String calculatePayOSSignature(Object requestDto, String checksumKey) {
        try {
            // Convert DTO sang JsonNode
            JsonNode rootNode = objectMapper.valueToTree(requestDto);

            // Bước 1: Deep sort tất cả các key theo thứ tự Alphabet (tương đương deepSortObj)
            JsonNode sortedNode = deepSortJsonNode(rootNode);

            // Bước 2: Tạo Query String (tương đương queryString trong createSignature)
            List<String> queryParams = new ArrayList<>();
            Iterator<Map.Entry<String, JsonNode>> fields = sortedNode.fields();

            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String key = field.getKey();
                JsonNode valueNode = field.getValue();

                String valueStr;
                if (valueNode.isArray() || valueNode.isObject()) {
                    valueStr = objectMapper.writeValueAsString(valueNode);
                } else if (valueNode.isNull() || valueNode.isMissingNode()) {
                    valueStr = "";
                } else {
                    valueStr = valueNode.asText();
                }

                // encodeURIComponent theo chuẩn JavaScript
                queryParams.add(encodeURIComponent(key) + "=" + encodeURIComponent(valueStr));
            }

            String queryString = String.join("&", queryParams);
            log.debug("PayOS Raw QueryString to HMAC: {}", queryString);

            // Bước 3: Băm HMAC-SHA256 chuỗi Query String vừa tạo
            return hmacSha256(queryString, checksumKey);

        } catch (Exception e) {
            throw new RuntimeException("Lỗi tính toán chữ ký PayOS: " + e.getMessage(), e);
        }
    }

    /**
     * Sắp xếp đệ quy các Key trong JsonNode (Tương đương deepSortObj trong JS)
     */
    private JsonNode deepSortJsonNode(JsonNode node) {
        if (node.isObject()) {
            ObjectNode sortedNode = objectMapper.createObjectNode();
            List<String> fieldNames = new ArrayList<>();
            node.fieldNames().forEachRemaining(fieldNames::add);
            Collections.sort(fieldNames);

            for (String fieldName : fieldNames) {
                sortedNode.set(fieldName, deepSortJsonNode(node.get(fieldName)));
            }
            return sortedNode;
        } else if (node.isArray()) {
            ArrayNode sortedArray = objectMapper.createArrayNode();
            for (JsonNode item : node) {
                sortedArray.add(deepSortJsonNode(item));
            }
            return sortedArray;
        }
        return node;
    }

    /**
     * Tái hiện mã hóa URL khớp 100% với encodeURIComponent() của JavaScript
     */
    private String encodeURIComponent(String s) {
        if (s == null) return "";
        return URLEncoder.encode(s, StandardCharsets.UTF_8)
                .replace("+", "%20")
                .replace("%21", "!")
                .replace("%27", "'")
                .replace("%28", "(")
                .replace("%29", ")")
                .replace("%7E", "~");
    }

    /**
     * Băm HMAC-SHA256 ra chuỗi Hex lowercase
     */
    private String hmacSha256(String data, String key) {
        try {
            Mac hmacSha256 = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            hmacSha256.init(secretKey);
            byte[] hash = hmacSha256.doFinal(data.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Lỗi mã hóa HMAC-SHA256", e);
        }
    }
}
