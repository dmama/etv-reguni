package ch.vd.unireg.entreprise;

import ch.vd.unireg.tiers.Etablissement;

/**
 * @author Raphaël Marmier, 2017-01-16, <raphael.marmier@vd.ch>
 */
public class EditEnseigneEtablissementView {

	private Long tiersId;
	private String enseigne;

	public EditEnseigneEtablissementView() {}

	public EditEnseigneEtablissementView(Etablissement etablissement) {
		this.tiersId = etablissement.getNumero();
		this.enseigne = etablissement.getEnseigne();
	}

	public Long getTiersId() {
		return tiersId;
	}

	public void setTiersId(Long tiersId) {
		this.tiersId = tiersId;
	}

	public String getEnseigne() {
		return enseigne;
	}

	public void setEnseigne(String enseigne) {
		this.enseigne = enseigne;
	}
}
