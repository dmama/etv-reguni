package ch.vd.uniregctb.interfaces.service.host;

import java.util.List;

import ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrativeUtilisateur;
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

	/**
	 * @return le client effectivement à utiliser
	 */
	private ServiceSecuriteService getClient() {
		return modeRest ? restClient : ejbClient;
	}

	@Override
	public List<CollectiviteAdministrativeUtilisateur> getCollectivitesUtilisateur(String visaOperateur) {
		return getClient().getCollectivitesUtilisateur(visaOperateur);
	}

	@Override
	public IfoSecProfil getProfileUtilisateur(String visaOperateur, int codeCollectivite) {
		return getClient().getProfileUtilisateur(visaOperateur, codeCollectivite);
	}

	@Override
	public List<Operateur> getUtilisateurs(List<TypeCollectivite> typesCollectivite) {
		return getClient().getUtilisateurs(typesCollectivite);
	}

	@Override
	public Operateur getOperateur(long individuNoTechnique) {
		return getClient().getOperateur(individuNoTechnique);
	}

	@Override
	public Operateur getOperateur(String visa) {
		return getClient().getOperateur(visa);
	}
}
