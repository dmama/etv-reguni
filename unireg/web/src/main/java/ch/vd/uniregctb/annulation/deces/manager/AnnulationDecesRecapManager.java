package ch.vd.uniregctb.annulation.deces.manager;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.uniregctb.annulation.deces.view.AnnulationDecesRecapView;

public interface AnnulationDecesRecapManager {

	/**
	 * Alimente la vue AnnulationDecesRecapView
	 *
	 * @param numero
	 * @return
	 */
	public AnnulationDecesRecapView get(Long numero);


	/**
	 * Persiste le rapport
	 *
	 * @param annulationDecesRecapView
	 */
	@Transactional(rollbackFor = Throwable.class)
	public void save(AnnulationDecesRecapView annulationDecesRecapView);

}
