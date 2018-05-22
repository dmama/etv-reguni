package ch.vd.unireg.webservices.securite.impl;

import ch.vd.unireg.webservices.securite.NiveauAutorisation;

/**
 * Classe utilitaire pour convertir des enums de <i>core</i> dans ceux de <i>web</i>.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class EnumHelper {

	public static NiveauAutorisation coreToWeb(ch.vd.unireg.type.Niveau niveau) {
		if (niveau == null) {
			return null;
		}

		final NiveauAutorisation value = NiveauAutorisation.fromValue(niveau.name());
		if (value == null) {
			throw new IllegalArgumentException();
		}
		return value;
	}
}
