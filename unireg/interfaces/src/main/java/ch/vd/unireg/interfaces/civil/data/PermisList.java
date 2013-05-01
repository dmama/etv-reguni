package ch.vd.unireg.interfaces.civil.data;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;

public interface PermisList extends List<Permis> {

	long getNumeroIndividu();

	/**
	 * Détermine et retourne le permis valide à la date spécifiée.
	 *
	 * @param date la date de validité du permis, ou <b>null</b> pour obtenir le dernier permis valide.
	 * @return le permis valide d'un individu à une date donnée.
	 */
	Permis getPermisActif(@Nullable RegDate date);

	/**
	 * Détermine et retourne le permis annulé à la date spécifiée.
	 *
	 * @param date la date <b>d'obtention</b> du permis annulé.
	 * @return le permis annulé d'un individu qui a été obtenu à une date donnée.
	 */
	Permis getPermisAnnule(@NotNull RegDate date);
}
