package ch.vd.uniregctb.general.manager;

import java.util.Iterator;
import java.util.List;

import ch.vd.infrastructure.model.CollectiviteAdministrative;
import ch.vd.securite.model.Operateur;
import ch.vd.uniregctb.general.view.UtilisateurView;
import ch.vd.uniregctb.interfaces.service.ServiceSecuriteService;

public class UtilisateurManagerImpl implements UtilisateurManager {

	private ServiceSecuriteService serviceSecuriteService;

	public ServiceSecuriteService getServiceSecuriteService() {
		return serviceSecuriteService;
	}

	public void setServiceSecuriteService(ServiceSecuriteService serviceSecuriteService) {
		this.serviceSecuriteService = serviceSecuriteService;
	}

	public UtilisateurView get(long noIndividuOperateur) {

		Operateur operateur = serviceSecuriteService.getOperateur(noIndividuOperateur);
		UtilisateurView utilisateurView = new UtilisateurView();
		utilisateurView.setNumeroIndividu(Long.valueOf(noIndividuOperateur));
		String prenomNom = "";
		if (operateur != null) {
			if (operateur.getPrenom() != null) {
				prenomNom = operateur.getPrenom();
			}
			if (operateur.getNom() != null) {
				prenomNom = prenomNom + " " + operateur.getNom();
			}
			utilisateurView.setPrenomNom(prenomNom);
			utilisateurView.setVisaOperateur(operateur.getCode());
			List<CollectiviteAdministrative> collectivitesAdministrative = serviceSecuriteService.getCollectivitesUtilisateur(operateur.getCode());
			Iterator<CollectiviteAdministrative> itCollectiviteAdministrative = collectivitesAdministrative.iterator();
			String officeImpot = null;
			while (itCollectiviteAdministrative.hasNext()) {
				CollectiviteAdministrative collectiviteAdministrative = itCollectiviteAdministrative.next();
				if (officeImpot != null) {
					officeImpot = officeImpot + ", " + collectiviteAdministrative.getNomCourt();
				}
				else {
					officeImpot = collectiviteAdministrative.getNomCourt();
				}
			}
			utilisateurView.setOfficeImpot(officeImpot);
		}

		return utilisateurView;
	}

}
