package ch.vd.unireg.metier.assujettissement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.infra.data.TypeRegimeFiscal;
import ch.vd.unireg.common.CollectionsUtils;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.metier.bouclement.ExerciceCommercial;
import ch.vd.unireg.parametrage.ParametreAppService;
import ch.vd.unireg.regimefiscal.RegimeFiscalConsolide;
import ch.vd.unireg.regimefiscal.RegimeFiscalService;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.FlagEntreprise;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.type.CategorieEntreprise;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.type.TypeContribuable;
import ch.vd.unireg.type.TypeDocument;
import ch.vd.unireg.type.TypeFlagEntreprise;

/**
 * Calculateur des périodes d'imposition des entreprises
 */
public class PeriodeImpositionPersonnesMoralesCalculator implements PeriodeImpositionCalculator<Entreprise> {

	private final ParametreAppService parametreService;
	private final TiersService tiersService;
	private final RegimeFiscalService regimeFiscalService;

	public PeriodeImpositionPersonnesMoralesCalculator(ParametreAppService parametreService, TiersService tiersService, RegimeFiscalService regimeFiscalService) {
		this.parametreService = parametreService;
		this.tiersService = tiersService;
		this.regimeFiscalService = regimeFiscalService;
	}

	/**
	 * Point d'entrée du calcul des périodes d'imposition pour les contribuables assimilés "personne morale"
	 * @param entreprise contribuable cible
	 * @param assujettissements les assujettissements du contribuable, calculés par ailleurs
	 * @return la liste des périodes d'imposition du contribuable
	 */
	@NotNull
	@Override
	public List<PeriodeImposition> determine(Entreprise entreprise, List<Assujettissement> assujettissements) throws AssujettissementException {

		// si pas d'assujettissement, pas de période d'imposition
		if (assujettissements == null || assujettissements.isEmpty()) {
			return Collections.emptyList();
		}

		// liste à retourner
		final List<PeriodeImposition> resultat = new LinkedList<>();

		// il faut au moins découper l'assujettissement en périodes correspondants aux exercices commerciaux
		// (mais on ne calcule pas les périodes d'assujettissement depuis la nuit des temps, seulement depuis une année extraite des paramètres)
		final int premiereAnneePeriodesImposition = parametreService.getPremierePeriodeFiscalePersonnesMorales();
		final List<ExerciceCommercial> exercicesBruts = tiersService.getExercicesCommerciaux(entreprise);
		final List<ExerciceCommercial> exercices = extraireDepuisPeriode(exercicesBruts, premiereAnneePeriodesImposition);
		for (ExerciceCommercial exercice : exercices) {
			for (Assujettissement assujettissement : assujettissements) {
				final DateRange intersection = DateRangeHelper.intersection(exercice, assujettissement);
				if (intersection != null) {

					// détermination de la cause de fermeture de l'assujettissement
					final PeriodeImposition.CauseFermeture causeFermeture;
					if (intersection.getDateFin() == assujettissement.getDateFin()) {
						if ((assujettissement.getMotifFractFin() == MotifAssujettissement.VENTE_IMMOBILIER || assujettissement.getMotifFractFin() == MotifAssujettissement.FIN_EXPLOITATION) && assujettissement instanceof HorsSuisse) {
							causeFermeture = PeriodeImposition.CauseFermeture.FIN_ASSUJETTISSEMENT_HS;
						}
						else {
							causeFermeture = PeriodeImposition.CauseFermeture.AUTRE;
						}
					}
					else {
						causeFermeture = null;
					}

					// détermination du type de contribuable et du type de document
					final TypeContribuable typeContribuable = computeTypeContribuable(entreprise, intersection.getDateFin(), assujettissement);
					final TypeDocument typeDocument = computeTypeDocument(entreprise, intersection.getDateFin());
					final CategorieEntreprise categorieEntreprise = getLastKnownCategorieEntrepriseAtOrBefore(entreprise, intersection.getDateFin());
					final TypeAutoriteFiscale typeAutoriteFiscalePrincipale = computeTypeAutoriteFiscalePrincipale(assujettissement);

					// OBSOLETE (supprimé) - [SIFISC-17721] sur les DP/APM, les déclarations sont optionnelles --> selon régime fiscal, voir ci-dessous.

					// Cas des 190-2 et 739 vaudois qui doivent être optionels
					final TypeRegimeFiscal typeRegimeFiscalVD = regimeFiscalService.getTypeRegimeFiscalVD(entreprise, exercice.getDateFin());
					final boolean isOptionnelle = typeContribuable == TypeContribuable.VAUDOIS_ORDINAIRE && regimeFiscalService.isRegimeFiscalDiOptionnelleVd(typeRegimeFiscalVD);

					// création de la structure pour la période d'imposition
					resultat.add(new PeriodeImpositionPersonnesMorales(intersection.getDateDebut(),
					                                                   intersection.getDateFin(),
					                                                   entreprise,
					                                                   isOptionnelle,
					                                                   false,
					                                                   causeFermeture,
					                                                   null,
					                                                   exercices,
					                                                   typeContribuable,
					                                                   typeDocument,
					                                                   categorieEntreprise,
					                                                   typeAutoriteFiscalePrincipale));
				}
			}
		}

		return DateRangeHelper.collate(resultat);
	}

	/**
	 * @param entreprise entreprise
	 * @param dateReference date de référence
	 * @return la dernière catégorie d'entreprise connue avant ou à la date de référence donnée
	 */
	@Nullable
	private CategorieEntreprise getLastKnownCategorieEntrepriseAtOrBefore(Entreprise entreprise, @NotNull RegDate dateReference) {
		final List<RegimeFiscalConsolide> regimesFiscaux = regimeFiscalService.getRegimesFiscauxVDNonAnnulesTrie(entreprise);
		if (regimesFiscaux.isEmpty()) {
			return null;
		}

		for (RegimeFiscalConsolide regime : CollectionsUtils.revertedOrder(regimesFiscaux)) {
			if (RegDateHelper.isAfterOrEqual(dateReference, regime.getDateDebut(), NullDateBehavior.EARLIEST)) {
				return regime.getCategorie();
			}
		}
		return null;
	}

	private static TypeContribuable computeTypeContribuable(Entreprise pm, RegDate dateFinPeriode, Assujettissement assujettissement) {
		// [SIFISC-18113] séparation des entreprise LIASF dans une population différente
		final FlagEntreprise flag = DateRangeHelper.rangeAt(pm.getFlagsNonAnnulesTries(), dateFinPeriode);
		if (flag != null && flag.getType() == TypeFlagEntreprise.UTILITE_PUBLIQUE) {
			return TypeContribuable.UTILITE_PUBLIQUE;
		}

		switch (assujettissement.getType()) {
		case HORS_CANTON:
			return TypeContribuable.HORS_CANTON;
		case HORS_SUISSE:
			return TypeContribuable.HORS_SUISSE;
		case VAUDOIS_ORDINAIRE:
			return TypeContribuable.VAUDOIS_ORDINAIRE;
		default:
			throw new IllegalArgumentException("Type d'assujettissement PM non-supporté dans le calculateur de périodes d'imposition : " + assujettissement.getType());
		}
	}

	private static TypeAutoriteFiscale computeTypeAutoriteFiscalePrincipale(Assujettissement assujettissement) {
		switch (assujettissement.getType()) {
		case HORS_CANTON:
			return TypeAutoriteFiscale.COMMUNE_HC;
		case HORS_SUISSE:
			return TypeAutoriteFiscale.PAYS_HS;
		case VAUDOIS_ORDINAIRE:
			return TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD;
		default:
			throw new IllegalArgumentException("Type d'assujettissement PM non-supporté dans le calculateur de périodes d'imposition : " + assujettissement.getType());
		}
	}

	@Nullable
	private TypeDocument computeTypeDocument(Entreprise pm, RegDate dateFinPeriode) throws AssujettissementException {

		// Avant cette année-là, Unireg n'enverra jamais de déclaration, donc au final le type de
		// document n'a aucune importance... De plus, on se heurte-là à des problèmes de données issues
		// de RegPM
		if (dateFinPeriode.year() < parametreService.getPremierePeriodeFiscaleDeclarationsPersonnesMorales()) {
			return null;
		}

		final CategorieEntreprise categorie = getLastKnownCategorieEntrepriseAtOrBefore(pm, dateFinPeriode);
		if (categorie == null) {
			throw new AssujettissementException(String.format("Impossible de déterminer la catégorie de l'entreprise %s au %s.",
			                                                  FormatNumeroHelper.numeroCTBToDisplay(pm.getNumero()),
			                                                  RegDateHelper.dateToDisplayString(dateFinPeriode)));
		}

		switch (categorie) {
		case APM:
			return TypeDocument.DECLARATION_IMPOT_APM_BATCH;
		case PM:
		case AUTRE:
			return TypeDocument.DECLARATION_IMPOT_PM_BATCH;
		case SP:
			return null;            // TODO si une SP est ici, c'est qu'elle est assujettie...
		default:
			throw new AssujettissementException("Type de catégorie d'entreprise non-supportée dans le calculateur de périodes d'imposition des personnes morales : " + categorie);
		}
	}

	/**
	 * @param bruts une liste d'exercices commerciaux
	 * @param premierePeriode une année
	 * @return la sous-liste des exercices commerciaux fournis dont la date de fin est au plus tôt dans l'année fournie
	 */
	@NotNull
	private static List<ExerciceCommercial> extraireDepuisPeriode(List<ExerciceCommercial> bruts, int premierePeriode) {
		if (bruts == null || bruts.isEmpty()) {
			return Collections.emptyList();
		}
		final List<ExerciceCommercial> nets = new ArrayList<>(bruts.size());
		for (ExerciceCommercial ex : bruts) {
			if (ex.getDateFin().year() >= premierePeriode) {
				nets.add(ex);
			}
		}
		return nets;
	}
}
