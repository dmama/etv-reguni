package ch.vd.unireg.registrefoncier.allegement;

public class AddDemandeDegrevementView extends AbstractEditDemandeDegrevementView {

	private long idContribuable;
	private long idImmeuble;

	public AddDemandeDegrevementView() {
	}

	public AddDemandeDegrevementView(long idContribuable, long idImmeuble) {
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
