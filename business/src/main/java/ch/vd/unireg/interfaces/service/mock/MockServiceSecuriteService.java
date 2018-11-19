package ch.vd.unireg.interfaces.service.mock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrative;
import ch.vd.unireg.interfaces.infra.data.TypeCollectivite;
import ch.vd.unireg.interfaces.service.ServiceSecuriteService;
import ch.vd.unireg.interfaces.service.host.IfoSecProcedureImpl;
import ch.vd.unireg.interfaces.service.host.IfoSecProfilImpl;
import ch.vd.unireg.interfaces.service.host.Operateur;
import ch.vd.unireg.security.IfoSecProcedure;
import ch.vd.unireg.security.IfoSecProfil;
import ch.vd.unireg.security.Role;

public abstract class MockServiceSecuriteService implements ServiceSecuriteService {

	private final Map<String, Operateur> operatorsByVisa = new HashMap<>();
	private final Map<Long, Operateur> operatorsByIndividu = new HashMap<>();
	private final Map<String, IfoSecProfil> profilesOperatorByIndividu = new HashMap<>();

	public MockServiceSecuriteService() {
		init();
	}

	protected abstract void init();

	@Override
	public List<CollectiviteAdministrative> getCollectivitesUtilisateur(String visaOperateur) {
		return null;
	}

	@Override
	public Integer getCollectiviteParDefaut(@NotNull String visaOperateur) {
		return null;
	}

	@Override
	public IfoSecProfil getProfileUtilisateur(String visaOperateur, int codeCollectivite) {
		return profilesOperatorByIndividu.get(visaOperateur);
	}

	@Override
	public List<Operateur> getUtilisateurs(List<TypeCollectivite> typesCollectivite) {
		return null;
	}

	@Override
	public Operateur getOperateur(long individuNoTechnique) {
		return operatorsByIndividu.get(individuNoTechnique);
	}

	@Override
	public Operateur getOperateur(@NotNull String visa) {
		return operatorsByVisa.get(visa);
	}

	protected void addOperateur(String visa, long noIndividuOperateur, Role... roles) {
		final Operateur o = new Operateur();
		o.setCode(visa);
		o.setIndividuNoTechnique(noIndividuOperateur);
		operatorsByIndividu.put(noIndividuOperateur, o);
		operatorsByVisa.put(visa, o);

		final IfoSecProfilImpl profile = new IfoSecProfilImpl();
		final List<IfoSecProcedure> procedures = new ArrayList<>();
		if (roles != null) {
			for (Role r : roles) {
				IfoSecProcedureImpl p = new IfoSecProcedureImpl();
				p.setCode(r.getIfosecCode());
				procedures.add(p);
			}
		}
		profile.setProcedures(procedures);
		profilesOperatorByIndividu.put(visa, profile);
	}
}
