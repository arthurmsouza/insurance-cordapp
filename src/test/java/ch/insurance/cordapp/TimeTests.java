package ch.insurance.cordapp;

import org.junit.Assert;
import org.junit.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;

public class TimeTests {

    public LocalDate get1stDayOfNextMonth() {
        YearMonth yearMonth = YearMonth.from(Instant.now().atZone(ZoneId.systemDefault()));
        return yearMonth.atEndOfMonth().plus(1, ChronoUnit.DAYS);
    }
    public Instant get1stDayOfNextMonth_Instant() {
        return this.get1stDayOfNextMonth().atStartOfDay(ZoneId.systemDefault()).toInstant();
    }

    @Test
    public void test_nextMonth1stDay() {
        LocalDate dayOfNextMonth = this.get1stDayOfNextMonth();
        Assert.assertEquals("next month 1st day is day = 1",
                dayOfNextMonth.get(ChronoField.DAY_OF_MONTH), 1);
    }
    @Test
    public void test_nextMonth1stDay_Instant() {
        this.get1stDayOfNextMonth_Instant();
    }
}
