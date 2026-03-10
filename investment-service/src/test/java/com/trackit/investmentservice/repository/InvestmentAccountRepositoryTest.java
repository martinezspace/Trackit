package com.trackit.investmentservice.repository;

import com.trackit.investmentservice.model.AccountType;
import com.trackit.investmentservice.model.InvestmentAccount;
import jakarta.persistence.criteria.CriteriaBuilder;
import org.aspectj.lang.annotation.Before;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// spins up only the JPA layer with H2 - no full Spring context
// faster than @SpringBootTest, only load repository beans
@DataJpaTest
public class InvestmentAccountRepositoryTest {

    @Autowired
    private InvestmentAccountRepository investmentAccountRepository;

    private UUID userId;
    private UUID otherUserId;
    private InvestmentAccount activeAccount;
    private InvestmentAccount inactiveAccount;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        otherUserId = UUID.randomUUID();

        activeAccount = new InvestmentAccount();
        activeAccount.setUserId(userId);
        activeAccount.setAccountType(AccountType.IKE);
        activeAccount.setBrokerName("XTB");
        activeAccount.setCurrency("PLN");
        activeAccount.setActive(true);
        investmentAccountRepository.save(activeAccount);

        inactiveAccount = new InvestmentAccount();
        inactiveAccount.setUserId(userId);
        inactiveAccount.setAccountType(AccountType.BROKERAGE);
        inactiveAccount.setBrokerName("mBank");
        inactiveAccount.setCurrency("PLN");
        inactiveAccount.setActive(false);
        investmentAccountRepository.save(inactiveAccount);

        InvestmentAccount otherUserAccount = new InvestmentAccount();
        otherUserAccount.setUserId(otherUserId);
        otherUserAccount.setAccountType(AccountType.PPK);
        otherUserAccount.setBrokerName("NN TFI");
        otherUserAccount.setCurrency("PLN");
        otherUserAccount.setActive(true);
        investmentAccountRepository.save(otherUserAccount);
    }

    //findByUserId

    @Test
    void findByUserId_returnsOnlyAccountsForThatUser() {
        List<InvestmentAccount> result = investmentAccountRepository.findByUserId(userId);

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(a -> a.getUserId().equals(userId));
    }

    @Test
    void findByUserId_returnsEmpty_whenUserHasNoAccounts() {
        List<InvestmentAccount> result = investmentAccountRepository.findByUserId(UUID.randomUUID());

        assertThat(result).isEmpty();
    }

    //findByUserIdAndActiveTrue

    @Test
    void findByUserIdAndActiveTrue_returnsOnlyActiveAccounts() {
        List<InvestmentAccount> result = investmentAccountRepository.findByUserIdAndActiveTrue(userId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getBrokerName()).isEqualTo("XTB");
        assertThat(result.get(0).isActive()).isTrue();
    }

    //findByIdAndUserId

    @Test
    void findByIdAndUserId_returnsAccount_whenOwnershipMatches() {
        Optional<InvestmentAccount> result = investmentAccountRepository
                .findByIdAndUserId(activeAccount.getId(), userId);

        assertThat(result).isPresent();
        assertThat(result.get().getBrokerName()).isEqualTo("XTB");
    }

    @Test
    void findByIdAndUserId_returnsEmpty_whenWrongUserId() {
        Optional<InvestmentAccount> result = investmentAccountRepository
                .findByIdAndUserId(activeAccount.getId(), otherUserId);

        assertThat(result).isEmpty();
    }

    @Test
    void findByIdAndUserId_returnsEmpty_whenAccountDoesNotExists() {
        Optional<InvestmentAccount> result = investmentAccountRepository
                .findByIdAndUserId(UUID.randomUUID(), userId);

        assertThat(result).isEmpty();
    }
}
