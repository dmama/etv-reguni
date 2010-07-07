package ch.vd.uniregctb.webservices.tiers2.impl;

import java.util.HashMap;
import java.util.Map;

import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.civil.model.EnumTypePermis;
import ch.vd.uniregctb.common.EtatCivilHelper;
import ch.vd.uniregctb.tiers.TiersDAO.Parts;
import ch.vd.uniregctb.webservices.tiers2.data.CategorieDebiteur;
import ch.vd.uniregctb.webservices.tiers2.data.EtatCivil;
import ch.vd.uniregctb.webservices.tiers2.data.EtatDeclaration;
import ch.vd.uniregctb.webservices.tiers2.data.ForFiscal;
import ch.vd.uniregctb.webservices.tiers2.data.ModeCommunication;
import ch.vd.uniregctb.webservices.tiers2.data.PeriodeDecompte;
import ch.vd.uniregctb.webservices.tiers2.data.PeriodiciteDecompte;
import ch.vd.uniregctb.webservices.tiers2.data.PersonnePhysique;
import ch.vd.uniregctb.webservices.tiers2.data.RapportEntreTiers;
import ch.vd.uniregctb.webservices.tiers2.data.Sexe;
import ch.vd.uniregctb.webservices.tiers2.data.TarifImpotSource;
import ch.vd.uniregctb.webservices.tiers2.data.TiersPart;
import ch.vd.uniregctb.webservices.tiers2.data.TypeActivite;
import ch.vd.uniregctb.webservices.tiers2.data.TypeAffranchissement;
import ch.vd.uniregctb.webservices.tiers2.data.TypeDocument;
import ch.vd.uniregctb.webservices.tiers2.data.TypeRecherche;
import ch.vd.uniregctb.webservices.tiers2.data.ForFiscal.MotifFor;

public abstract class EnumHelper {

	final static Map<ch.vd.registre.civil.model.EnumTypePermis, PersonnePhysique.Categorie> typePermis2Categorie = new HashMap<ch.vd.registre.civil.model.EnumTypePermis, PersonnePhysique.Categorie>();

	public static EtatCivil coreToWeb(ch.vd.uniregctb.type.EtatCivil etatCivil) {
		if (etatCivil == null) {
			return null;
		}

		final EtatCivil value = EtatCivil.fromValue(etatCivil.name());
		Assert.notNull(value);
		return value;
	}

	public static EtatCivil coreToWeb(ch.vd.registre.civil.model.EnumTypeEtatCivil etatCivil) {
		if (etatCivil == null) {
			return null;
		}

		final EtatCivil value = EtatCivil.fromValue(EtatCivilHelper.getString(etatCivil));
		Assert.notNull(value);
		return value;
	}

	public static CategorieDebiteur coreToWeb(ch.vd.uniregctb.type.CategorieImpotSource categorieImpotSource) {
		if (categorieImpotSource == null) {
			return null;
		}

		final CategorieDebiteur value = CategorieDebiteur.fromValue(categorieImpotSource.toString());
		Assert.notNull(value);
		return value;
	}

	public static EtatDeclaration.Type coreToWeb(ch.vd.uniregctb.type.TypeEtatDeclaration type) {
		if (type == null) {
			return null;
		}

		final EtatDeclaration.Type value = EtatDeclaration.Type.fromValue(type.toString());
		Assert.notNull(value);
		return value;
	}

	public static ForFiscal.GenreImpot coreToWeb(ch.vd.uniregctb.type.GenreImpot genreImpot) {
		if (genreImpot == null) {
			return null;
		}

		final ForFiscal.GenreImpot value = ForFiscal.GenreImpot.fromValue(genreImpot.name());
		Assert.notNull(value);
		return value;
	}

	public static ForFiscal.MotifRattachement coreToWeb(ch.vd.uniregctb.type.MotifRattachement rattachement) {
		if (rattachement == null) {
			return null;
		}

		final ForFiscal.MotifRattachement value = ForFiscal.MotifRattachement.fromValue(rattachement.name());
		Assert.notNull(value);
		return value;
	}

	public static ForFiscal.TypeAutoriteFiscale coreToWeb(ch.vd.uniregctb.type.TypeAutoriteFiscale typeForFiscal) {
		if (typeForFiscal == null) {
			return null;
		}

		final ForFiscal.TypeAutoriteFiscale value = ForFiscal.TypeAutoriteFiscale.fromValue(typeForFiscal.name());
		Assert.notNull(value);
		return value;
	}

	public static ForFiscal.ModeImposition coreToWeb(ch.vd.uniregctb.type.ModeImposition mode) {
		if (mode == null) {
			return null;
		}

		final ForFiscal.ModeImposition value = ForFiscal.ModeImposition.fromValue(mode.name());
		Assert.notNull(value);
		return value;
	}

	public static ModeCommunication coreToWeb(ch.vd.uniregctb.type.ModeCommunication periodiciteDecompte) {
		if (periodiciteDecompte == null) {
			return null;
		}

		final ModeCommunication value = ModeCommunication.fromValue(periodiciteDecompte.toString());
		Assert.notNull(value);
		return value;
	}

	public static PeriodeDecompte coreToWeb(ch.vd.uniregctb.type.PeriodeDecompte periodeDecompte) {
		if (periodeDecompte == null) {
			return null;
		}

		final PeriodeDecompte value = PeriodeDecompte.valueOf(periodeDecompte.toString());
		Assert.notNull(value);
		return value;
	}

	public static PeriodiciteDecompte coreToWeb(ch.vd.uniregctb.type.PeriodiciteDecompte periodiciteDecompte) {
		if (periodiciteDecompte == null) {
			return null;
		}

		final PeriodiciteDecompte value = PeriodiciteDecompte.fromValue(periodiciteDecompte.toString());
		Assert.notNull(value);
		return value;
	}

	public static PersonnePhysique.Categorie coreToWeb(ch.vd.uniregctb.type.CategorieEtranger categorie) {
		if (categorie == null) {
			return null;
		}

		// Remplace les permis "A", "B", ... par "PERMIS_A", "PERMIS_B", ...
		final String name = categorie.name().replaceAll("^(A|B|C|CI|F|G|L|N|S)$", "PERMIS_$1");

		final PersonnePhysique.Categorie value = PersonnePhysique.Categorie.fromValue(name);
		Assert.notNull(value);
		return value;
	}

	public static PersonnePhysique.Categorie coreToWeb(ch.vd.registre.civil.model.EnumTypePermis permis) {
		if (permis == null) {
			return null;
		}

		if (typePermis2Categorie.size() == 0) {
			initTypePermis2Categorie();
		}

		final PersonnePhysique.Categorie value = typePermis2Categorie.get(permis);
		Assert.notNull(value);
		return value;
	}

	private static synchronized void initTypePermis2Categorie() {
		if (typePermis2Categorie.size() == 0) {
			typePermis2Categorie.put(EnumTypePermis.ANNUEL, PersonnePhysique.Categorie._02_PERMIS_SEJOUR_B);
			typePermis2Categorie.put(EnumTypePermis.COURTE_DUREE, PersonnePhysique.Categorie._07_PERMIS_SEJOUR_COURTE_DUREE_L);
			typePermis2Categorie.put(EnumTypePermis.DIPLOMATE, PersonnePhysique.Categorie._11_DIPLOMATE);
			typePermis2Categorie.put(EnumTypePermis.ETABLLISSEMENT, PersonnePhysique.Categorie._03_ETABLI_C);
			typePermis2Categorie.put(EnumTypePermis.FONCTIONNAIRE_INTERNATIONAL, PersonnePhysique.Categorie._12_FONCTIONNAIRE_INTERNATIONAL);
			typePermis2Categorie.put(EnumTypePermis.FRONTALIER, PersonnePhysique.Categorie._06_FRONTALIER_G);
			typePermis2Categorie.put(EnumTypePermis.PERSONNE_A_PROTEGER, PersonnePhysique.Categorie._09_A_PROTEGER_S);
			typePermis2Categorie.put(EnumTypePermis.PROVISOIRE, PersonnePhysique.Categorie._05_ETRANGER_ADMIS_PROVISOIREMENT_F);
			typePermis2Categorie.put(EnumTypePermis.REQUERANT_ASILE_AVANT_DECISION, PersonnePhysique.Categorie._08_REQUERANT_ASILE_N);
			typePermis2Categorie.put(EnumTypePermis.REQUERANT_ASILE_REFUSE, PersonnePhysique.Categorie._05_ETRANGER_ADMIS_PROVISOIREMENT_F);
			typePermis2Categorie.put(EnumTypePermis.SUISSE_SOURCIER, PersonnePhysique.Categorie.SUISSE);
		}
	}

	public static RapportEntreTiers.Type coreToWeb(ch.vd.uniregctb.type.TypeRapportEntreTiers type) {
		if (type == null) {
			return null;
		}

		final RapportEntreTiers.Type value = RapportEntreTiers.Type.fromValue(type.toString());
		Assert.notNull(value);
		return value;
	}

	public static TarifImpotSource coreToWeb(ch.vd.uniregctb.type.TarifImpotSource tarif) {
		if (tarif == null) {
			return null;
		}

		final TarifImpotSource value = TarifImpotSource.fromValue(tarif.name());
		Assert.notNull(value);
		return value;
	}

	public static TypeActivite coreToWeb(ch.vd.uniregctb.type.TypeActivite typeActivite) {
		if (typeActivite == null) {
			return null;
		}

		final TypeActivite value = TypeActivite.fromValue(typeActivite.name());
		Assert.notNull(value);
		return value;
	}

	public static TypeDocument coreToWeb(ch.vd.uniregctb.type.TypeDocument type) {
		if (type == null) {
			return null;
		}

		if (type == ch.vd.uniregctb.type.TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH
				|| type == ch.vd.uniregctb.type.TypeDocument.DECLARATION_IMPOT_COMPLETE_LOCAL) {
			return TypeDocument.DECLARATION_IMPOT_COMPLETE;
		}

		final TypeDocument value = TypeDocument.fromValue(type.name());
		Assert.notNull(value);
		return value;
	}

	public static MotifFor coreToWeb(ch.vd.uniregctb.type.MotifFor ouverture) {
		if (ouverture == null) {
			return null;
		}

		final MotifFor value = MotifFor.fromValue(ouverture.name());
		Assert.notNull(value);
		return value;
	}

	public static TypeRecherche coreToWeb(ch.vd.uniregctb.tiers.TiersCriteria.TypeRecherche type) {
		if (type == null) {
			return null;
		}

		final TypeRecherche value = TypeRecherche.fromValue(type.name());
		Assert.notNull(value);
		return value;
	}

	public static ch.vd.uniregctb.tiers.TiersCriteria.TypeRecherche webToCore(TypeRecherche type) {
		if (type == null) {
			return null;
		}

		final ch.vd.uniregctb.tiers.TiersCriteria.TypeRecherche value = ch.vd.uniregctb.tiers.TiersCriteria.TypeRecherche.fromValue(type
				.name());
		Assert.notNull(value);
		return value;
	}

	public static Sexe coreToWeb(ch.vd.uniregctb.type.Sexe sexe) {
		if (sexe == null) {
			return null;
		}

		final Sexe value = Sexe.fromValue(sexe.name());
		Assert.notNull(value);
		return value;
	}

	public static Parts webToCore(TiersPart p) {
		if (p == null) {
			return null;
		}

		final Parts part = Parts.fromValue(p.name());
		Assert.notNull(part);
		return part;
	}

	public static TypeAffranchissement coreToWeb(ch.vd.uniregctb.interfaces.model.TypeAffranchissement t) {
		if (t == null) {
			return null;
		}

		final TypeAffranchissement type = TypeAffranchissement.fromValue(t.name());
		Assert.notNull(type);
		return type;
	}
}
