package ch.vd.uniregctb.audit;

public class AuditLineCriteria {

	private boolean showInfo = true;
	private boolean showSuccess = true;
	private boolean showError = true;
	private boolean showWarning = true;
	private boolean showEvCivil = true;

	public boolean isShowInfo() {
		return showInfo;
	}

	public void setShowInfo(boolean showInfo) {
		this.showInfo = showInfo;
	}

	public boolean isShowSuccess() {
		return showSuccess;
	}

	public void setShowSuccess(boolean showSuccess) {
		this.showSuccess = showSuccess;
	}

	public boolean isShowError() {
		return showError;
	}

	public void setShowError(boolean showError) {
		this.showError = showError;
	}

	public boolean isShowWarning() {
		return showWarning;
	}

	public void setShowWarning(boolean showWarning) {
		this.showWarning = showWarning;
	}

	public boolean isShowEvCivil() {
		return showEvCivil;
	}

	public void setShowEvCivil(boolean showEvCivil) {
		this.showEvCivil = showEvCivil;
	}
}
