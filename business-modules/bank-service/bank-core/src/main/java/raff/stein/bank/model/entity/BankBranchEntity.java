package raff.stein.bank.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import raff.stein.platformcore.model.audit.entity.BaseDateEntity;

import java.util.UUID;

/**
 * Entity representing a bank branch in a wealth management platform.
 * This model is designed to be internationally compatible, supporting all global banking standards.
 */
@Entity
@Table(name = "bank_branches", uniqueConstraints = {
        @UniqueConstraint(name = "uk_bank_branch_code", columnNames = {"bankCode", "branchCode"})})
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankBranchEntity extends BaseDateEntity<UUID> {

    /**
     * Unique identifier for the bank branch (UUID).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;


    /**
     * Bank code (internal or international identifier).
     */
    @Column(nullable = false)
    private String bankCode;

    /**
     * Branch code (internal or international identifier).
     */
    @Column(nullable = false)
    private String branchCode;

    /**
     * Full name of the bank (e.g., "Intesa Sanpaolo S.p.A.", "Deutsche Bank AG").
     * This is the legal name of the bank.
     */
    @Column(nullable = false)
    private String bankName;

    /**
     * Name of the bank (e.g., "Intesa Sanpaolo", "Deutsche Bank").
     */
    @Column(nullable = false)
    private String branchName;

    /**
     * SWIFT/BIC code for international identification.
     */
    @Column(nullable = false)
    private String swiftCode;

    /**
     * ISO country code (e.g., "IT", "US", "DE").
     */
    @Column(nullable = false, length = 2)
    private String countryCode;

    /**
     * Type of bank (e.g., retail, private, corporate).
     */
    @Column(nullable = false)
    private String bankType;

    /**
     * Operational status of the branch (e.g., active, closed, under_renovation).
     */
    @Column(nullable = false)
    private String status;

    /**
     * Full description of the bank (e.g., legal name).
     */
    @Column
    private String bankDescription;

    /**
     * Description of the branch (e.g., branch name or details).
     */
    @Column
    private String branchDescription;

    /**
     * City where the branch is located.
     */
    @Column(nullable = false)
    private String branchCity;

    /**
     * Full address of the branch.
     */
    @Column(nullable = false)
    private String address;

    /**
     * ZIP or postal code of the branch location.
     */
    @Column(nullable = false)
    private String zipCode;

    /**
     * Phone number for the branch.
     */
    @Column
    private String phoneNumber;

    /**
     * Email contact for the branch.
     */
    @Column
    private String email;



}
