package ch.vd.uniregctb.stats;

public class LoadMonitorStats {

	private final int chargeInstantanee;

	private final double moyenneCharge;

	public LoadMonitorStats(LoadMonitor monitor) {
		this.chargeInstantanee = monitor.getChargeInstantanee();
		this.moyenneCharge = monitor.getMoyenneChargeCinqMinutes();
	}

	public int getChargeInstantanee() {
		return chargeInstantanee;
	}

	public double getMoyenneCharge() {
		return moyenneCharge;
	}

	@Override
	public String toString() {
		return "LoadMonitorStats{" +
				"chargeInstantanee=" + chargeInstantanee +
				", moyenneCharge=" + moyenneCharge +
				'}';
	}
}
