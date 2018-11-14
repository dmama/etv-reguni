package ch.vd.unireg.general.manager;

import java.util.Iterator;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.general.view.UtilisateurView;
import ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrative;
import ch.vd.unireg.interfaces.service.ServiceSecuriteService;
import ch.vd.unireg.interfaces.service.host.Operateur;

public class UtilisateurManagerImpl implements UtilisateurManager {

	private ServiceSecuriteService serviceSecuriteService;

	public ServiceSecuriteService getServiceSecuriteService() {
		return serviceSecuriteService;
	}

	public void setServiceSecuriteService(ServiceSecuriteService serviceSecuriteService) {
		this.serviceSecuriteService = serviceSecuriteService;
	}

	@Override
	public UtilisateurView get(@NotNull String visaOperateur) {

		Operateur operateur = serviceSecuriteService.getOperateur(visaOperateur);
		UtilisateurView utilisateurView = new UtilisateurView();
		utilisateurView.setVisaOperateur(visaOperateur);
		String prenomNom = "";
		if (operateur != null) {
			if (operateur.getPrenom() != null) {
				prenomNom = operateur.getPrenom();
			}
			if (operateur.getNom() != null) {
				prenomNom = prenomNom + ' ' + operateur.getNom();
			}
			utilisateurView.setPrenomNom(prenomNom);
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
