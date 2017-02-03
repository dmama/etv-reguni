package ch.vd.uniregctb.foncier.migration;

import java.util.Map;

public class ValeurDegrevement {

	private final int periodeFiscale;
	private final Map<TypeUsage, MigrationDDUsage> usages;

	public ValeurDegrevement(int periodeFiscale, Map<TypeUsage, MigrationDDUsage> usages) {
		this.periodeFiscale = periodeFiscale;
		this.usages = usages;
	}

	public int getPeriodeFiscale() {
		return periodeFiscale;
	}

	public Map<TypeUsage, MigrationDDUsage> getUsages() {
		return usages;
	}
}
