package com.talex.server.dtos.requests.invoice;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SePayInvoiceBuyerDto {
    // "personal" | "business" — docs SePay đánh dấu tax_code/address là bắt buộc, kể cả ở
    // ví dụ mẫu cho buyer cá nhân. Account của TaleX (nền tảng xem phim/truyện, không phải
    // TMĐT) chưa bao giờ thu thập tax_code/address của user nên không có dữ liệu thật để
    // điền — gửi field rỗng thay vì bỏ hẳn (trước đây bỏ hẳn khiến SePay từ chối request với
    // thông báo lỗi sai lệch, báo nhầm thành thiếu template_code/invoice_series).
    private String type;

    private String name;

    @JsonProperty("tax_code")
    private String taxCode;

    private String address;

    private String email;
    private String phone;
}
