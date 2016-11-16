package ch.vd.uniregctb.registrefoncier.dao;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.dao.GenericDAO;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.registrefoncier.RapprochementRF;
import ch.vd.uniregctb.registrefoncier.TiersRF;

public interface RapprochementRFDAO extends GenericDAO<RapprochementRF, Long> {

	/**
	 * @param ctbId un numéro de contribuable
	 * @return tous les appariements liés à ce contribuable, y compris ceux qui ont été annulés / modifiés...
	 */
	@NotNull
	List<RapprochementRF> findByContribuable(long ctbId);

	/**
	 * @param dateReference la date de référence des rapprochements (si <code>null</code>, on renverra tous les tiers RF qui n'ont aucun rapprochement non-annulé du tout)
	 * @return la liste des TiersRF pour lesquels il n'existe aucun rapprochement non-annulé
	 */
	@NotNull
	List<TiersRF> findTiersRFSansRapprochement(@Nullable RegDate dateReference);
}
