package ch.vd.uniregctb.entreprise;

import ch.vd.uniregctb.tiers.Etablissement;

/**
 * @author Raphaël Marmier, 2016-01-20, <raphael.marmier@vd.ch>
 */
public class EditRaisonEnseigneEtablissementView {

	private Long tiersId;
	private String raisonSociale;
	private String enseigne;

	public EditRaisonEnseigneEtablissementView() {}

	public EditRaisonEnseigneEtablissementView(Etablissement etablissement) {
		this.tiersId = etablissement.getNumero();
		this.raisonSociale = etablissement.getRaisonSociale();
		this.enseigne = etablissement.getEnseigne();
	}

	public Long getTiersId() {
		return tiersId;
	}

	public void setTiersId(Long tiersId) {
		this.tiersId = tiersId;
	}

	public String getRaisonSociale() {
		return raisonSociale;
	}

	public void setRaisonSociale(String raisonSociale) {
		this.raisonSociale = raisonSociale;
	}

	public String getEnseigne() {
		return enseigne;
	}

	public void setEnseigne(String enseigne) {
		this.enseigne = enseigne;
	}
}
