package ch.vd.uniregctb.tiers.manager;

import ch.vd.uniregctb.tiers.ForDebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalAutreElementImposable;
import ch.vd.uniregctb.tiers.ForFiscalAutreImpot;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.MotifRattachement;

/**
 * Défini les types concrets (= instanciables) des fors fiscaux + quelques méthodes qui vont bien.
 *
 * @author msi
 */
public enum TypeForFiscal {

	PRINCIPAL() {
		@Override
		public ForFiscal newInstance() {
			return new ForFiscalPrincipal();
		}

		@Override
		public boolean matches(ForFiscal forFiscal) {
			return forFiscal instanceof ForFiscalPrincipal;
		}
	},
	SECONDAIRE() {
		@Override
		public ForFiscal newInstance() {
			return new ForFiscalSecondaire();
		}

		@Override
		public boolean matches(ForFiscal forFiscal) {
			return forFiscal instanceof ForFiscalSecondaire;
		}
	},
	DEBITEUR_PRESTATION_IMPOSABLE() {
		@Override
		public ForFiscal newInstance() {
			return new ForDebiteurPrestationImposable();
		}

		@Override
		public boolean matches(ForFiscal forFiscal) {
			return forFiscal instanceof ForDebiteurPrestationImposable;
		}
	},
	AUTRE_ELEMENT() {
		@Override
		public ForFiscal newInstance() {
			return new ForFiscalAutreElementImposable();
		}

		@Override
		public boolean matches(ForFiscal forFiscal) {
			return forFiscal instanceof ForFiscalAutreElementImposable;
		}
	},
	AUTRE_IMPOT() {
		@Override
		public ForFiscal newInstance() {
			return new ForFiscalAutreImpot();
		}

		@Override
		public boolean matches(ForFiscal forFiscal) {
			return forFiscal instanceof ForFiscalAutreImpot;
		}
	};

	/**
	 * Détermine le type de for à partir du genre d'impôt et du motif de rattachement.
	 */
	public static TypeForFiscal getType(GenreImpot genre, MotifRattachement motif) {

		final TypeForFiscal type;
		if (GenreImpot.REVENU_FORTUNE == genre) {
			if (MotifRattachement.DOMICILE == motif || MotifRattachement.DIPLOMATE_SUISSE == motif || MotifRattachement.DIPLOMATE_ETRANGER == motif) {
				type = PRINCIPAL;
			}
			else if (MotifRattachement.ACTIVITE_INDEPENDANTE == motif || MotifRattachement.IMMEUBLE_PRIVE == motif ||
					MotifRattachement.SEJOUR_SAISONNIER == motif || MotifRattachement.DIRIGEANT_SOCIETE == motif) {
				type = SECONDAIRE;
			}
			else {
				type = AUTRE_ELEMENT;
			}
		}
		else if (GenreImpot.DEBITEUR_PRESTATION_IMPOSABLE == genre) {
			type = DEBITEUR_PRESTATION_IMPOSABLE;
		} else {
			type = AUTRE_IMPOT;
		}

		return type;
	}

	/**
	 * Crée une nouvelle instance du for correspondant au type sélectionné.
	 */
	public abstract ForFiscal newInstance();

	/**
	 * Vérifie que le for fiscal passé en paramètre correspondant au type sélectionné.
	 */
	public abstract boolean matches(ForFiscal forFiscal);
}
