package com.att.tdp.issueflow.service;

import com.att.tdp.issueflow.entity.TicketStatus;
import com.att.tdp.issueflow.exception.BadRequestException;

import java.util.Map;

final class TicketStatusTransition {

    private static final Map<TicketStatus, TicketStatus> NEXT = Map.of(
            TicketStatus.TODO, TicketStatus.IN_PROGRESS,
            TicketStatus.IN_PROGRESS, TicketStatus.IN_REVIEW,
            TicketStatus.IN_REVIEW, TicketStatus.DONE
    );

    private TicketStatusTransition() {
    }

    static void validate(TicketStatus from, TicketStatus to) {
        if (from == to) {
            return;
        }
        if (from == TicketStatus.DONE) {
            throw new BadRequestException("DONE ticket cannot change status");
        }
        TicketStatus expected = NEXT.get(from);
        if (expected != to) {
            throw new BadRequestException("Invalid status transition from " + from + " to " + to);
        }
    }
}
