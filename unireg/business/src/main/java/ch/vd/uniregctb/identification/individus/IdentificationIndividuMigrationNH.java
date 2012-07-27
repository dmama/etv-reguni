package ch.vd.uniregctb.identification.individus;

import java.util.Collection;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.data.Nationalite;
import ch.vd.unireg.interfaces.civil.data.Pays;
import ch.vd.unireg.interfaces.civil.data.Permis;
import ch.vd.unireg.interfaces.civil.data.TypeEtatCivil;
import ch.vd.uniregctb.type.CategorieEtranger;
import ch.vd.uniregctb.type.TexteCasePostale;

public class IdentificationIndividuMigrationNH extends IdentificationIndividu {

	public final RegDate dateDeces;
	public final String sexe;
	public final String categorieEtranger;
	public final RegDate dateDebutValiditeAutorisation;
	public final Adresse adresse;
	public final Integer numeroOfsNationalite;
	public final TypeEtatCivil typeEtatCivil;
	public final RegDate dateDebutEtatCivil;

	public IdentificationIndividuMigrationNH(Individu individu) {
		super(individu);
		this.dateDeces = individu.getDateDeces();
		this.sexe = individu.isSexeMasculin() ? "MASCULIN" : "FEMININ";

		//permis
		if (individu.getPermis() != null && individu.getPermis().getPermisActif(null) != null) {
			final Permis dernierPermis = individu.getPermis().getPermisActif(null);
			this.categorieEtranger = CategorieEtranger.enumToCategorie(dernierPermis.getTypePermis()).name();
			this.dateDebutValiditeAutorisation = dernierPermis.getDateDebut();
		} else {
			this.categorieEtranger = null;
			this.dateDebutValiditeAutorisation = null;
		}
		// Adresse
		final Collection<ch.vd.unireg.interfaces.civil.data.Adresse> adresses = individu.getAdresses();
		if (adresses != null && !adresses.isEmpty()) {
			ch.vd.unireg.interfaces.civil.data.Adresse a = trouveLaBonneAdresse(adresses);
			this.adresse = new Adresse(a);
		} else {
			this.adresse = null;
		}

		// nationalité (on ne garde qu'une nationalité au hasard, sachant que si l'une est Suisse, elle a priorité)
		// copié-collé adapté de TiersServiceImpl#changeHabitantenNH()
		final Collection<Nationalite> nationalites = individu.getNationalites();
		if (nationalites != null) {
			Pays pays = null;
			for (Nationalite nationalite : nationalites) {
				if (nationalite.getDateFinValidite() == null) {
					if (pays == null) {
						pays = nationalite.getPays();
					} else if (!pays.isSuisse()) {
						pays = nationalite.getPays();
					}
				}
			}
			if (pays != null) {
				this.numeroOfsNationalite = pays.getNoOFS();
			} else {
				this.numeroOfsNationalite = null;
			}
		} else {
			this.numeroOfsNationalite = null;
		}

		ch.vd.unireg.interfaces.civil.data.EtatCivil etatCivil = individu.getEtatCivilCourant();
		if (etatCivil != null) {
			this.dateDebutEtatCivil = etatCivil.getDateDebut();
			this.typeEtatCivil = etatCivil.getTypeEtatCivil();
		} else {
			this.dateDebutEtatCivil = null;
			this.typeEtatCivil = null;
		}

	}

	private ch.vd.unireg.interfaces.civil.data.Adresse trouveLaBonneAdresse(Collection<ch.vd.unireg.interfaces.civil.data.Adresse> adresses) {
		ch.vd.unireg.interfaces.civil.data.Adresse result = null;
		for (ch.vd.unireg.interfaces.civil.data.Adresse adresse : adresses) {
			if (result == null) {
				result = adresse;
			} else {
				if (result.getDateFin() != null && adresse.getDateFin() == null) {
					result = adresse;
				} else if (result.getDateFin() == null && adresse.getDateFin() == null) {
					result = result.getDateDebut().isAfter(adresse.getDateDebut()) ? result : adresse;
				}  else if (result.getDateFin() != null && adresse.getDateFin() != null) {
					result = result.getDateDebut().isAfter(adresse.getDateDebut()) ? result : adresse;
				}
			}
		}
		return result;
	}



	public static class Adresse {

		private static final Integer NO_OFS_SUISSE = 8100;

		public final Integer npaCasePostale;
		public final Integer numeroCasePostale;
		public final TexteCasePostale texteCasePostale;
		public final RegDate dateDebut;
		public final Integer noOfsPays;
		public final String numeroAppartement;
		public final int numeroOrdrePostal;
		public final String numeroPostal;
		public final Integer numeroRue;
		public final String rue;
		public final String type;

		public Adresse(ch.vd.unireg.interfaces.civil.data.Adresse adresse) {
			if (adresse.getCasePostale() != null) {
				this.npaCasePostale = adresse.getCasePostale().getNpa();
				this.numeroCasePostale = adresse.getCasePostale().getNumero();
				this.texteCasePostale = adresse.getCasePostale().getType();
			} else {
				this.npaCasePostale = null;
				this.numeroCasePostale = null;
				this.texteCasePostale = null;
			}
			this.dateDebut = adresse.getDateDebut();
			this.noOfsPays = adresse.getNoOfsPays();
			this.numeroAppartement = adresse.getNumeroAppartement();
			this.numeroOrdrePostal = adresse.getNumeroOrdrePostal();
			this.numeroPostal = adresse.getNumeroPostal();
			this.numeroRue = adresse.getNumeroRue();
			this.rue = adresse.getRue();
			if (NO_OFS_SUISSE.equals(noOfsPays)) {
				this.type = "AdresseSuisse";
			} else {
				this.type = "AdresseEtrangere";
			}
		}
	}
}
