package ch.vd.unireg.evenement.externe;

import java.util.ArrayList;
import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.common.JobResults;
import ch.vd.unireg.tiers.TiersService;

public class TraiterEvenementExterneResult extends JobResults<Long, TraiterEvenementExterneResult> {

	public RegDate getDateTraitement() {
		return dateTraitement;
	}

	public boolean isInterrompu() {
		return interrompu;
	}

	public static class InfoEvenement {
		public final Long id;
		public final Long numeroTiers;

		public InfoEvenement(Long id, Long numeroTiers) {
			this.id = id;

			this.numeroTiers = numeroTiers;
		}
	}

	public static class Erreur {
		public final Long evenementId;
		public final String raison;

		public Erreur(Long evenementId, String raison) {
			this.raison = raison;
			this.evenementId = evenementId;
		}
	}

	public static class Traite extends InfoEvenement {
		//Specifique aux LR, a générifier au besoin dans le futur

		public final RegDate debut;
		public final RegDate fin;
		public final String action;

		public Traite(Long id, Long numeroTiers, String s, RegDate dateDebut, RegDate fdateFin) {
			super(id, numeroTiers);
			this.action = s;
			this.debut = dateDebut;
			this.fin = fdateFin;
		}
	}

	public final RegDate dateTraitement;
	public int nbEvenementTotal;
	public final List<Traite> traites = new ArrayList<>();
	public final List<Erreur> erreurs = new ArrayList<>();
	public boolean interrompu;

	public TraiterEvenementExterneResult(RegDate dateTraitement, TiersService tiersService, AdresseService adresseService) {
		super(tiersService, adresseService);
		this.dateTraitement = dateTraitement;
	}

	@Override
	public void addErrorException(Long element, Exception e) {
		++ nbEvenementTotal;
		erreurs.add(new Erreur(element, e.getMessage()));
	}

	@Override
	public void addAll(TraiterEvenementExterneResult right) {
		this.nbEvenementTotal += right.nbEvenementTotal;
		this.traites.addAll(right.traites);
		this.erreurs.addAll(right.erreurs);
	}

	public void addTraite(EvenementExterne event) {
		++ nbEvenementTotal;
		if (event instanceof QuittanceLR) {
			final QuittanceLR quittance = (QuittanceLR) event;
			traites.add(new Traite(quittance.getId(), quittance.getTiers().getNumero(), quittance.getType().name(), quittance.getDateDebut(), quittance.getDateFin()));
		}
		else {
			traites.add(new Traite(event.getId(), event.getTiers().getNumero(), event.getMessage(), null, null));
		}
	}

	public void addErreur(EvenementExterne event) {
		++ nbEvenementTotal;
		erreurs.add(new Erreur(event.getId(), event.getErrorMessage()));
	}
}
