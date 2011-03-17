package ch.vd.uniregctb.interfaces.service.mock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.vd.infrastructure.model.CollectiviteAdministrative;
import ch.vd.infrastructure.model.EnumTypeCollectivite;
import ch.vd.securite.model.Operateur;
import ch.vd.securite.model.Procedure;
import ch.vd.securite.model.ProfilOperateur;
import ch.vd.securite.model.impl.ProcedureImpl;
import ch.vd.securite.model.impl.ProfilOperateurImpl;
import ch.vd.uniregctb.interfaces.model.mock.MockOperateur;
import ch.vd.uniregctb.interfaces.service.ServiceSecuriteService;

public abstract class MockServiceSecuriteService implements ServiceSecuriteService {

	private final Map<String, Operateur> operatorsByVisa = new HashMap<String, Operateur>();
	private final Map<Long, Operateur> operatorsByIndividu = new HashMap<Long, Operateur>();
	private final Map<String, ProfilOperateur> profilesOperatorByIndividu = new HashMap<String, ProfilOperateur>();

	public MockServiceSecuriteService() {
		init();
	}

	protected abstract void init();

	public List<CollectiviteAdministrative> getCollectivitesUtilisateur(String visaOperateur) {
		return null;
	}

	public List<ProfilOperateur> getListeOperateursPourFonctionCollectivite(String codeFonction, int noCollectivite) {
		return null;
	}

	public ProfilOperateur getProfileUtilisateur(String visaOperateur, int codeCollectivite) {
		return profilesOperatorByIndividu.get(visaOperateur);
	}

	public List<Operateur> getUtilisateurs(List<EnumTypeCollectivite> typesCollectivite) {
		return null;
	}

	public Operateur getOperateur(long individuNoTechnique) {
		return operatorsByIndividu.get(Long.valueOf(individuNoTechnique));
	}

	public Operateur getOperateur(String visa) {
		return operatorsByVisa.get(visa);
	}

	protected void addOperateur(String visa, long noIndividu, String... roles) {
		final MockOperateur o = new MockOperateur(visa, noIndividu);
		operatorsByIndividu.put(Long.valueOf(noIndividu), o);
		operatorsByVisa.put(visa, o);

		final ProfilOperateurImpl profile = new ProfilOperateurImpl();
		final List<Procedure> procedures = new ArrayList<Procedure>();
		if (roles != null) {
			for (String r : roles) {
				ProcedureImpl p = new ProcedureImpl();
				p.setCode(r);
				procedures.add(p);
			}
		}
		profile.setProcedures(procedures);
		profilesOperatorByIndividu.put(visa, profile);
	}
}
