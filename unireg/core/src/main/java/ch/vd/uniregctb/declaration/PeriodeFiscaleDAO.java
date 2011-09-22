package ch.vd.uniregctb.declaration;

import java.util.List;

import ch.vd.registre.base.dao.GenericDAO;

public interface PeriodeFiscaleDAO extends GenericDAO<PeriodeFiscale, Long> {

	/**
	 * Renvoie toutes les periodes fiscales triée par orde decroissant
	 *
	 * @return une liste de PeriodeFiscale
	 */
	public List<PeriodeFiscale> getAllDesc();

	/**
	 * Renvoie la période fiscale de l'année demandée.
	 * <p>
	 * <p>
	 * <b>Attention !</b> cette méthode ne "voit" pas les modifications "en mémoire" (= non flushées) de la session courante. Dans le cas où
	 * ces modifications sont nécessaires, il est faudra flusher la session à la main.
	 * <p>
	 *
	 * @return PeriodeFiscale la période fiscale demandée
	 */
	public PeriodeFiscale getPeriodeFiscaleByYear(final int year);
}
