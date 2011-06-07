package ch.vd.uniregctb.tiers.manager;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureException;
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
	 * @throws ServiceInfrastructureException
	 */
	@Transactional(readOnly = true)
	public MouvementDetailView get(Long id) throws ServiceInfrastructureException {
		final MouvementDossier mvt = getMouvementDossierDAO().get(id);
		return getView(mvt, false);
	}

}
