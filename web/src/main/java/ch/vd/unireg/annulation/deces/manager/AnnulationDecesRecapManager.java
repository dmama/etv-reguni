package ch.vd.unireg.annulation.deces.manager;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.unireg.annulation.deces.view.AnnulationDecesRecapView;
import ch.vd.unireg.metier.MetierServiceException;

public interface AnnulationDecesRecapManager {

	/**
	 * Alimente la vue AnnulationDecesRecapView
	 */
	@Transactional(readOnly = true)
	AnnulationDecesRecapView get(Long numero);

	/**
	 * Persiste le rapport
	 */
	@Transactional(rollbackFor = Throwable.class)
	void save(AnnulationDecesRecapView annulationDecesRecapView) throws MetierServiceException;
}
