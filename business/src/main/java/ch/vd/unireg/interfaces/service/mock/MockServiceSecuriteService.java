package ch.vd.unireg.interfaces.service.mock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrative;
import ch.vd.unireg.interfaces.infra.data.TypeCollectivite;
import ch.vd.unireg.interfaces.service.ServiceSecuriteException;
import ch.vd.unireg.interfaces.service.ServiceSecuriteService;
import ch.vd.unireg.interfaces.service.host.Operateur;
import ch.vd.unireg.interfaces.service.host.ProcedureSecuriteImpl;
import ch.vd.unireg.interfaces.service.host.ProfileOperateurImpl;
import ch.vd.unireg.security.ProcedureSecurite;
import ch.vd.unireg.security.ProfileOperateur;
import ch.vd.unireg.security.Role;

public abstract class MockServiceSecuriteService implements ServiceSecuriteService {

	private final Map<String, Operateur> operatorsByVisa = new HashMap<>();
	private final Map<Long, Operateur> operatorsByIndividu = new HashMap<>();
	private final Map<String, ProfileOperateur> profilesOperatorByIndividu = new HashMap<>();

	public MockServiceSecuriteService() {
		init();
	}

	protected abstract void init();

	@Override
	public List<CollectiviteAdministrative> getCollectivitesUtilisateur(String visaOperateur) throws ServiceSecuriteException {
		return null;
	}

	@Override
	public Integer getCollectiviteParDefaut(@NotNull String visaOperateur) throws ServiceSecuriteException {
		return null;
	}

	@Override
	public ProfileOperateur getProfileUtilisateur(String visaOperateur, int codeCollectivite) throws ServiceSecuriteException {
		return profilesOperatorByIndividu.get(visaOperateur);
	}

	@Override
	public List<Operateur> getUtilisateurs(List<TypeCollectivite> typesCollectivite) throws ServiceSecuriteException {
		return null;
	}

	@Override
	public Operateur getOperateur(long individuNoTechnique) {
		return operatorsByIndividu.get(individuNoTechnique);
	}

	@Override
	public Operateur getOperateur(@NotNull String visa) throws ServiceSecuriteException {
		return operatorsByVisa.get(visa);
	}

	protected void addOperateur(String visa, long noIndividuOperateur, Role... roles) {

		final List<ProcedureSecurite> procedures = new ArrayList<>();
		if (roles != null) {
			for (Role r : roles) {
				procedures.add(new ProcedureSecuriteImpl(r.getIfosecCode(), null));
			}
		}

		final Operateur o = new Operateur();
		o.setCode(visa);
		o.setIndividuNoTechnique(noIndividuOperateur);
		operatorsByIndividu.put(noIndividuOperateur, o);
		operatorsByVisa.put(visa, o);
		profilesOperatorByIndividu.put(visa, new ProfileOperateurImpl(visa, procedures));
	}
}
