package ch.vd.uniregctb.interfaces.service.mock;

import java.util.List;

import ch.vd.infrastructure.model.CollectiviteAdministrative;
import ch.vd.infrastructure.model.EnumTypeCollectivite;
import ch.vd.securite.model.Operateur;
import ch.vd.uniregctb.interfaces.service.ServiceSecuriteService;
import ch.vd.uniregctb.security.IfoSecProfil;

public class ProxyServiceSecuriteService implements ServiceSecuriteService {

	private ServiceSecuriteService target;

	public ProxyServiceSecuriteService() {
		target = new DefaultMockServiceSecurite();
	}

	public void setUp(ServiceSecuriteService target) {
		this.target = target;
	}

	@Override
	public List<CollectiviteAdministrative> getCollectivitesUtilisateur(String visaOperateur) {
		return target.getCollectivitesUtilisateur(visaOperateur);
	}

	@Override
	public IfoSecProfil getProfileUtilisateur(String visaOperateur, int codeCollectivite) {
		return target.getProfileUtilisateur(visaOperateur, codeCollectivite);
	}

	@Override
	public List<Operateur> getUtilisateurs(List<EnumTypeCollectivite> typesCollectivite) {
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
