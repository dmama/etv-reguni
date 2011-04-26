package ch.vd.uniregctb.checker;

import ch.vd.uniregctb.common.TimeHelper;

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
		return TimeHelper.formatDuree(getUptimeSeconds() * 1000L);
	}

	public String getVersion() {
		return version;
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

