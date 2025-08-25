package raff.stein.customer.model.bo.mifid.config;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MifidAnswerOption {

    private Long id;
    private String optionText;
    private Integer score;
    private Integer orderIndex;
}

