package ch.vd.uniregctb.editique.impl;

import noNamespace.InfoEnteteDocumentDocument1;

/**
 * Classe utilitaire pour remplir un objet Expediteur
 * avec soit sa valeur, soit nil .... vive l'API xmlbeans...
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

	public void fill(InfoEnteteDocumentDocument1.InfoEnteteDocument.Expediteur expediteur) {
		if (adrMes != null) {
			expediteur.setAdrMes(adrMes);
		} else {
			expediteur.setNilAdrMes();
		}
		if (numFax != null) {
			expediteur.setNumFax(numFax);
		} else {
			expediteur.setNilNumFax();
		}
		if (numCCP != null) {
			expediteur.setNumCCP(numCCP);
		} else {
			expediteur.setNilNumCCP();
		}
		if (numTelephone != null) {
			expediteur.setNumTelephone(numTelephone);
		} else {
			expediteur.setNilNumTelephone();
		}
	}
}
