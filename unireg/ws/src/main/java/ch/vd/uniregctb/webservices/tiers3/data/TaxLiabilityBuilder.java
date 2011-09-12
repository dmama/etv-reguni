package ch.vd.uniregctb.webservices.tiers3.data;

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
import ch.vd.uniregctb.webservices.tiers3.impl.DataHelper;
import ch.vd.uniregctb.webservices.tiers3.impl.EnumHelper;

public abstract class TaxLiabilityBuilder {

	public static TaxLiability newTaxLiability(ch.vd.uniregctb.metier.assujettissement.Assujettissement assujettissement) {
		return builders.get(assujettissement.getClass()).instanciate(assujettissement);
	}

	private interface Builders {
		TaxLiability instanciate(ch.vd.uniregctb.metier.assujettissement.Assujettissement right);
	}

	private static Map<Class, Builders> builders = new HashMap<Class, Builders>();

	static {
		builders.put(ch.vd.uniregctb.metier.assujettissement.DiplomateSuisse.class, new Builders() {
			@Override
			public TaxLiability instanciate(ch.vd.uniregctb.metier.assujettissement.Assujettissement right) {
				return newSwissDiplomat((ch.vd.uniregctb.metier.assujettissement.DiplomateSuisse) right);
			}
		});
		builders.put(ch.vd.uniregctb.metier.assujettissement.HorsCanton.class, new Builders() {
			@Override
			public TaxLiability instanciate(ch.vd.uniregctb.metier.assujettissement.Assujettissement right) {
				return newOtherCanton((ch.vd.uniregctb.metier.assujettissement.HorsCanton) right);
			}
		});
		builders.put(ch.vd.uniregctb.metier.assujettissement.HorsSuisse.class, new Builders() {
			@Override
			public TaxLiability instanciate(ch.vd.uniregctb.metier.assujettissement.Assujettissement right) {
				return newForeignCountry((ch.vd.uniregctb.metier.assujettissement.HorsSuisse) right);
			}
		});
		builders.put(ch.vd.uniregctb.metier.assujettissement.Indigent.class, new Builders() {
			@Override
			public TaxLiability instanciate(ch.vd.uniregctb.metier.assujettissement.Assujettissement right) {
				return newIndigent((ch.vd.uniregctb.metier.assujettissement.Indigent) right);
			}
		});
		builders.put(ch.vd.uniregctb.metier.assujettissement.SourcierMixteArt137Al1.class, new Builders() {
			@Override
			public TaxLiability instanciate(ch.vd.uniregctb.metier.assujettissement.Assujettissement right) {
				return newMixedWithholding137Par1((ch.vd.uniregctb.metier.assujettissement.SourcierMixteArt137Al1) right);
			}
		});
		builders.put(ch.vd.uniregctb.metier.assujettissement.SourcierMixteArt137Al2.class, new Builders() {
			@Override
			public TaxLiability instanciate(ch.vd.uniregctb.metier.assujettissement.Assujettissement right) {
				return newMixedWithholding137Par2((ch.vd.uniregctb.metier.assujettissement.SourcierMixteArt137Al2) right);
			}
		});
		builders.put(ch.vd.uniregctb.metier.assujettissement.SourcierPur.class, new Builders() {
			@Override
			public TaxLiability instanciate(ch.vd.uniregctb.metier.assujettissement.Assujettissement right) {
				return newPureWithholding((ch.vd.uniregctb.metier.assujettissement.SourcierPur) right);
			}
		});
		builders.put(ch.vd.uniregctb.metier.assujettissement.VaudoisDepense.class, new Builders() {
			@Override
			public TaxLiability instanciate(ch.vd.uniregctb.metier.assujettissement.Assujettissement right) {
				return newExpenditureBased((ch.vd.uniregctb.metier.assujettissement.VaudoisDepense) right);
			}
		});
		builders.put(ch.vd.uniregctb.metier.assujettissement.VaudoisOrdinaire.class, new Builders() {
			@Override
			public TaxLiability instanciate(ch.vd.uniregctb.metier.assujettissement.Assujettissement right) {
				return newOrdinaryResident((ch.vd.uniregctb.metier.assujettissement.VaudoisOrdinaire) right);
			}
		});
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
		left.setTaxationAuthority(EnumHelper.coreToWeb(right.getTypeAutoriteFiscale()));
	}

	private static void fillTaxLiability(TaxLiability left, ch.vd.uniregctb.metier.assujettissement.Assujettissement right) {
		left.setDateFrom(DataHelper.coreToWeb(right.getDateDebut()));
		left.setDateTo(DataHelper.coreToWeb(right.getDateFin()));
		left.setStartReason(EnumHelper.coreToWeb(right.getMotifFractDebut()));
		left.setEndReason(EnumHelper.coreToWeb(right.getMotifFractFin()));
	}
}
