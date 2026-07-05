package com.talex.server.records;

import java.time.LocalDateTime;

public record CreatorVerificationStatus(
        Boolean isCreatorVerified,
        Boolean isTermsAccepted,
        String identityStatus,
        LocalDateTime identityVerifiedAt,
        String identityVerifiedNote,
        String taxId,
        String paymentStatus,
        LocalDateTime paymentVerifiedAt,
        String paymentVerifiedNote
) {
}
