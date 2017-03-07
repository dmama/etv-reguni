package ch.vd.uniregctb.contribuableAssocie.view;

public class ContribuableAssocieEditView {

	private long numeroDpi;
	private long numeroContribuable;

	public ContribuableAssocieEditView() {
	}

	public ContribuableAssocieEditView(long numeroDpi, long numeroContribuable) {
		this.numeroDpi = numeroDpi;
		this.numeroContribuable = numeroContribuable;
	}

	public long getNumeroDpi() {
		return numeroDpi;
	}

	public void setNumeroDpi(long numeroDpi) {
		this.numeroDpi = numeroDpi;
	}

	public long getNumeroContribuable() {
		return numeroContribuable;
	}

	public void setNumeroContribuable(long numeroContribuable) {
		this.numeroContribuable = numeroContribuable;
	}
}
