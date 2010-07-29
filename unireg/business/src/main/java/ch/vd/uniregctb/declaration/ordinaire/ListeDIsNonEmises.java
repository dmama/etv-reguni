package ch.vd.uniregctb.declaration.ordinaire;

import java.util.ArrayList;
import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.metier.assujettissement.TypeContribuableDI;
import ch.vd.uniregctb.tiers.Contribuable;

public class ListeDIsNonEmises extends EnvoiDIsResults<ListeDIsNonEmises> {

	/**
	 * Classe pour stocker les informations d'une ligne du fichier csv resulat
	 *
	 * @author xsifnr
	 *
	 */
	public class LigneRapport {

		private String nbCtb;
		private String dateDebut;
		private String dateFin;
		private String raison;
		private String details;

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

		public void setNbCtb(String nbCtb) {
			this.nbCtb = nbCtb;
		}

		public String getRaison() {
			return raison;
		}

		public void setRaison(String raison) {
			this.raison = raison;
		}

		public String getDetails() {
			return details;
		}

		public void setDetails(String details) {
			this.details = details;
		}

		public String getDateDebut() {
			return dateDebut;
		}

		public void setDateDebut(String dateDebut) {
			this.dateDebut = dateDebut;
		}

		public String getDateFin() {
			return dateFin;
		}

		public void setDateFin(String dateFin) {
			this.dateFin = dateFin;
		}


	}


	private final List<LigneRapport> diNonEmises = new ArrayList<LigneRapport>();


	public ListeDIsNonEmises(int anneePeriode, RegDate dateTraitement) {
		super (anneePeriode, TypeContribuableDI.VAUDOIS_ORDINAIRE, dateTraitement, 1000000, null, null);
	}


	public void addNonEmisePourRaisonInconnue(Long noCtb, RegDate dateDebut, RegDate dateFin) {
		diNonEmises.add(
			new LigneRapport(
				"" + noCtb,
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
				"" + ctb.getId(),
				RegDateHelper.dateToDisplayString(dateDebut),
				RegDateHelper.dateToDisplayString(dateFin),
				ErreurType.COLLISION_DI.description(),
				details
			)
		);
	}

	public void addErrorException(Long idCtb, Exception e) {
		diNonEmises.add(
				new LigneRapport(
					"" + idCtb,
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
				"" + ctb.getId(),
				"",
				"",
				ErreurType.EXCEPTION.description(),
				e.getMessage()
			)
		);
	}

	@Override
	public void addErrorForGestionNul(Contribuable ctb, RegDate dateDebut, RegDate dateFin, String details) {
		diNonEmises.add(
			new LigneRapport(
				"" + ctb.getId(),
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
				"" + ctb.getId(),
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
					"" + id,
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
					"" + id,
					"",
					"",
					"La tâche pour l'envoi de la DI est entrain d'être traîtée !",
					"Le batch d'envoi des DIs tourne en même temps que le batch de creation de la liste des DIs non émises ?!?"
				)
			);
	}

	public void addContribuableInvalide(Long ctbId, ValidationResults results) {
		StringBuilder sbErr = null;
		for (String err : results.getErrors()) {
			 if (sbErr == null) {
				 sbErr = new StringBuilder("ERREURS : ");
			 } else {
				 sbErr.append(", ");
			 }
			 sbErr.append(err);
		}
		StringBuilder sbWarn = null;
		for (String warn : results.getWarnings()) {
			 if (sbWarn == null) {
				 sbWarn = new StringBuilder("WARNING : ");
			 } else {
				 sbWarn.append(", ");
			 }
			 sbWarn.append(warn);
		}

		String messageDetail;
		if (sbErr != null && sbWarn == null) {
			messageDetail = sbErr.toString();
		} else if (sbErr == null && sbWarn != null) {
			messageDetail = sbWarn.toString();
		} else if (sbErr != null && sbWarn != null) {
			messageDetail = sbErr.toString() + " - " + sbWarn.toString();
		} else {
			messageDetail = "";
		}

		diNonEmises.add(
				new LigneRapport(
					"" + ctbId,
					"",
					"",
					"Ce contribuable n'est pas valide",
					messageDetail
				)
			);
	}

	public void addAll(ListeDIsNonEmises rapport) {
		super.addAll(rapport);
		diNonEmises.addAll(rapport.diNonEmises);
	}
}
