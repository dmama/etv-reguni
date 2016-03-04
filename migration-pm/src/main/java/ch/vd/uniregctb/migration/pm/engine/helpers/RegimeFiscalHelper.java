package ch.vd.uniregctb.migration.pm.engine.helpers;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.migration.pm.MigrationResultProduction;
import ch.vd.uniregctb.migration.pm.log.LogCategory;
import ch.vd.uniregctb.migration.pm.log.LogLevel;
import ch.vd.uniregctb.migration.pm.regpm.RegpmRegimeFiscal;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeRegimeFiscal;
import ch.vd.uniregctb.tiers.RegimeFiscal;
import ch.vd.uniregctb.type.TypeFlagEntreprise;

public class RegimeFiscalHelper {

	private final DateHelper dateHelper;

	private static final Set<RegpmTypeRegimeFiscal> REGIMES_POUR_SOCIETE_IMMOBILIERE = EnumSet.of(RegpmTypeRegimeFiscal._31_SOCIETE_ORDINAIRE,
	                                                                                              RegpmTypeRegimeFiscal._32_SOCIETE_ORDINAIRE_SUBVENTION,
	                                                                                              RegpmTypeRegimeFiscal._33_SOCIETE_ORDINAIRE_CARACTERE_SOCIAL,
	                                                                                              RegpmTypeRegimeFiscal._35_SOCIETE_ORDINAIRE_SIAL);

	public RegimeFiscalHelper(DateHelper dateHelper) {
		this.dateHelper = dateHelper;
	}

	/**
	 * Fournit une map des régimes fiscaux officielement admis
	 * @param regimes régimes de RegPM en vrac
	 * @param dateFinRegimes date de fin d'activité de l'entreprise
	 * @param portee portée des régimes fiscaux (donnée pour les logs)
	 * @param mr collecteur de messages de log
	 * @param <T> type exact des régimes fiscaux de RegPM
	 * @return une map des régimes fiscaux à prendre en compte par date de début de validité
	 */
	public <T extends RegpmRegimeFiscal> NavigableMap<RegDate, T> buildMapRegimesFiscaux(SortedSet<T> regimes, @Nullable RegDate dateFinRegimes, RegimeFiscal.Portee portee, MigrationResultProduction mr) {
		// collecte des régimes fiscaux, filtrage, tri
		final Map<RegDate, List<T>> liste = regimes.stream()
				.filter(r -> r.getDateAnnulation() == null)         // on ne migre pas les régimes fiscaux annulés
				.filter(rf -> {
					if (rf.getDateDebut() == null) {
						mr.addMessage(LogCategory.SUIVI, LogLevel.ERROR,
						              String.format("Régime fiscal %s %s ignoré en raison de sa date de début nulle (ou antérieure au 01.08.1291).",
						                            portee,
						                            rf.getType()));
						return false;
					}
					return true;
				})
				.filter(rf -> {
					if (dateHelper.isFutureDate(rf.getDateDebut())) {
						mr.addMessage(LogCategory.SUIVI, LogLevel.ERROR,
						              String.format("Régime fiscal %s %s ignoré en raison de sa date de début dans le futur (%s).",
						                            portee,
						                            rf.getType(),
						                            StringRenderers.DATE_RENDERER.toString(rf.getDateDebut())));
						return false;
					}
					return true;
				})
				.filter(rf -> {
					if (dateFinRegimes != null && dateFinRegimes.isBefore(rf.getDateDebut())) {
						mr.addMessage(LogCategory.SUIVI, LogLevel.ERROR,
						              String.format("Régime fiscal %s %s ignoré en raison de sa date de début (%s) postérieure à la date de fin d'activité de l'entreprise (%s).",
						                            portee,
						                            rf.getType(),
						                            StringRenderers.DATE_RENDERER.toString(rf.getDateDebut()),
						                            StringRenderers.DATE_RENDERER.toString(dateFinRegimes)));
						return false;
					}
					return true;
				})
				.peek(rf -> dateHelper.checkDateLouche(rf.getDateDebut(),
				                                       () -> String.format("Régime fiscal %s %s avec une date de début de validité",
				                                                           portee,
				                                                           rf.getType()),
				                                       LogCategory.SUIVI,
				                                       mr))
				.collect(Collectors.toMap(RegpmRegimeFiscal::getDateDebut,
				                          Collections::singletonList,
				                          (l1, l2) -> Stream.concat(l1.stream(), l2.stream()).collect(Collectors.toList())));

		// élimination des doublons de date de début, s'il en existe
		return liste.entrySet().stream()
				.map(entry -> {
					entry.getValue().subList(0, entry.getValue().size() - 1).stream()
							.forEach(regime -> mr.addMessage(LogCategory.SUIVI, LogLevel.ERROR,
							                                 String.format("Régime fiscal %s %s ignoré car il en existe un autre avec la même date de début (%s).",
							                                               portee,
							                                               regime.getType(),
							                                               StringRenderers.DATE_RENDERER.toString(regime.getDateDebut()))));
					return Pair.of(entry.getKey(), entry.getValue().get(entry.getValue().size() - 1));
				})
				.collect(Collectors.toMap(Pair::getLeft,
				                          Pair::getRight,
				                          (v1, v2) -> { throw new IllegalArgumentException("A ce stade, il ne devrait plus y avoir de dates identiques..."); },
				                          TreeMap::new));
	}

	/**
	 * @param type un type de régime fiscal côté RegPM
	 * @return le code du régime fiscal vaudois à considérer dans Unireg
	 */
	public String mapTypeRegimeFiscalVD(RegpmTypeRegimeFiscal type) {
		final RegpmTypeRegimeFiscal typeEffectif;
		switch (type) {
		case _01_ORDINAIRE:
		case _11_PARTICIPATIONS_HOLDING:
		case _12_PARTICIPATIONS_PART_IMPOSABLE:
		case _50_PLACEMENT_COLLECTIF_IMMEUBLE:
		case _60_TRANSPORTS_CONCESSIONNES:
		case _70_ORDINAIRE_ASSOCIATION_FONDATION:
		case _109_PM_AVEC_EXONERATION_ART_90G:
		case _190_PM_AVEC_EXONERATION_ART_90CEFH:
		case _709_PURE_UTILITE_PUBLIQUE:
		case _715_FONDATION_ECCLESIASTIQUE_ART_90D:
		case _719_BUTS_CULTUELS_ART_90H:
		case _729_INSTITUTIONS_DE_PREVOYANCE_ART_90I:
		case _739_CAISSES_ASSURANCES_SOCIALES_ART_90F:
		case _749_CONFEDERATION_ETAT_ETRANGER_ART_90AI:
		case _759_CANTON_ETABLISSEMENT_ART_90B:
		case _769_COMMUNE_ETABLISSEMENT_ART_90C:
		case _779_PLACEMENT_COLLECTIF_EXONERE_ART_90J:
		case _41C_SOCIETE_DE_BASE_MIXTE:
		case _42C_SOCIETE_DE_DOMICILE:
			typeEffectif = type;
			break;
		case _40_SOCIETE_DE_BASE:
			typeEffectif = RegpmTypeRegimeFiscal._41C_SOCIETE_DE_BASE_MIXTE;
			break;
		case _71_FONDATION_ECCLESIASTIQUE:
		case _72_FONDATION_PREVOYANCE:
		case _701_APM_IMPORTANTES:
		case _7020_SERVICES_ASSOCIATION_FONDATION:
		case _7032_APM_SI_SUBVENTIONNEE:
			typeEffectif = RegpmTypeRegimeFiscal._70_ORDINAIRE_ASSOCIATION_FONDATION;
			break;
		default:
			typeEffectif = RegpmTypeRegimeFiscal._01_ORDINAIRE;
			break;
		}
		return typeEffectif.getCode();
	}

	/**
	 * @param type un type de régime fiscal côté RegPM
	 * @return le code du régime fiscal fédéral à considérer dans Unireg
	 */
	public String mapTypeRegimeFiscalCH(RegpmTypeRegimeFiscal type) {
		final RegpmTypeRegimeFiscal typeEffectif;
		switch (type) {
		case _40_SOCIETE_DE_BASE:
			typeEffectif = RegpmTypeRegimeFiscal._41C_SOCIETE_DE_BASE_MIXTE;
			break;
		case _701_APM_IMPORTANTES:
		case _7020_SERVICES_ASSOCIATION_FONDATION:
		case _7032_APM_SI_SUBVENTIONNEE:
			typeEffectif = RegpmTypeRegimeFiscal._70_ORDINAIRE_ASSOCIATION_FONDATION;
			break;
		case _20_SOCIETE_DE_SERVICES:
		case _31_SOCIETE_ORDINAIRE:
		case _32_SOCIETE_ORDINAIRE_SUBVENTION:
		case _33_SOCIETE_ORDINAIRE_CARACTERE_SOCIAL:
		case _35_SOCIETE_ORDINAIRE_SIAL:
			typeEffectif = RegpmTypeRegimeFiscal._01_ORDINAIRE;
			break;
		default:
			typeEffectif = type;
			break;
		}
		return typeEffectif.getCode();
	}

	/**
	 * @param type un type de régime fiscal
	 * @return <code>true</code> si ce type correspond à un type de société immobilière
	 */
	public boolean isSocieteImmobiliere(RegpmTypeRegimeFiscal type) {
		return REGIMES_POUR_SOCIETE_IMMOBILIERE.contains(type);
	}

	/**
	 * @param type le type de régime fiscal
	 * @return si applicable, le type de flag entreprise correspondant au régime fiscal donné
	 */
	@Nullable
	public TypeFlagEntreprise getTypeFlagEntreprise(RegpmTypeRegimeFiscal type) {
		final TypeFlagEntreprise flag;
		switch (type) {
		case _20_SOCIETE_DE_SERVICES:
		case _7020_SERVICES_ASSOCIATION_FONDATION:
			flag = TypeFlagEntreprise.SOC_SERVICE;
			break;
		case _31_SOCIETE_ORDINAIRE:
			flag = TypeFlagEntreprise.SOC_IMM_ORDINAIRE;
			break;
		case _32_SOCIETE_ORDINAIRE_SUBVENTION:
			flag = TypeFlagEntreprise.SOC_IMM_SUBVENTIONNEE;
			break;
		case _33_SOCIETE_ORDINAIRE_CARACTERE_SOCIAL:
			flag = TypeFlagEntreprise.SOC_IMM_CARACTERE_SOCIAL;
			break;
		case _35_SOCIETE_ORDINAIRE_SIAL:
			flag = TypeFlagEntreprise.SOC_IMM_ACTIONNAIRES_LOCATAIRES;
			break;
		case _7032_APM_SI_SUBVENTIONNEE:
			flag = TypeFlagEntreprise.APM_SOC_IMM_SUBVENTIONNEE;
			break;
		default:
			flag = null;
			break;
		}
		return flag;
	}
}
