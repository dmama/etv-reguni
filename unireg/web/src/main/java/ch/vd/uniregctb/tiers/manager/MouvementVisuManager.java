package ch.vd.uniregctb.tiers.manager;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.uniregctb.mouvement.manager.AbstractMouvementManager;
import ch.vd.uniregctb.mouvement.view.MouvementDetailView;

/**
 * Classe offrant les méthodes de gestion du controller MouvementVisuController
 *
 * @author xcifde
 *
 */
public interface MouvementVisuManager extends AbstractMouvementManager {
	/**
	 * Charge les informations dans MouvementDetailView
	 *
	 * @param numero identifiant technique du mouvement
	 * @return un objet MouvementDetailView
	 */
	@Transactional(readOnly = true)
	public MouvementDetailView get(Long numero) throws InfrastructureException;

}
