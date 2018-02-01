package ch.vd.unireg.evenement.civil.engine.ech;

import org.apache.commons.lang3.mutable.Mutable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.IndividuApresEvenement;

/**
 * Stratégie de comparaison d'individu basée sur une date présente dans les données de l'individu
 */
public abstract class DateIndividuComparisonStrategy implements IndividuComparisonStrategy {

	@Override
	public boolean isFiscalementNeutre(IndividuApresEvenement originel, IndividuApresEvenement corrige, @NotNull Mutable<String> msg) {
		final RegDate dateOriginel = getDate(originel);
		final RegDate dateCorrige = getDate(corrige);
		final IndividuComparisonHelper.FieldMonitor monitor = new IndividuComparisonHelper.FieldMonitor();
		boolean neutre = true;
		if (dateOriginel != null && dateCorrige != null) {
			if (!areDatesIdentiques(dateOriginel, dateCorrige)) {
				IndividuComparisonHelper.fillMonitor(monitor, getNomAttribut());
				neutre = false;
			}
		}
		else if (dateOriginel != null || dateCorrige != null) {
			IndividuComparisonHelper.fillMonitorWithApparitionDisparition(dateOriginel == null, monitor, getNomAttribut());
			neutre = false;
		}

		if (!neutre) {
			msg.setValue(IndividuComparisonHelper.buildErrorMessage(monitor));
		}
		return neutre;
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
