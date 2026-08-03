package com.talex.server.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum BankBin {
    ABBANK("970425", "ABBANK", "Ngân hàng TMCP An Bình"),
    ACB("970416", "ACB", "Ngân hàng TMCP Á Châu"),
    AGRIBANK("970405", "Agribank", "Ngân hàng Nông nghiệp và Phát triển Nông thôn Việt Nam"),
    BAC_A_BANK("970409", "BacABank", "Ngân hàng TMCP Bắc Á"),
    BAOVIET_BANK("970438", "BaoVietBank", "Ngân hàng TMCP Bảo Việt"),
    BIDV("970418", "BIDV", "Ngân hàng TMCP Đầu tư và Phát triển Việt Nam"),
    CAKE("546034", "CAKE", "TMCP Việt Nam Thịnh Vượng - Ngân hàng số CAKE by VPBank"),
    CIMB("422589", "CIMB", "Ngân hàng TNHH MTV CIMB Việt Nam"),
    COOPBANK("970446", "COOPBANK", "Ngân hàng Hợp tác xã Việt Nam"),
    EXIMBANK("970431", "Eximbank", "Ngân hàng TMCP Xuất Nhập khẩu Việt Nam"),
    HDBANK("970437", "HDBank", "Ngân hàng TMCP Phát triển Thành phố Hồ Chí Minh"),
    KBANK("668888", "KBank", "Ngân hàng Đại chúng TNHH Kasikornbank"),
    KIENLONG_BANK("970452", "KienLongBank", "Ngân hàng TMCP Kiên Long"),
    LPBANK("970449", "LPBank", "Ngân hàng TMCP Lộc Phát Việt Nam"),
    MBBANK("970422", "MBBank", "Ngân hàng TMCP Quân đội"),
    MBV("970414", "MBV", "Ngân hàng TNHH MTV Việt Nam Hiện Đại"),
    MOMO("971025", "MoMo", "CTCP Dịch Vụ Di Động Trực Tuyến"),
    MSB("970426", "MSB", "Ngân hàng TMCP Hàng Hải Việt Nam"),
    NAM_A_BANK("970428", "NamABank", "Ngân hàng TMCP Nam Á"),
    NCB("970419", "NCB", "Ngân hàng TMCP Quốc Dân"),
    OCB("970448", "OCB", "Ngân hàng TMCP Phương Đông"),
    PGBANK("970430", "PGBank", "Ngân hàng TMCP Thịnh vượng và Phát triển"),
    PVCOMBANK("970412", "PVcomBank", "Ngân hàng TMCP Đại Chúng Việt Nam"),
    PVCOMBANK_PAY("971133", "PVcomBank Pay", "Ngân hàng TMCP Đại Chúng Việt Nam Ngân hàng số"),
    SACOMBANK("970403", "Sacombank", "Ngân hàng TMCP Sài Gòn Thương Tín"),
    SAIGON_BANK("970400", "SaigonBank", "Ngân hàng TMCP Sài Gòn Công Thương"),
    SCB("970429", "SCB", "Ngân hàng TMCP Sài Gòn"),
    SEABANK("970440", "SeABank", "Ngân hàng TMCP Đông Nam Á"),
    SHB("970443", "SHB", "Ngân hàng TMCP Sài Gòn - Hà Nội"),
    SHINHAN_BANK("970424", "ShinhanBank", "Ngân hàng TNHH MTV Shinhan Việt Nam"),
    TECHCOMBANK("970407", "Techcombank", "Ngân hàng TMCP Kỹ thương Việt Nam"),
    TIMO("963388", "Timo", "Ngân hàng số Timo by Ban Viet Bank"),
    TPBANK("970423", "TPBank", "Ngân hàng TMCP Tiên Phong"),
    UBANK("546035", "Ubank", "TMCP Việt Nam Thịnh Vượng - Ngân hàng số Ubank by VPBank"),
    VIB("970441", "VIB", "Ngân hàng TMCP Quốc tế Việt Nam"),
    VIET_A_BANK("970427", "VietABank", "Ngân hàng TMCP Việt Á"),
    VIETBANK("970433", "VietBank", "Ngân hàng TMCP Việt Nam Thương Tín"),
    VIET_CAPITAL_BANK("970454", "VietCapitalBank", "Ngân hàng TMCP Bản Việt"),
    VIETCOMBANK("970436", "Vietcombank", "Ngân hàng TMCP Ngoại Thương Việt Nam"),
    VIETINBANK("970415", "VietinBank", "Ngân hàng TMCP Công thương Việt Nam"),
    VPBANK("970432", "VPBank", "Ngân hàng TMCP Việt Nam Thịnh Vượng"),
    WOORI("970457", "Woori", "Ngân hàng TNHH MTV Woori Việt Nam"),
    CBBANK("970444", "CBBank", "Ngân hàng Thương mại TNHH MTV Xây dựng Việt Nam"),
    CITIBANK("533948", "Citibank", "Ngân hàng Citibank, N.A. - Chi nhánh Hà Nội"),
    DBS_BANK("796500", "DBSBank", "DBS Bank Ltd - Chi nhánh Thành phố Hồ Chí Minh"),
    GPBANK("970408", "GPBank", "Ngân hàng Thương mại TNHH MTV Dầu Khí Toàn Cầu"),
    VBSP("999888", "VBSP", "Ngân hàng Chính sách Xã hội"),
    VIETTEL_MONEY("971005", "ViettelMoney", "Tổng Công ty Dịch vụ số Viettel"),
    VIKKI("970406", "Vikki", "Ngân hàng TNHH MTV Số Vikki"),
    VNPT_MONEY("971011", "VNPTMoney", "VNPT Money"),
    VRB("970421", "VRB", "Ngân hàng Liên doanh Việt - Nga");

    private final String bin;
    private final String shortName;
    private final String fullName;
}