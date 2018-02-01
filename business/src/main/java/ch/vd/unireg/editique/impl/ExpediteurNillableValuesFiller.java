package ch.vd.unireg.editique.impl;

import noNamespace.InfoEnteteDocumentDocument1;

/**
 * Classe utilitaire pour remplir un objet Expediteur avec soit sa valeur, soit nil .... vive l'API xmlbeans...
 */
public class ExpediteurNillableValuesFiller {

	private String adrMes;
	private String numFax;
	private String numCCP;
	private String numTelephone;

	public void setAdrMes(String adrMes) {
		this.adrMes = adrMes;
	}

	public void setNumFax(String numFax) {
		this.numFax = numFax;
	}

	public void setNumCCP(String numCCP) {
		this.numCCP = numCCP;
	}

	public void setNumTelephone(String numTelephone) {
		this.numTelephone = numTelephone;
	}

	/**
	 * Remplit l'objet {@link InfoEnteteDocumentDocument1.InfoEnteteDocument.Expediteur} cible avec les value
	 *
	 * @param expediteurCible objet cible
	 */
	public void fill(InfoEnteteDocumentDocument1.InfoEnteteDocument.Expediteur expediteurCible) {
		if (adrMes != null) {
			expediteurCible.setAdrMes(adrMes);
		}
		else {
			expediteurCible.setNilAdrMes();
		}
		if (numFax != null) {
			expediteurCible.setNumFax(numFax);
		}
		else {
			expediteurCible.setNilNumFax();
		}
		if (numCCP != null) {
			expediteurCible.setNumCCP(numCCP);
		}
		else {
			expediteurCible.setNilNumCCP();
		}
		if (numTelephone != null) {
			expediteurCible.setNumTelephone(numTelephone);
		}
		else {
			expediteurCible.setNilNumTelephone();
		}
	}

	/**
	 * Initialise les champs du Filler avec les valeurs des champs correpondant d'un objet {@link InfoEnteteDocumentDocument1.InfoEnteteDocument.Expediteur}
	 *
	 * @param expediteurSource l'objet source
	 */
	public void init(InfoEnteteDocumentDocument1.InfoEnteteDocument.Expediteur expediteurSource) {
		adrMes = expediteurSource.getAdrMes();
		numTelephone = expediteurSource.getNumTelephone();
		numFax = expediteurSource.getNumFax();
		numCCP = expediteurSource.getNumCCP();
	}
}
