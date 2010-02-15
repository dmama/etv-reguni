package ch.vd.uniregctb.couple.view;

import java.util.Date;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.general.view.TiersGeneralView;
import ch.vd.uniregctb.type.EtatCivil;

public class CoupleRecapView {

	private Date dateDebut;
	
	private EtatCivil etatCivil;
	
	private TiersGeneralView premierePersonne;

	private TiersGeneralView secondePersonne;

	/**
	 * Indique si le couple sera enregistré dans un nouveau ménage commun.
	 */
	private boolean nouveauCtb = true;

	/**
	 * Indique quel est le contribuable ouvert existant à utiliser comme ménage commun. 
	 */
	private TiersGeneralView troisiemeTiers;
	
	private TypeUnion typeUnion = TypeUnion.COUPLE;
	
	private RegDate dateCoupleExistant;
	
	private String remarque;
	
	public TiersGeneralView getPremierePersonne() {
		return premierePersonne;
	}

	public void setPremierePersonne(TiersGeneralView premierePersonne) {
		this.premierePersonne = premierePersonne;
	}

	public TiersGeneralView getSecondePersonne() {
		return secondePersonne;
	}

	public void setSecondePersonne(TiersGeneralView secondePersonne) {
		this.secondePersonne = secondePersonne;
	}

	public TiersGeneralView getTroisiemeTiers() {
		return troisiemeTiers;
	}

	public void setTroisiemeTiers(TiersGeneralView troisiemeTiers) {
		this.troisiemeTiers = troisiemeTiers;
	}

	public Date getDateDebut() {
		return dateDebut;
	}

	public void setDateDebut(Date dateDebut) {
		this.dateDebut = dateDebut;
	}

	public EtatCivil getEtatCivil() {
		return etatCivil;
	}

	public void setEtatCivil(EtatCivil etatCivil) {
		this.etatCivil = etatCivil;
	}

	public TypeUnion getTypeUnion() {
		return typeUnion;
	}

	public void setTypeUnion(TypeUnion typeUnion) {
		this.typeUnion = typeUnion;
	}


	public String getRemarque() {
		return remarque;
	}

	public void setRemarque(String remarque) {
		this.remarque = remarque;
	}

	public boolean isNouveauCtb() {
		return nouveauCtb;
	}

	public void setNouveauCtb(boolean nouveauCtb) {
		this.nouveauCtb = nouveauCtb;
	}

	public RegDate getDateCoupleExistant() {
		return dateCoupleExistant;
	}

	public void setDateCoupleExistant(RegDate dateCoupleExistant) {
		this.dateCoupleExistant = dateCoupleExistant;
	}
}
