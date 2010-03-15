package ch.vd.uniregctb.deces.manager;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.uniregctb.deces.view.DecesRecapView;

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
	public DecesRecapView get(Long numero);


	/**
	 * Persiste le rapport
	 *
	 * @param decesRecapView
	 */
	@Transactional(rollbackFor = Throwable.class)
	public void save(DecesRecapView decesRecapView);


}
