package ch.vd.uniregctb.cache;

import java.util.Collection;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.AttributeIndividu;

/**
 * Interface implémentée par le service de pré-chauffage du cache du service civil
 */
public interface ServiceCivilCacheWarmer {

	/**
	 * Va chercher les individus liés aux tiers dont les numéros sont passés en paramètres
	 *
	 * @param noTiers numéros des tiers concernés
	 * @param date    date de référence pour le cache
	 * @param includesComposantsMenage
	 * @param parties parties des individus à mémoriser dans le cache
	 */
	void warmIndividusPourTiers(Collection<Long> noTiers, @Nullable RegDate date, boolean includesComposantsMenage, AttributeIndividu... parties);

	/**
	 * Va chercher les individus dont les numéros sont passés en paramètres
	 *
	 * @param nosIndividus numéros des individus concernés
	 * @param date         date de référence pour le cache
	 * @param parties      parties des individus à mémoriser dans le cache
	 */
	void warmIndividus(Collection<Long> nosIndividus, @Nullable RegDate date, AttributeIndividu... parties);

	/**
	 * @return <b>vrai</b> si l'implémentation courante du service civil possède un cache et que ce cache est susceptible d'être chauffé avec un appel à getIndividus().
	 */
	boolean isServiceWarmable();
}
