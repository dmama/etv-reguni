package ch.vd.unireg.admin.status;

import ch.vd.shared.statusmanager.StatusChecker;
import ch.vd.unireg.common.HtmlHelper;

public class ServiceStatusView {

	private final String name;
	private final String code;
	private final String description;

	public ServiceStatusView(StatusChecker checker) {
		this.name = checker.getName();

		String code;
		String description;
		try {
			checker.check();
			code = "OK";
			description = null;
		}
		catch (Exception e) {
			code = "KO";
			description = HtmlHelper.renderMultilines(e.getMessage());
		}
		this.code = code;
		this.description = description;
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
