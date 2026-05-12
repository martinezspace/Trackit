package com.trackit.bankaccountservice.repository;

import com.trackit.bankaccountservice.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class TransactionRuleRepositoryTest {

    @Autowired
    private TransactionRuleRepository transactionRuleRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private UUID userId;
    private UUID otherUserId;
    private Category foodCategory;
    private TransactionRule highPriorityRule;
    private TransactionRule lowPriorityRule;
    private TransactionRule inactiveRule;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        otherUserId = UUID.randomUUID();

        foodCategory = new Category();
        foodCategory.setName("Food");
        foodCategory.setType(CategoryType.DEBIT);
        foodCategory.setSystem(true);
        foodCategory.setSortOrder(1);
        categoryRepository.save(foodCategory);

        highPriorityRule = new TransactionRule();
        highPriorityRule.setUserId(userId);
        highPriorityRule.setName("Lidl rule");
        highPriorityRule.setMatchField(RuleMatchField.MERCHANT);
        highPriorityRule.setMatchPattern("lidl");
        highPriorityRule.setCategory(foodCategory);
        highPriorityRule.setPriority(100);
        highPriorityRule.setActive(true);
        transactionRuleRepository.save(highPriorityRule);

        lowPriorityRule = new TransactionRule();
        lowPriorityRule.setUserId(userId);
        lowPriorityRule.setName("Biedronka rule");
        lowPriorityRule.setMatchField(RuleMatchField.MERCHANT);
        lowPriorityRule.setMatchPattern("biedronka");
        lowPriorityRule.setCategory(foodCategory);
        lowPriorityRule.setPriority(10);
        lowPriorityRule.setActive(true);
        transactionRuleRepository.save(lowPriorityRule);

        // Inactive rule - soft deleted, should not appear in active queries
        inactiveRule = new TransactionRule();
        inactiveRule.setUserId(userId);
        inactiveRule.setName("Old Netflix rule");
        inactiveRule.setMatchField(RuleMatchField.DESCRIPTION);
        inactiveRule.setMatchPattern("netflix");
        inactiveRule.setCategory(foodCategory);
        inactiveRule.setPriority(50);
        inactiveRule.setActive(false);
        transactionRuleRepository.save(inactiveRule);

        // Rule for otherUserId - should never appear in userId queries
        TransactionRule otherUserRule = new TransactionRule();
        otherUserRule.setUserId(otherUserId);
        otherUserRule.setName("Other user rule");
        otherUserRule.setMatchField(RuleMatchField.MERCHANT);
        otherUserRule.setMatchPattern("zabka");
        otherUserRule.setCategory(foodCategory);
        otherUserRule.setPriority(10);
        otherUserRule.setActive(true);
        transactionRuleRepository.save(otherUserRule);
    }

    // findByUserIdAndActiveTrueOrderByPriorityDesc

    @Test
    public void findByUserIdAndActiveTrueOrderByPriorityDesc_returnsOnlyActiveRules() {
        List<TransactionRule> result = transactionRuleRepository
                .findByUserIdAndActiveTrueOrderByPriorityDesc(userId);

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(TransactionRule::isActive);
        assertThat(result).noneMatch(r -> r.getName().equals("Old Netflix rule"));
    }

    @Test
    public void findByUserIdAndActiveTrueOrderByPriorityDesc_orderedByPriorityDesc() {
        List<TransactionRule> result = transactionRuleRepository
                .findByUserIdAndActiveTrueOrderByPriorityDesc(userId);

        assertThat(result.get(0).getPriority()).isEqualTo(100);
        assertThat(result.get(1).getPriority()).isEqualTo(10);
    }

    @Test
    public void findByUserIdAndActiveTrueOrderByPriorityDesc_doesNotReturnOtherUsersRules() {
        List<TransactionRule> result = transactionRuleRepository
                .findByUserIdAndActiveTrueOrderByPriorityDesc(userId);

        assertThat(result).noneMatch(r -> r.getUserId().equals(otherUserId));
    }

    @Test
    public void findByUserIdAndActiveTrueOrderByPriorityDesc_returnsEmpty_whenUserHasNoActiveRules() {
        List<TransactionRule> result = transactionRuleRepository
                .findByUserIdAndActiveTrueOrderByPriorityDesc(UUID.randomUUID());

        assertThat(result).isEmpty();
    }

    // findByIdAndUserId

    @Test
    public void findByIdAndUserId_returnsRule_whenOwnershipMatches() {
        Optional<TransactionRule> result = transactionRuleRepository
                .findByIdAndUserId(highPriorityRule.getId(), userId);

        assertThat(result).isPresent();
        assertThat(result.get().getMatchPattern()).isEqualTo("lidl");
    }

    @Test
    public void findByIdAndUserId_returnsEmpty_whenWrongUserId() {
        Optional<TransactionRule> result = transactionRuleRepository
                .findByIdAndUserId(highPriorityRule.getId(), otherUserId);

        assertThat(result).isEmpty();
    }

    @Test
    public void findByIdAndUserId_returnsEmpty_whenRuleDoesNotExist() {
        Optional<TransactionRule> result = transactionRuleRepository
                .findByIdAndUserId(UUID.randomUUID(), userId);

        assertThat(result).isEmpty();
    }

    // soft delete

    @Test
    public void softDelete_hidesRuleFromActiveQuery_butKeepsItInDatabase() {
        highPriorityRule.setActive(false);
        transactionRuleRepository.save(highPriorityRule);

        List<TransactionRule> activeRules = transactionRuleRepository
                .findByUserIdAndActiveTrueOrderByPriorityDesc(userId);
        assertThat(activeRules).hasSize(1);
        assertThat(activeRules).noneMatch(r -> r.getId().equals(highPriorityRule.getId()));

        // Rule still exists in the database - soft delete not hard delete
        assertThat(transactionRuleRepository.findById(highPriorityRule.getId())).isPresent();
    }
}