package ch.vd.unireg.entreprise;

import ch.vd.unireg.tiers.Entreprise;

/**
 * @author Raphaël Marmier, 2016-010-18, <raphael.marmier@vd.ch>
 */
public class EditSecteurActiviteEntrepriseView {

	private Long tiersId;
	private String secteurActivite;

	public EditSecteurActiviteEntrepriseView() {}

	public EditSecteurActiviteEntrepriseView(Entreprise entreprise) {
		this.tiersId = entreprise.getNumero();
		this.secteurActivite = entreprise.getSecteurActivite();
	}

	public Long getTiersId() {
		return tiersId;
	}

	public void setTiersId(Long tiersId) {
		this.tiersId = tiersId;
	}

	public String getSecteurActivite() {
		return secteurActivite;
	}

	public void setSecteurActivite(String secteurActivite) {
		this.secteurActivite = secteurActivite;
	}
}
