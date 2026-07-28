package com.talex.server.services.ads.impls;

import com.talex.server.dtos.requests.ads.AdTopupRequestDto;
import com.talex.server.dtos.responses.ads.AdvertiseProfileResponseDto;
import com.talex.server.entities.ads.AdCampaign;
import com.talex.server.entities.ads.AdTransaction;
import com.talex.server.entities.ads.AdvertiseProfile;
import com.talex.server.entities.auth.Account;
import com.talex.server.enums.ads.AdTransactionType;
import com.talex.server.repositories.ads.AdCampaignRepository;
import com.talex.server.repositories.ads.AdTransactionRepository;
import com.talex.server.repositories.ads.AdvertiseProfileRepository;
import com.talex.server.repositories.auth.AccountRepository;
import com.talex.server.services.ads.IAdWalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdWalletServiceImpl implements IAdWalletService {

    private final AdvertiseProfileRepository profileRepository;
    private final AdTransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final AdCampaignRepository campaignRepository;

    @Override
    @Transactional
    public AdvertiseProfileResponseDto getOrCreateProfile(UUID accountId) {
        AdvertiseProfile profile = profileRepository.findByAccount_AccountId(accountId)
                .orElseGet(() -> {
                    Account account = accountRepository.findById(accountId)
                            .orElseThrow(() -> new RuntimeException("Account not found"));
                    AdvertiseProfile newProfile = AdvertiseProfile.builder()
                            .account(account)
                            .walletBalance(0L)
                            .build();
                    return profileRepository.save(newProfile);
                });
        return toDto(profile);
    }

    @Override
    @Transactional
    public AdvertiseProfileResponseDto setupProfile(UUID accountId, com.talex.server.dtos.requests.ads.AdProfileSetupRequestDto request) {
        AdvertiseProfile profile = profileRepository.findByAccount_AccountId(accountId)
                .orElseThrow(() -> new RuntimeException("Profile not found. Please visit dashboard first."));
        
        profile.setCompanyName(request.getCompanyName());
        profile.setPhone(request.getPhone());
        profile.setWebsite(request.getWebsite());
        profile.setIsSetupCompleted(true);
        
        return toDto(profileRepository.save(profile));
    }

    @Override
    @Transactional
    public AdvertiseProfileResponseDto topupWallet(UUID accountId, AdTopupRequestDto request) {
        AdvertiseProfile profile = profileRepository.findByAccount_AccountId(accountId)
                .orElseGet(() -> {
                    Account account = accountRepository.findById(accountId)
                            .orElseThrow(() -> new RuntimeException("Account not found"));
                    return profileRepository.save(AdvertiseProfile.builder().account(account).walletBalance(0L).build());
                });

        profile.setWalletBalance(profile.getWalletBalance() + request.getAmount());
        profileRepository.save(profile);

        AdTransaction transaction = AdTransaction.builder()
                .profile(profile)
                .amount(request.getAmount())
                .type(AdTransactionType.TOPUP)
                .note("Mockup Topup via Ad Center")
                .build();
        transactionRepository.save(transaction);

        return toDto(profile);
    }

    @Override
    @Transactional
    public void fundCampaign(UUID profileId, Long amount, UUID campaignId) {
        AdvertiseProfile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new RuntimeException("Profile not found"));
        AdCampaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new RuntimeException("Campaign not found"));

        if (profile.getWalletBalance() < amount) {
            throw new RuntimeException("Số dư không đủ. Vui lòng nạp thêm tiền vào ví tổng.");
        }

        profile.setWalletBalance(profile.getWalletBalance() - amount);
        profileRepository.save(profile);

        campaign.setCampaignBalance(campaign.getCampaignBalance() + amount);
        campaignRepository.save(campaign);

        AdTransaction transaction = AdTransaction.builder()
                .profile(profile)
                .campaign(campaign)
                .amount(amount)
                .type(AdTransactionType.FUND_CAMPAIGN)
                .note("Nạp tiền vào chiến dịch: " + campaign.getName())
                .build();
        transactionRepository.save(transaction);
    }

    @Override
    @Transactional
    public void chargeHeldFunds(UUID profileId, Long amount, UUID campaignId) {
        AdvertiseProfile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new RuntimeException("Profile not found"));
        AdCampaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new RuntimeException("Campaign not found"));

        AdTransaction transaction = AdTransaction.builder()
                .profile(profile)
                .campaign(campaign)
                .amount(amount)
                .type(AdTransactionType.CHARGE)
                .note("Charge funds after admin approval for campaign: " + campaign.getName())
                .build();
        transactionRepository.save(transaction);
    }

    @Override
    @Transactional
    public void refundHeldFunds(UUID profileId, Long amount, UUID campaignId, String note) {
        AdvertiseProfile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new RuntimeException("Profile not found"));
        AdCampaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new RuntimeException("Campaign not found"));

        profile.setWalletBalance(profile.getWalletBalance() + amount);
        profileRepository.save(profile);

        AdTransaction transaction = AdTransaction.builder()
                .profile(profile)
                .campaign(campaign)
                .amount(amount)
                .type(AdTransactionType.REFUND)
                .note(note)
                .build();
        transactionRepository.save(transaction);
    }

    @Override
    public java.util.List<com.talex.server.dtos.responses.ads.AdTransactionResponseDto> getWalletTransactions(UUID accountId) {
        AdvertiseProfile profile = profileRepository.findByAccount_AccountId(accountId)
                .orElseThrow(() -> new RuntimeException("Profile not found"));
        return transactionRepository.findByProfile_ProfileIdOrderByCreatedAtDesc(profile.getProfileId())
                .stream().map(this::toTransactionDto).collect(java.util.stream.Collectors.toList());
    }

    private com.talex.server.dtos.responses.ads.AdTransactionResponseDto toTransactionDto(AdTransaction transaction) {
        return com.talex.server.dtos.responses.ads.AdTransactionResponseDto.builder()
                .transactionId(transaction.getTransactionId())
                .amount(transaction.getAmount())
                .type(transaction.getType().name())
                .note(transaction.getNote())
                .createdAt(transaction.getCreatedAt())
                .build();
    }

    private AdvertiseProfileResponseDto toDto(AdvertiseProfile profile) {
        return AdvertiseProfileResponseDto.builder()
                .profileId(profile.getProfileId())
                .accountId(profile.getAccount().getAccountId())
                .walletBalance(profile.getWalletBalance())
                .billingInfo(profile.getBillingInfo())
                .companyName(profile.getCompanyName())
                .phone(profile.getPhone())
                .website(profile.getWebsite())
                .isSetupCompleted(profile.getIsSetupCompleted())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }
}
