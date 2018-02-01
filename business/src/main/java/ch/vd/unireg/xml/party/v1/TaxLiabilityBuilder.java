package ch.vd.uniregctb.xml.party.v1;

import java.util.HashMap;
import java.util.Map;

import ch.vd.unireg.xml.party.taxresidence.v1.ExpenditureBased;
import ch.vd.unireg.xml.party.taxresidence.v1.ForeignCountry;
import ch.vd.unireg.xml.party.taxresidence.v1.Indigent;
import ch.vd.unireg.xml.party.taxresidence.v1.MixedWithholding137Par1;
import ch.vd.unireg.xml.party.taxresidence.v1.MixedWithholding137Par2;
import ch.vd.unireg.xml.party.taxresidence.v1.OrdinaryResident;
import ch.vd.unireg.xml.party.taxresidence.v1.OtherCanton;
import ch.vd.unireg.xml.party.taxresidence.v1.PureWithholding;
import ch.vd.unireg.xml.party.taxresidence.v1.SwissDiplomat;
import ch.vd.unireg.xml.party.taxresidence.v1.TaxLiability;
import ch.vd.unireg.xml.party.taxresidence.v1.Withholding;
import ch.vd.uniregctb.xml.DataHelper;
import ch.vd.uniregctb.xml.EnumHelper;

public abstract class TaxLiabilityBuilder {

	public static TaxLiability newTaxLiability(ch.vd.uniregctb.metier.assujettissement.Assujettissement assujettissement) {
		return builders.get(assujettissement.getClass()).instanciate(assujettissement);
	}

	private interface Builders {
		TaxLiability instanciate(ch.vd.uniregctb.metier.assujettissement.Assujettissement right);
	}

	private static final Map<Class, Builders> builders = new HashMap<>();

	static {
		builders.put(ch.vd.uniregctb.metier.assujettissement.DiplomateSuisse.class, right -> newSwissDiplomat((ch.vd.uniregctb.metier.assujettissement.DiplomateSuisse) right));
		builders.put(ch.vd.uniregctb.metier.assujettissement.HorsCanton.class, right -> newOtherCanton((ch.vd.uniregctb.metier.assujettissement.HorsCanton) right));
		builders.put(ch.vd.uniregctb.metier.assujettissement.HorsSuisse.class, right -> newForeignCountry((ch.vd.uniregctb.metier.assujettissement.HorsSuisse) right));
		builders.put(ch.vd.uniregctb.metier.assujettissement.Indigent.class, right -> newIndigent((ch.vd.uniregctb.metier.assujettissement.Indigent) right));
		builders.put(ch.vd.uniregctb.metier.assujettissement.SourcierMixteArt137Al1.class, right -> newMixedWithholding137Par1((ch.vd.uniregctb.metier.assujettissement.SourcierMixteArt137Al1) right));
		builders.put(ch.vd.uniregctb.metier.assujettissement.SourcierMixteArt137Al2.class, right -> newMixedWithholding137Par2((ch.vd.uniregctb.metier.assujettissement.SourcierMixteArt137Al2) right));
		builders.put(ch.vd.uniregctb.metier.assujettissement.SourcierPur.class, right -> newPureWithholding((ch.vd.uniregctb.metier.assujettissement.SourcierPur) right));
		builders.put(ch.vd.uniregctb.metier.assujettissement.VaudoisDepense.class, right -> newExpenditureBased((ch.vd.uniregctb.metier.assujettissement.VaudoisDepense) right));
		builders.put(ch.vd.uniregctb.metier.assujettissement.VaudoisOrdinaire.class, right -> newOrdinaryResident((ch.vd.uniregctb.metier.assujettissement.VaudoisOrdinaire) right));
	}

	private static SwissDiplomat newSwissDiplomat(ch.vd.uniregctb.metier.assujettissement.DiplomateSuisse right) {
		SwissDiplomat left = new SwissDiplomat();
		fillTaxLiability(left, right);
		return left;
	}

	private static OtherCanton newOtherCanton(ch.vd.uniregctb.metier.assujettissement.HorsCanton right) {
		OtherCanton left = new OtherCanton();
		fillTaxLiability(left, right);
		return left;
	}

	private static ForeignCountry newForeignCountry(ch.vd.uniregctb.metier.assujettissement.HorsSuisse right) {
		ForeignCountry left = new ForeignCountry();
		fillTaxLiability(left, right);
		return left;
	}

	private static Indigent newIndigent(ch.vd.uniregctb.metier.assujettissement.Indigent right) {
		Indigent left = new Indigent();
		fillTaxLiability(left, right);
		return left;
	}

	private static MixedWithholding137Par1 newMixedWithholding137Par1(ch.vd.uniregctb.metier.assujettissement.SourcierMixteArt137Al1 right) {
		MixedWithholding137Par1 left = new MixedWithholding137Par1();
		fillWithholding(left, right);
		return left;
	}

	private static MixedWithholding137Par2 newMixedWithholding137Par2(ch.vd.uniregctb.metier.assujettissement.SourcierMixteArt137Al2 right) {
		MixedWithholding137Par2 left = new MixedWithholding137Par2();
		fillWithholding(left, right);
		return left;
	}

	private static PureWithholding newPureWithholding(ch.vd.uniregctb.metier.assujettissement.SourcierPur right) {
		PureWithholding left = new PureWithholding();
		fillWithholding(left, right);
		return left;
	}

	private static ExpenditureBased newExpenditureBased(ch.vd.uniregctb.metier.assujettissement.VaudoisDepense right) {
		ExpenditureBased left = new ExpenditureBased();
		fillTaxLiability(left, right);
		return left;
	}

	private static OrdinaryResident newOrdinaryResident(ch.vd.uniregctb.metier.assujettissement.VaudoisOrdinaire right) {
		OrdinaryResident left = new OrdinaryResident();
		fillTaxLiability(left, right);
		return left;
	}

	private static void fillWithholding(Withholding left, ch.vd.uniregctb.metier.assujettissement.Sourcier right) {
		fillTaxLiability(left, right);
		left.setTaxationAuthority(EnumHelper.coreToXMLv1(right.getTypeAutoriteFiscalePrincipale()));
	}

	private static void fillTaxLiability(TaxLiability left, ch.vd.uniregctb.metier.assujettissement.Assujettissement right) {
		left.setDateFrom(DataHelper.coreToXMLv1(right.getDateDebut()));
		left.setDateTo(DataHelper.coreToXMLv1(right.getDateFin()));
		left.setStartReason(EnumHelper.coreToXMLv1(right.getMotifFractDebut()));
		left.setEndReason(EnumHelper.coreToXMLv1(right.getMotifFractFin()));
	}
}
