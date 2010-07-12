package ch.vd.uniregctb.checker;

public class ApplicationChecker {

	private static final long startupTime = System.nanoTime();

	private String version;
	private ServiceCivilChecker serviceCivilChecker;
	private ServiceInfraChecker serviceInfraChecker;
	private ServiceSecuriteChecker serviceSecuriteChecker;
	private TiersSearcherChecker tiersSearcherChecker;

	public String getStatus() {
		final Status status;
		if (serviceCivilChecker.getStatus() == Status.OK && serviceInfraChecker.getStatus() == Status.OK && serviceSecuriteChecker.getStatus() == Status.OK &&
				tiersSearcherChecker.getStatus() == Status.OK) {
			status = Status.OK;
		}
		else {
			status = Status.KO;
		}
		return status.name();
	}

	public String getStatusJSON() {
		return "{'serviceCivil' : '" + serviceCivilChecker.getStatus().name() + "', " +
				"'serviceInfra' : '" + serviceInfraChecker.getStatus().name() + "', " +
				"'serviceSecurite' : '" + serviceSecuriteChecker.getStatus().name() + "', " +
				"'globalTiersSearcher' : '" + tiersSearcherChecker.getStatus().name() + "'}";
	}

	public static long getUptimeSeconds() {
		return (System.nanoTime() - startupTime) / 1000000000L;
	}

	public static String getUptimeString() {
		return buildTimeString(getUptimeSeconds());
	}

	public String getVersion() {
		return version;
	}

	private static String buildTimeString(long time) {
		StringBuilder str = new StringBuilder();

		long days = time / 3600 / 24;
		if (days > 0) {
			str.append(days);
			str.append(" jour(s), ");
			time -= days * 3600 * 24;
		}

		long hours = time / 3600;
		if (str.length() > 0 || hours > 0) {
			str.append(hours);
			str.append(" heure(s), ");
			time -= hours * 3600;
		}

		long minutes = time / 60;
		if (str.length() > 0 || minutes > 0) {
			str.append(minutes);
			str.append(" minute(s), ");
			time -= minutes * 60;
		}

		long sec = time;
		str.append(sec);
		str.append(" seconde(s).");

		return str.toString();
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setVersion(String version) {
		this.version = version;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setServiceCivilChecker(ServiceCivilChecker serviceCivilChecker) {
		this.serviceCivilChecker = serviceCivilChecker;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setServiceInfraChecker(ServiceInfraChecker serviceInfraChecker) {
		this.serviceInfraChecker = serviceInfraChecker;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setServiceSecuriteChecker(ServiceSecuriteChecker serviceSecuriteChecker) {
		this.serviceSecuriteChecker = serviceSecuriteChecker;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setTiersSearcherChecker(TiersSearcherChecker tiersSearcherChecker) {
		this.tiersSearcherChecker = tiersSearcherChecker;
	}
}