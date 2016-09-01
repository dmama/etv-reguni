package ch.vd.uniregctb.general.manager;

import java.util.Iterator;
import java.util.List;

import ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrative;
import ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrativeUtilisateur;
import ch.vd.uniregctb.general.view.UtilisateurView;
import ch.vd.uniregctb.interfaces.service.ServiceSecuriteService;
import ch.vd.uniregctb.interfaces.service.host.Operateur;

public class UtilisateurManagerImpl implements UtilisateurManager {

	private ServiceSecuriteService serviceSecuriteService;

	public ServiceSecuriteService getServiceSecuriteService() {
		return serviceSecuriteService;
	}

	public void setServiceSecuriteService(ServiceSecuriteService serviceSecuriteService) {
		this.serviceSecuriteService = serviceSecuriteService;
	}

	@Override
	public UtilisateurView get(long noIndividuOperateur) {

		Operateur operateur = serviceSecuriteService.getOperateur(noIndividuOperateur);
		UtilisateurView utilisateurView = new UtilisateurView();
		utilisateurView.setNumeroIndividu(noIndividuOperateur);
		String prenomNom = "";
		if (operateur != null) {
			if (operateur.getPrenom() != null) {
				prenomNom = operateur.getPrenom();
			}
			if (operateur.getNom() != null) {
				prenomNom = prenomNom + ' ' + operateur.getNom();
			}
			utilisateurView.setPrenomNom(prenomNom);
			utilisateurView.setVisaOperateur(operateur.getCode());
			List<CollectiviteAdministrativeUtilisateur> collectivitesAdministrative = serviceSecuriteService.getCollectivitesUtilisateur(operateur.getCode());
			Iterator<CollectiviteAdministrativeUtilisateur> itCollectiviteAdministrative = collectivitesAdministrative.iterator();
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
