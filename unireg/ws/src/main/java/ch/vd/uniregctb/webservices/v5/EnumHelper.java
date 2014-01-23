package ch.vd.uniregctb.webservices.v5;

import ch.vd.unireg.ws.security.v1.AllowedAccess;
import ch.vd.uniregctb.type.Niveau;

public abstract class EnumHelper {

	public static AllowedAccess toXml(Niveau niveau) {
		if (niveau == null) {
			return AllowedAccess.NONE;
		}
		switch (niveau) {
			case ECRITURE:
				return AllowedAccess.READ_WRITE;
			case LECTURE:
				return AllowedAccess.READ_ONLY;
			default:
				throw new IllegalArgumentException("Unsupported value: " + niveau);
		}
	}
}
