package ch.vd.uniregctb.tiers.manager;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureException;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.TiersCriteria.TypeTiers;
import ch.vd.uniregctb.tiers.view.TiersCriteriaView;

/**
 * Classe permettant la gestion du controller TiersListController
 *
 * @author xcifde
 *
 */
public class TiersListManagerImpl implements TiersListManager {

	private ServiceInfrastructureService serviceInfrastructureService;

	/**
	 * Initialise les champs avec les valeurs des paramètres
	 *
	 * @param tiersCriteriaView
	 * @param typeRecherche
	 * @param numero
	 * @param nomRaison
	 * @param localiteOuPays
	 * @param noOfsFor
	 * @param dateNaissance
	 * @param numeroAssureSocial
	 * @throws ServiceInfrastructureException
	 * @throws NumberFormatException
	 */
	@Override
	public void initFieldsWithParams(	TiersCriteriaView tiersCriteriaView,
										TypeTiers typeTiers,
										String numero,
										String nomRaison,
										String localiteOuPays,
										String noOfsFor,
										RegDate dateNaissance,
										String numeroAssureSocial)
										throws NumberFormatException, ServiceInfrastructureException {

		if (typeTiers != null) {
			tiersCriteriaView.setTypeTiers(typeTiers);
		}
		tiersCriteriaView.setNumeroFormatte(numero);
		tiersCriteriaView.setNomRaison(nomRaison);
		tiersCriteriaView.setLocaliteOuPays(localiteOuPays);
		if ((noOfsFor != null) && (!"".equals(noOfsFor))) {
			tiersCriteriaView.setNoOfsFor(noOfsFor);
			final Commune commune = serviceInfrastructureService.getCommuneByNumeroOfsEtendu(Integer.valueOf(noOfsFor), null);
			tiersCriteriaView.setForAll(commune.getNomMinuscule());
		}
		tiersCriteriaView.setDateNaissance(dateNaissance);
		tiersCriteriaView.setNumeroAVS(numeroAssureSocial);
	}

	public ServiceInfrastructureService getServiceInfrastructureService() {
		return serviceInfrastructureService;
	}

	public void setServiceInfrastructureService(ServiceInfrastructureService serviceInfrastructureService) {
		this.serviceInfrastructureService = serviceInfrastructureService;
	}

}
