package ch.vd.unireg.registrefoncier.allegement;

public class AddDegrevementView extends AbstractEditDegrevementView {

	private long idContribuable;
	private long idImmeuble;

	public AddDegrevementView() {
	}

	public AddDegrevementView(long idContribuable, long idImmeuble) {
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
