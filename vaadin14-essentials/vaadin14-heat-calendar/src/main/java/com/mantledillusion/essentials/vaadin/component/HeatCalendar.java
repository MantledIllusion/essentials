package com.mantledillusion.essentials.vaadin.component;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.shared.Registration;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.Temporal;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Component for displaying event occurrences on a week/day-based calendar.
 *
 * @param <EventType> The type of event the calendar displays.
 */
public class HeatCalendar<EventType> extends Composite<Component> implements HasSize {

    private static final class Range<N> {

        private N min;
        private N max;

        private final Comparator<N> comparator;

        @SafeVarargs
        private Range(Comparator<N> comparator, N... initialValues) {
            this.comparator = comparator;
            Arrays.stream(initialValues).forEach(this::digest);
        }

        private void digest(N value) {
            min = min == null ? value : (comparator.compare(min, value) > 0 ? value : min);
            max = max == null ? value : (comparator.compare(value, max) > 0 ? value : max);
        }
    }

    /**
     * The modes of how the calendar can display its weeks.
     */
    public enum DisplayMode {

        /**
         * Only show such weeks between the earliest and latest event given
         */
        DYNAMIC,

        /**
         * Statically display such weeks between {@link #getRangeFrom()} and {@link #getRangeUntil()}
         */
        FIXED,

        /**
         * Hybrid of {@link #DYNAMIC} and {@link #FIXED}; at least show the static range, but extend to earliest/latest event given
         */
        EXTENDED
    }

    /**
     * A specific week in a year.
     */
    public static final class Week implements Comparable<Week> {

        private final int week;
        private final int year;

        private Week(int week, int year) {
            this.week = week;
            this.year = year;
        }

        /**
         * Returns the week
         *
         * @return The week
         */
        public int getWeek() {
            return week;
        }

        /**
         * Returns the year as defined by {@link WeekFields#weekBasedYear()}.
         *
         * @return The year
         */
        public int getYear() {
            return year;
        }

        @Override
        public int compareTo(Week o) {
            return Optional
                    .of(Integer.compare(year, o.year))
                    .filter(val -> val != 0)
                    .orElse(Integer.compare(week, o.week));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof HeatCalendar.Week)) return false;
            Week week = (Week) o;
            return this.week == week.week && year == week.year;
        }

        @Override
        public int hashCode() {
            return Objects.hash(week, year);
        }

        /**
         * Factory method for {@link Week}s.
         *
         * @param week A week as defined by {@link WeekFields#weekOfWeekBasedYear()}.
         * @param year A year as defined by {@link WeekFields#weekBasedYear()}.
         * @return A new {@link Week}, never null
         */
        public static Week of(int week, int year) {
            return new Week(week, year);
        }
    }

    /**
     * A specific day in a week.
     */
    public static final class Day {

        private final int day;

        private Day(int day) {
            this.day = day;
        }

        /**
         * Returns the year as defined by {@link WeekFields#dayOfWeek()} ()}.
         *
         * @return The day
         */
        public int getDay() {
            return day;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Day)) return false;
            Day day1 = (Day) o;
            return day == day1.day;
        }

        @Override
        public int hashCode() {
            return Objects.hash(day);
        }
    }

    /**
     * An event value sum at a specific date.
     */
    public static final class Bucket {

        private final LocalDate date;
        private final Double value;

        private Bucket(LocalDate date, Bucket currentValue, Double additionalValue) {
            this.date = date;
            this.value = Optional
                    .ofNullable(additionalValue)
                    .map(additional -> Optional.ofNullable(currentValue)
                            .flatMap(current -> Optional.ofNullable(current.getValue()))
                            .map(current -> current + additional)
                            .orElse(additional))
                    .orElse(null);
        }

        /**
         * Returns the date this {@link Bucket} contains an event value sum for.
         *
         * @return The date, never null
         */
        public LocalDate getDate() {
            return date;
        }

        /**
         * Returns this {@link Bucket}'s event value sum.
         *
         * @return The sum or null, if there was no event at this {@link Bucket}'s date
         */
        public Double getValue() {
            return value;
        }
    }

    /**
     * A listener for the selection of a specific range in the calendar.
     */
    public interface RangeSelectedListener {

        /**
         * Called when a specific range is selected in the calendar.
         *
         * @param beginInclusive The date the range begins at (inclusive); might <b>not</b> be null
         * @param endExclusive The date the range ends at (exclusive); might <b>not</b> be null
         */
        void rangeSelected(LocalDate beginInclusive, LocalDate endExclusive);
    }

    private final Function<EventType, Temporal> timestampExtractor;
    private final Function<EventType, Double> valueExtractor;
    private final WeekFields weekFields = WeekFields.of(getLocale());

    private final HorizontalLayout calendar;
    private final List<RangeSelectedListener> listeners = new ArrayList<>();

    private Map<Week, Map<Day, Bucket>> buckets;
    private Range<Week> bucketHistoryRange;
    private Range<Double> bucketValueRange;

    private Function<Week, String> weekLabelGenerator = week -> String.valueOf(week.getWeek());
    private Function<Day, String> dayLabelGenerator = day -> DayOfWeek.of(((day.day + 5) % 7) + 1).getDisplayName(TextStyle.SHORT, getLocale());
    private Function<Bucket, String> bucketLabelGenerator = bucket -> String.valueOf(bucket.getDate().getDayOfMonth());

    private String bucketWidth;
    private String bucketHeight;

    private DisplayMode mode = DisplayMode.DYNAMIC;
    private Range<Week> fixedRange;
    private boolean showWeekLabel = true;
    private boolean showDayLabel = true;
    private boolean shadeMonths = true;
    private boolean showWeekends = true;
    private boolean markToday = true;
    private boolean emptyBucketsClickable = true;

    /**
     * Standard constructor.
     * <p>
     * Each event is counted as value=1.0
     *
     * @param timestampExtractor A {@link Function} able to extract a {@link Temporal} from events; might <b>not</b> be null.
     */
    public HeatCalendar(Function<EventType, Temporal> timestampExtractor) {
        this(timestampExtractor, element -> 1.0);
    }

    /**
     * Advanced constructor.
     *
     * @param timestampExtractor A {@link Function} able to extract a {@link Temporal} from events; might <b>not</b> be null.
     * @param valueExtractor A {@link Function} able to extract a {@link Double} from events; might <b>not</b> be null.
     */
    public HeatCalendar(Function<EventType, Temporal> timestampExtractor, Function<EventType, Double> valueExtractor) {
        if (timestampExtractor == null) {
            throw new IllegalArgumentException("Cannot extract timestamps from events using a null extractor");
        }
        this.timestampExtractor = timestampExtractor;

        if (valueExtractor == null) {
            throw new IllegalArgumentException("Cannot extract values from events using a null extractor");
        }
        this.valueExtractor = valueExtractor;

        this.calendar = new HorizontalLayout();
        this.calendar.setMargin(false);
        this.calendar.setPadding(false);
        this.calendar.setSpacing(false);
        this.calendar.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        this.calendar.getThemeList().set("spacing-xs", true);

        setRange(1, 1);
        setEvents(null);
    }

    @Override
    protected Component initContent() {
        return this.calendar;
    }

    /**
     * Sets the event the calendar is displaying.
     *
     * @param events The events to display; might be null.
     */
    public void setEvents(List<EventType> events) {
        this.buckets = new HashMap<>();
        this.bucketHistoryRange = new Range<>(Week::compareTo);
        this.bucketValueRange = new Range<>(Double::compareTo);

        Optional.ofNullable(events)
                .orElseGet(Collections::emptyList)
                .forEach(this::digestValue);

        switch (this.mode) {
            case FIXED:
                this.bucketHistoryRange.min = this.fixedRange.min;
                this.bucketHistoryRange.max = this.fixedRange.max;
                break;
            case EXTENDED:
                this.bucketHistoryRange.digest(this.fixedRange.min);
                this.bucketHistoryRange.digest(this.fixedRange.max);
                break;
            case DYNAMIC:
                // IGNORE FIXED RANGE ALL TOGETHER
                break;
        }

        render();
    }

    private void render() {
        this.calendar.removeAll();

        if (this.showDayLabel) {
            this.calendar.add(buildLabelColumn());
        }
        IntStream.range(
                        Optional.ofNullable(this.bucketHistoryRange.min).map(Week::getYear).orElse(0),
                        Optional.ofNullable(this.bucketHistoryRange.max).map(Week::getYear).map(year -> year + 1).orElse(0)
                )
                .forEach(year -> streamYear(year)
                        .map(week -> buildWeekColumn(week, this.buckets.get(week)))
                        .forEach(this.calendar::add));
    }

    private void digestValue(EventType element) {
        Temporal timestamp = this.timestampExtractor.apply(element);

        int year = timestamp.get(this.weekFields.weekBasedYear());
        int week = timestamp.get(this.weekFields.weekOfWeekBasedYear());
        int day = timestamp.get(this.weekFields.dayOfWeek());

        double value = this.valueExtractor.apply(element);
        Bucket bucket = this.buckets
                .computeIfAbsent(new Week(week, year), column -> {
                    this.bucketHistoryRange.digest(column);
                    return new HashMap<>();
                })
                .compute(new Day(day), (currentDay, currentBucket) ->
                        new Bucket(LocalDate.of(year, timestamp.get(ChronoField.MONTH_OF_YEAR), timestamp.get(ChronoField.DAY_OF_MONTH)), currentBucket, value));

        this.bucketValueRange.digest(bucket.getValue());
    }

    private Stream<Week> streamYear(int year) {
        int min;
        if (year == this.bucketHistoryRange.min.year) {
            min = this.bucketHistoryRange.min.week;
        } else {
            min = 1;
        }

        int max;
        if (year == this.bucketHistoryRange.max.year) {
            max = this.bucketHistoryRange.max.week;
        } else {
            max = (int) this.weekFields.weekOfWeekBasedYear().rangeRefinedBy(LocalDate.of(year, 1, 1)).getMaximum();
        }

        return IntStream.range(min, max+1)
                .mapToObj(week -> new Week(week, year));
    }

    private Component buildLabelColumn() {
        VerticalLayout column = new VerticalLayout();
        column.setMargin(false);
        column.setPadding(false);
        column.setSpacing(false);
        column.getThemeList().set("spacing-xs", true);

        if (this.showWeekLabel) {
            column.add(buildBucket(
                    null,
                    null,
                    null,
                    null,
                    null,
                    true,
                    false,
                    false
            ));
        }

        IntStream.range(this.showWeekends ? 1 : 2, this.showWeekends ? 8 : 7)
                .mapToObj(day -> buildBucket(
                        null,
                        null,
                        this.dayLabelGenerator.apply(new Day(day)),
                        null,
                        null,
                        true,
                        false,
                        false
                ))
                .forEach(column::add);

        return column;
    }

    private Component buildWeekColumn(Week week, Map<Day, Bucket> days) {
        VerticalLayout column = new VerticalLayout();
        column.setMargin(false);
        column.setPadding(false);
        column.setSpacing(false);
        column.getThemeList().set("spacing-xs", true);

        LocalDate weekStart = LocalDate.now()
                .with(this.weekFields.weekBasedYear(), week.year)
                .with(this.weekFields.weekOfWeekBasedYear(), week.week)
                .with(this.weekFields.dayOfWeek(), 1);

        column.add(buildBucket(
                null,
                null,
                this.weekLabelGenerator.apply(week),
                weekStart.plusDays(this.showWeekends ? 0 : 1),
                weekStart.plusDays(this.showWeekends ? 7 : 6),
                true,
                false,
                false
        ));

        IntStream.range(this.showWeekends ? 1 : 2, this.showWeekends ? 8 : 7)
                .mapToObj(day -> Optional
                        .ofNullable(days)
                        .map(weekdays -> Optional
                                .ofNullable(weekdays.get(new Day(day)))
                                .orElseGet(() -> empty(week, day)))
                        .orElseGet(() -> empty(week, day)))
                .map(bucket -> buildBucket(
                        this.bucketValueRange,
                        bucket.getValue(),
                        this.bucketLabelGenerator.apply(bucket),
                        bucket.getDate(),
                        bucket.getDate().plusDays(1),
                        false,
                        this.shadeMonths && bucket.getDate().getMonthValue() % 2 == 0,
                        this.markToday && bucket.getDate().equals(LocalDate.now())
                ))
                .forEach(column::add);

        return column;
    }

    private Bucket empty(Week week, int day) {
        return new Bucket(
                LocalDate.now()
                        .with(this.weekFields.weekBasedYear(), week.year)
                        .with(this.weekFields.weekOfWeekBasedYear(), week.week)
                        .with(this.weekFields.dayOfWeek(), day),
                null,
                null);
    }

    private Component buildBucket(Range<Double> valueRange, Double value, String label, LocalDate rangeBegin, LocalDate rangeEnd, boolean dimension, boolean shade, boolean mark) {
        Button bucket = new Button();
        bucket.setWidth(this.bucketWidth);
        bucket.setHeight(this.bucketHeight);
        bucket.setText(label);
        bucket.addThemeVariants(ButtonVariant.LUMO_SMALL);
        bucket.setThemeName("spacing-xs", true);

        if (mark) {
            bucket.getStyle().set("border", "solid 2px");
            bucket.getStyle().set("border-color", "rgba(64,64,64,0.3)");
        }

        if (shade) {
            bucket.getStyle().set("background-image", "radial-gradient(rgba(128,128,128,0.1) 2px, transparent 0), radial-gradient(rgba(128,128,128,0.1) 2px, transparent 0)");
            bucket.getStyle().set("background-position", "0 0, 10px 10px");
            bucket.getStyle().set("background-size", "20px 20px");
        }

        if (value != null) {
            double ratio = (value-valueRange.min) / (valueRange.max-valueRange.min);
            int r = (int) (Math.round(ratio * 256.0));
            int b = (int) (256.0 - ratio * 256.0);
            bucket.getStyle().set("background-color", "rgba("+r+",0,"+b+",0.1)");
        } else if (dimension) {
            bucket.getStyle().set("background-color", "rgba(128,128,128,0.05)");
        } else {
            bucket.getStyle().set("background-color", "rgba(128,128,128,0.02)");
        }

        if ((dimension || value != null || this.emptyBucketsClickable) && rangeBegin != null && rangeEnd != null) {
            bucket.addClickListener(event -> this.listeners.forEach(listener -> listener.rangeSelected(rangeBegin, rangeEnd)));
        } else {
            bucket.setEnabled(false);
        }

        return bucket;
    }

    /**
     * Sets the width for {@link Bucket}s.
     *
     * @see #setWidth(String)
     * @param width The width to set; might be null
     */
    public void setBucketWidth(String width) {
        this.bucketWidth = width;
        render();
    }

    /**
     * Sets full width for {@link Bucket}s.
     *
     * @see #setWidthFull()
     */
    public void setBucketWidthFull() {
        setBucketWidth("100%");
    }

    /**
     * Sets the height for {@link Bucket}s.
     *
     * @see #setHeight(String)
     * @param height The height to set; might be null
     */
    public void setBucketHeight(String height) {
        this.bucketHeight = height;
        render();
    }

    /**
     * Sets full height for {@link Bucket}s.
     *
     * @see #setHeightFull()
     */
    public void setBucketHeightFull() {
        setBucketHeight("100%");
    }

    /**
     * Adds a listener for when a range is selected in the calendar.
     *
     * @param listener The {@link RangeSelectedListener} to add; might <b>not</b> be null
     * @return A {@link Registration} to remove the {@link RangeSelectedListener} with, never null
     */
    public Registration addListener(RangeSelectedListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("Cannot add null as a listener");
        }
        this.listeners.add(listener);
        return () -> this.listeners.remove(listener);
    }

    /**
     * Removes a specific {@link RangeSelectedListener}.
     *
     * @param listener The listener to remove; might <b>not</b> be null
     * @return True if the listener was found and removed, false otherwise
     */
    public boolean removeListener(RangeSelectedListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("Cannot remove a null listener");
        }
        return this.listeners.remove(listener);
    }

    /**
     * Removes all {@link RangeSelectedListener}s.
     */
    public void removeListeners() {
        this.listeners.clear();
    }

    /**
     * Returns the current {@link DisplayMode}.
     *
     * @return The mode, never null
     */
    public DisplayMode getMode() {
        return mode;
    }

    /**
     * Sets the {@link DisplayMode}.
     * <p>
     * {@link DisplayMode#DYNAMIC} by default.
     *
     * @param mode The mode to set; might <b>not</b> be null
     */
    public void setMode(DisplayMode mode) {
        if (mode == null) {
            throw new IllegalArgumentException("Cannot set null mode");
        }
        this.mode = mode;
        render();
    }

    /**
     * Returns begin of the currently set {@link DisplayMode#FIXED} range.
     *
     * @return The begin of the range, never null
     */
    public LocalDate getRangeFrom() {
        return LocalDate.now()
                .with(this.weekFields.weekBasedYear(), this.fixedRange.min.year)
                .with(this.weekFields.weekOfWeekBasedYear(), this.fixedRange.min.week)
                .with(this.weekFields.dayOfWeek(), 1);
    }

    /**
     * Returns end of the currently set {@link DisplayMode#FIXED} range.
     *
     * @return The end of the range, never null
     */
    public LocalDate getRangeUntil() {
        return LocalDate.now()
                .with(this.weekFields.weekBasedYear(), this.fixedRange.max.year)
                .with(this.weekFields.weekOfWeekBasedYear(), this.fixedRange.max.week)
                .with(this.weekFields.dayOfWeek(), 7);
    }

    /**
     * Sets the {@link DisplayMode#FIXED} range the calendar is supposed to display.
     * <p>
     * Only has any effect if {@link #setMode(DisplayMode)} was set to {@link DisplayMode#FIXED}.
     * <p>
     * 1/1 by default.
     *
     * @param minusWeeks The amount of weeks in the past to display, in relation to the current week.
     * @param plusWeeks The amount of weeks in the future to display, in relation to the current week.
     */
    public void setRange(int minusWeeks, int plusWeeks) {
        setRange(LocalDate.now().minusWeeks(minusWeeks), LocalDate.now().plusWeeks(plusWeeks));
    }

    /**
     * Sets the {@link DisplayMode#FIXED} range the calendar is supposed to display.
     * <p>
     * Only has any effect if {@link #setMode(DisplayMode)} was set to {@link DisplayMode#FIXED}.
     * <p>
     * {@link LocalDate#minusWeeks(long)} / {@link LocalDate#plusWeeks(long)} with {@link LocalDate#now()} and 1/1 by default.
     *
     * @param from The date whose week marks the beginning of the range to display; might <b>not</b> be null.
     * @param until The date whose week marks the end of the range to display; might <b>not</b> be null.
     */
    public void setRange(LocalDate from, LocalDate until) {
        if (from == null) {
            throw new IllegalArgumentException("Cannot set null as range from");
        } else if (until == null) {
            throw new IllegalArgumentException("Cannot set null as range until");
        } else if (!from.isBefore(until)) {
            throw new IllegalArgumentException("Can only set range from to a date before until");
        }
        setRange(new Week(from.get(this.weekFields.weekOfWeekBasedYear()), from.get(this.weekFields.weekBasedYear())),
                new Week(until.get(this.weekFields.weekOfWeekBasedYear()), until.get(this.weekFields.weekBasedYear())));
    }

    /**
     * Sets the {@link DisplayMode#FIXED} range the calendar is supposed to display.
     * <p>
     * Only has any effect if {@link #setMode(DisplayMode)} was set to {@link DisplayMode#FIXED}.
     * <p>
     * {@link Week} with current year and -1/+1 week by default.
     *
     * @param from The week marking the beginning of the range to display; might <b>not</b> be null.
     * @param until The week marking the end of the range to display; might <b>not</b> be null.
     */
    public void setRange(Week from, Week until) {
        if (from == null) {
            throw new IllegalArgumentException("Cannot set null as range from");
        } else if (until == null) {
            throw new IllegalArgumentException("Cannot set null as range until");
        } else if (from.compareTo(until) > 0) {
            throw new IllegalArgumentException("Can only set range from to a date before until");
        }
        this.fixedRange = new Range<>(Week::compareTo, from, until);

        if (this.mode != DisplayMode.DYNAMIC) {
            render();
        }
    }

    /**
     * Sets a {@link Function} to render the calendar's week labels (x-axis) with.
     * <p>
     * Renders {@link Week#getWeek()} by default.
     *
     * @param weekLabelGenerator The generator; might <b>not</b> be null.
     */
    public void setWeekLabelGenerator(Function<Week, String> weekLabelGenerator) {
        if (weekLabelGenerator == null) {
            throw new IllegalArgumentException("Cannot add a null generator");
        }
        this.weekLabelGenerator = weekLabelGenerator;
        render();
    }

    /**
     * Sets a {@link Function} to render the calendar's day labels (y-axis) with.
     * <p>
     * Renders {@link Day#getDay()} by default.
     *
     * @param dayLabelGenerator The generator; might <b>not</b> be null.
     */
    public void setDayLabelGenerator(Function<Day, String> dayLabelGenerator) {
        if (dayLabelGenerator == null) {
            throw new IllegalArgumentException("Cannot add a null generator");
        }
        this.dayLabelGenerator = dayLabelGenerator;
        render();
    }

    /**
     * Sets a {@link Function} to render the calendar's bucket labels with.
     * <p>
     * Renders {@link Bucket#getDate()} with {@link LocalDate#getDayOfMonth()} by default.
     *
     * @param bucketLabelGenerator The generator; might <b>not</b> be null.
     */
    public void setBucketLabelGenerator(Function<Bucket, String> bucketLabelGenerator) {
        if (bucketLabelGenerator == null) {
            throw new IllegalArgumentException("Cannot add a null generator");
        }
        this.bucketLabelGenerator = bucketLabelGenerator;
        render();
    }

    /**
     * Determines whether the week label (x-axis) is shown.
     *
     * @return True if week labels are shown, false otherwise
     */
    public boolean isShowWeekLabel() {
        return showWeekLabel;
    }

    /**
     * Sets whether the week label (x-axis) should be shown.
     *
     * @param showWeekLabel True if week labels should be shown, false otherweise
     */
    public void setShowWeekLabel(boolean showWeekLabel) {
        this.showWeekLabel = showWeekLabel;
        render();
    }

    /**
     * Determines whether the day label (y-axis) is shown.
     *
     * @return True if day labels are shown, false otherwise
     */
    public boolean isShowDayLabel() {
        return showDayLabel;
    }

    /**
     * Sets whether the day label (y-axis) should be shown.
     *
     * @param showDayLabel True if day labels should be shown, false otherweise
     */
    public void setShowDayLabel(boolean showDayLabel) {
        this.showDayLabel = showDayLabel;
        render();
    }

    /**
     * Determines whether odd-number months should be shaded with a dot-pattern for better visibility.
     *
     * @return True if months are shaded, false otherwse
     */
    public boolean isShadeMonths() {
        return shadeMonths;
    }

    /**
     * Sets whether odd-number months should be shaded with a dot-pattern for better visibility.
     *
     * @param shadeMonths True if months should be shaded, false otherwse
     */
    public void setShadeMonths(boolean shadeMonths) {
        this.shadeMonths = shadeMonths;
        render();
    }

    /**
     * Determines whether the rows of {@link DayOfWeek#SATURDAY} and {@link DayOfWeek#SUNDAY} should be shown.
     *
     * @return True if these weekdays are shown, false otherwise
     */
    public boolean isShowWeekends() {
        return showWeekends;
    }

    /**
     * Sets whether the rows of {@link DayOfWeek#SATURDAY} and {@link DayOfWeek#SUNDAY} should be shown.
     *
     * @param showWeekends True if these weekdays are shown, false otherwise
     */
    public void setShowWeekends(boolean showWeekends) {
        this.showWeekends = showWeekends;
        render();
    }

    /**
     * Determines whether the {@link Bucket} of {@link LocalDate#now()} is marked with a border.
     *
     * @return True if today is marked, false otherwise
     */
    public boolean isMarkToday() {
        return markToday;
    }

    /**
     * Sets whether the {@link Bucket} of {@link LocalDate#now()} is marked with a border.
     *
     * @param markToday True if today should be marked, false otherwise
     */
    public void setMarkToday(boolean markToday) {
        this.markToday = markToday;
        render();
    }

    /**
     * Determines whether {@link Bucket} without events should be selectable as range.
     *
     * @return True if such {@link Bucket}s are selectable, false otherwise
     */
    public boolean isEmptyBucketsClickable() {
        return emptyBucketsClickable;
    }

    /**
     * Sets whether {@link Bucket} without events should be selectable as range.
     *
     * @param emptyBucketsClickable True if such {@link Bucket}s should be selectable, false otherwise
     */
    public void setEmptyBucketsClickable(boolean emptyBucketsClickable) {
        this.emptyBucketsClickable = emptyBucketsClickable;
        render();
    }
}
