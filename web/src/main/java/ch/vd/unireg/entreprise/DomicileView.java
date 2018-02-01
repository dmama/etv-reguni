package ch.vd.uniregctb.entreprise;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.DomicileEtablissement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

public abstract class DomicileView implements DateRange {

	private Long tiersId;
	private RegDate dateDebut;
	private RegDate dateFin;
	private TypeAutoriteFiscale typeAutoriteFiscale;
	private Integer noAutoriteFiscale;
	private String nomAutoriteFiscale;

	public DomicileView() {}

	public DomicileView(Long tiersId, RegDate dateDebut, RegDate dateFin, TypeAutoriteFiscale typeAutoriteFiscale, Integer noAutoriteFiscale, String nomAutoriteFiscale) {
		this.tiersId = tiersId;
		this.dateDebut = dateDebut;
		this.dateFin = dateFin;
		this.typeAutoriteFiscale = typeAutoriteFiscale;
		this.noAutoriteFiscale = noAutoriteFiscale;
		this.nomAutoriteFiscale = nomAutoriteFiscale;
	}

	public Long getTiersId() {
		return tiersId;
	}

	public void setTiersId(Long tiersId) {
		this.tiersId = tiersId;
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
	public static final class Add extends DomicileView {
		public Add() {
		}

		public Add(Long tiersId, RegDate dateDebut, RegDate dateFin, TypeAutoriteFiscale typeAutoriteFiscale, Integer noAutoriteFiscale, String nomAutoriteFiscale) {
			super(tiersId, dateDebut, dateFin, typeAutoriteFiscale, noAutoriteFiscale, nomAutoriteFiscale);
		}
	}

	/**
	 * Classe concrète pour l'édition
	 */
	public static final class Edit extends DomicileView {
		private Long id;
		private boolean peutEditerDateFin;

		public Edit() {
		}

		public Edit(DomicileEtablissement dom, boolean peutEditerDateFin) {
			this(dom.getId(), dom.getEtablissement().getNumero(), dom.getDateDebut(), dom.getDateFin(), dom.getTypeAutoriteFiscale(), dom.getNumeroOfsAutoriteFiscale(), peutEditerDateFin);
		}

		public Edit(Long id, Long tiersId, RegDate dateDebut, RegDate dateFin, TypeAutoriteFiscale typeAutoriteFiscale, Integer noAutoriteFiscale, boolean peutEditerDateFin) {
			super(tiersId, dateDebut, dateFin, typeAutoriteFiscale, noAutoriteFiscale, null);
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
