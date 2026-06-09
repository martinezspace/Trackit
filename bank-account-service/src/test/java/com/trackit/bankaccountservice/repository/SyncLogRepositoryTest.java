package com.trackit.bankaccountservice.repository;

import com.trackit.bankaccountservice.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class SyncLogRepositoryTest {

    @Autowired
    private SyncLogRepository syncLogRepository;

    @Autowired
    private BankConnectionRepository bankConnectionRepository;

    @Autowired
    private TestEntityManager entityManager;

    private BankConnection connection;
    private BankConnection otherConnection;
    private SyncLog completedLog;
    private SyncLog failedLog;

    @BeforeEach
    void setUp() {
        UUID userId = UUID.randomUUID();

        connection = new BankConnection();
        connection.setUserId(userId);
        connection.setInstitutionId("MONZO_GB");
        connection.setInstitutionName("Monzo");
        connection.setCredentialsId("req-monzo-001");
        connection.setStatus(ConnectionStatus.ACTIVE);
        bankConnectionRepository.save(connection);

        otherConnection = new BankConnection();
        otherConnection.setUserId(UUID.randomUUID());
        otherConnection.setInstitutionId("ING_PL");
        otherConnection.setInstitutionName("ING");
        otherConnection.setCredentialsId("req-ing-001");
        otherConnection.setStatus(ConnectionStatus.ACTIVE);
        bankConnectionRepository.save(otherConnection);

        completedLog = new SyncLog();
        completedLog.setConnection(connection);
        completedLog.setTrigger(SyncTrigger.MANUAL);
        completedLog.setStatus(SyncStatus.COMPLETED);
        completedLog.setTransactionsFetched(100);
        completedLog.setTransactionsNew(25);
        completedLog.setTransactionsSuggested(10);
        completedLog.setCompletedAt(LocalDateTime.now());
        syncLogRepository.save(completedLog);

        failedLog = new SyncLog();
        failedLog.setConnection(connection);
        failedLog.setTrigger(SyncTrigger.SCHEDULED);
        failedLog.setStatus(SyncStatus.FAILED);
        failedLog.setErrorMessage("GoCardless API timeout");
        failedLog.setCompletedAt(LocalDateTime.now());
        syncLogRepository.save(failedLog);

        // Log for a different connection - should not appear in our queries
        SyncLog otherLog = new SyncLog();
        otherLog.setConnection(otherConnection);
        otherLog.setTrigger(SyncTrigger.WEBHOOK);
        otherLog.setStatus(SyncStatus.COMPLETED);
        syncLogRepository.save(otherLog);
    }

    // findByConnectionIdOrderByStartedAtDesc

    @Test
    public void findByConnectionId_returnsOnlyLogsForThatConnection() {
        Page<SyncLog> result = syncLogRepository
                .findByConnectionIdOrderByStartedAtDesc(connection.getId(), PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).allMatch(l -> l.getConnection().getId().equals(connection.getId()));
    }

    @Test
    public void findByConnectionId_doesNotReturnOtherConnectionLogs() {
        Page<SyncLog> result = syncLogRepository
                .findByConnectionIdOrderByStartedAtDesc(connection.getId(), PageRequest.of(0, 10));

        assertThat(result.getContent()).noneMatch(l -> l.getConnection().getId().equals(otherConnection.getId()));
    }

    @Test
    public void findByConnectionId_returnsEmpty_whenConnectionHasNoLogs() {
        BankConnection emptyConnection = new BankConnection();
        emptyConnection.setUserId(UUID.randomUUID());
        emptyConnection.setInstitutionId("PEKAO_PL");
        emptyConnection.setInstitutionName("Pekao");
        emptyConnection.setCredentialsId("req-pekao-001");
        emptyConnection.setStatus(ConnectionStatus.ACTIVE);
        bankConnectionRepository.save(emptyConnection);

        Page<SyncLog> result = syncLogRepository
                .findByConnectionIdOrderByStartedAtDesc(emptyConnection.getId(), PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @Test
    public void findByConnectionId_paginationWorks() {
        for (int i = 0; i < 3; i++) {
            SyncLog log = new SyncLog();
            log.setConnection(connection);
            log.setTrigger(SyncTrigger.MANUAL);
            log.setStatus(SyncStatus.COMPLETED);
            syncLogRepository.save(log);
        }

        Page<SyncLog> firstPage = syncLogRepository
                .findByConnectionIdOrderByStartedAtDesc(connection.getId(), PageRequest.of(0, 2));

        assertThat(firstPage.getContent()).hasSize(2);
        assertThat(firstPage.getTotalElements()).isEqualTo(5);
        assertThat(firstPage.getTotalPages()).isEqualTo(3);
    }

    // persistence

    @Test
    public void save_persistsAllFieldsForCompletedLog() {
        // Flush writes to DB, clear removes from first-level cache
        // Without this, findById returns the same in-memory object
        // which has null timestamps before Hibernate refreshes them
        entityManager.flush();
        entityManager.clear();

        SyncLog found = syncLogRepository.findById(completedLog.getId()).orElseThrow();

        assertThat(found.getStatus()).isEqualTo(SyncStatus.COMPLETED);
        assertThat(found.getTrigger()).isEqualTo(SyncTrigger.MANUAL);
        assertThat(found.getTransactionsFetched()).isEqualTo(100);
        assertThat(found.getTransactionsNew()).isEqualTo(25);
        assertThat(found.getTransactionsSuggested()).isEqualTo(10);
        assertThat(found.getCompletedAt()).isNotNull();
        assertThat(found.getStartedAt()).isNotNull();
    }

    @Test
    public void save_persistsErrorMessageForFailedLog() {
        SyncLog found = syncLogRepository.findById(failedLog.getId()).orElseThrow();

        assertThat(found.getStatus()).isEqualTo(SyncStatus.FAILED);
        assertThat(found.getErrorMessage()).isEqualTo("GoCardless API timeout");
    }
}