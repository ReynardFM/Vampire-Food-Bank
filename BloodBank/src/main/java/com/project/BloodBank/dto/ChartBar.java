package com.project.BloodBank.dto;

/**
 * One row of a dashboard bar chart, split into what was already there and what arrived today.
 *
 * The widths are worked out here rather than in the template: the two segments sit side by side
 * inside the track, so they have to be clamped together to stay within 100%.
 *
 * A plain class with getters rather than a record, because Thymeleaf's ${bar.label} property
 * syntax resolves getLabel() and does not pick up a record's accessor.
 */
public class ChartBar {

    /** Roughly how wide one unit is, matching the original chart scale. */
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

    public static ChartBar of(String label, String key, long total, long today) {
        long cappedToday = Math.min(Math.max(today, 0), Math.max(total, 0));
        long earlier = Math.max(total - cappedToday, 0);

        int todayPercent = (int) Math.min(cappedToday * PERCENT_PER_UNIT, 100);
        int earlierPercent = (int) Math.min(earlier * PERCENT_PER_UNIT, 100 - todayPercent);

        return new ChartBar(label, key, total, cappedToday, earlierPercent, todayPercent);
    }

    public String getLabel() {
        return label;
    }

    /** Lower-case suffix used to pick the bar colour, e.g. "pending". */
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
