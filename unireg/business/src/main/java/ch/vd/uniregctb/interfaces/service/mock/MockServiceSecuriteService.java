package ch.vd.uniregctb.interfaces.service.mock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.vd.infrastructure.model.CollectiviteAdministrative;
import ch.vd.infrastructure.model.EnumTypeCollectivite;
import ch.vd.securite.model.Operateur;
import ch.vd.securite.model.ProfilOperateur;
import ch.vd.uniregctb.interfaces.model.mock.MockOperateur;
import ch.vd.uniregctb.interfaces.service.ServiceSecuriteService;
import ch.vd.uniregctb.interfaces.service.host.IfoSecProcedureImpl;
import ch.vd.uniregctb.interfaces.service.host.IfoSecProfilImpl;
import ch.vd.uniregctb.security.IfoSecProcedure;
import ch.vd.uniregctb.security.IfoSecProfil;

public abstract class MockServiceSecuriteService implements ServiceSecuriteService {

	private final Map<String, Operateur> operatorsByVisa = new HashMap<String, Operateur>();
	private final Map<Long, Operateur> operatorsByIndividu = new HashMap<Long, Operateur>();
	private final Map<String, IfoSecProfil> profilesOperatorByIndividu = new HashMap<String, IfoSecProfil>();

	public MockServiceSecuriteService() {
		init();
	}

	protected abstract void init();

	@Override
	public List<CollectiviteAdministrative> getCollectivitesUtilisateur(String visaOperateur) {
		return null;
	}

	@Override
	public List<ProfilOperateur> getListeOperateursPourFonctionCollectivite(String codeFonction, int noCollectivite) {
		return null;
	}

	@Override
	public IfoSecProfil getProfileUtilisateur(String visaOperateur, int codeCollectivite) {
		return profilesOperatorByIndividu.get(visaOperateur);
	}

	@Override
	public List<Operateur> getUtilisateurs(List<EnumTypeCollectivite> typesCollectivite) {
		return null;
	}

	@Override
	public Operateur getOperateur(long individuNoTechnique) {
		return operatorsByIndividu.get(individuNoTechnique);
	}

	@Override
	public Operateur getOperateur(String visa) {
		return operatorsByVisa.get(visa);
	}

	protected void addOperateur(String visa, long noIndividu, String... roles) {
		final MockOperateur o = new MockOperateur(visa, noIndividu);
		operatorsByIndividu.put(noIndividu, o);
		operatorsByVisa.put(visa, o);

		final IfoSecProfilImpl profile = new IfoSecProfilImpl();
		final List<IfoSecProcedure> procedures = new ArrayList<IfoSecProcedure>();
		if (roles != null) {
			for (String r : roles) {
				IfoSecProcedureImpl p = new IfoSecProcedureImpl();
				p.setCode(r);
				procedures.add(p);
			}
		}
		profile.setProcedures(procedures);
		profilesOperatorByIndividu.put(visa, profile);
	}
}
