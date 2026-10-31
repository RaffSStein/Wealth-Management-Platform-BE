package raff.stein.user.model.user;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class UserSummaryPage {

    private List<UserSummary> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
