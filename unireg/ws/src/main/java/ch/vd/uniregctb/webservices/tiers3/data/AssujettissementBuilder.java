package ch.vd.uniregctb.webservices.tiers3.data;

import java.util.HashMap;
import java.util.Map;

import ch.vd.uniregctb.webservices.tiers3.Assujettissement;
import ch.vd.uniregctb.webservices.tiers3.DiplomateSuisse;
import ch.vd.uniregctb.webservices.tiers3.HorsCanton;
import ch.vd.uniregctb.webservices.tiers3.HorsSuisse;
import ch.vd.uniregctb.webservices.tiers3.Indigent;
import ch.vd.uniregctb.webservices.tiers3.Sourcier;
import ch.vd.uniregctb.webservices.tiers3.SourcierMixte;
import ch.vd.uniregctb.webservices.tiers3.SourcierPur;
import ch.vd.uniregctb.webservices.tiers3.VaudoisDepense;
import ch.vd.uniregctb.webservices.tiers3.VaudoisOrdinaire;
import ch.vd.uniregctb.webservices.tiers3.impl.DataHelper;
import ch.vd.uniregctb.webservices.tiers3.impl.EnumHelper;

public abstract class AssujettissementBuilder {

	public static Assujettissement newAssujettissement(ch.vd.uniregctb.metier.assujettissement.Assujettissement assujettissement) {
		return builders.get(assujettissement.getClass()).instanciate(assujettissement);
	}

	private interface Builders {
		Assujettissement instanciate(ch.vd.uniregctb.metier.assujettissement.Assujettissement right);
	}

	private static Map<Class, Builders> builders = new HashMap<Class, Builders>();

	static {
		builders.put(ch.vd.uniregctb.metier.assujettissement.DiplomateSuisse.class, new Builders() {
			@Override
			public Assujettissement instanciate(ch.vd.uniregctb.metier.assujettissement.Assujettissement right) {
				return newDiplomateSuisse((ch.vd.uniregctb.metier.assujettissement.DiplomateSuisse) right);
			}
		});
		builders.put(ch.vd.uniregctb.metier.assujettissement.HorsCanton.class, new Builders() {
			@Override
			public Assujettissement instanciate(ch.vd.uniregctb.metier.assujettissement.Assujettissement right) {
				return newHorsCanton((ch.vd.uniregctb.metier.assujettissement.HorsCanton) right);
			}
		});
		builders.put(ch.vd.uniregctb.metier.assujettissement.HorsSuisse.class, new Builders() {
			@Override
			public Assujettissement instanciate(ch.vd.uniregctb.metier.assujettissement.Assujettissement right) {
				return newHorsSuisse((ch.vd.uniregctb.metier.assujettissement.HorsSuisse) right);
			}
		});
		builders.put(ch.vd.uniregctb.metier.assujettissement.Indigent.class, new Builders() {
			@Override
			public Assujettissement instanciate(ch.vd.uniregctb.metier.assujettissement.Assujettissement right) {
				return newIndigent((ch.vd.uniregctb.metier.assujettissement.Indigent) right);
			}
		});
		builders.put(ch.vd.uniregctb.metier.assujettissement.SourcierMixte.class, new Builders() {
			@Override
			public Assujettissement instanciate(ch.vd.uniregctb.metier.assujettissement.Assujettissement right) {
				return newSourcierMixte((ch.vd.uniregctb.metier.assujettissement.SourcierMixte) right);
			}
		});
		builders.put(ch.vd.uniregctb.metier.assujettissement.SourcierPur.class, new Builders() {
			@Override
			public Assujettissement instanciate(ch.vd.uniregctb.metier.assujettissement.Assujettissement right) {
				return newSourcierPur((ch.vd.uniregctb.metier.assujettissement.SourcierPur) right);
			}
		});
		builders.put(ch.vd.uniregctb.metier.assujettissement.VaudoisDepense.class, new Builders() {
			@Override
			public Assujettissement instanciate(ch.vd.uniregctb.metier.assujettissement.Assujettissement right) {
				return newVaudoisDepense((ch.vd.uniregctb.metier.assujettissement.VaudoisDepense) right);
			}
		});
		builders.put(ch.vd.uniregctb.metier.assujettissement.VaudoisOrdinaire.class, new Builders() {
			@Override
			public Assujettissement instanciate(ch.vd.uniregctb.metier.assujettissement.Assujettissement right) {
				return newVaudoisOrdinaire((ch.vd.uniregctb.metier.assujettissement.VaudoisOrdinaire) right);
			}
		});
	}

	private static DiplomateSuisse newDiplomateSuisse(ch.vd.uniregctb.metier.assujettissement.DiplomateSuisse right) {
		DiplomateSuisse left = new DiplomateSuisse();
		fillAssujettissement(left, right);
		return left;
	}

	private static HorsCanton newHorsCanton(ch.vd.uniregctb.metier.assujettissement.HorsCanton right) {
		HorsCanton left = new HorsCanton();
		fillAssujettissement(left, right);
		return left;
	}

	private static HorsSuisse newHorsSuisse(ch.vd.uniregctb.metier.assujettissement.HorsSuisse right) {
		HorsSuisse left = new HorsSuisse();
		fillAssujettissement(left, right);
		return left;
	}

	private static Indigent newIndigent(ch.vd.uniregctb.metier.assujettissement.Indigent right) {
		Indigent left = new Indigent();
		fillAssujettissement(left, right);
		return left;
	}

	private static SourcierMixte newSourcierMixte(ch.vd.uniregctb.metier.assujettissement.SourcierMixte right) {
		SourcierMixte left = new SourcierMixte();
		fillSourcier(left, right);
		return left;
	}

	private static SourcierPur newSourcierPur(ch.vd.uniregctb.metier.assujettissement.SourcierPur right) {
		SourcierPur left = new SourcierPur();
		fillSourcier(left, right);
		return left;
	}

	private static VaudoisDepense newVaudoisDepense(ch.vd.uniregctb.metier.assujettissement.VaudoisDepense right) {
		VaudoisDepense left = new VaudoisDepense();
		fillAssujettissement(left, right);
		return left;
	}

	private static VaudoisOrdinaire newVaudoisOrdinaire(ch.vd.uniregctb.metier.assujettissement.VaudoisOrdinaire right) {
		VaudoisOrdinaire left = new VaudoisOrdinaire();
		fillAssujettissement(left, right);
		return left;
	}

	private static void fillSourcier(Sourcier left, ch.vd.uniregctb.metier.assujettissement.Sourcier right) {
		fillAssujettissement(left, right);
		left.setTypeAutoriteFiscale(EnumHelper.coreToWeb(right.getTypeAutoriteFiscale()));
	}

	private static void fillAssujettissement(Assujettissement left, ch.vd.uniregctb.metier.assujettissement.Assujettissement right) {
		left.setDateDebut(DataHelper.coreToWeb(right.getDateDebut()));
		left.setDateFin(DataHelper.coreToWeb(right.getDateFin()));
		left.setMotifDebut(EnumHelper.coreToWeb(right.getMotifFractDebut()));
		left.setMotifFin(EnumHelper.coreToWeb(right.getMotifFractFin()));
	}
}
