package ch.vd.uniregctb.security;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ch.vd.uniregctb.interfaces.service.ServiceSecuriteService;

public class IfoSecServiceImpl implements IfoSecService {

	private ServiceSecuriteService securiteService;
	private IfoSecBypass globalBypass = null;
	private final Map<String, IfoSecBypass> bypassPerUser = new HashMap<>();

	public void setSecuriteService(ServiceSecuriteService securiteService) {
		this.securiteService = securiteService;
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean isGranted(Role role, String visaOperateur, int codeCollectivite) {

		// on test les éventuels bypasses de IfoSec

		if (globalBypass != null && globalBypass.isGranted(role, codeCollectivite)) {
			return true;
		}

		final IfoSecBypass userBypass = bypassPerUser.get(visaOperateur);
		if (userBypass != null && userBypass.isGranted(role, codeCollectivite)) {
			return true;
		}

		// on test les profils normaux

		final IfoSecProfil profile = securiteService.getProfileUtilisateur(visaOperateur, codeCollectivite);
		if (profile == null) {
			// pas de profile, pas de droit
			return false;
		}

		final List<IfoSecProcedure> procedures = profile.getProcedures();
		if (procedures == null) {
			// pas de procédure, pas de droit
			return false;
		}

		for (IfoSecProcedure p : procedures) {
			String code = p.getCode();
			Role r = Role.fromIfoSec(code);
			if (r == role) {
				// c'est bon, la procédure est trouvée
				return true;
			}
		}

		// pas de procédure trouvée, pas de droit
		return false;
	}

	@Override
	public void addBypass(IfoSecBypass bypass) {
		if (bypass.getUser() == null) {
			this.globalBypass = bypass;
		}
		else {
			this.bypassPerUser.put(bypass.getUser(), bypass);
		}
	}

	@Override
	public Set<Role> getBypass(String visa) {

		Set<Role> roles = new HashSet<>();

		if (globalBypass != null) {
			 roles.addAll(globalBypass.getProcedures());
		}

		if (bypassPerUser != null) {
			final IfoSecBypass bypass = bypassPerUser.get(visa);
			if (bypass != null) {
				roles.addAll(bypass.getProcedures());
			}
		}

		return roles;
	}
}
