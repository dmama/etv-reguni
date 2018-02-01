package ch.vd.unireg.xml.party.v1;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.unireg.xml.party.taxresidence.v1.SimplifiedTaxLiability;
import ch.vd.unireg.xml.party.taxresidence.v1.SimplifiedTaxLiabilityType;
import ch.vd.unireg.xml.DataHelper;

public class SimplifiedTaxLiabilityBuilder {
	public static SimplifiedTaxLiability newSimplifiedTaxLiability(ch.vd.unireg.metier.assujettissement.Assujettissement assujettissement, SimplifiedTaxLiabilityType type) {
		final SimplifiedTaxLiability a = new SimplifiedTaxLiability();
		a.setDateFrom(DataHelper.coreToXMLv1(assujettissement.getDateDebut()));
		a.setDateTo(DataHelper.coreToXMLv1(assujettissement.getDateFin()));
		a.setType(type);
		return a;
	}

	public static SimplifiedTaxLiability toVD(ch.vd.unireg.metier.assujettissement.Assujettissement a) {

		final SimplifiedTaxLiability result;

		if (a instanceof ch.vd.unireg.metier.assujettissement.DiplomateSuisse) {
			result = null;
		}
		else if (a instanceof ch.vd.unireg.metier.assujettissement.HorsCanton) {
			result = newSimplifiedTaxLiability(a, SimplifiedTaxLiabilityType.LIMITED);
		}
		else if (a instanceof ch.vd.unireg.metier.assujettissement.HorsSuisse) {
			result = newSimplifiedTaxLiability(a, SimplifiedTaxLiabilityType.LIMITED);
		}
		else if (a instanceof ch.vd.unireg.metier.assujettissement.SourcierMixte) {
			ch.vd.unireg.metier.assujettissement.SourcierMixte mixte = (ch.vd.unireg.metier.assujettissement.SourcierMixte) a;
			if (mixte.getTypeAutoriteFiscalePrincipale() == ch.vd.unireg.type.TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
				result = newSimplifiedTaxLiability(a, SimplifiedTaxLiabilityType.UNLIMITED);
			}
			else {
				result = newSimplifiedTaxLiability(a, SimplifiedTaxLiabilityType.LIMITED);
			}
		}
		else if (a instanceof ch.vd.unireg.metier.assujettissement.SourcierPur) {
			// un sourcier pure n'est pas assujetti au rôle ordinaire.
			result = null;
		}
		else {
			Assert.isTrue(a instanceof ch.vd.unireg.metier.assujettissement.VaudoisOrdinaire
					|| a instanceof ch.vd.unireg.metier.assujettissement.VaudoisDepense
					|| a instanceof ch.vd.unireg.metier.assujettissement.Indigent);
			result = newSimplifiedTaxLiability(a, SimplifiedTaxLiabilityType.UNLIMITED);
		}

		// [UNIREG-1517] l'assujettissement courant est laissé ouvert
		if (result != null && result.getDateTo() != null) {
			final RegDate aujourdhui = RegDate.get();
			final RegDate dateFin = DataHelper.xmlToCore(result.getDateTo());
			if (dateFin.isAfter(aujourdhui)) {
				result.setDateTo(null);
			}
		}

		return result;
	}

	public static SimplifiedTaxLiability toCH(ch.vd.unireg.metier.assujettissement.Assujettissement a) {
		final SimplifiedTaxLiability result;

		if (a instanceof ch.vd.unireg.metier.assujettissement.DiplomateSuisse) {
			result = newSimplifiedTaxLiability(a, SimplifiedTaxLiabilityType.UNLIMITED);
		}
		else if (a instanceof ch.vd.unireg.metier.assujettissement.HorsCanton) {
			result = null; // il sera assujetti de manière illimité dans son canton de résidence
		}
		else if (a instanceof ch.vd.unireg.metier.assujettissement.HorsSuisse) {
			result = newSimplifiedTaxLiability(a, SimplifiedTaxLiabilityType.LIMITED);
		}
		else if (a instanceof ch.vd.unireg.metier.assujettissement.SourcierMixte) {
			ch.vd.unireg.metier.assujettissement.SourcierMixte mixte = (ch.vd.unireg.metier.assujettissement.SourcierMixte) a;
			if (mixte.getTypeAutoriteFiscalePrincipale() == ch.vd.unireg.type.TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
				result = newSimplifiedTaxLiability(a, SimplifiedTaxLiabilityType.UNLIMITED);
			}
			else {
				result = newSimplifiedTaxLiability(a, SimplifiedTaxLiabilityType.LIMITED);
			}
		}
		else if (a instanceof ch.vd.unireg.metier.assujettissement.SourcierPur) {
			// un sourcier pure n'est pas assujetti au rôle ordinaire.
			result = null;
		}
		else {
			Assert.isTrue(a instanceof ch.vd.unireg.metier.assujettissement.VaudoisOrdinaire
					|| a instanceof ch.vd.unireg.metier.assujettissement.VaudoisDepense
					|| a instanceof ch.vd.unireg.metier.assujettissement.Indigent);
			result = newSimplifiedTaxLiability(a, SimplifiedTaxLiabilityType.UNLIMITED);
		}

		// [UNIREG-1517] l'assujettissement courant est laissé ouvert
		if (result != null && result.getDateTo() != null) {
			final RegDate aujourdhui = RegDate.get();
			final RegDate dateFin = DataHelper.xmlToCore(result.getDateTo());
			if (dateFin.isAfter(aujourdhui)) {
				result.setDateTo(null);
			}
		}

		return result;
	}
}
