package ch.vd.unireg.interfaces.service.mock;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrative;
import ch.vd.unireg.interfaces.infra.data.TypeCollectivite;
import ch.vd.unireg.interfaces.service.ServiceSecuriteException;
import ch.vd.unireg.interfaces.service.ServiceSecuriteService;
import ch.vd.unireg.interfaces.service.host.Operateur;
import ch.vd.unireg.security.ProfileOperateur;

public class ProxyServiceSecuriteService implements ServiceSecuriteService {

	private ServiceSecuriteService target;

	public ProxyServiceSecuriteService() {
		target = new DefaultMockServiceSecurite();
	}

	public void setUp(ServiceSecuriteService target) {
		this.target = target;
	}

	@Override
	public List<CollectiviteAdministrative> getCollectivitesUtilisateur(String visaOperateur) throws ServiceSecuriteException {
		return target.getCollectivitesUtilisateur(visaOperateur);
	}

	@Override
	public Integer getCollectiviteParDefaut(@NotNull String visaOperateur) throws ServiceSecuriteException {
		return target.getCollectiviteParDefaut(visaOperateur);
	}

	@Override
	public ProfileOperateur getProfileUtilisateur(String visaOperateur, int codeCollectivite) throws ServiceSecuriteException {
		return target.getProfileUtilisateur(visaOperateur, codeCollectivite);
	}

	@Override
	public List<Operateur> getUtilisateurs(List<TypeCollectivite> typesCollectivite) throws ServiceSecuriteException {
		return target.getUtilisateurs(typesCollectivite);
	}

	@Override
	public Operateur getOperateur(long individuNoTechnique) {
		return target.getOperateur(individuNoTechnique);
	}

	@Override
	public Operateur getOperateur(@NotNull String visa) throws ServiceSecuriteException {
		return target.getOperateur(visa);
	}
}
