package ch.vd.unireg.fusion.manager;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.unireg.fusion.view.FusionRecapView;

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
	@Transactional(readOnly = true)
	FusionRecapView get(Long numeroNonHabitant, Long numeroHabitant) ;

	/**
	 * Persiste le rapport de travail
	 * @param rapportView
	 */
	@Transactional(rollbackFor = Throwable.class)
	void save(FusionRecapView fusionRecapView) ;
}
