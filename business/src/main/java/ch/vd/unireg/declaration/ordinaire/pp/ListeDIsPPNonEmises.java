package ch.vd.uniregctb.declaration.ordinaire.pp;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.metier.assujettissement.CategorieEnvoiDIPP;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.TiersService;

public class ListeDIsPPNonEmises extends AbstractEnvoiDIsPPResults<ListeDIsPPNonEmises> {

	/**
	 * Classe pour stocker les informations d'une ligne du fichier csv resulat
	 *
	 * @author xsifnr
	 */
	public static class LigneRapport {

		private final String nbCtb;
		private final String dateDebut;
		private final String dateFin;
		private final String raison;
		private final String details;

		public LigneRapport(String nbCtb, String dateDebut, String dateFin, String raison, String details) {
			super();
			this.nbCtb = nbCtb;
			this.dateDebut = dateDebut;
			this.dateFin = dateFin;
			this.raison = raison;
			this.details = details;
		}

		public String getNbCtb() {
			return nbCtb;
		}

		public String getRaison() {
			return raison;
		}

		public String getDetails() {
			return details;
		}

		public String getDateDebut() {
			return dateDebut;
		}

		public String getDateFin() {
			return dateFin;
		}
	}


	private final List<LigneRapport> diNonEmises = new ArrayList<>();


	public ListeDIsPPNonEmises(int anneePeriode, RegDate dateTraitement, TiersService tiersService, AdresseService adresseService) {
		super(anneePeriode, CategorieEnvoiDIPP.VAUDOIS_COMPLETE, dateTraitement, 1000000, null, null, null, 1, tiersService, adresseService);
	}


	public void addNonEmisePourRaisonInconnue(Long noCtb, @Nullable RegDate dateDebut, @Nullable RegDate dateFin) {
		diNonEmises.add(
				new LigneRapport(
						String.valueOf(noCtb),
						RegDateHelper.dateToDisplayString(dateDebut),
						RegDateHelper.dateToDisplayString(dateFin),
						"Impossible de déterminer la raison pour laquelle le contribuable n'a pas reçue de DI",
						""
				)
		);
	}

	@Override
	public void addErrorDICollision(Contribuable ctb, RegDate dateDebut, RegDate dateFin, String details) {
		diNonEmises.add(
				new LigneRapport(
						String.valueOf(ctb.getId()),
						RegDateHelper.dateToDisplayString(dateDebut),
						RegDateHelper.dateToDisplayString(dateFin),
						ErreurType.COLLISION_DI.description(),
						details
				)
		);
	}

	@Override
	public void addErrorException(Long idCtb, Exception e) {
		diNonEmises.add(
				new LigneRapport(
						String.valueOf(idCtb),
						"",
						"",
						ErreurType.EXCEPTION.description(),
						e.getMessage()
				)
		);
	}

	@Override
	public void addErrorException(Contribuable ctb, Exception e) {
		diNonEmises.add(
				new LigneRapport(
						String.valueOf(ctb.getId()),
						"",
						"",
						ErreurType.EXCEPTION.description(),
						e.getMessage()
				)
		);
	}

	@Override
	public void addErrorForGestionNul(Contribuable ctb, @Nullable RegDate dateDebut, @Nullable RegDate dateFin, String details) {
		diNonEmises.add(
				new LigneRapport(
						String.valueOf(ctb.getId()),
						RegDateHelper.dateToDisplayString(dateDebut),
						RegDateHelper.dateToDisplayString(dateFin),
						ErreurType.FOR_GESTION_NUL.description(),
						details
				)
		);
	}

	@Override
	public void addIgnoreDIDejaExistante(Contribuable ctb, RegDate dateDebut, RegDate dateFin) {
		diNonEmises.add(
				new LigneRapport(
						String.valueOf(ctb.getId()),
						RegDateHelper.dateToDisplayString(dateDebut),
						RegDateHelper.dateToDisplayString(dateFin),
						IgnoreType.DI_DEJA_EXISTANTE.description(),
						""
				)
		);
	}

	public List<LigneRapport> getLignes() {
		return diNonEmises;
	}

	public int getNombreDeDIsNonEmises() {
		return diNonEmises != null ? diNonEmises.size() : 0;
	}

	public void addTacheNonTraitee(Long id) {
		diNonEmises.add(
				new LigneRapport(
						String.valueOf(id),
						"",
						"",
						"La tâche pour l'envoi de la DI n'a pas été traîtée",
						""
				)
		);
	}

	public void addEntrainDEtreEmise(Long id) {
		diNonEmises.add(
				new LigneRapport(
						String.valueOf(id),
						"",
						"",
						"La tâche pour l'envoi de la DI est entrain d'être traîtée !",
						"Le batch d'envoi des DIs tourne en même temps que le batch de creation de la liste des DIs non émises ?!?"
				)
		);
	}

	@Override
	public void addAll(ListeDIsPPNonEmises rapport) {
		super.addAll(rapport);
		diNonEmises.addAll(rapport.diNonEmises);
	}
}
