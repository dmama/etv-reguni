package ch.vd.unireg.interfaces.infra.data;

import java.io.Serializable;

import ch.vd.registre.base.date.RegDate;

public class LocaliteImpl implements Localite, Serializable {

	private static final long serialVersionUID = -8507140048340746513L;
	
	private final Commune commune;
	private final RegDate dateFin;
	private final Integer chiffreComplementaire;
	private final Integer complementNPA;
	private final Integer npa;
	private final Integer noCommune;
	private final Integer noOrdre;
	private final String nomAbregeMajuscule;
	private final String nomAbregeMinuscule;
	private final String nomCompletMajuscule;
	private final String nomCompletMinuscule;
	private final boolean valide;

	public static LocaliteImpl get(ch.vd.infrastructure.model.Localite target) {
		if (target == null) {
			return null;
		}
		return new LocaliteImpl(target);
	}

	private LocaliteImpl(ch.vd.infrastructure.model.Localite target) {
		this.commune = CommuneImpl.get(target.getCommuneLocalite());
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

	@Override
	public Integer getChiffreComplementaire() {
		return chiffreComplementaire;
	}

	@Override
	public Commune getCommuneLocalite() {
		return commune;
	}

	@Override
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

	@Override
	public RegDate getDateFinValidite() {
		return dateFin;
	}

	@Override
	public Integer getNPA() {
		return npa;
	}

	@Override
	public Integer getNoCommune() {
		return noCommune;
	}

	@Override
	public Integer getNoOrdre() {
		return noOrdre;
	}

	@Override
	public String getNomAbregeMajuscule() {
		return nomAbregeMajuscule;
	}

	@Override
	public String getNomAbregeMinuscule() {
		return nomAbregeMinuscule;
	}

	@Override
	public String getNomCompletMajuscule() {
		return nomCompletMajuscule;
	}

	@Override
	public String getNomCompletMinuscule() {
		return nomCompletMinuscule;
	}

	@Override
	public boolean isValide() {
		return valide;
	}
}
