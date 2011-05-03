package ch.vd.uniregctb.annulation.separation.manager;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.uniregctb.annulation.separation.view.AnnulationSeparationRecapView;
import ch.vd.uniregctb.metier.MetierServiceException;
import ch.vd.uniregctb.tiers.MenageCommun;

public interface AnnulationSeparationRecapManager {

	/**
	 * Alimente la vue AnnulationSeparationRecapView
	 *
	 * @param numero
	 * @return
	 */
	@Transactional(readOnly = true)
	AnnulationSeparationRecapView get(Long numero);


	/**
	 * Persiste le rapport
	 *
	 * @param annulationSeparationRecapView
	 */
	@Transactional(rollbackFor = Throwable.class)
	MenageCommun save(AnnulationSeparationRecapView annulationSeparationRecapView) throws MetierServiceException;


	/**
	 * @param noCtb numéro du contribuable dont on regarde les fors
	 * @return <code>true</code> si le dernier for fiscal principal du contribuable a bien été fermé pour motif "séparation", <code>false</code> sinon
	 */
	@Transactional(readOnly = true)
	boolean isDernierForFiscalPrincipalFermePourSeparation(long noCtb);
}
