package ch.vd.uniregctb.annulation.couple.manager;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.annulation.couple.view.AnnulationCoupleRecapView;
import ch.vd.uniregctb.metier.MetierServiceException;

public interface AnnulationCoupleRecapManager {

	/**
	 * Alimente la vue AnnulationCoupleRecapView
	 *
	 * @param numero
	 * @return
	 */
	@Transactional(readOnly = true)
	AnnulationCoupleRecapView get(Long numero);


	/**
	 * Persiste le rapport
	 *
	 * @param annulationCoupleRecapView
	 */
	@Transactional(rollbackFor = Throwable.class)
	void save(AnnulationCoupleRecapView annulationCoupleRecapView) throws MetierServiceException;

	/**
	 *
	 */
	@Transactional(readOnly = true)
	boolean isMenageCommunAvecPrincipal(long noCtb, RegDate date);

}
