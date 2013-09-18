package ch.vd.uniregctb.tiers.view;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.declaration.Periodicite;
import ch.vd.uniregctb.iban.IbanValidator;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.type.CategorieImpotSource;
import ch.vd.uniregctb.type.ModeCommunication;
import ch.vd.uniregctb.type.PeriodeDecompte;
import ch.vd.uniregctb.type.PeriodiciteDecompte;

public class DebiteurEditView {

	private Long id;
	private Long idCtbAssocie;
	private CategorieImpotSource categorieImpotSource;
	private ModeCommunication modeCommunication;
	private PeriodiciteDecompte nouvellePeriodicite;
	private final PeriodiciteDecompte periodiciteActive;
	private RegDate dateDebutNouvellePeriodicite;
	private PeriodeDecompte periodeDecompte;
	private final ComplementView complement;
	private Long logicielId;
	private boolean sansLREmises;

	public DebiteurEditView() {
		this.complement = new ComplementView();
		this.periodiciteActive = null;
	}

	public DebiteurEditView(DebiteurPrestationImposable dpi, IbanValidator ibanValidator) {
		this.id = dpi.getNumero();
		this.idCtbAssocie = dpi.getContribuableId();
		this.categorieImpotSource = dpi.getCategorieImpotSource();
		this.modeCommunication = dpi.getModeCommunication();
		this.logicielId = dpi.getLogicielId();
		this.sansLREmises = dpi.isSansLREmises();
		final Periodicite dernierePeriodicite = dpi.getDernierePeriodicite();
		if (dernierePeriodicite != null) {
			this.nouvellePeriodicite = dernierePeriodicite.getPeriodiciteDecompte();
			this.periodeDecompte = dernierePeriodicite.getPeriodeDecompte();
			this.dateDebutNouvellePeriodicite = dernierePeriodicite.getDateDebut();
		}
		final Periodicite periodiciteActive = dpi.getPeriodiciteAt(RegDate.get());
		this.periodiciteActive = periodiciteActive != null ? periodiciteActive.getPeriodiciteDecompte() : null;
		this.complement = new ComplementView(dpi, null, null, ibanValidator);
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getIdCtbAssocie() {
		return idCtbAssocie;
	}

	public void setIdCtbAssocie(Long idCtbAssocie) {
		this.idCtbAssocie = idCtbAssocie;
	}

	public CategorieImpotSource getCategorieImpotSource() {
		return categorieImpotSource;
	}

	public void setCategorieImpotSource(CategorieImpotSource categorieImpotSource) {
		this.categorieImpotSource = categorieImpotSource;
	}

	public ModeCommunication getModeCommunication() {
		return modeCommunication;
	}

	public void setModeCommunication(ModeCommunication modeCommunication) {
		this.modeCommunication = modeCommunication;
	}

	public PeriodiciteDecompte getNouvellePeriodicite() {
		return nouvellePeriodicite;
	}

	public void setNouvellePeriodicite(PeriodiciteDecompte nouvellePeriodicite) {
		this.nouvellePeriodicite = nouvellePeriodicite;
	}

	public PeriodiciteDecompte getPeriodiciteActive() {
		return periodiciteActive;
	}

	public PeriodeDecompte getPeriodeDecompte() {
		return periodeDecompte;
	}

	public void setPeriodeDecompte(PeriodeDecompte periodeDecompte) {
		this.periodeDecompte = periodeDecompte;
	}

	public ComplementView getComplement() {
		return complement;
	}

	public Long getLogicielId() {
		return logicielId;
	}

	public void setLogicielId(Long logicielId) {
		this.logicielId = logicielId;
	}

	public boolean isSansLREmises() {
		return sansLREmises;
	}

	public void setSansLREmises(boolean sansLREmises) {
		this.sansLREmises = sansLREmises;
	}

	public RegDate getDateDebutNouvellePeriodicite() {
		return dateDebutNouvellePeriodicite;
	}

	public void setDateDebutNouvellePeriodicite(RegDate dateDebutNouvellePeriodicite) {
		this.dateDebutNouvellePeriodicite = dateDebutNouvellePeriodicite;
	}
}
