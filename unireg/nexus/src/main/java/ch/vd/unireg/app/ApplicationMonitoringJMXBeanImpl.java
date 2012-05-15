package ch.vd.unireg.app;

import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;

import ch.vd.registre.jmx.interfaces.ApplicationMonitoringJmxBean;

@ManagedResource
public class ApplicationMonitoringJMXBeanImpl implements ApplicationMonitoringJmxBean {

	private ApplicationChecker checker;

	@Override
	@ManagedAttribute
	public String getDescription() {
		return "Unireg (Nexus)";
	}

	@Override
	@ManagedAttribute
	public String getInformations() {
		return "Le cache centralisé d'Unireg";
	}

	@Override
	@ManagedAttribute
	public String getStatus() {
		return checker.getStatus();
	}

	@Override
	@ManagedAttribute
	public String getStatusJSON() {
		return checker.getStatusJSON();
	}

	@Override
	@ManagedAttribute
	public long getUptime() {
		return ApplicationChecker.getUptimeSeconds();
	}

	@Override
	@ManagedAttribute
	public String getUptimeInformations() {
		return ApplicationChecker.getUptimeString();
	}

	@Override
	@ManagedAttribute
	public String getVersion() {
		return checker.getVersion();
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setChecker(ApplicationChecker checker) {
		this.checker = checker;
	}
}
