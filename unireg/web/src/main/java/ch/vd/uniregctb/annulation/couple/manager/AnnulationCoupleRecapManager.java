package ch.vd.uniregctb.annulation.couple.manager;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.uniregctb.annulation.couple.view.AnnulationCoupleRecapView;

public interface AnnulationCoupleRecapManager {

	/**
	 * Alimente la vue AnnulationCoupleRecapView
	 *
	 * @param numero
	 * @return
	 */
	public AnnulationCoupleRecapView get(Long numero);


	/**
	 * Persiste le rapport
	 *
	 * @param annulationCoupleRecapView
	 */
	@Transactional(rollbackFor = Throwable.class)
	public void save(AnnulationCoupleRecapView annulationCoupleRecapView);

}
