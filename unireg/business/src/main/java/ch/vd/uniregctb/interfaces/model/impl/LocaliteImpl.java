package ch.vd.uniregctb.interfaces.model.impl;

import java.io.Serializable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.CommuneSimple;
import ch.vd.uniregctb.interfaces.model.Localite;

public class LocaliteImpl implements Localite, Serializable {

	private static final long serialVersionUID = 2998886376392463517L;
	
	private CommuneSimple commune = null;
	private final RegDate dateFin;
	private Integer chiffreComplementaire;
	private Integer complementNPA;
	private Integer npa;
	private Integer noCommune;
	private Integer noOrdre;
	private String nomAbregeMajuscule;
	private String nomAbregeMinuscule;
	private String nomCompletMajuscule;
	private String nomCompletMinuscule;
	private boolean valide;

	public static LocaliteImpl get(ch.vd.infrastructure.model.Localite target) {
		if (target == null) {
			return null;
		}
		return new LocaliteImpl(target);
	}

	private LocaliteImpl(ch.vd.infrastructure.model.Localite target) {
		this.commune = CommuneSimpleImpl.get(target.getCommuneLocalite());
		this.dateFin = RegDate.get(target.getDateFinValidite());
		this.chiffreComplementaire = target.getChiffreComplementaire();
		this.complementNPA = initComplementNPA(target.getComplementNPA());
		this.npa = target.getNPA();
		this.noCommune = target.getNoCommune();
		this.noOrdre = target.getNoOrdre();
		this.nomAbregeMajuscule = target.getNomAbregeMajuscule();
		this.nomAbregeMinuscule = target.getNomAbregeMinuscule();
		this.nomCompletMajuscule = target.getNomCompletMajuscule();
		this.nomCompletMinuscule = target.getNomCompletMinuscule();
		this.valide = target.isValide();
	}

	public Integer getChiffreComplementaire() {
		return chiffreComplementaire;
	}

	public CommuneSimple getCommuneLocalite() {
		return commune;
	}

	public Integer getComplementNPA() {
		return complementNPA;
	}

	private Integer initComplementNPA(Integer c) {
		if (c == null || c == 0) {	// un complément de 0 signifie pas de complément
			return null;
		}
		else {
			return c;
		}
	}

	public RegDate getDateFinValidite() {
		return dateFin;
	}

	public Integer getNPA() {
		return npa;
	}

	public Integer getNoCommune() {
		return noCommune;
	}

	public Integer getNoOrdre() {
		return noOrdre;
	}

	public String getNomAbregeMajuscule() {
		return nomAbregeMajuscule;
	}

	public String getNomAbregeMinuscule() {
		return nomAbregeMinuscule;
	}

	public String getNomCompletMajuscule() {
		return nomCompletMajuscule;
	}

	public String getNomCompletMinuscule() {
		return nomCompletMinuscule;
	}

	public boolean isValide() {
		return valide;
	}
}
