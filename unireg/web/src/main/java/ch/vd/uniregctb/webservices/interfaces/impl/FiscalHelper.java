package ch.vd.uniregctb.webservices.interfaces.impl;

import java.util.Set;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.date.DateRangeHelper.Range;
import ch.vd.registre.common.service.RegistreException;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

public class FiscalHelper {
	/**
	 * Recherche le menage commun actif auquel est rattaché une personne
	 *
	 * @param personne
	 *            la personne potentiellement rattachée à un ménage commun
	 * @param periode
	 * @return le ménage commun trouvé, ou null si cette personne n'est pas rattaché au ménage.
	 * @throws Exception
	 * @throws RegistreException
	 *             si plus d'un ménage commun est trouvé.
	 */
	public static MenageCommun getMenageCommunActifAt(final Contribuable personne, final Range periode) throws Exception {

		if (personne == null) {
			return null;
		}

		if(personne instanceof MenageCommun){
			return (MenageCommun)personne;
		}

		MenageCommun menageCommun = null;

		final Set<RapportEntreTiers> rapportsSujet = personne.getRapportsSujet();
		if (rapportsSujet != null) {
			for (RapportEntreTiers rapportSujet : rapportsSujet) {
				if (!rapportSujet.isAnnule() && TypeRapportEntreTiers.APPARTENANCE_MENAGE.equals(rapportSujet.getType())
						&& RegDateHelper.isBeforeOrEqual(periode.getDateDebut(), rapportSujet.getDateFin(), NullDateBehavior.LATEST)) {
					/*
					 * le rapport de l'apartenance a été trouvé, on en déduit donc le tiers ménage
					 */
					if (menageCommun != null) {
						throw new Exception("Plus d'un ménage commun trouvé pour la personne = [" + personne.toString() + "]");
					}

					menageCommun = (MenageCommun) rapportSujet.getObjet();
					// on verifie la presence d'un for principal ou secondaire sur la période

					if (!isForActifSurPeriode(menageCommun, periode)) {
						menageCommun = null;
					}
				}
			}
		}

		return menageCommun;
	}


	/**
	 * Recherche la presence d'un for actif sur une période
	 *
	 * @param contribuableUnireg
	 * @param periode
	 * @return booleen
	 */
	public static boolean isForActifSurPeriode(final ch.vd.uniregctb.tiers.Contribuable contribuableUnireg, final Range periode) {

		for (ForFiscal f : contribuableUnireg.getForsFiscaux()) {
			if (DateRangeHelper.intersect(f, periode) && !f.isAnnule()) {
				return true;
			}
		}
		return false;
	}
}
