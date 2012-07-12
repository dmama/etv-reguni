package ch.vd.uniregctb.efacture;

import ch.vd.registre.base.date.RegDate;

public abstract class AbstractEtatView {

	private final RegDate dateObtention;
	private final String motifObtention;
	private final ArchiveKey documentArchiveKey;    // Pas de document relatif au destinataire, seulement pour la demande,
													// prévu dans un futur proche c'est pour cela
												    // que ce champ est ici et non dans la sous classe EtatDemandeView

	protected AbstractEtatView(RegDate dateObtention, String motifObtention, ArchiveKey documentArchiveKey) {
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