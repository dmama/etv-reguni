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

	private boolean hasHistoFlag(HistoFlag flag) {
		return histoFlags.hasHistoFlag(flag);
	}

	@SuppressWarnings("unused")
	public boolean isAdressesHisto() {
		return hasHistoFlag(HistoFlag.ADRESSES);
	}

	@SuppressWarnings("unused")
	public boolean isAdressesHistoCiviles() {
		return hasHistoFlag(HistoFlag.ADRESSES_CIVILES);
	}

	@SuppressWarnings("unused")
	public boolean isAdressesHistoCivilesConjoint() {
		return hasHistoFlag(HistoFlag.ADRESSES_CIVILES_CONJOINT);
	}

	@SuppressWarnings("unused")
	public boolean isRapportsEntreTiersHisto() {
		return hasHistoFlag(HistoFlag.RAPPORTS_ENTRE_TIERS);
	}

	@SuppressWarnings("unused")
	public boolean isEtablissementsHisto() {
		return hasHistoFlag(HistoFlag.ETABLISSEMENTS);
	}

	@SuppressWarnings("unused")
	public boolean isRapportsPrestationHisto() {
		return hasHistoFlag(HistoFlag.RAPPORTS_PRESTATION);
	}

	@SuppressWarnings("unused")
	public boolean isCtbAssocieHisto() {
		return hasHistoFlag(HistoFlag.CTB_ASSOCIE);
	}

	@SuppressWarnings("unused")
	public boolean isRaisonsSocialesHisto() {
		return hasHistoFlag(HistoFlag.RAISONS_SOCIALES);
	}

	@SuppressWarnings("unused")
	public boolean isNomsAdditionnelsHisto() {
		return hasHistoFlag(HistoFlag.NOMS_ADDITIONNELS);
	}

	@SuppressWarnings("unused")
	public boolean isSiegesHisto() {
		return hasHistoFlag(HistoFlag.SIEGES);
	}

	@SuppressWarnings("unused")
	public boolean isFormesJuridiquesHisto() {
		return hasHistoFlag(HistoFlag.FORMES_JURIDIQUES);
	}

	@SuppressWarnings("unused")
	public boolean isCapitauxHisto() {
		return hasHistoFlag(HistoFlag.CAPITAUX);
	}

	@SuppressWarnings("unused")
	public boolean isDomicilesHisto() {
		return hasHistoFlag(HistoFlag.DOMICILES);
	}

	@SuppressWarnings("unused")
	public boolean isFlagsEntrepriseHisto(GroupeFlagsEntreprise groupe) {
		switch (groupe) {
		case LIBRE:
			return hasHistoFlag(HistoFlag.FLAGS_ENTREPRISE_LIBRE);
		case SI_SERVICE_UTILITE_PUBLIQUE:
			return hasHistoFlag(HistoFlag.FLAGS_ENTREPRISE_SISUP);
		default:
			throw new IllegalArgumentException("Group de flags d'entreprise inconnu ici : " + groupe);
		}
	}

	@SuppressWarnings("unused")
	public boolean isLabelsHisto() {
		return hasHistoFlag(HistoFlag.LABELS);
	}

	@SuppressWarnings("unused")
	public boolean isLabelsConjointHisto() {
		return hasHistoFlag(HistoFlag.LABELS_CONJOINT);
	}

	@SuppressWarnings("unused")
	public boolean isMandatairesCourrierHisto() {
		return hasHistoFlag(HistoFlag.MANDATAIRES_COURRIER);
	}

	@SuppressWarnings("unused")
	public boolean isMandatairesPerceptionHisto() {
		return hasHistoFlag(HistoFlag.MANDATAIRES_PERCEPTION);
	}

	@SuppressWarnings("unused")
	public boolean isRegimesFiscauxCHHisto() {
		return hasHistoFlag(HistoFlag.REGIMES_FISCAUX_CH);
	}

	@SuppressWarnings("unused")
	public boolean isRegimesFiscauxVDHisto() {
		return hasHistoFlag(HistoFlag.REGIMES_FISCAUX_VD);
	}

	@SuppressWarnings("unused")
	public boolean isSituationsFamilleHisto() {
		return hasHistoFlag(HistoFlag.SITUATIONS_FAMILLE);
	}

	@SuppressWarnings("unused")
	public boolean isAllegementsFiscauxHisto() {
		return hasHistoFlag(HistoFlag.ALLEGEMENTS_FISCAUX);
	}

	@SuppressWarnings("unused")
	public boolean isPeriodicitesHisto() {
		return hasHistoFlag(HistoFlag.PERIODICITES_HISTO);
	}

	@SuppressWarnings("unused")
	public boolean isRemarquesHisto() {
		return hasHistoFlag(HistoFlag.REMARQUES);
	}
}
