package ch.vd.unireg.declaration.source;

import java.util.ArrayList;
import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.common.JobResults;
import ch.vd.unireg.declaration.Periodicite;
import ch.vd.unireg.tiers.DebiteurPrestationImposable;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.type.PeriodiciteDecompte;

public class EnvoiLRsResults extends JobResults<Long, EnvoiLRsResults> {

	public enum ErreurType {
		EXCEPTION(EXCEPTION_DESCRIPTION),
		ROLLBACK("Le traitement du débiteur a échoué et a été abandonné"),
		COLLISION_LR("une déclaration existe déjà, mais elle ne correspond pas à celle calculée");

		private final String description;

		ErreurType(String description) {
			this.description = description;
		}

		public String description() {
			return description;
		}
	}

	public static class Traite extends Info {

		public final PeriodiciteDecompte periodicite;

		public final RegDate dateDebut;

		public final RegDate dateFin;

		public Traite(long noCtb, PeriodiciteDecompte periodicite, RegDate dateDebut, RegDate dateFin, String nomCtb) {
			super(noCtb, null, periodicite.toString(), nomCtb);
			this.periodicite = periodicite;
			this.dateDebut = dateDebut;
			this.dateFin = dateFin;
		}

		@Override
		public String getDescriptionRaison() {
			return String.format("%s - %s (%s)", RegDateHelper.dateToDisplayString(dateDebut), RegDateHelper.dateToDisplayString(dateFin), periodicite);
		}
	}

	public static class Erreur extends Info {
		public final ErreurType raison;

		public Erreur(long noCtb, ErreurType raison, String details, String nomCtb) {
			super(noCtb, null, details, nomCtb);
			this.raison = raison;
		}

		@Override
		public String getDescriptionRaison() {
			return raison.description;
		}
	}

	public final RegDate dateTraitement;
	public final RegDate dateFinPeriode;
	public int nbDPIsTotal;										// nombre de DPI analysés
	public final List<Traite> LRTraitees = new ArrayList<>(); 	// LR émises
	public final List<Erreur> LREnErreur = new ArrayList<>();	// LR non-émises pour cause d'erreur
	public boolean interrompu;
	public int nbLrMensuellesTraitees;
	public int nbLrTrimestriellesTraitees;
	public int nbLrSemestriellesTraitees;
	public int nbLrAnnuellesTraitees;
	public int nbLrUniquesTraitees;

	public EnvoiLRsResults(RegDate dateTraitement, RegDate dateFinPeriode, TiersService tiersService, AdresseService adresseService) {
		super(tiersService, adresseService);
		this.dateTraitement = dateTraitement;
		this.dateFinPeriode = dateFinPeriode;
	}

	@Override
	public void addAll(EnvoiLRsResults right) {
		this.nbDPIsTotal += right.nbDPIsTotal;
		this.nbLrMensuellesTraitees += right.nbLrMensuellesTraitees;
		this.nbLrTrimestriellesTraitees += right.nbLrTrimestriellesTraitees;
		this.nbLrSemestriellesTraitees += right.nbLrSemestriellesTraitees;
		this.nbLrAnnuellesTraitees += right.nbLrAnnuellesTraitees;
		this.nbLrUniquesTraitees += right.nbLrUniquesTraitees;
		this.LRTraitees.addAll(right.LRTraitees);
		this.LREnErreur.addAll(right.LREnErreur);
	}

	public void addLrTraitee(DebiteurPrestationImposable dpi, RegDate dateDebut, RegDate dateFin) {
		//[UNIREG-3115] Periodicite non trouvé en debut de periode de lR on cherche à la fin.
		final Periodicite periodiciteAt = dpi.findPeriodicite(dateDebut,dateFin);

		final PeriodiciteDecompte periodicite = periodiciteAt.getPeriodiciteDecompte();

		switch (periodicite) {
			case MENSUEL:
				++ nbLrMensuellesTraitees;
				break;
			case TRIMESTRIEL:
				++ nbLrTrimestriellesTraitees;
				break;
			case SEMESTRIEL:
				++ nbLrSemestriellesTraitees;
				break;
			case ANNUEL:
				++ nbLrAnnuellesTraitees;
				break;
			case UNIQUE:
				++ nbLrUniquesTraitees;
				break;
		}
		LRTraitees.add(new Traite(dpi.getNumero(), periodicite, dateDebut, dateFin, getNom(dpi.getNumero())));
	}

	public void addDebiteur(DebiteurPrestationImposable dpi) {
		++ nbDPIsTotal;
	}

	public void addErrorLRCollision(DebiteurPrestationImposable dpi, String details) {
		LREnErreur.add(new Erreur(dpi.getNumero(), ErreurType.COLLISION_LR, details, getNom(dpi.getNumero())));
	}

	public void addOnCommitException(Long dpiId, Exception e) {
		LREnErreur.add(new Erreur(dpiId, ErreurType.ROLLBACK, e.getMessage(), getNom(dpiId)));
	}

	public String getMoisFinPeriode() {
		return String.format("%02d.%04d", dateFinPeriode.month(), dateFinPeriode.year());
	}

	@Override
	public void addErrorException(Long element, Exception e) {
		addOnCommitException(element, e);
	}
}
