package com.example.ecommerce.schedule;

import com.example.ecommerce.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderSchedule {

    private final OrderService orderService;

    // Chạy định kỳ lúc 02:00 hằng ngày để hủy đơn PENDING quá hạn.
    @Scheduled(cron = "0 0 2 * * *", zone = "Asia/Ho_Chi_Minh")
    public void cancelExpiredOrders() {

        log.info("Starting scheduled job to cancel expired orders");

        orderService.cancelExpiredOrders();

        log.info("Finished scheduled job to cancel expired orders");
    }
}
