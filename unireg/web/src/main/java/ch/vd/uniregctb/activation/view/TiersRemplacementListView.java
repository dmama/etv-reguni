package ch.vd.uniregctb.activation.view;

import ch.vd.uniregctb.general.view.TypeAvatar;
import ch.vd.uniregctb.tiers.view.TiersCriteriaView;

public class TiersRemplacementListView extends TiersCriteriaView {
	/**
	 *
	 */
	private static final long serialVersionUID = -3672591899328600683L;
	private String type;

	public String getType() {
		return type;
	}

	public void setType(String type) {

		if ((TypeAvatar.HOMME.toString().equals(type))
				|| (TypeAvatar.FEMME.toString().equals(type))
				|| (TypeAvatar.SEXE_INCONNU.toString().equals(type))) {
			setTypeTiers(TypeTiers.PERSONNE_PHYSIQUE);
		}

		if ((TypeAvatar.MC_MIXTE.toString().equals(type))
				|| (TypeAvatar.MC_HOMME_SEUL.toString().equals(type))
				|| (TypeAvatar.MC_FEMME_SEULE.toString().equals(type))
				|| (TypeAvatar.MC_HOMME_HOMME.toString().equals(type))
				|| (TypeAvatar.MC_FEMME_FEMME.toString().equals(type))
				|| (TypeAvatar.MC_SEXE_INCONNU.toString().equals(type))) {
			setTypeTiers(TypeTiers.MENAGE_COMMUN);
		}

		if (TypeAvatar.ENTREPRISE.toString().equals(type)) {
			setTypeTiers(TypeTiers.ENTREPRISE);
		}

		if (TypeAvatar.ETABLISSEMENT.toString().equals(type)) {
			setTypeTiers(TypeTiers.ETABLISSEMENT);
		}

		if (TypeAvatar.AUTRE_COMM.toString().equals(type)) {
			setTypeTiers(TypeTiers.AUTRE_COMMUNAUTE);
		}

		if (TypeAvatar.COLLECT_ADMIN.toString().equals(type)) {
			setTypeTiers(TypeTiers.COLLECTIVITE_ADMINISTRATIVE);
		}

		if (TypeAvatar.DEBITEUR.toString().equals(type)) {
			setTypeTiers(TypeTiers.DEBITEUR_PRESTATION_IMPOSABLE);
		}

		this.type = type;
	}


}
