package ch.vd.uniregctb.mouvement.manager;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.uniregctb.mouvement.MouvementDossier;
import ch.vd.uniregctb.mouvement.view.MouvementDetailView;

public interface AbstractMouvementManager {

	/**
	 * Point d'entrée principal pour bâtir la view détaillée pour un mouvement de dossier donné
	 *
	 * @param mvt le mouvement depuis lequel bâtir la vue
	 * @param isExtraction
	 * @return la vue
	 * @throws ch.vd.infrastructure.service.ServiceInfrastructureException
	 */
	@Transactional(readOnly = true)
	MouvementDetailView getView(MouvementDossier mvt, boolean isExtraction) throws ServiceInfrastructureException;

}
