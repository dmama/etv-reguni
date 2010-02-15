package ch.vd.uniregctb.interfaces.service.mock;

import java.util.List;

import org.springframework.util.Assert;

import ch.vd.infrastructure.model.CollectiviteAdministrative;
import ch.vd.infrastructure.model.EnumTypeCollectivite;
import ch.vd.securite.model.Operateur;
import ch.vd.securite.model.ProfilOperateur;
import ch.vd.uniregctb.interfaces.service.ServiceSecuriteService;

public class ProxyServiceSecuriteService implements ServiceSecuriteService {

	private ServiceSecuriteService target;

	public ProxyServiceSecuriteService() {
		target = new DefaultMockServiceSecurite();
	}

	public void setUp(ServiceSecuriteService target) {
		Assert.notNull(target);
		this.target = target;
	}

	public void tearDown() {
		this.target = null;
	}

	public List<CollectiviteAdministrative> getCollectivitesUtilisateur(String visaOperateur) {
		return target.getCollectivitesUtilisateur(visaOperateur);
	}

	public List<ProfilOperateur> getListeOperateursPourFonctionCollectivite(String codeFonction, int noCollectivite) {
		return target.getListeOperateursPourFonctionCollectivite(codeFonction, noCollectivite);
	}

	public ProfilOperateur getProfileUtilisateur(String visaOperateur, int codeCollectivite) {
		return target.getProfileUtilisateur(visaOperateur, codeCollectivite);
	}

	public List<Operateur> getUtilisateurs(List<EnumTypeCollectivite> typesCollectivite) {
		return target.getUtilisateurs(typesCollectivite);
	}

	public Operateur getOperateur(long individuNoTechnique) {
		return target.getOperateur(individuNoTechnique);
	}

	public Operateur getOperateur(String visa) {
		return target.getOperateur(visa);
	}
}
