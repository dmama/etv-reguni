package ch.vd.uniregctb.registrefoncier.allegement;

public class AddExonerationView extends AbstractEditExonerationView {

	private long idContribuable;
	private long idImmeuble;

	public AddExonerationView() {
	}

	public AddExonerationView(long idContribuable, long idImmeuble) {
		this.idContribuable = idContribuable;
		this.idImmeuble = idImmeuble;
	}

	public long getIdContribuable() {
		return idContribuable;
	}

	public void setIdContribuable(long idContribuable) {
		this.idContribuable = idContribuable;
	}

	public long getIdImmeuble() {
		return idImmeuble;
	}

	public void setIdImmeuble(long idImmeuble) {
		this.idImmeuble = idImmeuble;
	}
}
