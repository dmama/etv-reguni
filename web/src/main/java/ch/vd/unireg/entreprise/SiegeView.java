package ch.vd.unireg.entreprise;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.tiers.DomicileEtablissement;
import ch.vd.unireg.type.TypeAutoriteFiscale;

public abstract class SiegeView implements DateRange {

	private Long etablissementId;
	private Long entrepriseId;
	private RegDate dateDebut;
	private RegDate dateFin;
	private TypeAutoriteFiscale typeAutoriteFiscale;
	private Integer noAutoriteFiscale;
	private String nomAutoriteFiscale;

	public SiegeView() {
	}

	public SiegeView(Long etablissementId, Long entrepriseId, RegDate dateDebut, RegDate dateFin, TypeAutoriteFiscale typeAutoriteFiscale, Integer noAutoriteFiscale, String nomAutoriteFiscale) {
		this.etablissementId = etablissementId;
		this.entrepriseId = entrepriseId;
		this.dateDebut = dateDebut;
		this.dateFin = dateFin;
		this.typeAutoriteFiscale = typeAutoriteFiscale;
		this.noAutoriteFiscale = noAutoriteFiscale;
		this.nomAutoriteFiscale = nomAutoriteFiscale;
	}

	public Long getEtablissementId() {
		return etablissementId;
	}

	public void setEtablissementId(Long etablissementId) {
		this.etablissementId = etablissementId;
	}

	public Long getEntrepriseId() {
		return entrepriseId;
	}

	public void setEntrepriseId(Long entrepriseId) {
		this.entrepriseId = entrepriseId;
	}

	@Override
	public RegDate getDateDebut() {
		return dateDebut;
	}

	public void setDateDebut(RegDate dateDebut) {
		this.dateDebut = dateDebut;
	}

	@Override
	public RegDate getDateFin() {
		return dateFin;
	}

	public void setDateFin(RegDate dateFin) {
		this.dateFin = dateFin;
	}

	public TypeAutoriteFiscale getTypeAutoriteFiscale() {
		return typeAutoriteFiscale;
	}

	public void setTypeAutoriteFiscale(TypeAutoriteFiscale typeAutoriteFiscale) {
		this.typeAutoriteFiscale = typeAutoriteFiscale;
	}

	public Integer getNoAutoriteFiscale() {
		return noAutoriteFiscale;
	}

	public void setNoAutoriteFiscale(Integer noAutoriteFiscale) {
		this.noAutoriteFiscale = noAutoriteFiscale;
	}

	public String getNomAutoriteFiscale() {
		return nomAutoriteFiscale;
	}

	public void setNomAutoriteFiscale(String nomAutoriteFiscale) {
		this.nomAutoriteFiscale = nomAutoriteFiscale;
	}

	/**
	 * Classe concrète pour l'ajout
	 */
	public static final class Add extends SiegeView {
		public Add() {
		}

		public Add(Long etablissementId, Long entrepriseId, RegDate dateDebut, RegDate dateFin, TypeAutoriteFiscale typeAutoriteFiscale, Integer noAutoriteFiscale, String nomAutoriteFiscale) {
			super(etablissementId, entrepriseId, dateDebut, dateFin, typeAutoriteFiscale, noAutoriteFiscale, nomAutoriteFiscale);
		}
	}

	/**
	 * Classe concrète pour l'édition
	 */
	public static final class Edit extends SiegeView {
		private Long id;
		private boolean peutEditerDateFin;

		public Edit() {
		}

		public Edit(DomicileEtablissement dom, Long entrepriseId, boolean peutEditerDateFin) {
			this(dom.getId(), dom.getEtablissement().getNumero(), entrepriseId, dom.getDateDebut(), dom.getDateFin(), dom.getTypeAutoriteFiscale(), dom.getNumeroOfsAutoriteFiscale(), peutEditerDateFin);
		}

		public Edit(Long id, Long tiersId, Long entrepriseId, RegDate dateDebut, RegDate dateFin, TypeAutoriteFiscale typeAutoriteFiscale, Integer noAutoriteFiscale, boolean peutEditerDateFin) {
			super(tiersId, entrepriseId, dateDebut, dateFin, typeAutoriteFiscale, noAutoriteFiscale, null);
			this.id = id;
			this.peutEditerDateFin = peutEditerDateFin;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public boolean isPeutEditerDateFin() {
			return peutEditerDateFin;
		}

		public void setPeutEditerDateFin(boolean peutEditerDateFin) {
			this.peutEditerDateFin = peutEditerDateFin;
		}
	}
}
