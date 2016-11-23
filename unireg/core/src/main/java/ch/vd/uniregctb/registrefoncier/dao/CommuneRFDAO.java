package ch.vd.uniregctb.registrefoncier.dao;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.dao.GenericDAO;
import ch.vd.uniregctb.registrefoncier.CommuneRF;
import ch.vd.uniregctb.registrefoncier.key.CommuneRFKey;

public interface CommuneRFDAO extends GenericDAO<CommuneRF, Long> {

	/**
	 * @param communeRFKey la clé d'une commune du registre foncier
	 * @return la commune active pour la clé spécifiée; ou <b>null</b> si aucune ne correspond.
	 */
	@Nullable
	CommuneRF findActive(@NotNull CommuneRFKey communeRFKey);
}
