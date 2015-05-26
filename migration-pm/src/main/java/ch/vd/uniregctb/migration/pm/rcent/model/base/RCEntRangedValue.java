package ch.vd.uniregctb.migration.pm.rcent.model.base;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.migration.pm.historizer.container.DateRanged;

/**
 *
 * @param <T>
 */
public class RCEntRangedValue<T> extends RCEntRangedElement {
	@NotNull
	private final T value;

	public RCEntRangedValue(RegDate beginDate, RegDate endDateDate, @NotNull T value) {
		super(beginDate, endDateDate);
		this.value = value;
	}

	public RCEntRangedValue(DateRanged<T> dr) {
		super(dr.getDateDebut(), dr.getDateFin());
		this.value = dr.getPayload();
	}

	@NotNull
	public T getValue() {
		return value;
	}
}
