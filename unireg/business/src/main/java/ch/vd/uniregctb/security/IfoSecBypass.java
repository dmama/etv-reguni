package ch.vd.uniregctb.security;

import java.util.HashSet;
import java.util.Set;

/**
 * Informations de bypass des procédures IfoSec valable pour un utilisateur (user!=null), ou valable pour tous les utilisateurs
 * (user==null).
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class IfoSecBypass {

	private final String user;
	private final int oid;
	private final String oidSigle;
	private final Set<Role> procedures = new HashSet<Role>();

	public IfoSecBypass(int oid, String oidSigle, String procedures) {
		this.user = null;
		this.oid = oid;
		this.oidSigle = oidSigle;
		initProcedures(procedures);
	}

	public IfoSecBypass(String user, int oid, String oidSigle, String procedures) {
		this.user = user;
		this.oid = oid;
		this.oidSigle = oidSigle;
		initProcedures(procedures);
	}

	private void initProcedures(String procedures) {
		final String[] array = procedures.split(", ");
		for (String s : array) {
			final String code = s.replace("[", "").replace("]", "");
			final Role r = Role.fromIfoSec(code);
			if (r != null) {
				this.procedures.add(r);
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
