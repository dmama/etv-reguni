package ch.vd.uniregctb.xml.party.v5;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.xml.party.taxresidence.v4.ExpenditureBased;
import ch.vd.unireg.xml.party.taxresidence.v4.ForeignCountry;
import ch.vd.unireg.xml.party.taxresidence.v4.Indigent;
import ch.vd.unireg.xml.party.taxresidence.v4.MixedWithholding137Par1;
import ch.vd.unireg.xml.party.taxresidence.v4.MixedWithholding137Par2;
import ch.vd.unireg.xml.party.taxresidence.v4.OrdinaryResident;
import ch.vd.unireg.xml.party.taxresidence.v4.OtherCanton;
import ch.vd.unireg.xml.party.taxresidence.v4.PureWithholding;
import ch.vd.unireg.xml.party.taxresidence.v4.SwissDiplomat;
import ch.vd.unireg.xml.party.taxresidence.v4.TaxLiability;
import ch.vd.unireg.xml.party.taxresidence.v4.Withholding;
import ch.vd.uniregctb.metier.assujettissement.Assujettissement;
import ch.vd.uniregctb.xml.DataHelper;
import ch.vd.uniregctb.xml.EnumHelper;

public abstract class TaxLiabilityBuilder {

	public static TaxLiability newTaxLiability(Assujettissement assujettissement) {
		return _newTaxLiability(assujettissement);
	}

	private static <T extends Assujettissement> TaxLiability _newTaxLiability(T assujettissement) {
		//noinspection unchecked
		final Builder<T> builder = (Builder<T>) BUILDERS.get(assujettissement.getClass());
		if (builder == null) {
			throw new IllegalArgumentException("Classe d'assujettissement non-supportée : " + assujettissement.getClass());
		}
		return builder.instanciate(assujettissement);
	}

	private interface Builder<T extends Assujettissement> {
		TaxLiability instanciate(T right);
	}

	private static final Map<Class<? extends Assujettissement>, Builder<? extends Assujettissement>> BUILDERS = buildBuilders();

	/**
	 * Si tous les ajouts à la map passent par ici, on a ainsi une certaine garantie quant au lien entre le type de la clé et le type de la valeur
	 * (lien dont nous pourrons nous servir lors de la récupération des données, voir {@link #_newTaxLiability(Assujettissement)})
	 */
	private static <T extends Assujettissement> void registerBuilder(Map<Class<? extends Assujettissement>, Builder<? extends Assujettissement>> map,
	                                                                 Class<T> clazz, Builder<T> builder) {
		map.put(clazz, builder);
	}

	@NotNull
	private static Map<Class<? extends Assujettissement>, Builder<? extends Assujettissement>> buildBuilders() {
		final Map<Class<? extends Assujettissement>, Builder<? extends Assujettissement>> map = new HashMap<>();
		registerBuilder(map, ch.vd.uniregctb.metier.assujettissement.DiplomateSuisse.class, TaxLiabilityBuilder::newSwissDiplomat);
		registerBuilder(map, ch.vd.uniregctb.metier.assujettissement.HorsCanton.class, TaxLiabilityBuilder::newOtherCanton);
		registerBuilder(map, ch.vd.uniregctb.metier.assujettissement.HorsSuisse.class, TaxLiabilityBuilder::newForeignCountry);
		registerBuilder(map, ch.vd.uniregctb.metier.assujettissement.Indigent.class, TaxLiabilityBuilder::newIndigent);
		registerBuilder(map, ch.vd.uniregctb.metier.assujettissement.SourcierMixteArt137Al1.class, TaxLiabilityBuilder::newMixedWithholding137Par1);
		registerBuilder(map, ch.vd.uniregctb.metier.assujettissement.SourcierMixteArt137Al2.class, TaxLiabilityBuilder::newMixedWithholding137Par2);
		registerBuilder(map, ch.vd.uniregctb.metier.assujettissement.SourcierPur.class, TaxLiabilityBuilder::newPureWithholding);
		registerBuilder(map, ch.vd.uniregctb.metier.assujettissement.VaudoisDepense.class, TaxLiabilityBuilder::newExpenditureBased);
		registerBuilder(map, ch.vd.uniregctb.metier.assujettissement.VaudoisOrdinaire.class, TaxLiabilityBuilder::newOrdinaryResident);
		return Collections.unmodifiableMap(map);
	}

	private static SwissDiplomat newSwissDiplomat(ch.vd.uniregctb.metier.assujettissement.DiplomateSuisse right) {
		final SwissDiplomat left = new SwissDiplomat();
		fillTaxLiability(left, right);
		return left;
	}

	private static OtherCanton newOtherCanton(ch.vd.uniregctb.metier.assujettissement.HorsCanton right) {
		final OtherCanton left = new OtherCanton();
		fillTaxLiability(left, right);
		return left;
	}

	private static ForeignCountry newForeignCountry(ch.vd.uniregctb.metier.assujettissement.HorsSuisse right) {
		final ForeignCountry left = new ForeignCountry();
		fillTaxLiability(left, right);
		return left;
	}

	private static Indigent newIndigent(ch.vd.uniregctb.metier.assujettissement.Indigent right) {
		final Indigent left = new Indigent();
		fillTaxLiability(left, right);
		return left;
	}

	private static MixedWithholding137Par1 newMixedWithholding137Par1(ch.vd.uniregctb.metier.assujettissement.SourcierMixteArt137Al1 right) {
		final MixedWithholding137Par1 left = new MixedWithholding137Par1();
		fillWithholding(left, right);
		return left;
	}

	private static MixedWithholding137Par2 newMixedWithholding137Par2(ch.vd.uniregctb.metier.assujettissement.SourcierMixteArt137Al2 right) {
		final MixedWithholding137Par2 left = new MixedWithholding137Par2();
		fillWithholding(left, right);
		return left;
	}

	private static PureWithholding newPureWithholding(ch.vd.uniregctb.metier.assujettissement.SourcierPur right) {
		final PureWithholding left = new PureWithholding();
		fillWithholding(left, right);
		return left;
	}

	private static ExpenditureBased newExpenditureBased(ch.vd.uniregctb.metier.assujettissement.VaudoisDepense right) {
		final ExpenditureBased left = new ExpenditureBased();
		fillTaxLiability(left, right);
		return left;
	}

	private static OrdinaryResident newOrdinaryResident(ch.vd.uniregctb.metier.assujettissement.VaudoisOrdinaire right) {
		final OrdinaryResident left = new OrdinaryResident();
		fillTaxLiability(left, right);
		return left;
	}

	private static void fillWithholding(Withholding left, ch.vd.uniregctb.metier.assujettissement.Sourcier right) {
		fillTaxLiability(left, right);
		left.setTaxationAuthority(EnumHelper.coreToXMLv4(right.getTypeAutoriteFiscalePrincipale()));
	}

	private static void fillTaxLiability(TaxLiability left, ch.vd.uniregctb.metier.assujettissement.Assujettissement right) {
		left.setDateFrom(DataHelper.coreToXMLv2(right.getDateDebut()));
		left.setDateTo(DataHelper.coreToXMLv2(right.getDateFin()));
		left.setStartReason(EnumHelper.coreToXMLv4(right.getMotifFractDebut()));
		left.setEndReason(EnumHelper.coreToXMLv4(right.getMotifFractFin()));
	}
}
