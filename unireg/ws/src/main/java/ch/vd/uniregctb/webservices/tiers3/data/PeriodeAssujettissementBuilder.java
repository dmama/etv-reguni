package ch.vd.uniregctb.webservices.tiers3.data;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.webservices.tiers3.PeriodeAssujettissement;
import ch.vd.uniregctb.webservices.tiers3.TypePeriodeAssujettissement;
import ch.vd.uniregctb.webservices.tiers3.impl.DataHelper;

public class PeriodeAssujettissementBuilder {
	public static PeriodeAssujettissement newPeriodeAssujettissement(ch.vd.uniregctb.metier.assujettissement.Assujettissement assujettissement, TypePeriodeAssujettissement type) {
		final PeriodeAssujettissement a = new PeriodeAssujettissement();
		a.setDateDebut(DataHelper.coreToWeb(assujettissement.getDateDebut()));
		a.setDateFin(DataHelper.coreToWeb(assujettissement.getDateFin()));
		a.setType(type);
		return a;
	}

	public static PeriodeAssujettissement toLIC(ch.vd.uniregctb.metier.assujettissement.Assujettissement a) {

		final PeriodeAssujettissement result;

		if (a instanceof ch.vd.uniregctb.metier.assujettissement.DiplomateSuisse) {
			result = null;
		}
		else if (a instanceof ch.vd.uniregctb.metier.assujettissement.HorsCanton) {
			result = newPeriodeAssujettissement(a, TypePeriodeAssujettissement.LIMITE);
		}
		else if (a instanceof ch.vd.uniregctb.metier.assujettissement.HorsSuisse) {
			result = newPeriodeAssujettissement(a, TypePeriodeAssujettissement.LIMITE);
		}
		else if (a instanceof ch.vd.uniregctb.metier.assujettissement.SourcierMixte) {
			ch.vd.uniregctb.metier.assujettissement.SourcierMixte mixte = (ch.vd.uniregctb.metier.assujettissement.SourcierMixte) a;
			if (mixte.getTypeAutoriteFiscale() == ch.vd.uniregctb.type.TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
				result = newPeriodeAssujettissement(a, TypePeriodeAssujettissement.ILLIMITE);
			}
			else {
				result = newPeriodeAssujettissement(a, TypePeriodeAssujettissement.LIMITE);
			}
		}
		else if (a instanceof ch.vd.uniregctb.metier.assujettissement.SourcierPur) {
			// un sourcier pure n'est pas assujetti au rôle ordinaire.
			result = null;
		}
		else {
			Assert.isTrue(a instanceof ch.vd.uniregctb.metier.assujettissement.VaudoisOrdinaire
					|| a instanceof ch.vd.uniregctb.metier.assujettissement.VaudoisDepense
					|| a instanceof ch.vd.uniregctb.metier.assujettissement.Indigent);
			result = newPeriodeAssujettissement(a, TypePeriodeAssujettissement.ILLIMITE);
		}

		// [UNIREG-1517] l'assujettissement courant est laissé ouvert
		if (result != null && result.getDateFin() != null) {
			final RegDate aujourdhui = RegDate.get();
			final RegDate dateFin = DataHelper.webToCore(result.getDateFin());
			if (dateFin.isAfter(aujourdhui)) {
				result.setDateFin(null);
			}
		}

		return result;
	}

	public static PeriodeAssujettissement toLIFD(ch.vd.uniregctb.metier.assujettissement.Assujettissement a) {
		final PeriodeAssujettissement result;

		if (a instanceof ch.vd.uniregctb.metier.assujettissement.DiplomateSuisse) {
			result = newPeriodeAssujettissement(a, TypePeriodeAssujettissement.ILLIMITE);
		}
		else if (a instanceof ch.vd.uniregctb.metier.assujettissement.HorsCanton) {
			result = null; // il sera assujetti de manière illimité dans son canton de résidence
		}
		else if (a instanceof ch.vd.uniregctb.metier.assujettissement.HorsSuisse) {
			result = newPeriodeAssujettissement(a, TypePeriodeAssujettissement.LIMITE);
		}
		else if (a instanceof ch.vd.uniregctb.metier.assujettissement.SourcierMixte) {
			ch.vd.uniregctb.metier.assujettissement.SourcierMixte mixte = (ch.vd.uniregctb.metier.assujettissement.SourcierMixte) a;
			if (mixte.getTypeAutoriteFiscale() == ch.vd.uniregctb.type.TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
				result = newPeriodeAssujettissement(a, TypePeriodeAssujettissement.ILLIMITE);
			}
			else {
				result = newPeriodeAssujettissement(a, TypePeriodeAssujettissement.LIMITE);
			}
		}
		else if (a instanceof ch.vd.uniregctb.metier.assujettissement.SourcierPur) {
			// un sourcier pure n'est pas assujetti au rôle ordinaire.
			result = null;
		}
		else {
			Assert.isTrue(a instanceof ch.vd.uniregctb.metier.assujettissement.VaudoisOrdinaire
					|| a instanceof ch.vd.uniregctb.metier.assujettissement.VaudoisDepense
					|| a instanceof ch.vd.uniregctb.metier.assujettissement.Indigent);
			result = newPeriodeAssujettissement(a, TypePeriodeAssujettissement.ILLIMITE);
		}

		// [UNIREG-1517] l'assujettissement courant est laissé ouvert
		if (result != null && result.getDateFin() != null) {
			final RegDate aujourdhui = RegDate.get();
			final RegDate dateFin = DataHelper.webToCore(result.getDateFin());
			if (dateFin.isAfter(aujourdhui)) {
				result.setDateFin(null);
			}
		}

		return result;
	}
}
