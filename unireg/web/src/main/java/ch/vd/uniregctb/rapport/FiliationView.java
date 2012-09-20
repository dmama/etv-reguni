package ch.vd.uniregctb.rapport;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.uniregctb.common.NomCourrierViewPart;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportFiliation;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.Sexe;

public class FiliationView {

	private String dateDebut;
	private String dateFin;
	private RapportFiliation.Type type;
	private Long numeroAutreTiers;
	private String messageAutreTiersAbsent;
	private NomCourrierViewPart nomCourrier;
	private String toolTipMessage;

	public FiliationView(RapportFiliation filiation, String nomInd, TiersService tiersService) {
		this.dateDebut = RegDateHelper.dateToDisplayString(filiation.getDateDebut());
		this.dateFin = RegDateHelper.dateToDisplayString(filiation.getDateFin());
		this.type = filiation.getType();
		final PersonnePhysique autrePersonnePhysique = filiation.getAutrePersonnePhysique();
		if (autrePersonnePhysique == null) {
			this.numeroAutreTiers = null;
			this.messageAutreTiersAbsent = "Impossible de trouver la personne qui corresponds à l'individu n°" + filiation.getAutreIndividu().getNoTechnique();
		}
		else {
			this.numeroAutreTiers = autrePersonnePhysique.getNumero();
			this.messageAutreTiersAbsent = null;
		}

		final Individu autre = filiation.getAutreIndividu();
		this.nomCourrier = initNomCourrier(autre, tiersService);
		this.toolTipMessage = initTooltipMessage(filiation, nomInd, tiersService);
	}

	private static NomCourrierViewPart initNomCourrier(Individu autre, TiersService tiersService) {
		final String nomBrut = tiersService.getNomPrenom(autre);
		final String nom;
		if (autre.getDateDeces() != null) {
			if (autre.getSexe() == Sexe.MASCULIN) {
				nom = String.format("%s, défunt", nomBrut);
			}
			else {
				nom = String.format("%s, défunte", nomBrut);
			}
		}
		else {
			nom = nomBrut;
		}
		return new NomCourrierViewPart(nom);
	}

	private static String initTooltipMessage(RapportFiliation filiation, String nomInd, TiersService tiersService) {

		final String tooltip;

		final Individu autre = filiation.getAutreIndividu();
		final boolean ferme = filiation.getDateFin() != null;

		if (filiation.getType() == RapportFiliation.Type.ENFANT) {
			final String nomEnfant = tiersService.getNomPrenom(autre);
			tooltip = String.format("%s %s l'enfant de %s", nomEnfant, ferme ? "était" : "est", nomInd);
		}
		else {
			final String nomParent = tiersService.getNomPrenom(autre);
			final String verbe = ferme ? "était" : "est";
			final String type = autre.getSexe() == null ? "le parent" : (autre.getSexe() == Sexe.MASCULIN ? "le père" : "la mère");
			tooltip = String.format("%s %s %s de %s", nomParent, verbe, type, nomInd);
		}

		return tooltip;
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

	public RapportFiliation.Type getType() {
		return type;
	}

	public void setType(RapportFiliation.Type type) {
		this.type = type;
	}

	public Long getNumeroAutreTiers() {
		return numeroAutreTiers;
	}

	public void setNumeroAutreTiers(Long numeroAutreTiers) {
		this.numeroAutreTiers = numeroAutreTiers;
	}

	public String getMessageAutreTiersAbsent() {
		return messageAutreTiersAbsent;
	}

	public void setMessageAutreTiersAbsent(String messageAutreTiersAbsent) {
		this.messageAutreTiersAbsent = messageAutreTiersAbsent;
	}

	public NomCourrierViewPart getNomCourrier() {
		return nomCourrier;
	}

	public void setNomCourrier(NomCourrierViewPart nomCourrier) {
		this.nomCourrier = nomCourrier;
	}

	public String getToolTipMessage() {
		return toolTipMessage;
	}

	public void setToolTipMessage(String toolTipMessage) {
		this.toolTipMessage = toolTipMessage;
	}
}
