package ch.vd.uniregctb.efacture;

import ch.vd.registre.base.date.RegDate;

public abstract class AbstractEtat {

	private final RegDate dateObtention;
	private final String motifObtention;
	private final String documentArchiveKey;

	protected AbstractEtat(RegDate dateObtention, String motifObtention, String documentArchiveKey) {
		this.dateObtention = dateObtention;
		this.motifObtention = motifObtention;
		this.documentArchiveKey = documentArchiveKey;
	}

	public RegDate getDateObtention() {
		return dateObtention;
	}

	public String getMotifObtention() {
		return motifObtention;
	}

	public String getDocumentArchiveKey() {
		return documentArchiveKey;
	}
}