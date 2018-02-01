package ch.vd.unireg.tiers.manager;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.mouvement.MouvementDossier;
import ch.vd.unireg.mouvement.manager.AbstractMouvementManagerImpl;
import ch.vd.unireg.mouvement.view.MouvementDetailView;

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
	@Override
	@Transactional(readOnly = true)
	public MouvementDetailView get(Long id) throws ServiceInfrastructureException {
		final MouvementDossier mvt = getMouvementDossierDAO().get(id);
		return getView(mvt, false);
	}

}
