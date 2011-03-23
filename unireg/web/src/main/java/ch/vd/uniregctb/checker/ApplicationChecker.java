package ch.vd.uniregctb.checker;

public class ApplicationChecker {

	private static final long startupTime = System.nanoTime();

	private String version;
	private ServiceCivilChecker serviceCivilChecker;
	private ServiceHostInfraChecker serviceHostInfraChecker;
	private ServiceFidorChecker serviceFidorChecker;
	private ServiceSecuriteChecker serviceSecuriteChecker;
	private ServiceBVRChecker serviceBVRChecker;

	public String getStatus() {
		final Status status;
		if (serviceCivilChecker.getStatus() == Status.OK && serviceHostInfraChecker.getStatus() == Status.OK && serviceFidorChecker.getStatus() == Status.OK &&
				serviceSecuriteChecker.getStatus() == Status.OK && serviceBVRChecker.getStatus() == Status.OK) {
			status = Status.OK;
		}
		else {
			status = Status.KO;
		}
		return status.name();
	}

	public String getStatusJSON() {
		return "{'serviceCivil' : '" + serviceCivilChecker.getStatus().name() + "', " +
				"'serviceHostInfra' : '" + serviceHostInfraChecker.getStatus().name() + "', " +
				"'serviceFidor' : '" + serviceFidorChecker.getStatus().name() + "', " +
				"'serviceSecurite' : '" + serviceSecuriteChecker.getStatus().name() + "', " +
				"'serviceBVRPlus' : '" + serviceBVRChecker.getStatus().name() + "'}";
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

	public void setVersion(String version) {
		this.version = version;
	}

	public void setServiceCivilChecker(ServiceCivilChecker serviceCivilChecker) {
		this.serviceCivilChecker = serviceCivilChecker;
	}

	public void setServiceHostInfraChecker(ServiceHostInfraChecker serviceHostInfraChecker) {
		this.serviceHostInfraChecker = serviceHostInfraChecker;
	}

	public void setServiceFidorChecker(ServiceFidorChecker serviceFidorChecker) {
		this.serviceFidorChecker = serviceFidorChecker;
	}

	public void setServiceSecuriteChecker(ServiceSecuriteChecker serviceSecuriteChecker) {
		this.serviceSecuriteChecker = serviceSecuriteChecker;
	}

	public void setServiceBVRChecker(ServiceBVRChecker serviceBVRChecker) {
		this.serviceBVRChecker = serviceBVRChecker;
	}
}

