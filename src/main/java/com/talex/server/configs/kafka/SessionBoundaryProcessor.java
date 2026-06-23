package com.talex.server.configs.kafka;

import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.processor.PunctuationType;
import org.apache.kafka.streams.processor.api.ContextualProcessor;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.KeyValueStore;

import java.time.Duration;

//public class SessionBoundaryProcessor extends ContextualProcessor<String, String, String, String> {
//    private KeyValueStore<String, String> stateStore;
//    private final long INACTIVITY_GAP_MS = 30000; // 30 giây cấu hình session
//
//    @Override
//    public void init(ProcessorContext<String, String> context) {
//        super.init(context);
//        // Lấy bộ đệm lưu trữ trạng thái ra (Tên store phải trùng với cấu hình ở Topology)
//        this.stateStore = context.getStateStore("session-boundary-store");
//
//        // Đăng ký một con quét ngầm (Punctuator) cứ mỗi 5 giây thức dậy kiểm tra một lần
//        context.schedule(Duration.ofSeconds(5), PunctuationType.WALL_CLOCK_TIME, timestamp -> {
//            try (KeyValueIterator<String, String> iterator = stateStore.all()) {
//                while (iterator.hasNext()) {
//                    var entry = iterator.next();
//                    String sessionId = entry.key;
//                    String[] parts = entry.value.split(";"); // Phân tách các phần bằng dấu chấm phẩy
//
//                    long lastHeartbeat = Long.parseLong(parts[2]);
//
//                    // Kiểm tra nếu thời gian hiện tại vượt quá 30s kể từ tin nhắn cuối cùng
//                    if (timestamp - lastHeartbeat > INACTIVITY_GAP_MS) {
//                        String firstMessage = parts[0];
//                        String lastMessage = parts[1];
//
//                        // 1. GỬI TIN NHẮN ĐẦU TIÊN XUỐNG DOWNSTREAM (Topic watch-summary)
//                        context().forward(new Record<>(sessionId, "START_SESSION:" + firstMessage, timestamp));
//
//                        // 2. GỬI TIN NHẮN KẾT THÚC XUỐNG DOWNSTREAM
//                        context().forward(new Record<>(sessionId, "END_SESSION:" + lastMessage, timestamp));
//
//                        // 3. XÓA SẠCH BỘ ĐỆM (Kích hoạt lại trạng thái isEmpty cho lần sau nếu trùng sessionId)
//                        stateStore.delete(sessionId);
//                    }
//                }
//            }
//        });
//    }
//
//    @Override
//    public void process(Record<String, String> record) {
//        String sessionId = record.key();
//        String currentRawMessage = record.value();
//        long now = context().currentSystemTimeMs(); // Lấy thời gian hệ thống hiện tại
//
//        String existingState = stateStore.get(sessionId);
//
//        if (existingState == null) {
//            // Trường hợp ĐỆM TRỐNG -> Đây chính là phát súng đầu tiên của Session
//            // Cấu trúc lưu: TinNhắnĐầu ; TinNhắnCuối ; ThờiGianMớiNhất
//            String newStateValue = currentRawMessage + ";" + currentRawMessage + ";" + now;
//            stateStore.put(sessionId, newStateValue);
//        } else {
//            // Trường hợp ĐÃ CÓ ĐỆM -> User vẫn đang xem phim trong vòng tuần hoàn 30s
//            String[] parts = existingState.split(";");
//            String firstMessage = parts[0]; // Giữ nguyên bản tin đầu tiên không đổi
//
//            // Cập nhật lại bản tin mới nhất và gia hạn thêm thời gian Heartbeat
//            String updatedStateValue = firstMessage + ";" + currentRawMessage + ";" + now;
//            stateStore.put(sessionId, updatedValue);
//        }
//    }
//}
