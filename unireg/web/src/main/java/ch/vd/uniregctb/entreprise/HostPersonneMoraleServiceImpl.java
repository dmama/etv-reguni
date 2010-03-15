package ch.vd.uniregctb.entreprise;

import ch.vd.uniregctb.interfaces.model.FormeJuridique;
import ch.vd.uniregctb.interfaces.model.PersonneMorale;
import ch.vd.uniregctb.interfaces.service.ServicePersonneMoraleService;

import java.util.List;

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
		final List<FormeJuridique> formes = pm.getFormesJuridiques();
		if (formes != null && !formes.isEmpty()) {
			final FormeJuridique derniere = formes.get(formes.size() - 1);
			entrepriseView.setFormeJuridique(derniere.getCode());
		}

		return entrepriseView;

	}

}
