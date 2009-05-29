package net.sourceforge.fenixedu.domain.assiduousness;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sourceforge.fenixedu.dataTransferObject.assiduousness.YearMonth;
import net.sourceforge.fenixedu.domain.RootDomainObject;
import net.sourceforge.fenixedu.util.NumberUtils;

import org.joda.time.DateTimeFieldType;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.joda.time.Partial;

public class AssiduousnessClosedMonth extends AssiduousnessClosedMonth_Base {

    public AssiduousnessClosedMonth(AssiduousnessStatusHistory assiduousnessStatusHistory, ClosedMonth closedMonth,
	    Duration balance, Duration totalComplementaryWeeklyRestBalance, Duration totalWeeklyRestBalance,
	    Duration holidayRest, Duration balanceToDiscount, double vacations, double tolerance, double article17,
	    double article66, Integer maximumWorkingDays, Integer workedDaysWithBonusDaysDiscount,
	    Integer workedDaysWithA17VacationsDaysDiscount, Duration finalBalance, Duration finalBalanceToCompensate,
	    LocalDate beginDate, LocalDate endDate, Duration totalWorkedTime) {
	setRootDomainObject(RootDomainObject.getInstance());
	setBalance(balance);
	setBalanceToDiscount(balanceToDiscount);
	setAssiduousnessStatusHistory(assiduousnessStatusHistory);
	setClosedMonth(closedMonth);
	setSaturdayBalance(totalComplementaryWeeklyRestBalance);
	setSundayBalance(totalWeeklyRestBalance);
	setHolidayBalance(holidayRest);
	setVacations(vacations);
	setTolerance(tolerance);
	setArticle17(article17);
	setArticle66(article66);
	setAccumulatedArticle66(0.0);
	setAccumulatedUnjustified(0.0);
	setUnjustifiedDays(0);
	setMaximumWorkingDays(maximumWorkingDays);
	setWorkedDaysWithBonusDaysDiscount(workedDaysWithBonusDaysDiscount);
	setWorkedDaysWithA17VacationsDaysDiscount(workedDaysWithA17VacationsDaysDiscount);
	setFinalBalance(finalBalance);
	setFinalBalanceToCompensate(finalBalanceToCompensate);
	setBeginDate(beginDate);
	setEndDate(endDate);
	setTotalWorkedTime(totalWorkedTime);
    }

    public HashMap<String, Duration> getPastJustificationsDurations() {
	HashMap<String, Duration> pastJustificationsDurations = new HashMap<String, Duration>();
	for (AssiduousnessClosedMonth assiduousnessClosedMonth : getAssiduousnessStatusHistory().getAssiduousnessClosedMonths()) {
	    if (assiduousnessClosedMonth.getClosedMonth().getClosedYearMonth().get(DateTimeFieldType.year()) == getClosedMonth()
		    .getClosedYearMonth().get(DateTimeFieldType.year())
		    && assiduousnessClosedMonth.getClosedMonth().getClosedYearMonth().get(DateTimeFieldType.monthOfYear()) < getClosedMonth()
			    .getClosedYearMonth().get(DateTimeFieldType.monthOfYear())) {
		for (ClosedMonthJustification closedMonthJustification : assiduousnessClosedMonth.getClosedMonthJustifications()) {
		    if (closedMonthJustification.getJustificationMotive().getActive()) {
			String code = closedMonthJustification.getJustificationMotive().getGiafCode(
				assiduousnessClosedMonth.getAssiduousnessStatusHistory());
			Duration duration = pastJustificationsDurations.get(code);
			if (duration == null) {
			    duration = Duration.ZERO;
			}
			duration = duration.plus(closedMonthJustification.getJustificationDuration());
			pastJustificationsDurations.put(code, duration);
		    }
		}
	    }
	}
	return pastJustificationsDurations;
    }

    public HashMap<String, Duration> getClosedMonthJustificationsMap() {
	HashMap<String, Duration> closedMonthJustificationscodesMap = new HashMap<String, Duration>();
	for (ClosedMonthJustification closedMonthJustification : getClosedMonthJustifications()) {
	    String code = closedMonthJustification.getJustificationMotive().getGiafCode(getAssiduousnessStatusHistory());
	    Duration duration = closedMonthJustificationscodesMap.get(code);
	    if (duration == null) {
		duration = Duration.ZERO;
	    }
	    duration = duration.plus(closedMonthJustification.getJustificationDuration());
	    closedMonthJustificationscodesMap.put(code, duration);
	}
	return closedMonthJustificationscodesMap;
    }

    private double getTotalUnjustifiedPercentage(LocalDate beginDate, LocalDate endDate) {
	double unjustified = 0;
	Duration balanceWithoutDiscount = getBalance().plus(getBalanceToDiscount());
	Duration averageWorkTimeDuration = getAssiduousnessStatusHistory().getAssiduousness().getAverageWorkTimeDuration(
		beginDate, endDate);
	if (!balanceWithoutDiscount.isShorterThan(Duration.ZERO)) {
	    unjustified = getUnjustifiedPercentage(getAssiduousnessExtraWorks());
	} else {
	    Duration unjustifiedTotalDuration = Duration.ZERO;
	    for (AssiduousnessExtraWork extraWork : getAssiduousnessExtraWorks()) {
		unjustifiedTotalDuration = unjustifiedTotalDuration.plus(extraWork.getUnjustified());
	    }
	    long balanceToProcess = Math.abs(balanceWithoutDiscount.getMillis()) > unjustifiedTotalDuration.getMillis() ? Math
		    .abs(balanceWithoutDiscount.getMillis()) : unjustifiedTotalDuration.getMillis();
	    long balanceAfterTolerance = balanceToProcess - Assiduousness.IST_TOLERANCE_TIME.getMillis();
	    if (balanceAfterTolerance > 0) {
		unjustified = (double) balanceAfterTolerance / (double) averageWorkTimeDuration.getMillis();
	    }
	}

	if (beginDate.getMonthOfYear() == 12 && getFinalBalanceToCompensate().isLongerThan(Duration.ZERO)) {
	    unjustified = unjustified
		    + (getFinalBalanceToCompensate().getMillis() / (double) averageWorkTimeDuration.getMillis());
	}
	return unjustified;
    }

    private double getUnjustifiedPercentage(List<AssiduousnessExtraWork> assiduousnessExtraWorks) {
	double unjustified = 0;
	long tempIstTorelanceTime = Assiduousness.IST_TOLERANCE_TIME.getMillis();
	for (AssiduousnessExtraWork extraWork : assiduousnessExtraWorks) {
	    if (extraWork.getUnjustified().isLongerThan(Duration.ZERO)) {
		long unjustifiedAfterTolerance = extraWork.getUnjustified().getMillis() - tempIstTorelanceTime;
		if (unjustifiedAfterTolerance > 0) {
		    unjustified += ((double) unjustifiedAfterTolerance / (double) extraWork.getWorkScheduleType()
			    .getNormalWorkPeriod().getWorkPeriodDuration().getMillis());
		}
		tempIstTorelanceTime = unjustifiedAfterTolerance > 0 ? 0 : tempIstTorelanceTime
			- extraWork.getUnjustified().getMillis();
	    }
	}
	return unjustified;
    }

    public void delete() {
	removeRootDomainObject();
	removeAssiduousnessStatusHistory();
	List<AssiduousnessExtraWork> assiduousnessExtraWorks = new ArrayList<AssiduousnessExtraWork>(getAssiduousnessExtraWorks());
	for (AssiduousnessExtraWork assiduousnessExtraWork : assiduousnessExtraWorks) {
	    getAssiduousnessExtraWorks().remove(assiduousnessExtraWork);
	    assiduousnessExtraWork.delete();
	}
	List<ClosedMonthJustification> closedMonthJustifications = new ArrayList<ClosedMonthJustification>(
		getClosedMonthJustifications());
	for (ClosedMonthJustification closedMonthJustification : closedMonthJustifications) {
	    getClosedMonthJustifications().remove(closedMonthJustification);
	    closedMonthJustification.delete();
	}

	List<AssiduousnessClosedDay> assiduousnessClosedDays = new ArrayList<AssiduousnessClosedDay>(getAssiduousnessClosedDays());
	for (AssiduousnessClosedDay assiduousnessClosedDay : assiduousnessClosedDays) {
	    getAssiduousnessClosedDays().remove(assiduousnessClosedDay);
	    assiduousnessClosedDay.delete();
	}
	deleteDomainObject();
    }

    public void setAllUnjustifiedAndAccumulatedArticle66() {
	AssiduousnessClosedMonth lastAssiduousnessClosedMonth = getPreviousAssiduousnessClosedMonth();
	double unjustified = 0;
	double a66 = 0;
	double previousAccumulatedA66 = 0;
	double previousUnjustified = 0;

	if (lastAssiduousnessClosedMonth != null) {
	    previousAccumulatedA66 = lastAssiduousnessClosedMonth.getAccumulatedArticle66();
	    previousUnjustified = lastAssiduousnessClosedMonth.getAccumulatedUnjustified();
	}
	LocalDate beginDate = new LocalDate(getClosedMonth().getClosedYearMonth().get(DateTimeFieldType.year()), getClosedMonth()
		.getClosedYearMonth().get(DateTimeFieldType.monthOfYear()), 01);
	LocalDate endDate = new LocalDate(getClosedMonth().getClosedYearMonth().get(DateTimeFieldType.year()), getClosedMonth()
		.getClosedYearMonth().get(DateTimeFieldType.monthOfYear()), beginDate.dayOfMonth().getMaximumValue());
	unjustified = getTotalUnjustifiedPercentage(beginDate, endDate);

	unjustified = NumberUtils.formatDoubleWithoutRound(unjustified, 1);

	double anualRemaining = Assiduousness.MAX_A66_PER_YEAR - previousAccumulatedA66;
	double monthRemaining = anualRemaining > Assiduousness.MAX_A66_PER_MONTH ? Assiduousness.MAX_A66_PER_MONTH
		: anualRemaining;
	if (getArticle66() < monthRemaining && unjustified > 0) {
	    monthRemaining = monthRemaining - getArticle66();
	    if (unjustified <= monthRemaining) {
		a66 = unjustified;
		unjustified = 0;
	    } else {
		unjustified -= monthRemaining;
		a66 = monthRemaining;
	    }
	}
	a66 = NumberUtils.formatDoubleWithoutRound(a66, 1);
	setAccumulatedArticle66(previousAccumulatedA66 + a66);
	setAccumulatedUnjustified(previousUnjustified + unjustified);

	int countUnjustifiedWorkingDays = 0;
	for (Leave leave : getAssiduousnessStatusHistory().getAssiduousness().getLeaves(beginDate, endDate)) {
	    if (leave.getJustificationMotive().getAcronym().equalsIgnoreCase("FINJUST")) {
		countUnjustifiedWorkingDays += leave.getWorkDaysBetween(new Interval(beginDate.toDateTimeAtStartOfDay(), endDate
			.toDateTimeAtStartOfDay()));
	    }
	}
	setUnjustifiedDays(countUnjustifiedWorkingDays);
    }

    public AssiduousnessClosedMonth getPreviousAssiduousnessClosedMonth() {
	Partial partial = getClosedMonth().getClosedYearMonth();
	int previousMonth = partial.get(DateTimeFieldType.monthOfYear()) - 1;
	if (previousMonth <= 0) {
	    return null;
	}
	ClosedMonth previousClosedMonth = ClosedMonth.getClosedMonth(new YearMonth(partial.get(DateTimeFieldType.year()),
		previousMonth));
	if (previousClosedMonth != null) {
	    AssiduousnessClosedMonth assiduousnessClosedMonth = previousClosedMonth
		    .getAssiduousnessClosedMonth(getAssiduousnessStatusHistory());
	    if (assiduousnessClosedMonth != null
		    && assiduousnessClosedMonth.getAssiduousnessStatusHistory().equals(getAssiduousnessStatusHistory())) {
		return assiduousnessClosedMonth;
	    }
	}
	return null;
    }

    public Duration getTotalNightBalance() {
	Duration result = Duration.ZERO;
	for (AssiduousnessExtraWork assiduousnessExtraWork : getAssiduousnessExtraWorks()) {
	    result = result.plus(assiduousnessExtraWork.getNightBalance());
	}
	return result;
    }

    public Duration getTotalUnjustifiedBalance() {
	Duration result = Duration.ZERO;
	for (AssiduousnessExtraWork assiduousnessExtraWork : getAssiduousnessExtraWorks()) {
	    result = result.plus(assiduousnessExtraWork.getUnjustified());
	}
	return result;
    }

    public Duration getTotalFirstLevelBalance() {
	Duration result = Duration.ZERO;
	for (AssiduousnessExtraWork assiduousnessExtraWork : getAssiduousnessExtraWorks()) {
	    result = result.plus(assiduousnessExtraWork.getFirstLevelBalance());
	}
	return result;
    }

    public Duration getTotalSecondLevelBalance() {
	Duration result = Duration.ZERO;
	for (AssiduousnessExtraWork assiduousnessExtraWork : getAssiduousnessExtraWorks()) {
	    result = result.plus(assiduousnessExtraWork.getSecondLevelBalance());
	}
	return result;
    }

    public Map<WorkScheduleType, Duration> getNightWorkByWorkScheduleType() {
	Map<WorkScheduleType, Duration> nightWorkByWorkScheduleType = new HashMap<WorkScheduleType, Duration>();
	for (AssiduousnessExtraWork assiduousnessExtraWork : getAssiduousnessExtraWorks()) {
	    Duration duration = nightWorkByWorkScheduleType.get(assiduousnessExtraWork.getWorkScheduleType());
	    if (duration == null) {
		duration = Duration.ZERO;
	    }
	    duration = duration.plus(assiduousnessExtraWork.getNightBalance());
	    nightWorkByWorkScheduleType.put(assiduousnessExtraWork.getWorkScheduleType(), duration);
	}
	return nightWorkByWorkScheduleType;
    }

    public int getThisMonthUnjustifiedDays() {
	int unjustifiedDays = 0;
	double unjustified = getAccumulatedUnjustified();
	AssiduousnessClosedMonth previousAssiduousnessClosedMonth = getPreviousAssiduousnessClosedMonth();
	if (previousAssiduousnessClosedMonth != null) {
	    unjustifiedDays = (int) Math.floor(unjustified
		    - Math.floor(previousAssiduousnessClosedMonth.getAccumulatedUnjustified()));
	} else {
	    unjustifiedDays = (int) Math.floor(unjustified);
	}

	return unjustifiedDays;
    }

    public int getThisMonthArticle66() {
	int article66Days = 0;
	double article66 = getAccumulatedArticle66();
	AssiduousnessClosedMonth previousAssiduousnessClosedMonth = getPreviousAssiduousnessClosedMonth();
	if (previousAssiduousnessClosedMonth != null) {
	    article66Days = (int) Math.floor(article66 - Math.floor(previousAssiduousnessClosedMonth.getAccumulatedArticle66()));
	} else {
	    article66Days = (int) Math.floor(article66);
	}
	return article66Days;
    }

}
