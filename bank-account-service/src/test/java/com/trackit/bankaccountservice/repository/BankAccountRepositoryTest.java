package com.trackit.bankaccountservice.repository;

import com.trackit.bankaccountservice.model.BankAccount;
import com.trackit.bankaccountservice.model.BankConnection;
import com.trackit.bankaccountservice.model.ConnectionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class BankAccountRepositoryTest {

    @Autowired
    private BankAccountRepository bankAccountRepository;

    @Autowired
    private BankConnectionRepository bankConnectionRepository;

    private UUID userId;
    private UUID otherUserId;
    private BankConnection connection;
    private BankAccount activeAccount;
    private BankAccount inactiveAccount;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        otherUserId = UUID.randomUUID();

        connection = new BankConnection();
        connection.setUserId(userId);
        connection.setInstitutionId("MONZO_GB");
        connection.setInstitutionName("Monzo");
        connection.setRequisitionId("req-monzo-001");
        connection.setStatus(ConnectionStatus.ACTIVE);
        bankConnectionRepository.save(connection);

        activeAccount = new BankAccount();
        activeAccount.setConnection(connection);
        activeAccount.setUserId(userId);
        activeAccount.setExternalAccountId("ext-001");
        activeAccount.setName("Current Account");
        activeAccount.setCurrency("PLN");
        activeAccount.setActive(true);
        bankAccountRepository.save(activeAccount);

        // Inactive account - same user, deactivated
        inactiveAccount = new BankAccount();
        inactiveAccount.setConnection(connection);
        inactiveAccount.setUserId(userId);
        inactiveAccount.setExternalAccountId("ext-002");
        inactiveAccount.setName("Savings Account");
        inactiveAccount.setCurrency("PLN");
        inactiveAccount.setActive(false);
        bankAccountRepository.save(inactiveAccount);

        // Account for otherUserId - should never appear in userId queries
        BankConnection otherConnection = new BankConnection();
        otherConnection.setUserId(otherUserId);
        otherConnection.setInstitutionId("ING_PL");
        otherConnection.setInstitutionName("ING Bank");
        otherConnection.setRequisitionId("req-ing-001");
        otherConnection.setStatus(ConnectionStatus.ACTIVE);
        bankConnectionRepository.save(otherConnection);

        BankAccount otherUserAccount = new BankAccount();
        otherUserAccount.setConnection(otherConnection);
        otherUserAccount.setUserId(otherUserId);
        otherUserAccount.setExternalAccountId("ext-other");
        otherUserAccount.setName("Other Account");
        otherUserAccount.setCurrency("EUR");
        otherUserAccount.setActive(true);
        bankAccountRepository.save(otherUserAccount);
    }

    // findByUserIdAndActiveTrue

    @Test
    public void findByUserIdAndActiveTrue_returnsOnlyActiveAccounts() {
        List<BankAccount> result = bankAccountRepository.findByUserIdAndActiveTrue(userId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getExternalAccountId()).isEqualTo("ext-001");
        assertThat(result.get(0).isActive()).isTrue();
    }

    @Test
    public void findByUserIdAndActiveTrue_doesNotReturnOtherUsersAccounts() {
        List<BankAccount> result = bankAccountRepository.findByUserIdAndActiveTrue(userId);

        assertThat(result).noneMatch(a -> a.getUserId().equals(otherUserId));
    }

    @Test
    public void findByUserIdAndActiveTrue_returnsEmpty_whenAllAccountsDeactivated() {
        activeAccount.setActive(false);
        bankAccountRepository.save(activeAccount);

        List<BankAccount> result = bankAccountRepository.findByUserIdAndActiveTrue(userId);

        assertThat(result).isEmpty();
    }

    // findByConnectionId

    @Test
    public void findByConnectionId_returnsAllAccountsForConnection() {
        List<BankAccount> result = bankAccountRepository.findByConnectionId(connection.getId());

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(a -> a.getConnection().getId().equals(connection.getId()));
    }

    @Test
    public void findByConnectionId_returnsEmpty_forDifferentConnection() {
        List<BankAccount> result = bankAccountRepository.findByConnectionId(UUID.randomUUID());

        assertThat(result).isEmpty();
    }

    // findByIdAndUserId

    @Test
    public void findByIdAndUserId_returnsAccount_whenOwnershipMatches() {
        Optional<BankAccount> result = bankAccountRepository
                .findByIdAndUserId(activeAccount.getId(), userId);

        assertThat(result).isPresent();
        assertThat(result.get().getExternalAccountId()).isEqualTo("ext-001");
    }

    @Test
    public void findByIdAndUserId_returnsEmpty_whenWrongUserId() {
        Optional<BankAccount> result = bankAccountRepository
                .findByIdAndUserId(activeAccount.getId(), otherUserId);

        assertThat(result).isEmpty();
    }

    @Test
    public void findByIdAndUserId_returnsEmpty_whenAccountDoesNotExist() {
        Optional<BankAccount> result = bankAccountRepository
                .findByIdAndUserId(UUID.randomUUID(), userId);

        assertThat(result).isEmpty();
    }

    // findByConnectionIdAndExternalAccountId

    @Test
    public void findByConnectionIdAndExternalAccountId_returnsAccount_forDeduplication() {
        Optional<BankAccount> result = bankAccountRepository
                .findByConnectionIdAndExternalAccountId(connection.getId(), "ext-001");

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(activeAccount.getId());
    }

    @Test
    public void findByConnectionIdAndExternalAccountId_returnsEmpty_whenNotFound() {
        Optional<BankAccount> result = bankAccountRepository
                .findByConnectionIdAndExternalAccountId(connection.getId(), "ext-999");

        assertThat(result).isEmpty();
    }

    // persistence

    @Test
    public void save_persistsBalance() {
        activeAccount.setCurrentBalance(new BigDecimal("1500.00"));
        activeAccount.setAvailableBalance(new BigDecimal("1200.00"));
        bankAccountRepository.save(activeAccount);

        BankAccount found = bankAccountRepository.findById(activeAccount.getId()).orElseThrow();
        assertThat(found.getCurrentBalance()).isEqualByComparingTo("1500.00");
        assertThat(found.getAvailableBalance()).isEqualByComparingTo("1200.00");
    }
}