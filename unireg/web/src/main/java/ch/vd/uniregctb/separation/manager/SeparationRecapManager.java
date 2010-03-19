package ch.vd.uniregctb.separation.manager;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.uniregctb.separation.view.SeparationRecapView;


public interface SeparationRecapManager {

	/**
	 * Alimente la vue SeparationRecapView
	 *
	 * @param numero
	 * @return
	 */
	@Transactional(readOnly = true)
	public SeparationRecapView get(Long numero);


	/**
	 * Persiste le rapport
	 *
	 * @param separationRecapView
	 */
	@Transactional(rollbackFor = Throwable.class)
	public void save(SeparationRecapView separationRecapView);

}
