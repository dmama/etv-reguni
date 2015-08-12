package ch.vd.uniregctb.migration.pm.fusion;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;

/**
 * Interface de consultation des fusions de communes historiques, telles que connues dans Ref-Inf
 * (cette information n'est actuellement pas fournie par FiDoR, donc si seule la migration en a besoin, on va la construire nous-mêmes ici)
 */
public interface FusionCommunesProvider {

	/**
	 * @param noOfs le numéro OFS d'une commune suisse
	 * @param dateFusion une date de référence
	 * @return la liste des numéros OFS des communes suisses qui se sont fondues dans la commune en question à la date donnée
	 */
	@NotNull
	List<Integer> getCommunesAvant(int noOfs, @NotNull RegDate dateFusion);

	/**
	 * @param noOfs le numéro OFS d'une commune suisse
	 * @param dateDisparition une date de référence
	 * @return la liste des numéros OFS des communes suisses dans lesquelles la commune en question a été absorbée au lendemain de la date donnée
	 */
	@NotNull
	List<Integer> getCommunesApres(int noOfs, @NotNull RegDate dateDisparition);
}
