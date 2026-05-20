package com.att.tdp.issueflow.audit;

import com.att.tdp.issueflow.entity.AppUser;
import com.att.tdp.issueflow.entity.AuditLog;
import com.att.tdp.issueflow.entity.UserRole;
import com.att.tdp.issueflow.repository.AppUserRepository;
import com.att.tdp.issueflow.repository.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock AuditLogRepository auditLogRepository;
    @Mock AppUserRepository appUserRepository;

    @Test
    void persistsAuditEntry() {
        AuditService service = new AuditService(auditLogRepository, appUserRepository);
        service.log("SYSTEM", "AUTO_ASSIGN", "TICKET", "1", null, "2");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog saved = captor.getValue();
        assertEquals("SYSTEM", saved.getActor());
        assertEquals("AUTO_ASSIGN", saved.getAction());
    }

    @Test
    void mapsUserActorToPerformedBy() {
        AuditService service = new AuditService(auditLogRepository, appUserRepository);
        AuditLog log = new AuditLog();
        log.setId(1L);
        log.setActor("admin");
        log.setAction("CREATE");
        log.setEntityType("TICKET");
        log.setEntityId("5");
        log.setOldValue("LOW");
        log.setNewValue("HIGH");

        AppUser admin = new AppUser();
        admin.setId(99L);
        admin.setUsername("admin");
        admin.setRole(UserRole.ADMIN);
        when(appUserRepository.findByUsernameIgnoreCase("admin")).thenReturn(Optional.of(admin));

        var response = service.toResponse(log);
        assertEquals(99L, response.performedBy());
        assertEquals("USER", response.actor());
        assertEquals("LOW", response.oldValue());
        assertEquals("HIGH", response.newValue());
    }

    @Test
    void mapsSystemActorWithoutPerformedBy() {
        AuditService service = new AuditService(auditLogRepository, appUserRepository);
        AuditLog log = new AuditLog();
        log.setActor("SYSTEM");
        log.setAction("AUTO_ESCALATION");
        log.setEntityType("TICKET");
        log.setEntityId("1");

        var response = service.toResponse(log);
        assertNull(response.performedBy());
        assertEquals("SYSTEM", response.actor());
    }
}
