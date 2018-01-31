package ch.vd.unireg.app;

import org.codehaus.jackson.map.ObjectMapper;

import ch.vd.shared.statusmanager.StatusManager;
import ch.vd.uniregctb.common.TimeHelper;

public class ApplicationChecker {

	private static final long startupTime = System.nanoTime();

	private final ObjectMapper mapper = new ObjectMapper();

	private String version;
	private StatusManager statusManager;

	public String getStatus() {
		return statusManager.getStatus();
	}

	public String getStatusJSON() {
		try {
			return mapper.writeValueAsString(statusManager.getDetailedStatus());
		}
		catch (Exception e) {
			return "{'exception':'" + e.getMessage() + "'}";
		}
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

	public void setStatusManager(StatusManager statusManager) {
		this.statusManager = statusManager;
	}
}

