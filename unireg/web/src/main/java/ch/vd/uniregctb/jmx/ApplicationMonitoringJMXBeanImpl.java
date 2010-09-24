package ch.vd.uniregctb.jmx;

import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;

import ch.vd.registre.jmx.interfaces.ApplicationMonitoringJmxBean;
import ch.vd.uniregctb.checker.ApplicationChecker;

@ManagedResource
public class ApplicationMonitoringJMXBeanImpl implements ApplicationMonitoringJmxBean {

	private ApplicationChecker checker;

	@ManagedAttribute
	public String getDescription() {
		return "Unireg (web)";
	}

	@ManagedAttribute
	public String getInformations() {
		return "Le registre cantonal vaudois des contribuables";
	}

	@ManagedAttribute
	public String getStatus() {
		return checker.getStatus();
	}

	@ManagedAttribute
	public String getStatusJSON() {
		return checker.getStatusJSON();
	}

	@ManagedAttribute
	public long getUptime() {
		return ApplicationChecker.getUptimeSeconds();
	}

	@ManagedAttribute
	public String getUptimeInformations() {
		return ApplicationChecker.getUptimeString();
	}

	@ManagedAttribute
	public String getVersion() {
		return checker.getVersion();
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setChecker(ApplicationChecker checker) {
		this.checker = checker;
	}
}
