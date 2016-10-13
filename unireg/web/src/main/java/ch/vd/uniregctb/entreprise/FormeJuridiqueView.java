package ch.vd.uniregctb.entreprise;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.FormeJuridiqueFiscaleEntreprise;
import ch.vd.uniregctb.type.FormeJuridiqueEntreprise;

public abstract class FormeJuridiqueView implements DateRange {

	private Long tiersId;
	private RegDate dateDebut;
	private RegDate dateFin;
	private FormeJuridiqueEntreprise formeJuridique;

	public FormeJuridiqueView() {
	}

	public FormeJuridiqueView(Long tiersId, RegDate dateDebut, RegDate dateFin, FormeJuridiqueEntreprise formeJuridique) {
		this.tiersId = tiersId;
		this.dateDebut = dateDebut;
		this.dateFin = dateFin;
		this.formeJuridique = formeJuridique;
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

	public FormeJuridiqueEntreprise getFormeJuridique() {
		return formeJuridique;
	}

	public void setFormeJuridique(FormeJuridiqueEntreprise formeJuridique) {
		this.formeJuridique = formeJuridique;
	}

	/**
	 * Classe concrète pour l'ajout
	 */
	public static final class Add extends FormeJuridiqueView {
		public Add() {
		}

		public Add(Long tiersId, RegDate dateDebut, RegDate dateFin, FormeJuridiqueEntreprise formeJuridique) {
			super(tiersId, dateDebut, dateFin, formeJuridique);
		}
	}

	/**
	 * Classe concrète pour l'édition
	 */
	public static final class Edit extends FormeJuridiqueView {
		private Long id;

		public Edit() {
		}

		public Edit(FormeJuridiqueFiscaleEntreprise fj) {
			this(fj.getId(), fj.getEntreprise().getNumero(), fj.getDateDebut(), fj.getDateFin(), fj.getFormeJuridique());
		}

		public Edit(Long id, Long tiersId, RegDate dateDebut, RegDate dateFin, FormeJuridiqueEntreprise formeJuridique) {
			super(tiersId, dateDebut, dateFin, formeJuridique);
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
