package ch.vd.uniregctb.efacture;

import java.util.Date;

public abstract class AbstractEtatView {

	private final Date dateObtention;
	private final String motifObtention;

	/**
	 * XSIFNR : Pas de document relatif au destinataire, seulement pour la demande, prévu dans un futur proche, c'est pour cela
	 * que ce champ est ici et non dans la sous classe EtatDemandeView
	 */
	private final ArchiveKey documentArchiveKey;

	/**
	 * URL de visualisation (optionelle) du document dans une application externe (RepElec ?)
	 */
	private final String urlVisualisationExterneDocument;

	protected AbstractEtatView(Date dateObtention, String motifObtention, ArchiveKey documentArchiveKey, String urlVisualisationExterneDocument) {
		this.dateObtention = dateObtention;
		this.motifObtention = motifObtention;
		this.documentArchiveKey = documentArchiveKey;
		this.urlVisualisationExterneDocument = urlVisualisationExterneDocument;
	}

	public Date getDateObtention() {
		return dateObtention;
	}

	public String getMotifObtention() {
		return motifObtention;
	}

	public ArchiveKey getDocumentArchiveKey() {
		return documentArchiveKey;
	}

	public String getUrlVisualisationExterneDocument() {
		return urlVisualisationExterneDocument;
	}
}