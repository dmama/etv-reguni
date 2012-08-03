package ch.vd.uniregctb.evenement.civil.engine.ech;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.IndividuApresEvenement;
import ch.vd.uniregctb.common.DataHolder;

/**
 * Stratégie de comparaison d'individu basée sur une date présente dans les données de l'individu
 */
public abstract class DateIndividuComparisonStrategy implements IndividuComparisonStrategy {

	@Override
	public boolean sansDifferenceFiscalementImportante(IndividuApresEvenement originel, IndividuApresEvenement corrige, @NotNull DataHolder<String> msg) {
		final RegDate dateOriginel = getDate(originel);
		final RegDate dateCorrige = getDate(corrige);
		if (!areDatesIdentiques(dateOriginel, dateCorrige)) {
			msg.set(getNomAttribut());
			return false;
		}
		return true;
	}

	/**
	 * A surcharger si on veut un jour ne pas avoir une comparaison stricte entre les deux dates
	 * @param dateOriginelle date de l'individu avant correction
	 * @param dateCorrigee date de l'individu après correction
	 * @return si oui ou non les deux dates sont à considérer comme identiques
	 */
	protected boolean areDatesIdentiques(@Nullable RegDate dateOriginelle, @Nullable RegDate dateCorrigee) {
		return dateOriginelle == dateCorrigee;
	}

	/**
	 * Extraction de la date importante des données de l'individu
	 * @param individu individu dont il faut extraire la donnée
	 * @return date importante, pour cette stratégie, extraite de l'individu
	 */
	@Nullable
	protected abstract RegDate getDate(IndividuApresEvenement individu);

	/**
	 * @return Le message d'explication sur la différence
	 */
	@NotNull
	protected abstract String getNomAttribut();
}
