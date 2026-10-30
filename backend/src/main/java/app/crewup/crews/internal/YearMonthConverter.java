package app.crewup.crews.internal;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.LocalDate;
import java.time.YearMonth;

/** Stores a month as the date of its first day, so the database can sort and check it (plan §3). */
@Converter(autoApply = true)
class YearMonthConverter implements AttributeConverter<YearMonth, LocalDate> {

    @Override
    public LocalDate convertToDatabaseColumn(YearMonth month) {
        return month == null ? null : month.atDay(1);
    }

    @Override
    public YearMonth convertToEntityAttribute(LocalDate date) {
        return date == null ? null : YearMonth.from(date);
    }
}
