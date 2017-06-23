package ch.vd.uniregctb.tiers.view;

import ch.vd.uniregctb.tiers.HistoFlag;
import ch.vd.uniregctb.tiers.HistoFlags;
import ch.vd.uniregctb.type.GroupeFlagsEntreprise;

/**
 * Structure model pour l'ecran de visualisation des Tiers
 */
public class TiersVisuView extends TiersView {

	/**
	 * Les différents flags de visualisation d'historique demandés
	 */
	private final HistoFlags histoFlags;

	public TiersVisuView(HistoFlags histoFlags) {
		this.histoFlags = histoFlags;
	}

	private boolean hasFlag(HistoFlag flag) {
		return histoFlags.hasHistoFlag(flag);
	}

	public boolean isAdressesHisto() {
		return hasFlag(HistoFlag.ADRESSES);
	}

	public boolean isAdressesHistoCiviles() {
		return hasFlag(HistoFlag.ADRESSES_CIVILES);
	}

	public boolean isAdressesHistoCivilesConjoint() {
		return hasFlag(HistoFlag.ADRESSES_CIVILES_CONJOINT);
	}

	public boolean isRapportsPrestationHisto() {
		return hasFlag(HistoFlag.RAPPORTS_PRESTATION);
	}

	public boolean isCtbAssocieHisto() {
		return hasFlag(HistoFlag.CTB_ASSOCIE);
	}

	public boolean isRaisonsSocialesHisto() {
		return hasFlag(HistoFlag.RAISONS_SOCIALES);
	}

	public boolean isNomsAdditionnelsHisto() {
		return hasFlag(HistoFlag.NOMS_ADDITIONNELS);
	}

	public boolean isSiegesHisto() {
		return hasFlag(HistoFlag.SIEGES);
	}

	public boolean isFormesJuridiquesHisto() {
		return hasFlag(HistoFlag.FORMES_JURIDIQUES);
	}

	public boolean isCapitauxHisto() {
		return hasFlag(HistoFlag.CAPITAUX);
	}

	public boolean isDomicilesHisto() {
		return hasFlag(HistoFlag.DOMICILES);
	}

	public boolean isFlagsEntrepriseHisto(GroupeFlagsEntreprise groupe) {
		switch (groupe) {
		case LIBRE:
			return hasFlag(HistoFlag.FLAGS_ENTREPRISE_LIBRE);
		case SI_SERVICE_UTILITE_PUBLIQUE:
			return hasFlag(HistoFlag.FLAGS_ENTREPRISE_SISUP);
		default:
			throw new IllegalArgumentException("Group de flags d'entreprise inconnu ici : " + groupe);
		}
	}

	public boolean isLabelsHisto() {
		return hasFlag(HistoFlag.LABELS);
	}

	public boolean isLabelsConjointHisto() {
		return hasFlag(HistoFlag.LABELS_CONJOINT);
	}

	public boolean isMandatairesCourrierHisto() {
		return hasFlag(HistoFlag.MANDATAIRES_COURRIER);
	}

	public boolean isMandatairesPerceptionHisto() {
		return hasFlag(HistoFlag.MANDATAIRES_PERCEPTION);
	}

	public boolean isRegimesFiscauxCHHisto() {
		return hasFlag(HistoFlag.REGIMES_FISCAUX_CH);
	}

	public boolean isRegimesFiscauxVDHisto() {
		return hasFlag(HistoFlag.REGIMES_FISCAUX_VD);
	}

	public boolean isSituationsFamilleHisto() {
		return hasFlag(HistoFlag.SITUATIONS_FAMILLE);
	}

	public boolean isAllegementsFiscauxHisto() {
		return hasFlag(HistoFlag.ALLEGEMENTS_FISCAUX);
	}

	public boolean isPeriodicitesHisto() {
		return hasFlag(HistoFlag.PERIODICITES_HISTO);
	}

	public boolean isRemarquesHisto() {
		return hasFlag(HistoFlag.REMARQUES);
	}
}
