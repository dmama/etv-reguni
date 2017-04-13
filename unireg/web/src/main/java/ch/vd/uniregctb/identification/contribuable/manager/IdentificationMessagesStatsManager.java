package ch.vd.uniregctb.identification.contribuable.manager;

import java.util.List;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuableCriteria;
import ch.vd.uniregctb.identification.contribuable.view.IdentificationMessagesStatsCriteriaView;
import ch.vd.uniregctb.identification.contribuable.view.IdentificationMessagesStatsResultView;

public interface IdentificationMessagesStatsManager {



	/**
	 * Alimente la vue
	 *
	 * @return
	 */
	IdentificationMessagesStatsCriteriaView getView() ;


	/**
	 * Calcule le nombre de tache selon les critères choisies
	 */
	@Transactional(readOnly = true)
	List<IdentificationMessagesStatsResultView> calculerStats(IdentificationContribuableCriteria bean);

}
