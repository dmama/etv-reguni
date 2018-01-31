package ch.vd.uniregctb.entreprise;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.CapitalFiscalEntreprise;

public abstract class CapitalView implements DateRange {

	private Long tiersId;
	private RegDate dateDebut;
	private RegDate dateFin;
	private Long montant;
	private String monnaie;

	public CapitalView() {
	}

	public CapitalView(Long tiersId, RegDate dateDebut, RegDate dateFin, Long montant, String monnaie) {
		this.tiersId = tiersId;
		this.dateDebut = dateDebut;
		this.dateFin = dateFin;
		this.montant = montant;
		this.monnaie = monnaie;
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

	public Long getMontant() {
		return montant;
	}

	public void setMontant(Long montant) {
		this.montant = montant;
	}

	public String getMonnaie() {
		return monnaie;
	}

	public void setMonnaie(String monnaie) {
		this.monnaie = monnaie;
	}

	/**
	 * Classe concrète pour l'ajout de capital
	 */
	public static final class Add extends CapitalView {
		public Add() {
		}

		public Add(Long tiersId, RegDate dateDebut, RegDate dateFin, Long montant, String monnaie) {
			super(tiersId, dateDebut, dateFin, montant, monnaie);
		}
	}

	/**
	 * Classe concrète pour l'édition de capital
	 */
	public static final class Edit extends CapitalView {
		private long id;

		public Edit() {
		}

		public Edit(CapitalFiscalEntreprise cf) {
			this(cf.getId(), cf.getEntreprise().getNumero(), cf.getDateDebut(), cf.getDateFin(), cf.getMontant().getMontant(), cf.getMontant().getMonnaie());
		}

		public Edit(long id, Long tiersId, RegDate dateDebut, RegDate dateFin, Long montant, String monnaie) {
			super(tiersId, dateDebut, dateFin, montant, monnaie);
			this.id = id;
		}

		public long getId() {
			return id;
		}

		public void setId(long id) {
			this.id = id;
		}
	}
}
