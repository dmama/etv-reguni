package ch.vd.uniregctb.admin.status;

import ch.vd.uniregctb.checker.ServiceChecker;
import ch.vd.uniregctb.common.HtmlHelper;

public class ServiceStatusView {
	private String name;
	private String code;
	private String description;

	public ServiceStatusView(String name, ServiceChecker checker) {
		this.name = name;
		this.code = checker.getStatus().name();
		this.description = HtmlHelper.renderMultilines(checker.getStatusDetails());
	}

	public String getName() {
		return name;
	}

	public String getCode() {
		return code;
	}

	public String getDescription() {
		return description;
	}
}
