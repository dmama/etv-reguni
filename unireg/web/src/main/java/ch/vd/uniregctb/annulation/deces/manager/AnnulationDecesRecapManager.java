package ch.vd.uniregctb.annulation.deces.manager;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.uniregctb.annulation.deces.view.AnnulationDecesRecapView;
import ch.vd.uniregctb.metier.MetierServiceException;

public interface AnnulationDecesRecapManager {

	/**
	 * Alimente la vue AnnulationDecesRecapView
	 */
	@Transactional(readOnly = true)
	public AnnulationDecesRecapView get(Long numero);

	/**
	 * Persiste le rapport
	 */
	@Transactional(rollbackFor = Throwable.class)
	public void save(AnnulationDecesRecapView annulationDecesRecapView) throws MetierServiceException;
}
