package ch.vd.uniregctb.stats;

public class LoadMonitorStats {

	private final int chargeInstantannee;

	private final double moyenneCharge;

	public LoadMonitorStats(LoadMonitor monitor) {
		this.chargeInstantannee = monitor.getChargeInstantannee();
		this.moyenneCharge = monitor.getMoyenneChargeCinqMinutes();
	}

	public int getChargeInstantannee() {
		return chargeInstantannee;
	}

	public double getMoyenneCharge() {
		return moyenneCharge;
	}

	@Override
	public String toString() {
		return "LoadMonitorStats{" +
				"chargeInstantannee=" + chargeInstantannee +
				", moyenneCharge=" + moyenneCharge +
				'}';
	}
}
