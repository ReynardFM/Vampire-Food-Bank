package com.project.BloodBank.dto;

// One row of a dashboard bar chart, split into what was already there and what arrived today.
//
// The widths are worked out here rather than in the template, because the two segments sit side by
// side inside one track and have to be clamped together to stay within 100%. Arithmetic like that
// is unreadable in Thymeleaf and cannot be tested there.
//
// A plain class with getters rather than a record: Thymeleaf's ${bar.label} looks for getLabel(),
// and a record generates label() instead.
public class ChartBar {

    // How wide one unit is, matching the original chart scale. Ten means a bar fills the track at
    // ten items, which suits a small blood bank; a larger one would want a real scale.
    private static final int PERCENT_PER_UNIT = 10;

    private final String label;
    private final String key;
    private final long total;
    private final long today;
    private final int earlierPercent;
    private final int todayPercent;

    private ChartBar(String label, String key, long total, long today,
                     int earlierPercent, int todayPercent) {
        this.label = label;
        this.key = key;
        this.total = total;
        this.today = today;
        this.earlierPercent = earlierPercent;
        this.todayPercent = todayPercent;
    }

    // Builds a bar, working out the two segment widths.
    //
    // The clamping is defensive rather than decorative. The totals and today's counts come from two
    // separate queries, so a request decided between them could report a "today" larger than the
    // total - which would render a segment wider than its own track.
    public static ChartBar of(String label, String key, long total, long today) {
        // today can never exceed total, and neither can be negative.
        long cappedToday = Math.min(Math.max(today, 0), Math.max(total, 0));
        long earlier = Math.max(total - cappedToday, 0);

        // Today's segment is measured first and keeps its width; the earlier segment then takes
        // whatever is left. That way the highlight stays visible even on a full bar.
        int todayPercent = (int) Math.min(cappedToday * PERCENT_PER_UNIT, 100);
        int earlierPercent = (int) Math.min(earlier * PERCENT_PER_UNIT, 100 - todayPercent);

        return new ChartBar(label, key, total, cappedToday, earlierPercent, todayPercent);
    }

    public String getLabel() {
        return label;
    }

    // Lower-case suffix the template appends to build a CSS class, e.g. "pending" becomes
    // bar-pending. That is how each status bar gets its own colour.
    public String getKey() {
        return key;
    }

    public long getTotal() {
        return total;
    }

    public long getToday() {
        return today;
    }

    public int getEarlierPercent() {
        return earlierPercent;
    }

    public int getTodayPercent() {
        return todayPercent;
    }
}
