package ch.vd.unireg.xml.party.v4;

import ch.vd.unireg.xml.DataHelper;
import ch.vd.unireg.xml.party.taxresidence.v3.SimplifiedTaxLiability;
import ch.vd.unireg.xml.party.taxresidence.v3.SimplifiedTaxLiabilityType;

public class SimplifiedTaxLiabilityBuilder {

	public static SimplifiedTaxLiability newSimplifiedTaxLiability(ch.vd.unireg.metier.assujettissement.Assujettissement assujettissement, SimplifiedTaxLiabilityType type) {
		final SimplifiedTaxLiability a = new SimplifiedTaxLiability();
		a.setDateFrom(DataHelper.coreToXMLv2(assujettissement.getDateDebut()));
		a.setDateTo(DataHelper.coreToXMLv2(assujettissement.getDateFin()));
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
		else if (a instanceof ch.vd.unireg.metier.assujettissement.VaudoisOrdinaire
				|| a instanceof ch.vd.unireg.metier.assujettissement.VaudoisDepense
				|| a instanceof ch.vd.unireg.metier.assujettissement.Indigent) {
			result = newSimplifiedTaxLiability(a, SimplifiedTaxLiabilityType.UNLIMITED);
		}
		else {
			throw new IllegalArgumentException("Type d'assujettissement inconnu = [" + a + "]");
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
		else if (a instanceof ch.vd.unireg.metier.assujettissement.VaudoisOrdinaire
				|| a instanceof ch.vd.unireg.metier.assujettissement.VaudoisDepense
				|| a instanceof ch.vd.unireg.metier.assujettissement.Indigent) {
			result = newSimplifiedTaxLiability(a, SimplifiedTaxLiabilityType.UNLIMITED);
		}
		else {
			throw new IllegalArgumentException("Type d'assujettissement inconnu = [" + a + "]");
		}

		return result;
	}
}
