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

    // ── 429 Concurrency / Rate Limit ──────────────────────────────────────────

    /**
     * Không lấy được Distributed Lock — có giao dịch coin khác đang xử lý
     * cho cùng tài khoản. Client nên thử lại sau vài giây.
     */
    COIN_PROCESSING(6029, HttpStatus.TOO_MANY_REQUESTS, "Giao dịch Coin đang được xử lý, vui lòng thử lại sau"),

    // ── 5xx Server Errors ─────────────────────────────────────────────────────

    /** Không tìm thấy ví Coin của tài khoản (trong trường hợp không dùng Lazy Init). */
    WALLET_NOT_FOUND(6500, HttpStatus.NOT_FOUND, "Không tìm thấy ví Coin của tài khoản");

    private final int code;
    private final HttpStatus httpStatus;
    private final String defaultMessage;
}
