package ch.vd.unireg.interfaces.service.mock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrative;
import ch.vd.unireg.interfaces.infra.data.TypeCollectivite;
import ch.vd.unireg.interfaces.securite.data.OperateurImpl;
import ch.vd.unireg.interfaces.securite.data.ProcedureSecuriteImpl;
import ch.vd.unireg.interfaces.securite.data.ProfileOperateurImpl;
import ch.vd.unireg.interfaces.service.ServiceSecuriteException;
import ch.vd.unireg.interfaces.service.ServiceSecuriteService;
import ch.vd.unireg.security.Operateur;
import ch.vd.unireg.security.ProcedureSecurite;
import ch.vd.unireg.security.ProfileOperateur;
import ch.vd.unireg.security.Role;

public abstract class MockServiceSecuriteService implements ServiceSecuriteService {

	private final Map<String, Operateur> operatorsByVisa = new HashMap<>();
	private final Map<String, ProfileOperateur> profilesOperatorByIndividu = new HashMap<>();

	public MockServiceSecuriteService() {
		init();
	}

	protected abstract void init();

	@NotNull
	@Override
	public List<CollectiviteAdministrative> getCollectivitesUtilisateur(String visaOperateur) throws ServiceSecuriteException {
		return Collections.emptyList();
	}

	@Nullable
	@Override
	public Integer getCollectiviteParDefaut(@NotNull String visaOperateur) throws ServiceSecuriteException {
		return null;
	}

	@Nullable
	@Override
	public ProfileOperateur getProfileUtilisateur(String visaOperateur, int codeCollectivite) throws ServiceSecuriteException {
		return profilesOperatorByIndividu.get(visaOperateur);
	}

	@NotNull
	@Override
	public List<Operateur> getUtilisateurs(List<TypeCollectivite> typesCollectivite) throws ServiceSecuriteException {
		return Collections.emptyList();
	}

	@Nullable
	@Override
	public Operateur getOperateur(@NotNull String visa) throws ServiceSecuriteException {
		return operatorsByVisa.get(visa);
	}

	protected void addOperateur(String visa, Role... roles) {

		final List<ProcedureSecurite> procedures = new ArrayList<>();
		if (roles != null) {
			for (Role r : roles) {
				procedures.add(new ProcedureSecuriteImpl(r.getCodeProcedure(), null));
			}
		}

		final OperateurImpl o = new OperateurImpl();
		o.setCode(visa);
		operatorsByVisa.put(visa, o);
		profilesOperatorByIndividu.put(visa, new ProfileOperateurImpl(visa, procedures));
	}

	@Override
	public void ping() throws ServiceSecuriteException {
	}
}
