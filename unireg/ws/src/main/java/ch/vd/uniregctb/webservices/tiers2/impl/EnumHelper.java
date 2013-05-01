package ch.vd.uniregctb.webservices.tiers2.impl;

import java.util.EnumMap;
import java.util.Map;

import ch.vd.registre.base.utils.Assert;
import ch.vd.unireg.interfaces.civil.data.TypeEtatCivil;
import ch.vd.uniregctb.tiers.TiersDAO.Parts;
import ch.vd.uniregctb.type.TypePermis;
import ch.vd.uniregctb.webservices.tiers2.data.CategorieDebiteur;
import ch.vd.uniregctb.webservices.tiers2.data.EtatCivil;
import ch.vd.uniregctb.webservices.tiers2.data.EtatDeclaration;
import ch.vd.uniregctb.webservices.tiers2.data.ForFiscal;
import ch.vd.uniregctb.webservices.tiers2.data.ForFiscal.MotifFor;
import ch.vd.uniregctb.webservices.tiers2.data.ModeCommunication;
import ch.vd.uniregctb.webservices.tiers2.data.PeriodeDecompte;
import ch.vd.uniregctb.webservices.tiers2.data.PeriodiciteDecompte;
import ch.vd.uniregctb.webservices.tiers2.data.PersonnePhysique;
import ch.vd.uniregctb.webservices.tiers2.data.RapportEntreTiers;
import ch.vd.uniregctb.webservices.tiers2.data.Sexe;
import ch.vd.uniregctb.webservices.tiers2.data.TarifImpotSource;
import ch.vd.uniregctb.webservices.tiers2.data.TiersPart;
import ch.vd.uniregctb.webservices.tiers2.data.TypeAffranchissement;
import ch.vd.uniregctb.webservices.tiers2.data.TypeDocument;
import ch.vd.uniregctb.webservices.tiers2.data.TypeRecherche;

public abstract class EnumHelper {

	final static Map<TypePermis, PersonnePhysique.Categorie> typePermis2Categorie = new EnumMap<>(TypePermis.class);

	public static EtatCivil coreToWeb(ch.vd.uniregctb.type.EtatCivil etatCivil) {
		if (etatCivil == null) {
			return null;
		}

		if (etatCivil == ch.vd.uniregctb.type.EtatCivil.PARTENARIAT_SEPARE) {
			return EtatCivil.SEPARE; // [SIFISC-6042]
		}

		final EtatCivil value = EtatCivil.fromValue(etatCivil.name());
		Assert.notNull(value);
		return value;
	}

	public static EtatCivil coreToWeb(TypeEtatCivil etatCivil) {
		if (etatCivil == null) {
			return null;
		}

		switch (etatCivil) {
		case CELIBATAIRE:
			return EtatCivil.CELIBATAIRE;
		case DIVORCE:
			return EtatCivil.DIVORCE;
		case PACS:
			return EtatCivil.LIE_PARTENARIAT_ENREGISTRE;
		case MARIE:
			return EtatCivil.MARIE;
		case PACS_TERMINE:
		case PACS_SEPARE:
			return EtatCivil.PARTENARIAT_DISSOUS_JUDICIAIREMENT;
		case PACS_VEUF:
			return EtatCivil.PARTENARIAT_DISSOUS_DECES;
		case SEPARE:
			return EtatCivil.SEPARE;
		case VEUF:
			return EtatCivil.VEUF;
		case NON_MARIE:
			return EtatCivil.NON_MARIE;
		default:
			throw new IllegalArgumentException("Type d'état civil inconnu = [" + etatCivil + ']');
		}
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
		switch (categorie) {
		case _02_PERMIS_SEJOUR_B:
			return PersonnePhysique.Categorie._02_PERMIS_SEJOUR_B;
		case _03_ETABLI_C:
			return PersonnePhysique.Categorie._03_ETABLI_C;
		case _04_CONJOINT_DIPLOMATE_CI:
			return PersonnePhysique.Categorie._04_CONJOINT_DIPLOMATE_CI;
		case _05_ETRANGER_ADMIS_PROVISOIREMENT_F:
			return PersonnePhysique.Categorie._05_ETRANGER_ADMIS_PROVISOIREMENT_F;
		case _06_FRONTALIER_G:
			return PersonnePhysique.Categorie._06_FRONTALIER_G;
		case _07_PERMIS_SEJOUR_COURTE_DUREE_L:
			return PersonnePhysique.Categorie._07_PERMIS_SEJOUR_COURTE_DUREE_L;
		case _08_REQUERANT_ASILE_N:
			return PersonnePhysique.Categorie._08_REQUERANT_ASILE_N;
		case _09_A_PROTEGER_S:
			return PersonnePhysique.Categorie._09_A_PROTEGER_S;
		case _10_TENUE_DE_S_ANNONCER:
			return PersonnePhysique.Categorie._10_TENUE_DE_S_ANNONCER;
		case _11_DIPLOMATE_OU_FONCT_INTER_AVEC_IMMUNITE:
			return PersonnePhysique.Categorie._11_DIPLOMATE;
		case _12_FONCT_INTER_SANS_IMMUNITE:
			return PersonnePhysique.Categorie._12_FONCTIONNAIRE_INTERNATIONAL;
		case _13_NON_ATTRIBUEE:
			return PersonnePhysique.Categorie._13_NON_ATTRIBUEE;
		default:
			throw new IllegalArgumentException("Type de categorie inconnue = [" + categorie + ']');
		}
	}

	public static PersonnePhysique.Categorie coreToWeb(TypePermis permis) {
		if (permis == null) {
			return null;
		}

		if (typePermis2Categorie.isEmpty()) {
			initTypePermis2Categorie();
		}

		final PersonnePhysique.Categorie value = typePermis2Categorie.get(permis);
		Assert.notNull(value);
		return value;
	}

	private static synchronized void initTypePermis2Categorie() {
		if (typePermis2Categorie.isEmpty()) {
			typePermis2Categorie.put(TypePermis.SEJOUR, PersonnePhysique.Categorie._02_PERMIS_SEJOUR_B);
			typePermis2Categorie.put(TypePermis.ETABLISSEMENT, PersonnePhysique.Categorie._03_ETABLI_C);
			typePermis2Categorie.put(TypePermis.CONJOINT_DIPLOMATE, PersonnePhysique.Categorie._04_CONJOINT_DIPLOMATE_CI);
			typePermis2Categorie.put(TypePermis.ETRANGER_ADMIS_PROVISOIREMENT, PersonnePhysique.Categorie._05_ETRANGER_ADMIS_PROVISOIREMENT_F);
			typePermis2Categorie.put(TypePermis.FRONTALIER, PersonnePhysique.Categorie._06_FRONTALIER_G);
			typePermis2Categorie.put(TypePermis.COURTE_DUREE, PersonnePhysique.Categorie._07_PERMIS_SEJOUR_COURTE_DUREE_L);
			typePermis2Categorie.put(TypePermis.REQUERANT_ASILE, PersonnePhysique.Categorie._08_REQUERANT_ASILE_N);
			typePermis2Categorie.put(TypePermis.PERSONNE_A_PROTEGER, PersonnePhysique.Categorie._09_A_PROTEGER_S);
			typePermis2Categorie.put(TypePermis.PERSONNE_TENUE_DE_S_ANNONCER, PersonnePhysique.Categorie._10_TENUE_DE_S_ANNONCER);
			typePermis2Categorie.put(TypePermis.DIPLOMATE_OU_FONCT_INTER_AVEC_IMMUNITE, PersonnePhysique.Categorie._11_DIPLOMATE);
			typePermis2Categorie.put(TypePermis.FONCT_INTER_SANS_IMMUNITE, PersonnePhysique.Categorie._12_FONCTIONNAIRE_INTERNATIONAL);
			typePermis2Categorie.put(TypePermis.PAS_ATTRIBUE, PersonnePhysique.Categorie._13_NON_ATTRIBUEE);
			typePermis2Categorie.put(TypePermis.SUISSE_SOURCIER, PersonnePhysique.Categorie.SUISSE);
			typePermis2Categorie.put(TypePermis.PROVISOIRE, PersonnePhysique.Categorie._13_NON_ATTRIBUEE);
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
