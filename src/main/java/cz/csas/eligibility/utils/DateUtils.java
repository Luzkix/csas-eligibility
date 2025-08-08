package cz.csas.eligibility.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;

public class DateUtils {

    /**
     * Function checks whether the client is adult or not.
     * @param birthDate is a string in format "yyyy-MM-dd" (e.g. 1954-07-04) with birth date of the client
     * @return boolean; true = is adult, false = is not adult
     */
    public static boolean isAdult (String birthDate) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate dateOfBirth = LocalDate.parse(birthDate, formatter);
        LocalDate today = LocalDate.now();
        int age = Period.between(dateOfBirth, today).getYears();
        return age >= 18;
    }

    /**
     * Converts LocalDateTime into OffsetDateTime.
     * @param localDateTime is date time in LocalDateTime format
     * @return OffsetDateTime; converted LocalDateTime into OffsetDateTime
     */
    public static OffsetDateTime convertToSystemOffsetDateTime(LocalDateTime localDateTime) {
        return localDateTime == null ? null :localDateTime.atOffset(OffsetDateTime.now().getOffset());
    }
}
