package ch.vd.unireg.security;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

/**
 * Informations de bypass des procédures de sécurity valable pour un utilisateur (user!=null), ou valable pour tous les utilisateurs
 * (user==null).
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class SecurityBypass {

	private final String user;
	private final int oid;
	private final String oidSigle;
	private final Set<Role> procedures = EnumSet.noneOf(Role.class);

	public SecurityBypass(int oid, String oidSigle, String procedures) {
		this.user = null;
		this.oid = oid;
		this.oidSigle = oidSigle;
		initProcedures(procedures);
	}

	public SecurityBypass(String user, int oid, String oidSigle, String procedures) {
		this.user = user;
		this.oid = oid;
		this.oidSigle = oidSigle;
		initProcedures(procedures);
	}

	private void initProcedures(String procedures) {
		if ("ALL".equals(procedures)) {
			this.procedures.addAll(Arrays.asList(Role.values()));
		}
		else {
			final String[] array = procedures.split("[, ]+");
			for (String s : array) {
				final String code = s.replace("[", "").replace("]", "");
				final Role r = Role.fromCodeProcedure(code);
				if (r != null) {
					this.procedures.add(r);
				}
			}
		}
	}

	public String getUser() {
		return user;
	}

	public int getOid() {
		return oid;
	}

	public String getOidSigle() {
		return oidSigle;
	}

	public Set<Role> getProcedures() {
		return procedures;
	}

	public boolean isGranted(Role role, int codeCollectivite) {
		return oid == codeCollectivite && procedures.contains(role);
	}

}
