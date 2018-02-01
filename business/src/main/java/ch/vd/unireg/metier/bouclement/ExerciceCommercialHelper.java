package ch.vd.uniregctb.metier.bouclement;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateConstants;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.CollectionsUtils;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

/**
 * Bean utilitaire pour les fonctionnalités spécifiques aux exercices commerciaux exposés par Unireg
 */
public class ExerciceCommercialHelper {

	private final TiersService tiersService;

	public ExerciceCommercialHelper(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	/**
	 * @param entreprise une entreprise
	 * @return la liste des exercices commerciaux dont il faut tenir compte dans l'IHM et les web-services (où il ne faut pas montrer d'exercice commercial après la fin d'assujettissement)
	 */
	public List<ExerciceCommercial> getExercicesCommerciauxExposables(Entreprise entreprise) {
		final List<ExerciceCommercial> all = tiersService.getExercicesCommerciaux(entreprise);
		final RegDate dateFinExposition = getDateFinExposition(entreprise);
		if (dateFinExposition == null) {
			return all;
		}
		else {
			final List<ExerciceCommercial> exposables = new ArrayList<>(all.size());
			for (ExerciceCommercial exercice : all) {
				if (RegDateHelper.isBeforeOrEqual(exercice.getDateDebut(), dateFinExposition, NullDateBehavior.LATEST)) {
					exposables.add(exercice);
				}
			}
			return exposables;
		}
	}

	/**
	 * La date de fin à prendre en compte (pour ne pas afficher/exposer les exercices commerciaux postérieurs à cette date)
	 * @param entreprise l'entreprise concernée
	 * @return <code>null</code> si l'entreprise a un for vaudois sans date de fin, une date très lointaine dans le passé (01.08.1291 par exemple) pour
	 * les entreprises pour lesquelles il n'y a aucun for vaudois, et la date de fin du dernier for vaudois (quel qu'il soit) sinon.
	 */
	@Nullable
	private static RegDate getDateFinExposition(Entreprise entreprise) {
		// [SIFISC-24554] on va prendre la date de fin du dernier for vaudois IBC
		final List<ForFiscal> forsVaudois = entreprise.getForsFiscauxNonAnnules(false).stream()
				.filter(ff -> ff.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD)
				.filter(ff -> ff.getGenreImpot() == GenreImpot.BENEFICE_CAPITAL)
				.sorted(Comparator.comparing(ForFiscal::getDateFin, NullDateBehavior.LATEST::compare))
				.collect(Collectors.toList());

		// pas de fors vaudois IBC du tout, aucun exercice commercial
		if (forsVaudois.isEmpty()) {
			return DateConstants.EXTENDED_VALIDITY_RANGE.getDateDebut();        // vraiment très loin dans le passé...
		}

		// il faut maintenant prendre la date de fin du dernier de ces fors (ils sont triés par date de fin croissante)
		return CollectionsUtils.getLastElement(forsVaudois).getDateFin();
	}
}
