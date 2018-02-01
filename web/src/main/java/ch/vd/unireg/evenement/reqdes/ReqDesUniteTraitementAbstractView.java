package ch.vd.unireg.evenement.reqdes;

import java.io.Serializable;
import java.util.Date;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.NomPrenom;
import ch.vd.unireg.reqdes.EtatTraitement;
import ch.vd.unireg.reqdes.EvenementReqDes;
import ch.vd.unireg.reqdes.InformationsActeur;
import ch.vd.unireg.reqdes.UniteTraitement;

public abstract class ReqDesUniteTraitementAbstractView implements Serializable {

	private static final long serialVersionUID = 8744735065807279084L;

	private final long id;
	private final EtatTraitement etat;
	private final NomPrenom notaire;
	private final String visaNotaire;
	private final NomPrenom operateur;
	private final String visaOperateur;
	private final String numeroMinute;
	private final RegDate dateActe;
	private final Date dateTraitement;

	public ReqDesUniteTraitementAbstractView(UniteTraitement ut) {
		final EvenementReqDes evenement = ut.getEvenement();

		this.id = ut.getId();
		this.etat = ut.getEtat();
		this.dateActe = evenement.getDateActe();
		this.numeroMinute = evenement.getNumeroMinute();
		this.dateTraitement = ut.getDateTraitement();

		final InformationsActeur notaire = evenement.getNotaire();
		this.notaire = new NomPrenom(notaire.getNom(), notaire.getPrenom());
		this.visaNotaire = notaire.getVisa();

		final InformationsActeur operateur = evenement.getOperateur();
		if (operateur != null) {
			this.operateur = new NomPrenom(operateur.getNom(), operateur.getPrenom());
			this.visaOperateur = operateur.getVisa();
		}
		else {
			this.operateur = null;
			this.visaOperateur = null;
		}
	}

	public long getId() {
		return id;
	}

	public EtatTraitement getEtat() {
		return etat;
	}

	public NomPrenom getNotaire() {
		return notaire;
	}

	public String getVisaNotaire() {
		return visaNotaire;
	}

	public NomPrenom getOperateur() {
		return operateur;
	}

	public String getVisaOperateur() {
		return visaOperateur;
	}

	public String getNumeroMinute() {
		return numeroMinute;
	}

	public RegDate getDateActe() {
		return dateActe;
	}

	public Date getDateTraitement() {
		return dateTraitement;
	}

	public boolean isRecyclable() {
		return etat == EtatTraitement.A_TRAITER || etat == EtatTraitement.EN_ERREUR;
	}

	public boolean isForceable() {
		return etat != EtatTraitement.TRAITE && etat != EtatTraitement.FORCE;
	}
}
