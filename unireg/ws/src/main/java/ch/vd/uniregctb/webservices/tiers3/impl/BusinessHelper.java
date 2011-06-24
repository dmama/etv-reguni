package ch.vd.uniregctb.webservices.tiers3.impl;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;

public class BusinessHelper {

	/**
	 * @return retourne la raison sociale du débiteur spécifié.
	 */
	public static String getDebtorName(final DebiteurPrestationImposable debiteur, @Nullable RegDate date, AdresseService service) {

		if (date == null) {
			date = RegDate.get();
		}

		String raison = "";
		try {
			List<String> list = service.getNomCourrier(debiteur, date, false);
			if (!list.isEmpty()) {
				raison = list.get(0); // on ignore joyeusement une éventuelle deuxième ligne
			}
		}
		catch (AdresseException e) {
			// Si on a une exception on renvoie une raison sociale nulle
			raison = "";
		}
		return raison;
	}
}
