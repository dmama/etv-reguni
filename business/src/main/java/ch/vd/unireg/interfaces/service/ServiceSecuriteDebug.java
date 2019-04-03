package ch.vd.unireg.interfaces.service;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrative;
import ch.vd.unireg.interfaces.infra.data.TypeCollectivite;
import ch.vd.unireg.interfaces.service.host.Operateur;
import ch.vd.unireg.security.IfoSecBypass;
import ch.vd.unireg.security.ProfileOperateur;
import ch.vd.unireg.security.Role;

/**
 * Service de sécurité pour les développeurs qui permet de bypasser certaines procédures de sécurité.
 */
public class ServiceSecuriteDebug implements ServiceSecuriteService, ServiceSecuriteBypass {

	private IfoSecBypass globalBypass = null;
	private final Map<String, IfoSecBypass> bypassPerUser = new HashMap<>();
	private ServiceSecuriteService target;

	public void setTarget(ServiceSecuriteService target) {
		this.target = target;
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

		final Set<Role> roles = EnumSet.noneOf(Role.class);

		if (globalBypass != null) {
			roles.addAll(globalBypass.getProcedures());
		}

		final IfoSecBypass bypass = bypassPerUser.get(visa);
		if (bypass != null) {
			roles.addAll(bypass.getProcedures());
		}

		return roles;
	}

	@Override
	public boolean isGranted(@NotNull Role role, @NotNull String visaOperateur, int codeCollectivite) {

		// on test les éventuels bypasses de IfoSec

		if (globalBypass != null && globalBypass.isGranted(role, codeCollectivite)) {
			return true;
		}

		final IfoSecBypass userBypass = bypassPerUser.get(visaOperateur);
		if (userBypass != null && userBypass.isGranted(role, codeCollectivite)) {
			return true;
		}

		// on test les profils normaux
		return target.isGranted(role, visaOperateur, codeCollectivite);
	}

	@Override
	@NotNull
	public List<CollectiviteAdministrative> getCollectivitesUtilisateur(String visaOperateur) throws ServiceSecuriteException {
		return target.getCollectivitesUtilisateur(visaOperateur);
	}

	@Override
	@Nullable
	public Integer getCollectiviteParDefaut(@NotNull String visaOperateur) throws ServiceSecuriteException {
		return target.getCollectiviteParDefaut(visaOperateur);
	}

	@Override
	@Nullable
	public ProfileOperateur getProfileUtilisateur(String visaOperateur, int codeCollectivite) throws ServiceSecuriteException {
		return target.getProfileUtilisateur(visaOperateur, codeCollectivite);
	}

	@Override
	@NotNull
	public List<Operateur> getUtilisateurs(List<TypeCollectivite> typesCollectivite) throws ServiceSecuriteException {
		return target.getUtilisateurs(typesCollectivite);
	}

	@Override
	@Nullable
	public Operateur getOperateur(@NotNull String visa) throws ServiceSecuriteException {
		return target.getOperateur(visa);
	}

	@Override
	public void ping() throws ServiceSecuriteException {
		target.ping();
	}
}
