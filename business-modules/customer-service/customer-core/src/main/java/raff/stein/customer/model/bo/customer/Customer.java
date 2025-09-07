package raff.stein.customer.model.bo.customer;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class Customer {

    private UUID id;
    private UUID userId;
    // anagraphic data
    private String customerType;
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private String gender;
    private String nationality;
    private String companyName;
    private String taxId;
    private String phoneNumber;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String zipCode;
    private String country;
    private String customerStatus;
    private LocalDate onboardingDate;

    // related objects
    private List<CustomerFinancials> customerFinancials;
    private List<CustomerGoals> customerGoals;

}
