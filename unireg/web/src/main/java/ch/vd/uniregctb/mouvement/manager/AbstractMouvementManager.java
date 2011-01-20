package ch.vd.uniregctb.mouvement.manager;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.uniregctb.mouvement.MouvementDossier;
import ch.vd.uniregctb.mouvement.view.MouvementDetailView;

public interface AbstractMouvementManager {

	/**
	 * Point d'entrée principal pour bâtir la view détaillée pour un mouvement de dossier donné
	 * @param mvt le mouvement depuis lequel bâtir la vue
	 * @return la vue
	 * @throws ch.vd.infrastructure.service.InfrastructureException
	 */
	@Transactional(readOnly = true)
	MouvementDetailView getView(MouvementDossier mvt) throws InfrastructureException;

}
