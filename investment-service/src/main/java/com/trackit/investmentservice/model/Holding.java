package com.trackit.investmentservice.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.cglib.core.Local;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "holdings",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_account_instrument",
                        columnNames = {"account_id", "instrument_id"}
                )
        }
)
public class Holding {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private InvestmentAccount account;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instrument_id", nullable = false)
    private Instrument instrument;

    //Current quantity held - updated after every import or transaction cancel
    @NotNull
    @Column(name = "quantity", nullable = false, precision = 15, scale = 6)
    private BigDecimal quantity;

    //Weighted average purchase price across all BUY transactions
    @NotNull
    @Column(name = "avg_purchase_price", nullable = false, precision = 15, scale = 4)
    private BigDecimal avgPurchasePrice;

    //Total amount spent buying this instrument in this account
    @NotNull
    @Column(name = "total_invested", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalInvested;

    //Latest price - updated by PriceWorker Lambda on a schedule
    //Nullanle until first price fetch
    @Column(name = "current_price", precision = 15, scale = 4)
    private BigDecimal currentPrice;

    //quantity x current_price - recalculated when price updates
    @Column(name = "current_value", precision = 15, scale = 2)
    private BigDecimal currentValue;

    //current_value - total_invested
    @Column(name = "unrealized_pnl", precision = 15, scale = 2)
    private BigDecimal unrealizedPnL;

    //unrealized_pnl / total_invested x 100
    @Column(name = "unrealized_pnl_pct", precision = 8, scale = 4)
    private BigDecimal unrealizedPnLPct;

    @NotNull
    @Size(min = 3, max = 3)
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    //When current_price was last updated by PriceWorker
    @Column(name = "last_price_update")
    private LocalDateTime lastPriceUpdate;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
