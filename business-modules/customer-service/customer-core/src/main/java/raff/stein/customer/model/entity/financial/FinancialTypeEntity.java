package raff.stein.customer.model.entity.financial;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import raff.stein.customer.model.entity.financial.enumeration.FinancialTypeEnum;
import raff.stein.platformcore.model.audit.entity.BaseEntity;

@Entity
@Table(name = "financial_types")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinancialTypeEntity extends BaseEntity<Integer> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Name of the financial type (e.g. TRAVELS,MEDICAL_EXPENSES,UTILITIES,BASIC_NECESSITIES,INVESTMENT_COMMITMENT,
    // EDUCATION,TRANSPORT,SHOPPING,INVESTMENTS,PAC, ...)
    @Column(nullable = false, length = 100)
    private String name;

    // Type of financial type (e.g. INCOME,EXPENSE)
    @Column(nullable = false, length = 100)
    @Enumerated(EnumType.STRING)
    private FinancialTypeEnum type;

    @Column(length = 500)
    private String description;
}

