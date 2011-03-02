package ch.vd.uniregctb.evenement.civil.interne.tutelle;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilHandlerBase;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilHandlerException;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

public abstract class AbstractTutelleHandler extends EvenementCivilHandlerBase {

	protected RapportEntreTiers getRapportTutelleOuvert(PersonnePhysique pupille, RegDate date) throws EvenementCivilHandlerException {
		
		RapportEntreTiers tutelle = null;
		int nombreRapportTutelleOuverts = 0;
		for (RapportEntreTiers rapportEntreTiers : pupille.getRapportsSujet()) {
			if ((TypeRapportEntreTiers.TUTELLE == rapportEntreTiers.getType() ||
					TypeRapportEntreTiers.CURATELLE == rapportEntreTiers.getType() ||
					TypeRapportEntreTiers.CONSEIL_LEGAL == rapportEntreTiers.getType()) &&
					RegDateHelper.isBetween(date, rapportEntreTiers.getDateDebut(), rapportEntreTiers.getDateFin(), null)) {
				tutelle = rapportEntreTiers;
				nombreRapportTutelleOuverts++;
			}
		}
		if (nombreRapportTutelleOuverts > 1)
			throw new EvenementCivilHandlerException("Plus d'un rapport tutelle, curatelle ou conseil légal actif a été trouvé");
		return tutelle;
	}
}
