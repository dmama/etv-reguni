package ch.vd.uniregctb.webservices.tiers3.data;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.webservices.tiers3.Assujettissement;
import ch.vd.uniregctb.webservices.tiers3.TypeAssujettissement;
import ch.vd.uniregctb.webservices.tiers3.impl.DataHelper;

public class AssujettissementBuilder {
	public static Assujettissement newAssujettissement(ch.vd.uniregctb.metier.assujettissement.Assujettissement assujettissement, TypeAssujettissement type) {
		final Assujettissement a = new Assujettissement();
		a.setDateDebut(DataHelper.coreToWeb(assujettissement.getDateDebut()));
		a.setDateFin(DataHelper.coreToWeb(assujettissement.getDateFin()));
		a.setType(type);
		return a;
	}

	public static Assujettissement toLIC(ch.vd.uniregctb.metier.assujettissement.Assujettissement a) {

		final Assujettissement result;

		if (a instanceof ch.vd.uniregctb.metier.assujettissement.DiplomateSuisse) {
			result = null;
		}
		else if (a instanceof ch.vd.uniregctb.metier.assujettissement.HorsCanton) {
			result = newAssujettissement(a, TypeAssujettissement.LIMITE);
		}
		else if (a instanceof ch.vd.uniregctb.metier.assujettissement.HorsSuisse) {
			result = newAssujettissement(a, TypeAssujettissement.LIMITE);
		}
		else if (a instanceof ch.vd.uniregctb.metier.assujettissement.SourcierMixte) {
			ch.vd.uniregctb.metier.assujettissement.SourcierMixte mixte = (ch.vd.uniregctb.metier.assujettissement.SourcierMixte) a;
			if (mixte.getTypeAutoriteFiscale() == ch.vd.uniregctb.type.TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
				result = newAssujettissement(a, TypeAssujettissement.ILLIMITE);
			}
			else {
				result = newAssujettissement(a, TypeAssujettissement.LIMITE);
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
			result = newAssujettissement(a, TypeAssujettissement.ILLIMITE);
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

	public static Assujettissement toLIFD(ch.vd.uniregctb.metier.assujettissement.Assujettissement a) {
		final Assujettissement result;

		if (a instanceof ch.vd.uniregctb.metier.assujettissement.DiplomateSuisse) {
			result = newAssujettissement(a, TypeAssujettissement.ILLIMITE);
		}
		else if (a instanceof ch.vd.uniregctb.metier.assujettissement.HorsCanton) {
			result = null; // il sera assujetti de manière illimité dans son canton de résidence
		}
		else if (a instanceof ch.vd.uniregctb.metier.assujettissement.HorsSuisse) {
			result = newAssujettissement(a, TypeAssujettissement.LIMITE);
		}
		else if (a instanceof ch.vd.uniregctb.metier.assujettissement.SourcierMixte) {
			ch.vd.uniregctb.metier.assujettissement.SourcierMixte mixte = (ch.vd.uniregctb.metier.assujettissement.SourcierMixte) a;
			if (mixte.getTypeAutoriteFiscale() == ch.vd.uniregctb.type.TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
				result = newAssujettissement(a, TypeAssujettissement.ILLIMITE);
			}
			else {
				result = newAssujettissement(a, TypeAssujettissement.LIMITE);
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
			result = newAssujettissement(a, TypeAssujettissement.ILLIMITE);
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
