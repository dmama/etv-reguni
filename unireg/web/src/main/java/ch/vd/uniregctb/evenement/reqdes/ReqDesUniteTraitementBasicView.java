package ch.vd.uniregctb.evenement.reqdes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.NomPrenom;
import ch.vd.uniregctb.reqdes.EtatTraitement;
import ch.vd.uniregctb.reqdes.EvenementReqDes;
import ch.vd.uniregctb.reqdes.InformationsActeur;
import ch.vd.uniregctb.reqdes.PartiePrenante;
import ch.vd.uniregctb.reqdes.UniteTraitement;

public class ReqDesUniteTraitementBasicView {

	private final long id;
	private final EtatTraitement etat;
	private final NomPrenom notaire;
	private final String visaNotaire;
	private final NomPrenom operateur;
	private final String visaOperateur;
	private final String numeroMinute;
	private final RegDate dateActe;
	private final Date dateTraitement;
	private final NomPrenom partiePrenante1;
	private final NomPrenom partiePrenante2;

	public ReqDesUniteTraitementBasicView(UniteTraitement ut) {
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

		final List<PartiePrenante> partiesPrenantes = new ArrayList<>(ut.getPartiesPrenantes());
		Collections.sort(partiesPrenantes, new Comparator<PartiePrenante>() {
			@Override
			public int compare(PartiePrenante o1, PartiePrenante o2) {
				return Long.compare(o1.getId(), o2.getId());
			}
		});
		if (!partiesPrenantes.isEmpty()) {
			final PartiePrenante pp1 = partiesPrenantes.get(0);
			partiePrenante1 = new NomPrenom(pp1.getNom(), pp1.getPrenoms());
			if (partiesPrenantes.size() > 1) {
				final PartiePrenante pp2 = partiesPrenantes.get(1);
				partiePrenante2 = new NomPrenom(pp2.getNom(), pp2.getPrenoms());
			}
			else {
				partiePrenante2 = null;
			}
		}
		else {
			partiePrenante1 = null;
			partiePrenante2 = null;
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

	public NomPrenom getPartiePrenante2() {
		return partiePrenante2;
	}

	public NomPrenom getPartiePrenante1() {
		return partiePrenante1;
	}
}
