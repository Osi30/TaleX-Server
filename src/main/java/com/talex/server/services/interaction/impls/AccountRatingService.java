package com.talex.server.services.interaction.impls;

import com.talex.server.dtos.interaction.request.RatingRequest;
import com.talex.server.dtos.interaction.response.AccountRatingResponse;
import com.talex.server.dtos.interaction.response.SeriesRatingResponse;
import com.talex.server.entities.auth.Account;
import com.talex.server.entities.interaction.AccountRating;
import com.talex.server.entities.series.Series;
import com.talex.server.repositories.auth.AccountRepository;
import com.talex.server.repositories.interaction.AccountRatingRepository;
import com.talex.server.repositories.series.SeriesRepository;
import com.talex.server.services.interaction.IAccountRatingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountRatingService implements IAccountRatingService {
    private final AccountRatingRepository ratingRepository;
    private final AccountRepository accountRepository;
    private final SeriesRepository seriesRepository;

    @Override
    @Transactional
    public void upsertRating(RatingRequest request) {
        ratingRepository.findByAccountAccountIdAndSeriesSeriesId(request.getAccountId(), request.getSeriesId())
                .ifPresentOrElse(
                        existingRating -> {
                            // Update: Nếu tồn tại thì cập nhật số sao đánh giá mới
                            double ratingDelta = request.getRate() - existingRating.getRate();
                            existingRating.setRate(request.getRate());
                            ratingRepository.save(existingRating);

                            if (ratingDelta != 0) {
                                ratingRepository.updateSeriesRatingMetrics(request.getSeriesId(), ratingDelta, 0);
                            }
                        },
                        () -> {
                            // Create: Nếu chưa có thì khởi tạo bản ghi mới
                            Account account = accountRepository.findById(request.getAccountId())
                                    .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại"));
                            Series series = seriesRepository.findById(request.getSeriesId())
                                    .orElseThrow(() -> new RuntimeException("Series không tồn tại"));

                            AccountRating newRating = AccountRating.builder()
                                    .account(account)
                                    .series(series)
                                    .rate(request.getRate())
                                    .build();
                            ratingRepository.save(newRating);
                            ratingRepository.updateSeriesRatingMetrics(request.getSeriesId(), request.getRate(), 1);
                        }
                );
    }

    @Override
    @Transactional
    public void deleteRating(UUID accountId, String seriesId) {
        AccountRating rating = ratingRepository.findByAccountAccountIdAndSeriesSeriesId(accountId, seriesId)
                .orElseThrow(() -> new RuntimeException("Bạn chưa đánh giá series này"));
        double oldRate = rating.getRate();
        ratingRepository.delete(rating);
        ratingRepository.updateSeriesRatingMetrics(seriesId, -oldRate, -1);
    }

    @Override
    @Transactional(readOnly = true)
    public Slice<AccountRatingResponse> getRatingsByAccount(UUID accountId, Pageable pageable) {
        if (!accountRepository.existsById(accountId)) {
            throw new RuntimeException("Tài khoản không tồn tại để thực hiện tác vụ đánh giá");
        }
        return ratingRepository.findRatingsByAccountId(accountId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Slice<SeriesRatingResponse> getRatingsBySeries(String seriesId, Pageable pageable) {
        if (!seriesRepository.existsById(seriesId)) {
            throw new RuntimeException("Series không tồn tại đê thực hiện tác vụ đánh giá");
        }
        return ratingRepository.findRatingsBySeriesId(seriesId, pageable);
    }
}
