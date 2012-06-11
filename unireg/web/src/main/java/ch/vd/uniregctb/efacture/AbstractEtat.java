package ch.vd.uniregctb.efacture;

import ch.vd.registre.base.date.RegDate;

public abstract class AbstractEtat {

	private final RegDate dateObtention;
	private final String motifObtention;
	private final ArchiveKey documentArchiveKey;

	protected AbstractEtat(RegDate dateObtention, String motifObtention, ArchiveKey documentArchiveKey) {
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

	public ArchiveKey getDocumentArchiveKey() {
		return documentArchiveKey;
	}
}