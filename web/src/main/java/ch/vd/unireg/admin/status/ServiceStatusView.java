package ch.vd.unireg.admin.status;

import org.jetbrains.annotations.NotNull;

import ch.vd.shared.statusmanager.StatusManager;
import ch.vd.unireg.common.HtmlHelper;

public class ServiceStatusView {

	private final String name;
	private final String code;
	private final String description;

	public ServiceStatusView(@NotNull StatusManager statusManager, @NotNull String checkerName) {
		this.name = checkerName;
		final String status = statusManager.getDetailedStatus().get(checkerName);
		if ("OK".equals(status)) {
			this.code = "OK";
			this.description = null;
		}
		else {
			this.code = "KO";
			this.description = HtmlHelper.renderMultilines(status);
		}
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
