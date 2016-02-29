package ch.vd.uniregctb.webservices.securite.impl;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.webservices.securite.NiveauAutorisation;

/**
 * Classe utilitaire pour convertir des enums de <i>core</i> dans ceux de <i>web</i>.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class EnumHelper {

	public static NiveauAutorisation coreToWeb(ch.vd.uniregctb.type.Niveau niveau) {
		if (niveau == null) {
			return null;
		}

		final NiveauAutorisation value = NiveauAutorisation.fromValue(niveau.name());
		Assert.notNull(value);
		return value;
	}
}
