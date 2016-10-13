package ch.vd.uniregctb.entreprise;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.RaisonSocialeFiscaleEntreprise;

public abstract class RaisonSocialeView implements DateRange {

	private Long tiersId;
	private RegDate dateDebut;
	private RegDate dateFin;
	private String raisonSociale;

	public RaisonSocialeView() {}

	public RaisonSocialeView(Long tiersId, RegDate dateDebut, RegDate dateFin, String raisonSociale) {
		this.tiersId = tiersId;
		this.dateDebut = dateDebut;
		this.dateFin = dateFin;
		this.raisonSociale = raisonSociale;
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

	public String getRaisonSociale() {
		return raisonSociale;
	}

	public void setRaisonSociale(String raisonSociale) {
		this.raisonSociale = raisonSociale;
	}

	/**
	 * Classe concrète pour l'ajout
	 */
	public static final class Add extends RaisonSocialeView {
		public Add() {
		}

		public Add(Long tiersId, RegDate dateDebut, RegDate dateFin, String raisonSociale) {
			super(tiersId, dateDebut, dateFin, raisonSociale);
		}
	}

	/**
	 * Classe concrète pour l'édition
	 */
	public static final class Edit extends RaisonSocialeView {
		private Long id;

		public Edit() {
		}

		public Edit(RaisonSocialeFiscaleEntreprise rs) {
			this(rs.getId(), rs.getEntreprise().getNumero(), rs.getDateDebut(), rs.getDateFin(), rs.getRaisonSociale());
		}

		public Edit(Long id, Long tiersId, RegDate dateDebut, RegDate dateFin, String raisonSociale) {
			super(tiersId, dateDebut, dateFin, raisonSociale);
			this.id = id;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}
	}
}
