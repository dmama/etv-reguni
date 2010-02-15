package ch.vd.uniregctb.entreprise;

import ch.vd.uniregctb.interfaces.model.PersonneMorale;
import ch.vd.uniregctb.interfaces.service.ServicePersonneMoraleService;

/**
 *  Re-organisation des informations de l'entreprise pour l'affichage Web
 *
 * @author xcifde
 *
 */
public class HostPersonneMoraleServiceImpl implements HostPersonneMoraleService {

	private ServicePersonneMoraleService servicePersonneMoraleService;

	public ServicePersonneMoraleService getServicePersonneMoraleService() {
		return servicePersonneMoraleService;
	}

	public void setServicePersonneMoraleService(ServicePersonneMoraleService servicePersonneMoraleService) {
		this.servicePersonneMoraleService = servicePersonneMoraleService;
	}

	/**
	 * Alimente une vue EntrepriseView en fonction du numero d'entreprise
	 *
	 * @return un objet EntrepriseView
	 */
	public EntrepriseView get(Long numeroEntreprise) {

		EntrepriseView entrepriseView = new EntrepriseView();

		PersonneMorale pm = servicePersonneMoraleService.getPersonneMorale(numeroEntreprise);

		entrepriseView.setRaisonSociale(pm.getRaisonSociale());
		entrepriseView.setFormeJuridique(pm.getFormeJuridique().getName());

		return entrepriseView;

	}

}
