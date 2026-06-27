package com.talex.server.exceptions.codes;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum CoinErrorCode {

    // ── 4xx Client Errors ─────────────────────────────────────────────────────

    /** Số dư ví không đủ để thực hiện giao dịch. */
    INSUFFICIENT_BALANCE(6001, HttpStatus.BAD_REQUEST, "Số dư Coin không đủ để thực hiện giao dịch"),

    /** User đã điểm danh hôm nay rồi. */
    ALREADY_CHECKED_IN(6002, HttpStatus.CONFLICT, "Bạn đã điểm danh hôm nay rồi"),

    /** Số tiền giao dịch không hợp lệ (âm hoặc bằng 0). */
    INVALID_AMOUNT(6003, HttpStatus.BAD_REQUEST, "Số lượng Coin giao dịch phải lớn hơn 0"),

    /** Mã nhiệm vụ là business key và phải duy nhất trong toàn hệ thống. */
    MISSION_CODE_ALREADY_EXISTS(6004, HttpStatus.CONFLICT, "Mã nhiệm vụ đã tồn tại"),

    /** Reward ad session is missing, expired, or belongs to another account. */
    AD_SESSION_INVALID(6005, HttpStatus.BAD_REQUEST, "Ad session is invalid or has expired"),

    /** User completed the reward ad before the minimum watch duration. */
    AD_WATCH_TIME_TOO_SHORT(6006, HttpStatus.BAD_REQUEST, "You must watch the ad for the minimum required time"),

    // ── 429 Concurrency / Rate Limit ──────────────────────────────────────────

    /**
     * Không lấy được Distributed Lock — có giao dịch coin khác đang xử lý
     * cho cùng tài khoản. Client nên thử lại sau vài giây.
     */
    COIN_PROCESSING(6029, HttpStatus.TOO_MANY_REQUESTS, "Giao dịch Coin đang được xử lý, vui lòng thử lại sau"),

    // ── 5xx Server Errors ─────────────────────────────────────────────────────

    /** Không tìm thấy ví Coin của tài khoản (trong trường hợp không dùng Lazy Init). */
    WALLET_NOT_FOUND(6500, HttpStatus.NOT_FOUND, "Không tìm thấy ví Coin của tài khoản"),

    /** Không tìm thấy cấu hình nhiệm vụ theo ID được yêu cầu. */
    MISSION_NOT_FOUND(6501, HttpStatus.NOT_FOUND, "Không tìm thấy nhiệm vụ");

    private final int code;
    private final HttpStatus httpStatus;
    private final String defaultMessage;
}
