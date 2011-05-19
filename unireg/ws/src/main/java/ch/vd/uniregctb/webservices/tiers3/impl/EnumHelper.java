package ch.vd.uniregctb.webservices.tiers3.impl;

import java.util.HashMap;
import java.util.Map;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.EtatCivilHelper;
import ch.vd.uniregctb.tiers.TiersDAO.Parts;
import ch.vd.uniregctb.type.TypePermis;
import ch.vd.uniregctb.webservices.tiers3.CategorieDebiteur;
import ch.vd.uniregctb.webservices.tiers3.CategoriePersonnePhysique;
import ch.vd.uniregctb.webservices.tiers3.EtatCivil;
import ch.vd.uniregctb.webservices.tiers3.GenreImpot;
import ch.vd.uniregctb.webservices.tiers3.ModeCommunication;
import ch.vd.uniregctb.webservices.tiers3.ModeImposition;
import ch.vd.uniregctb.webservices.tiers3.MotifFor;
import ch.vd.uniregctb.webservices.tiers3.MotifRattachement;
import ch.vd.uniregctb.webservices.tiers3.PeriodeDecompte;
import ch.vd.uniregctb.webservices.tiers3.PeriodiciteDecompte;
import ch.vd.uniregctb.webservices.tiers3.Sexe;
import ch.vd.uniregctb.webservices.tiers3.TarifImpotSource;
import ch.vd.uniregctb.webservices.tiers3.TiersPart;
import ch.vd.uniregctb.webservices.tiers3.TypeActivite;
import ch.vd.uniregctb.webservices.tiers3.TypeAffranchissement;
import ch.vd.uniregctb.webservices.tiers3.TypeAutoriteFiscale;
import ch.vd.uniregctb.webservices.tiers3.TypeDocument;
import ch.vd.uniregctb.webservices.tiers3.TypeEtatDeclaration;
import ch.vd.uniregctb.webservices.tiers3.TypeRapportEntreTiers;
import ch.vd.uniregctb.webservices.tiers3.TypeRecherche;

public abstract class EnumHelper {

	final static Map<TypePermis, CategoriePersonnePhysique> typePermis2Categorie = new HashMap<TypePermis, CategoriePersonnePhysique>();

	public static EtatCivil coreToWeb(ch.vd.uniregctb.type.EtatCivil etatCivil) {
		if (etatCivil == null) {
			return null;
		}

		final EtatCivil value = EtatCivil.fromValue(etatCivil.name());
		Assert.notNull(value);
		return value;
	}

	public static EtatCivil coreToWeb(ch.vd.uniregctb.interfaces.model.TypeEtatCivil etatCivil) {
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

	public static TypeEtatDeclaration coreToWeb(ch.vd.uniregctb.type.TypeEtatDeclaration type) {
		if (type == null) {
			return null;
		}

		final TypeEtatDeclaration value = TypeEtatDeclaration.fromValue(type.toString());
		Assert.notNull(value);
		return value;
	}

	public static GenreImpot coreToWeb(ch.vd.uniregctb.type.GenreImpot genreImpot) {
		if (genreImpot == null) {
			return null;
		}

		final GenreImpot value = GenreImpot.fromValue(genreImpot.name());
		Assert.notNull(value);
		return value;
	}

	public static MotifRattachement coreToWeb(ch.vd.uniregctb.type.MotifRattachement rattachement) {
		if (rattachement == null) {
			return null;
		}

		final MotifRattachement value = MotifRattachement.fromValue(rattachement.name());
		Assert.notNull(value);
		return value;
	}

	public static TypeAutoriteFiscale coreToWeb(ch.vd.uniregctb.type.TypeAutoriteFiscale typeForFiscal) {
		if (typeForFiscal == null) {
			return null;
		}

		final TypeAutoriteFiscale value = TypeAutoriteFiscale.fromValue(typeForFiscal.name());
		Assert.notNull(value);
		return value;
	}

	public static ModeImposition coreToWeb(ch.vd.uniregctb.type.ModeImposition mode) {
		if (mode == null) {
			return null;
		}

		final ModeImposition value = ModeImposition.fromValue(mode.name());
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

	public static CategoriePersonnePhysique coreToWeb(ch.vd.uniregctb.type.CategorieEtranger categorie) {
		if (categorie == null) {
			return null;
		}

		// Remplace les permis "A", "B", ... par "PERMIS_A", "PERMIS_B", ...
		final String name = categorie.name().replaceAll("^(A|B|C|CI|F|G|L|N|S)$", "PERMIS_$1");

		final CategoriePersonnePhysique value = CategoriePersonnePhysique.fromValue(name);
		Assert.notNull(value);
		return value;
	}

	public static CategoriePersonnePhysique coreToWeb(TypePermis permis) {
		if (permis == null) {
			return null;
		}

		if (typePermis2Categorie.size() == 0) {
			initTypePermis2Categorie();
		}

		final CategoriePersonnePhysique value = typePermis2Categorie.get(permis);
		Assert.notNull(value);
		return value;
	}

	private static synchronized void initTypePermis2Categorie() {
		if (typePermis2Categorie.size() == 0) {
			typePermis2Categorie.put(TypePermis.ANNUEL, CategoriePersonnePhysique.C_02_PERMIS_SEJOUR_B);
			typePermis2Categorie.put(TypePermis.COURTE_DUREE, CategoriePersonnePhysique.C_07_PERMIS_SEJOUR_COURTE_DUREE_L);
			typePermis2Categorie.put(TypePermis.DIPLOMATE, CategoriePersonnePhysique.C_11_DIPLOMATE);
			typePermis2Categorie.put(TypePermis.ETABLISSEMENT, CategoriePersonnePhysique.C_03_ETABLI_C);
			typePermis2Categorie.put(TypePermis.FONCTIONNAIRE_INTERNATIONAL, CategoriePersonnePhysique.C_12_FONCTIONNAIRE_INTERNATIONAL);
			typePermis2Categorie.put(TypePermis.FRONTALIER, CategoriePersonnePhysique.C_06_FRONTALIER_G);
			typePermis2Categorie.put(TypePermis.PERSONNE_A_PROTEGER, CategoriePersonnePhysique.C_09_A_PROTEGER_S);
			typePermis2Categorie.put(TypePermis.PROVISOIRE, CategoriePersonnePhysique.C_05_ETRANGER_ADMIS_PROVISOIREMENT_F);
			typePermis2Categorie.put(TypePermis.REQUERANT_ASILE_AVANT_DECISION, CategoriePersonnePhysique.C_08_REQUERANT_ASILE_N);
			typePermis2Categorie.put(TypePermis.REQUERANT_ASILE_REFUSE, CategoriePersonnePhysique.C_05_ETRANGER_ADMIS_PROVISOIREMENT_F);
			typePermis2Categorie.put(TypePermis.SUISSE_SOURCIER, CategoriePersonnePhysique.SUISSE);
		}
	}

	public static TypeRapportEntreTiers coreToWeb(ch.vd.uniregctb.type.TypeRapportEntreTiers type) {
		if (type == null) {
			return null;
		}

		final TypeRapportEntreTiers value = TypeRapportEntreTiers.fromValue(type.toString());
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
