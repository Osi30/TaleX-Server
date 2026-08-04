package com.talex.server.services.auth.impls;

import com.talex.server.entities.auth.Account;
import com.talex.server.repositories.auth.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Insert account Google mới trong transaction RIÊNG (REQUIRES_NEW), tách khỏi
 * transaction ngoài của {@code googleLogin}. Lý do: nếu 2 request cùng tạo
 * account Google song song (double-click / mobile retry), request thua cuộc sẽ
 * bị {@code DataIntegrityViolationException} do unique constraint (email/googleSubId).
 * Nếu insert nằm CHUNG transaction với các lookup trước đó, Spring sẽ đánh dấu
 * transaction đó rollback-only, khiến bước re-lookup account (đã tạo bởi request
 * thắng cuộc) ngay sau đó không tin cậy được. Cô lập vào transaction riêng để
 * exception chỉ rollback đúng phần insert, transaction ngoài vẫn dùng được bình
 * thường cho re-lookup. `saveAndFlush` để buộc flush ngay, bắt lỗi constraint
 * đồng bộ thay vì trì hoãn tới cuối transaction.
 */
@Component
@RequiredArgsConstructor
public class GoogleAccountCreationExecutor {

    private final AccountRepository accountRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Account createIsolated(Account newAccount) {
        return accountRepository.saveAndFlush(newAccount);
    }
}
