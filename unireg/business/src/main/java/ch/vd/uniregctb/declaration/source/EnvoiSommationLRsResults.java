package ch.vd.uniregctb.declaration.source;

import java.util.ArrayList;
import java.util.List;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.JobResults;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;
import ch.vd.uniregctb.declaration.IdentifiantDeclaration;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.CategorieImpotSource;

public class EnvoiSommationLRsResults extends JobResults<IdentifiantDeclaration, EnvoiSommationLRsResults> {

	public static class Traite extends Info {

		private final DateRange periodeLr;

		public Traite(long noCtb, DeclarationImpotSource lr, String nomCtb) {
			super(noCtb, null, DateRangeHelper.toDisplayString(lr), nomCtb);
			this.periodeLr = new DateRangeHelper.Range(lr.getDateDebut(), lr.getDateFin());
		}

		@Override
		public String getDescriptionRaison() {
			return String.format("La LR %s a été sommée", DateRangeHelper.toDisplayString(periodeLr));
		}
	}
	
	public enum ErreurType {
		EXCEPTION(EXCEPTION_DESCRIPTION),
		ROLLBACK("Le traitement du lot a échoué et a été rollbacké");

		private final String description;

		ErreurType(String description) {
			this.description = description;
		}

		public String description() {
			return description;
		}
	}
	
	public static class Erreur extends Info {
		public final ErreurType raison;
		public final DateRange periodeLr;
		
		public Erreur(long noCtb, DeclarationImpotSource lr, ErreurType raison, String details, String nomCtb) {
			super(noCtb, null, details, nomCtb);
			this.periodeLr = lr != null ? new DateRangeHelper.Range(lr.getDateDebut(), lr.getDateFin()) : null;
			this.raison = raison;
		}

		@Override
		public String getDescriptionRaison() {
			if (periodeLr != null)
				return String.format("%s pour la LR %s", raison.description, DateRangeHelper.toDisplayString(periodeLr));
			else 
				return raison.description;
		}
	}

	public final CategorieImpotSource categorie;
	public final RegDate dateFinPeriode;
	public final RegDate dateTraitement;
	
	public int nbLRsTotal;             // nombre de LR analysées
	public final List<Traite> lrSommees = new ArrayList<>();      // LR sommées
	public final List<Erreur> sommationLREnErreurs = new ArrayList<>();   //sommation LR KO
	public boolean interrompu;
	
	public EnvoiSommationLRsResults(CategorieImpotSource categorie, RegDate dateFinPeriode, RegDate dateTrait, TiersService tiersService, AdresseService adresseService) {
		super(tiersService, adresseService);
		this.categorie = categorie;
		this.dateFinPeriode = dateFinPeriode;
		this.dateTraitement = dateTrait;
	}
	
	@Override
	public void addAll(EnvoiSommationLRsResults right) {
		this.nbLRsTotal += right.nbLRsTotal;
		this.lrSommees.addAll(right.lrSommees);
		this.sommationLREnErreurs.addAll(right.sommationLREnErreurs);
	}

	@Override
	public void addErrorException(IdentifiantDeclaration element, Exception e) {
		sommationLREnErreurs.add(new Erreur(element.getNumeroTiers(), null, ErreurType.ROLLBACK, e.getMessage(), getNom(element.getNumeroTiers())));
	}

	public void addLRSommee(DebiteurPrestationImposable dpi, DeclarationImpotSource lr) {
		lrSommees.add(new Traite(dpi.getNumero(), lr, getNom(dpi.getNumero())));
	}
	
	public void addError(DebiteurPrestationImposable dpi, DeclarationImpotSource lr, Exception e) {
		sommationLREnErreurs.add(new Erreur(dpi.getNumero(), lr, ErreurType.EXCEPTION, e.getMessage(), getNom(dpi.getNumero())));
	}
}
