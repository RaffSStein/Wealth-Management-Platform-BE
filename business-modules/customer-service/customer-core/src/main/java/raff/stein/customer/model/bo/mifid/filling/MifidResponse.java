package raff.stein.customer.model.bo.mifid.filling;

import lombok.Builder;
import lombok.Data;
import raff.stein.customer.model.bo.mifid.config.MifidAnswerOption;
import raff.stein.customer.model.bo.mifid.config.MifidQuestion;

import java.time.LocalDate;

@Data
@Builder
public class MifidResponse {

    private Long id;
    private MifidQuestion question;
    private MifidAnswerOption answerOption;
    private String freeText;
    private Double numericValue;
    private LocalDate dateValue;

}

