package com.trackit.investmentservice.service;

import com.trackit.investmentservice.repository.InvestmentAccountRepository;
import org.springframework.stereotype.Service;

@Service
public class InvestmentAccountService {
    private final InvestmentAccountRepository investmentAccountRepository;

    public InvestmentAccountService(InvestmentAccountRepository investmentAccountRepository) {
        this.investmentAccountRepository = investmentAccountRepository;
    }
}
