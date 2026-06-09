package com.trackit.bankaccountservice.repository;

import com.trackit.bankaccountservice.model.BankConnection;
import com.trackit.bankaccountservice.model.ConnectionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class BankConnectionRepositoryTest {

    @Autowired
    private BankConnectionRepository bankConnectionRepository;

    @Autowired
    private TestEntityManager entityManager;

    private UUID userId;
    private UUID otherUserId;
    private BankConnection activeConnection;
    private BankConnection expiredConnection;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        otherUserId = UUID.randomUUID();

        activeConnection = new BankConnection();
        activeConnection.setUserId(userId);
        activeConnection.setInstitutionId("MONZO_MONZGB2L");
        activeConnection.setInstitutionName("Monzo");
        activeConnection.setCredentialsId("req-monzo-001");
        activeConnection.setStatus(ConnectionStatus.ACTIVE);
        bankConnectionRepository.save(activeConnection);

        expiredConnection = new BankConnection();
        expiredConnection.setUserId(userId);
        expiredConnection.setInstitutionId("ING_PL");
        expiredConnection.setInstitutionName("ING Bank");
        expiredConnection.setCredentialsId("req-ing-001");
        expiredConnection.setStatus(ConnectionStatus.EXPIRED);
        bankConnectionRepository.save(expiredConnection);

        // Connection for otherUserId - should never appear in userId queries
        BankConnection otherUserConnection = new BankConnection();
        otherUserConnection.setUserId(otherUserId);
        otherUserConnection.setInstitutionId("PKO_PL");
        otherUserConnection.setInstitutionName("PKO BP");
        otherUserConnection.setCredentialsId("req-pko-001");
        otherUserConnection.setStatus(ConnectionStatus.ACTIVE);
        bankConnectionRepository.save(otherUserConnection);
    }

    // findByUserId

    @Test
    public void findByUserId_returnsAllConnectionsForThatUser() {
        List<BankConnection> result = bankConnectionRepository.findByUserId(userId);

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(c -> c.getUserId().equals(userId));
    }

    @Test
    public void findByUserId_doesNotReturnOtherUsersConnections() {
        List<BankConnection> result = bankConnectionRepository.findByUserId(userId);

        assertThat(result).noneMatch(c -> c.getUserId().equals(otherUserId));
    }

    @Test
    public void findByUserId_returnsEmpty_whenUserHasNoConnections() {
        List<BankConnection> result = bankConnectionRepository.findByUserId(UUID.randomUUID());

        assertThat(result).isEmpty();
    }

    // findByIdAndUserId

    @Test
    public void findByIdAndUserId_returnsConnection_whenOwnershipMatches() {
        Optional<BankConnection> result = bankConnectionRepository
                .findByIdAndUserId(activeConnection.getId(), userId);

        assertThat(result).isPresent();
        assertThat(result.get().getInstitutionId()).isEqualTo("MONZO_MONZGB2L");
    }

    @Test
    public void findByIdAndUserId_returnsEmpty_whenWrongUserId() {
        Optional<BankConnection> result = bankConnectionRepository
                .findByIdAndUserId(activeConnection.getId(), otherUserId);

        assertThat(result).isEmpty();
    }

    @Test
    public void findByIdAndUserId_returnsEmpty_whenConnectionDoesNotExist() {
        Optional<BankConnection> result = bankConnectionRepository
                .findByIdAndUserId(UUID.randomUUID(), userId);

        assertThat(result).isEmpty();
    }

    // findByCredentialsId

    @Test
    public void findByCredentialsId_returnsConnection_whenExists() {
        Optional<BankConnection> result = bankConnectionRepository
                .findByCredentialsId("req-monzo-001");

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(activeConnection.getId());
    }

    @Test
    public void findByCredentialsId_returnsEmpty_whenNotFound() {
        Optional<BankConnection> result = bankConnectionRepository
                .findByCredentialsId("req-nonexistent");

        assertThat(result).isEmpty();
    }

    // persistence

    @Test
    public void save_persistsAllFields() {
        BankConnection connection = new BankConnection();
        connection.setUserId(userId);
        connection.setInstitutionId("PEKAO_PL");
        connection.setInstitutionName("Pekao SA");
        connection.setCredentialsId("req-pekao-001");
        connection.setStatus(ConnectionStatus.PENDING);
        connection.setExpiresAt(LocalDateTime.now().plusDays(90));
        bankConnectionRepository.save(connection);

        // Flush writes to DB, clear removes from first-level cache
        // Without this, findById returns the same in-memory object
        // which has null timestamps before Hibernate refreshes them
        entityManager.flush();
        entityManager.clear();

        BankConnection found = bankConnectionRepository.findById(connection.getId()).orElseThrow();
        assertThat(found.getInstitutionName()).isEqualTo("Pekao SA");
        assertThat(found.getStatus()).isEqualTo(ConnectionStatus.PENDING);
        assertThat(found.getExpiresAt()).isNotNull();
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isNotNull();
    }

    @Test
    public void delete_removesConnection() {
        bankConnectionRepository.delete(activeConnection);

        assertThat(bankConnectionRepository.findById(activeConnection.getId())).isEmpty();
    }
}