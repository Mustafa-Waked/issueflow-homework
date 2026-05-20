package com.att.tdp.issueflow.scheduler;

import com.att.tdp.issueflow.service.TicketService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OverdueEscalationScheduler {
    private final TicketService ticketService;
    public OverdueEscalationScheduler(TicketService ticketService) { this.ticketService = ticketService; }

    @Scheduled(fixedDelay = 60000)
    public void run() {
        ticketService.escalateOverdue();
    }
}
