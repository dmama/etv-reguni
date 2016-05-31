package ch.vd.uniregctb.deces.manager;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.uniregctb.deces.view.DecesRecapView;
import ch.vd.uniregctb.metier.MetierServiceException;

/**
 *
 * @author xcifde
 *
 */
public interface DecesRecapManager {

	/**
	 * Alimente la vue DecesRecapView
	 *
	 * @param numero
	 * @return
	 */
	@Transactional(readOnly = true)
	DecesRecapView get(Long numero);


	/**
	 * Persiste le rapport
	 *
	 * @param decesRecapView
	 */
	@Transactional(rollbackFor = Throwable.class)
	void save(DecesRecapView decesRecapView) throws MetierServiceException;

	/**
	 * @param numeroCtb numéro de tiers du contribuable dont on veut savoir s'il est décédé
	 * @return <code>true</code> si le contribuable est une personne physique décédée, <code>false</code> sinon
	 */
	@Transactional(readOnly = true)
	boolean isDecede(long numeroCtb);
}
