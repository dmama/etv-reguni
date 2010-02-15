package ch.vd.uniregctb.annulation.separation.manager;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.uniregctb.annulation.separation.view.AnnulationSeparationRecapView;
import ch.vd.uniregctb.tiers.MenageCommun;

public interface AnnulationSeparationRecapManager {

	/**
	 * Alimente la vue AnnulationSeparationRecapView
	 *
	 * @param numero
	 * @return
	 */
	public AnnulationSeparationRecapView get(Long numero);


	/**
	 * Persiste le rapport
	 *
	 * @param annulationSeparationRecapView
	 */
	@Transactional(rollbackFor = Throwable.class)
	public MenageCommun save(AnnulationSeparationRecapView annulationSeparationRecapView);


}
