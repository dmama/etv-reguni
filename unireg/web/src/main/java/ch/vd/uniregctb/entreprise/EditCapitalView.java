package ch.vd.uniregctb.entreprise;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.tiers.CapitalFiscalEntreprise;

public class EditCapitalView implements DateRange {

	private Long id;
	private RegDate dateDebut;
	private RegDate dateFin;
	private Long tiersId;
	private Long montant;
	private String monnaie;

	public EditCapitalView() {}

	public EditCapitalView(CapitalFiscalEntreprise cf) {
		this(cf.getId(), cf.getEntreprise().getNumero(), cf.getDateDebut(), cf.getDateFin(), cf.getMontant().getMontant(), cf.getMontant().getMonnaie());
	}

	public EditCapitalView(Long id, Long tiersId, RegDate dateDebut, RegDate dateFin, Long montant, String monnaie) {
		this.id = id;
		this.dateDebut = dateDebut;
		this.dateFin = dateFin;
		this.tiersId = tiersId;
		this.montant = montant;
		this.monnaie = monnaie;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
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

	@Override
	public boolean isValidAt(RegDate date) {
		return RegDateHelper.isBetween(date, dateDebut, dateFin, NullDateBehavior.LATEST);
	}

}
