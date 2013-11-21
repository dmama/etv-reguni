package ch.vd.uniregctb.metier.piis;

import java.util.List;

import ch.vd.uniregctb.metier.assujettissement.AssujettissementException;
import ch.vd.uniregctb.tiers.PersonnePhysique;

/**
 * Service de calcul des périodes d'imposition pour l'impôt source
 */
public interface PeriodeImpositionImpotSourceService {

	/**
	 * Calcul des périodes d'imposition de l'impôt source pour la personne physique indiquée
	 * @param pp personne physique
	 * @return liste des périodes d'imposition de l'impôt source calculées
	 */
	List<PeriodeImpositionImpotSource> determine(PersonnePhysique pp);
}
