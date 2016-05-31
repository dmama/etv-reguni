package ch.vd.uniregctb.tiers;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateConstants;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.metier.assujettissement.Assujettissement;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementException;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementService;
import ch.vd.uniregctb.metier.bouclement.ExerciceCommercial;

/**
 * Bean utilitaire pour les fonctionnalités spécifiques aux exercices commerciaux dans l'IHM Unireg
 */
public class ExerciceCommercialWebHelper {

	private final TiersService tiersService;
	private final AssujettissementService assujettissementService;

	public ExerciceCommercialWebHelper(TiersService tiersService, AssujettissementService assujettissementService) {
		this.tiersService = tiersService;
		this.assujettissementService = assujettissementService;
	}

	/**
	 * @param entreprise une entreprise
	 * @return la liste des exercices commerciaux dont il faut tenir compte dans l'IHM (où il ne faut pas montrer d'exercice commercial après la fin d'assujettissement)
	 */
	public List<ExerciceCommercial> getExercicesCommerciauxAffichables(Entreprise entreprise) {
		final List<ExerciceCommercial> all = tiersService.getExercicesCommerciaux(entreprise);
		final RegDate dateFinAssujettissement = getDateFinAssujettissement(entreprise);
		if (dateFinAssujettissement == null) {
			return all;
		}
		else {
			final List<ExerciceCommercial> affichables = new ArrayList<>(all.size());
			for (ExerciceCommercial exercice : all) {
				if (RegDateHelper.isBeforeOrEqual(exercice.getDateDebut(), dateFinAssujettissement, NullDateBehavior.LATEST)) {
					affichables.add(exercice);
				}
			}
			return affichables;
		}
	}

	/**
	 * La date de fin d'assujettissment à prendre en compte (pour ne pas afficher les exercices commerciaux postérieurs à cette date)
	 * @param entreprise l'entreprise concernée
	 * @return <code>null</code> si l'entreprise est toujours assujettie, une date très lointaine dans le passé (01.08.1291 par exemple) pour
	 * les entreprises pour lesquelles on ne calcule aucun assujettissement, et la date de fin du dernier assujettissement (quel qu'il soit) sinon.
	 */
	@Nullable
	private RegDate getDateFinAssujettissement(Entreprise entreprise) {
		try {
			final List<Assujettissement> assujettissements = assujettissementService.determine(entreprise);
			if (assujettissements == null || assujettissements.isEmpty()) {
				return DateConstants.EXTENDED_VALIDITY_RANGE.getDateDebut();        // vraiement très loin dans le passé...
			}

			return assujettissements.get(assujettissements.size() - 1).getDateFin();
		}
		catch (AssujettissementException e) {
			// problème lors du calcul, pas plus grave que ça, pour l'utilisation qu'on en a, de toute façon
			return null;
		}
	}
}
