package com.att.tdp.issueflow.service;

import com.att.tdp.issueflow.entity.TicketStatus;
import com.att.tdp.issueflow.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TicketStatusTransitionTest {

    @Test
    void allowsForwardTransitions() {
        assertDoesNotThrow(() -> TicketStatusTransition.validate(TicketStatus.TODO, TicketStatus.IN_PROGRESS));
        assertDoesNotThrow(() -> TicketStatusTransition.validate(TicketStatus.IN_PROGRESS, TicketStatus.IN_REVIEW));
        assertDoesNotThrow(() -> TicketStatusTransition.validate(TicketStatus.IN_REVIEW, TicketStatus.DONE));
    }

    @Test
    void rejectsBackwardAndSkippedTransitions() {
        assertThrows(BadRequestException.class,
                () -> TicketStatusTransition.validate(TicketStatus.IN_PROGRESS, TicketStatus.TODO));
        assertThrows(BadRequestException.class,
                () -> TicketStatusTransition.validate(TicketStatus.TODO, TicketStatus.DONE));
        assertThrows(BadRequestException.class,
                () -> TicketStatusTransition.validate(TicketStatus.TODO, TicketStatus.IN_REVIEW));
    }

    @Test
    void rejectsDoneStatusChanges() {
        assertThrows(BadRequestException.class,
                () -> TicketStatusTransition.validate(TicketStatus.DONE, TicketStatus.IN_REVIEW));
    }
}
