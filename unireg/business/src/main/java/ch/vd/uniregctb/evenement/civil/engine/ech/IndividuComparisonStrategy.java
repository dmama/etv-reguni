package ch.vd.uniregctb.evenement.civil.engine.ech;

import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.interfaces.civil.data.IndividuApresEvenement;
import ch.vd.uniregctb.common.DataHolder;

/**
 * Stratégie de comparaison entre deux individus, utilisée lors du traitement d'un événement civil eCH de correction
 */
public interface IndividuComparisonStrategy {

	/**
	 * @param originel Etat primitif de l'individu civil
	 * @param corrige Etat corrigé de l'individu civil
	 * @param msg (out) dans le cas où une différence significative est trouvée, le nom de l'attribut qui est différent (date de naissance, adresse de résidence...)
	 * @return <code>true</code> si aucune différence fiscalement importante n'est à signaler, <code>false</code> s'il y en a au moins une, au contraire (auquel cas le paramètre msg doit être renseigné)
	 */
	boolean isFiscalementNeutre(IndividuApresEvenement originel, IndividuApresEvenement corrige, @NotNull DataHolder<String> msg);
}
