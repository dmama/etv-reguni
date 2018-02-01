package ch.vd.unireg.registrefoncier.dao;

import java.util.Collection;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.dao.GenericDAO;
import ch.vd.unireg.common.pagination.ParamPagination;
import ch.vd.unireg.registrefoncier.SituationRF;

public interface SituationRFDAO extends GenericDAO<SituationRF, Long> {

	/**
	 * @param noOfsCommunes les numéros Ofs de communes
	 * @param pagination    la pagination des résultats
	 * @return les situations non-annulées qui pointent vers les communes spécifiées (sans tenir compte des situations qui possèdent une surcharge fiscale)
	 */
	List<SituationRF> findSituationNonSurchargeesSurCommunes(@NotNull Collection<Integer> noOfsCommunes, @NotNull ParamPagination pagination);

	/**
	 * @param noOfsCommunes les numéros Ofs de communes
	 * @return le nombre de situations non-annulées qui pointent vers les communes spécifiées (sans tenir compte des situations qui possèdent une surcharge fiscale)
	 */
	int countSituationsNonSurchargeesSurCommunes(Collection<Integer> noOfsCommunes);
}
