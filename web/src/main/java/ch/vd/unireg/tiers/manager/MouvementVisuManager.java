package ch.vd.unireg.tiers.manager;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.unireg.interfaces.infra.InfrastructureException;
import ch.vd.unireg.mouvement.manager.AbstractMouvementManager;
import ch.vd.unireg.mouvement.view.MouvementDetailView;

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
	MouvementDetailView get(Long numero) throws InfrastructureException;

}
