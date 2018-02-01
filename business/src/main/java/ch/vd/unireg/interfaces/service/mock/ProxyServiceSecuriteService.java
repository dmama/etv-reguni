package ch.vd.unireg.interfaces.service.mock;

import java.util.List;

import ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrativeUtilisateur;
import ch.vd.unireg.interfaces.infra.data.TypeCollectivite;
import ch.vd.unireg.interfaces.service.ServiceSecuriteService;
import ch.vd.unireg.interfaces.service.host.Operateur;
import ch.vd.unireg.security.IfoSecProfil;

public class ProxyServiceSecuriteService implements ServiceSecuriteService {

	private ServiceSecuriteService target;

	public ProxyServiceSecuriteService() {
		target = new DefaultMockServiceSecurite();
	}

	public void setUp(ServiceSecuriteService target) {
		this.target = target;
	}

	@Override
	public List<CollectiviteAdministrativeUtilisateur> getCollectivitesUtilisateur(String visaOperateur) {
		return target.getCollectivitesUtilisateur(visaOperateur);
	}

	@Override
	public IfoSecProfil getProfileUtilisateur(String visaOperateur, int codeCollectivite) {
		return target.getProfileUtilisateur(visaOperateur, codeCollectivite);
	}

	@Override
	public List<Operateur> getUtilisateurs(List<TypeCollectivite> typesCollectivite) {
		return target.getUtilisateurs(typesCollectivite);
	}

	@Override
	public Operateur getOperateur(long individuNoTechnique) {
		return target.getOperateur(individuNoTechnique);
	}

	@Override
	public Operateur getOperateur(String visa) {
		return target.getOperateur(visa);
	}
}
