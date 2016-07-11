package ch.vd.uniregctb.interfaces.service.host;

import java.util.List;

import ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrative;
import ch.vd.unireg.interfaces.infra.data.TypeCollectivite;
import ch.vd.uniregctb.interfaces.service.ServiceSecuriteService;
import ch.vd.uniregctb.security.IfoSecProfil;

public class ServiceSecuriteMarshaller implements ServiceSecuriteService {
	private ServiceSecuriteService ejbClient;
	private ServiceSecuriteService restClient;
	private boolean modeRest;

	public void setEjbClient(ServiceSecuriteService ejbClient) {
		this.ejbClient = ejbClient;
	}

	public void setRestClient(ServiceSecuriteService restClient) {
		this.restClient = restClient;
	}

	public void setModeRest(boolean modeRest) {
		this.modeRest = modeRest;
	}

	@Override
	public List<CollectiviteAdministrative> getCollectivitesUtilisateur(String visaOperateur) {
		if (modeRest) {
			return restClient.getCollectivitesUtilisateur(visaOperateur);
		}
		return ejbClient.getCollectivitesUtilisateur(visaOperateur);
	}

	@Override
	public IfoSecProfil getProfileUtilisateur(String visaOperateur, int codeCollectivite) {
		if (modeRest) {
			return restClient.getProfileUtilisateur(visaOperateur,codeCollectivite);
		}
		return ejbClient.getProfileUtilisateur(visaOperateur,codeCollectivite);
	}

	@Override
	public List<Operateur> getUtilisateurs(List<TypeCollectivite> typesCollectivite) {
		if (modeRest) {
			return restClient.getUtilisateurs(typesCollectivite);
		}
		return ejbClient.getUtilisateurs(typesCollectivite);
	}

	@Override
	public Operateur getOperateur(long individuNoTechnique) {
		if (modeRest) {
			return restClient.getOperateur(individuNoTechnique);
		}
		return ejbClient.getOperateur(individuNoTechnique);
	}

	@Override
	public Operateur getOperateur(String visa) {
		if (modeRest) {
			return restClient.getOperateur(visa);
		}
		return ejbClient.getOperateur(visa);
	}
}
