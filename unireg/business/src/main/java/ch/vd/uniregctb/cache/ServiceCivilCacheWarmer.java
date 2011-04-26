package ch.vd.uniregctb.cache;

import java.util.Collection;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.AttributeIndividu;

/**
 * Interface implémentée par le service de pré-chauffage du cache du service civil
 */
public interface ServiceCivilCacheWarmer {

	/**
	 * Va chercher les individus liés aux tiers dont les numéros sont passés en paramètres
	 * @param noTiers numéros des tiers concernés
	 * @param date date de référence pour le cache
	 * @param partie parties des individus à mémoriser dans le cache
	 */
	void warmIndividusPourTiers(Collection<Long> noTiers, RegDate date, AttributeIndividu... parties);
}
