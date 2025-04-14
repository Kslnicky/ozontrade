package me.hikaricp.yellowexchange.utils;

import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class PaginationUtil {

    public List<Integer> paginate(int pages, int page, int maxButtons) {
        maxButtons = Math.max(1, maxButtons);

        int halfMaxButtons = maxButtons / 2;

        int startPage;
        int endPage;
        if (pages <= maxButtons) {
            startPage = 1;
            endPage = pages;
        } else if (page <= halfMaxButtons) {
            startPage = 1;
            endPage = maxButtons;
        } else if (page >= pages - halfMaxButtons) {
            startPage = pages - maxButtons + 1;
            endPage = pages;
        } else {
            startPage = page - halfMaxButtons;
            endPage = page + halfMaxButtons;
        }

        List<Integer> page_range = new ArrayList<>();
        for (int i = startPage; i <= endPage; i++) {
            page_range.add(i);
        }

        return page_range;
    }
}
