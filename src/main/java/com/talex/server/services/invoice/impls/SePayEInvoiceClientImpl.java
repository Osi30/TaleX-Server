package com.talex.server.services.invoice.impls;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.talex.server.configs.properties.SePayProperties;
import com.talex.server.dtos.requests.invoice.SePayCreateInvoiceRequestDto;
import com.talex.server.dtos.responses.invoice.*;
import com.talex.server.services.invoice.SePayEInvoiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class SePayEInvoiceClientImpl implements SePayEInvoiceClient {

    private static final String TOKEN_CACHE_KEY = "SEPAY_EINVOICE_TOKEN";
    private static final String PROVIDER_ACCOUNT_CACHE_KEY = "SEPAY_EINVOICE_PROVIDER_ACCOUNT";

    private final SePayProperties sePayProperties;
    private final RestTemplate restTemplate;

    // Token thật ra sống 24h nhưng cache ngắn hơn (23h) để tránh dùng token sắp hết hạn.
    private final Cache<String, String> tokenCache = Caffeine.newBuilder()
            .expireAfterWrite(23, TimeUnit.HOURS)
            .maximumSize(1)
            .build();

    private final Cache<String, SePayProviderAccountDetailDto> providerAccountCache = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS)
            .maximumSize(1)
            .build();

    @Override
    public SePayProviderAccountDetailDto resolveDefaultProviderAccount() {
        SePayProviderAccountDetailDto cached = providerAccountCache.getIfPresent(PROVIDER_ACCOUNT_CACHE_KEY);
        if (cached != null) {
            return cached;
        }

        SePayEnvelopeDto<SePayProviderAccountListDataDto> listResponse = exchange(
                HttpMethod.GET, "/v1/provider-accounts", null,
                new ParameterizedTypeReference<SePayEnvelopeDto<SePayProviderAccountListDataDto>>() {
                });

        List<SePayProviderAccountSummaryDto> accounts = listResponse.getData() != null
                ? listResponse.getData().getItems()
                : null;
        if (accounts == null || accounts.isEmpty()) {
            throw new IllegalStateException("SePay eInvoice: no provider-account configured for this credential");
        }

        String accountId = accounts.stream()
                .filter(SePayProviderAccountSummaryDto::isActive)
                .findFirst()
                .orElse(accounts.get(0))
                .getId();

        SePayEnvelopeDto<SePayProviderAccountDetailDto> detailResponse = exchange(
                HttpMethod.GET, "/v1/provider-accounts/" + accountId, null,
                new ParameterizedTypeReference<SePayEnvelopeDto<SePayProviderAccountDetailDto>>() {
                });

        SePayProviderAccountDetailDto detail = detailResponse.getData();
        if (detail == null || detail.getTemplates() == null || detail.getTemplates().isEmpty()) {
            throw new IllegalStateException("SePay eInvoice: provider-account " + accountId + " has no invoice template");
        }

        providerAccountCache.put(PROVIDER_ACCOUNT_CACHE_KEY, detail);
        return detail;
    }

    @Override
    public SePayCreateInvoiceResponseDataDto createInvoice(SePayCreateInvoiceRequestDto request) {
        SePayEnvelopeDto<SePayCreateInvoiceResponseDataDto> response = exchange(
                HttpMethod.POST, "/v1/invoices/create", request,
                new ParameterizedTypeReference<SePayEnvelopeDto<SePayCreateInvoiceResponseDataDto>>() {
                });
        return response.getData();
    }

    @Override
    public SePayInvoiceStatusDataDto checkCreationStatus(String trackingCode) {
        SePayEnvelopeDto<SePayInvoiceStatusDataDto> response = exchange(
                HttpMethod.GET, "/v1/invoices/create/check/" + trackingCode, null,
                new ParameterizedTypeReference<SePayEnvelopeDto<SePayInvoiceStatusDataDto>>() {
                });
        return response.getData();
    }

    private <T> T exchange(HttpMethod method, String path, Object body, ParameterizedTypeReference<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(fetchAccessToken());
        HttpEntity<Object> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<T> response = restTemplate.exchange(
                    sePayProperties.getEinvoiceBaseUrl() + path, method, entity, responseType);
            return response.getBody();
        } catch (RestClientException exception) {
            log.error("SePay eInvoice API call failed: {} {}", method, path, exception);
            throw exception;
        }
    }

    private String fetchAccessToken() {
        String cached = tokenCache.getIfPresent(TOKEN_CACHE_KEY);
        if (cached != null) {
            return cached;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(sePayProperties.getEinvoiceUsername(), sePayProperties.getEinvoicePassword());
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<SePayEnvelopeDto<SePayTokenDataDto>> response = restTemplate.exchange(
                sePayProperties.getEinvoiceBaseUrl() + "/v1/token",
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<SePayEnvelopeDto<SePayTokenDataDto>>() {
                });

        SePayEnvelopeDto<SePayTokenDataDto> body = response.getBody();
        if (body == null || body.getData() == null || body.getData().getAccessToken() == null) {
            throw new IllegalStateException("SePay eInvoice: unable to obtain access_token");
        }

        String accessToken = body.getData().getAccessToken();
        tokenCache.put(TOKEN_CACHE_KEY, accessToken);
        return accessToken;
    }
}
