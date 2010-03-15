package ch.vd.uniregctb.tiers.manager;

import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.uniregctb.mouvement.MouvementDossier;
import ch.vd.uniregctb.mouvement.manager.AbstractMouvementManagerImpl;
import ch.vd.uniregctb.mouvement.view.MouvementDetailView;

/**
 * Classe offrant les méthodes de gestion du controller MouvementVisuController
 *
 * @author xcifde
 *
 */
public class MouvementVisuManagerImpl extends AbstractMouvementManagerImpl implements MouvementVisuManager {

	/**
	 * Charge les informations dans MouvementDetailView
	 *
	 * @param id
	 * @return un objet MouvementDetailView
	 * @throws InfrastructureException
	 */
	public MouvementDetailView get(Long id) throws InfrastructureException {
		final MouvementDossier mvt = getMouvementDossierDAO().get(id);
		return getView(mvt);
	}

}
