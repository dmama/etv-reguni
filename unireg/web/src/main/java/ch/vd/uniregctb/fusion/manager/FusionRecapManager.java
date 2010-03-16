package ch.vd.uniregctb.fusion.manager;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.uniregctb.fusion.view.FusionRecapView;

/**
 * Methodes pour gerer FusionRecapController
 *
 * @author xcifde
 *
 */
public interface FusionRecapManager {

	/**
	 * Alimente la vue FusionRecapView
	 *
	 * @param numeroNonHabitant
	 * @param numeroHabitant
	 * @return
	 */
	public FusionRecapView get (Long numeroNonHabitant, Long numeroHabitant) ;

	/**
	 * Persiste le rapport de travail
	 * @param rapportView
	 */
	@Transactional(rollbackFor = Throwable.class)
	public void save(FusionRecapView fusionRecapView) ;
}
