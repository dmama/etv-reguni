package ch.vd.unireg.registrefoncier.dao;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.dao.GenericDAO;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.registrefoncier.RapprochementRF;
import ch.vd.unireg.registrefoncier.TiersRF;

public interface RapprochementRFDAO extends GenericDAO<RapprochementRF, Long> {

	/**
	 * @param ctbId un numéro de contribuable
	 * @param noAutoFlush <code>true</code> s'il faut empêcher un flush de la session
	 * @return tous les appariements liés à ce contribuable, y compris ceux qui ont été annulés / modifiés...
	 */
	@NotNull
	List<RapprochementRF> findByContribuable(long ctbId, boolean noAutoFlush);

	/**
	 * @param tiersRFId un identifiant technique de tiers RF
	 * @param noAutoFlush <code>true</code> s'il faut empêcher un flush de la session
	 * @return tous les appariements liés à ce tiers RF, y compris ceux qui ont été annulés / modifiés...
	 */
	@NotNull
	List<RapprochementRF> findByTiersRF(long tiersRFId, boolean noAutoFlush);

	/**
	 * @param dateReference la date de référence des rapprochements (si <code>null</code>, on renverra tous les tiers RF qui n'ont aucun rapprochement non-annulé du tout)
	 * @return la liste des TiersRF pour lesquels il n'existe aucun rapprochement non-annulé
	 */
	@NotNull
	List<TiersRF> findTiersRFSansRapprochement(@Nullable RegDate dateReference);
}
