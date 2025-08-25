package raff.stein.customer.model.entity.customer;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import raff.stein.customer.model.entity.financial.FinancialTypeEntity;
import raff.stein.platformcore.model.audit.entity.BaseDateEntity;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "customer_financials")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerFinancialEntity extends BaseDateEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relationships

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false, insertable = false, updatable = false, referencedColumnName = "id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonIgnore
    private CustomerEntity customer;

    @Column(name = "customer_id", nullable = false)
    @Setter
    private UUID customerId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "financial_type_id", nullable = false, insertable = false, updatable = false,  referencedColumnName = "id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonIgnore
    @Setter
    private FinancialTypeEntity financialType;

    @Column(name = "financial_type_id", nullable = false)
    @Setter
    private Integer financialTypeId;

    // Fields

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(length = 500)
    private String description;

    public void updateFrom(CustomerFinancialEntity source) {
        this.amount = source.getAmount();
        this.description = source.getDescription();
    }

}
