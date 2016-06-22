package ch.vd.unireg.wsclient.host.interfaces;

import ch.vd.infrastructure.model.rest.TypeCollectivite;

public class ServiceHelper {

	public static String extractCodeTypeCollectivite(TypeCollectivite[] types) {
		final StringBuilder s= new StringBuilder();
		for (int i = 0; i < types.length; i++) {
			s.append(types[i].getCodeTypeCollectivite());
			if (i < types.length-1) {
				s.append(",");
			}

		}
		return s.toString();
	}
}
