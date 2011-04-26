package ch.vd.uniregctb.declaration;

import java.io.Serializable;

import ch.vd.registre.base.date.RegDate;

public class ListeRecapCriteria implements Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = -5991415127042157289L;

	private String periodicite;

	private RegDate periode;

	private String categorie;

	private String etat;

	private String modeCommunication;


	public String getPeriodicite() {
		return periodicite;
	}

	public void setPeriodicite(String periodicite) {
		this.periodicite = periodicite;
	}

	public RegDate getPeriode() {
		return periode;
	}

	public void setPeriode(RegDate periode) {
		this.periode = periode;
	}

	public String getCategorie() {
		return categorie;
	}

	public void setCategorie(String categorie) {
		this.categorie = categorie;
	}

	public String getEtat() {
		return etat;
	}

	public void setEtat(String etat) {
		this.etat = etat;
	}

	public String getModeCommunication() {
		return modeCommunication;
	}

	public void setModeCommunication(String modeCommunication) {
		this.modeCommunication = modeCommunication;
	}

	/**
	 * @return true si aucun paramétre de recherche n'est renseigné. false
	 *         autrement.
	 */
	public boolean isEmpty() {
		boolean flag= 	(modeCommunication == null || "TOUS".equals(modeCommunication))
						&& (etat == null || "TOUS".equals(etat))
						&& (categorie == null || "TOUTES".equals(categorie))
						&& (periodicite == null || "TOUTES".equals(periodicite))
						&& periode == null;
		return flag;
	}

}
