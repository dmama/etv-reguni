package ch.vd.uniregctb.activation.view;

import ch.vd.uniregctb.general.view.TiersGeneralView;
import ch.vd.uniregctb.tiers.view.TiersCriteriaView;

public class TiersRemplacementListView extends TiersCriteriaView{
	/**
	 *
	 */
	private static final long serialVersionUID = -3672591899328600683L;
	private String type;

	public String getType() {
		return type;
	}

	public void setType(String type) {

		if (		(TiersGeneralView.TypeTiers.HOMME.toString().equals(type))
				|| 	(TiersGeneralView.TypeTiers.FEMME.toString().equals(type))
				|| 	(TiersGeneralView.TypeTiers.SEXE_INCONNU.toString().equals(type))) {
			setTypeTiers(TypeTiers.PERSONNE_PHYSIQUE);
		}

		if (		(TiersGeneralView.TypeTiers.MC_MIXTE.toString().equals(type))
				|| 	(TiersGeneralView.TypeTiers.MC_HOMME_SEUL.toString().equals(type))
				|| 	(TiersGeneralView.TypeTiers.MC_FEMME_SEULE.toString().equals(type))
				|| 	(TiersGeneralView.TypeTiers.MC_HOMME_HOMME.toString().equals(type))
				|| 	(TiersGeneralView.TypeTiers.MC_FEMME_FEMME.toString().equals(type))
				|| 	(TiersGeneralView.TypeTiers.MC_SEXE_INCONNU.toString().equals(type))) {
			setTypeTiers(TypeTiers.MENAGE_COMMUN);
		}

		if (TiersGeneralView.TypeTiers.ENTREPRISE.toString().equals(type)) {
			setTypeTiers(TypeTiers.ENTREPRISE);
		}

		if (TiersGeneralView.TypeTiers.ETABLISSEMENT.toString().equals(type)) {
			setTypeTiers(TypeTiers.ETABLISSEMENT);
		}

		if (TiersGeneralView.TypeTiers.AUTRE_COMM.toString().equals(type)) {
			setTypeTiers(TypeTiers.AUTRE_COMMUNAUTE);
		}

		if (TiersGeneralView.TypeTiers.COLLECT_ADMIN.toString().equals(type)) {
			setTypeTiers(TypeTiers.COLLECTIVITE_ADMINISTRATIVE);
		}

		if (TiersGeneralView.TypeTiers.DEBITEUR.toString().equals(type)) {
			setTypeTiers(TypeTiers.DEBITEUR_PRESTATION_IMPOSABLE);
		}

		this.type = type;
	}



}
