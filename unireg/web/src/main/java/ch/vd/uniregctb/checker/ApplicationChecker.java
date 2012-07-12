package ch.vd.uniregctb.checker;

import ch.vd.uniregctb.common.TimeHelper;

public class ApplicationChecker {

	private static final long startupTime = System.nanoTime();

	private String version;
	private ServiceCivilChecker serviceCivilChecker;
	private ServiceInfraChecker serviceInfraChecker;
	private ServiceSecuriteChecker serviceSecuriteChecker;
	private ServiceBVRChecker serviceBVRChecker;

	public String getStatus() {
		final Status status;
		if (serviceCivilChecker.getStatus() == Status.OK && serviceInfraChecker.getStatus() == Status.OK &&
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
				"'serviceInfra' : '" + serviceInfraChecker.getStatus().name() + "', " +
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
	public void setServiceBVRChecker(ServiceBVRChecker serviceBVRChecker) {
		this.serviceBVRChecker = serviceBVRChecker;
	}
}

