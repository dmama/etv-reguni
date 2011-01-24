package ch.vd.uniregctb.tiers.view;

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
	private Boolean sansSommation;
	private Boolean sansListeRecapitulative;
	private PeriodiciteDecompte periodiciteCourante;
	private PeriodeDecompte periodeDecompte;
	private ComplementView complement;

	public DebiteurEditView() {
		this.complement = new ComplementView();
	}

	public DebiteurEditView(DebiteurPrestationImposable dpi, IbanValidator ibanValidator) {
		this.id = dpi.getNumero();
		this.idCtbAssocie = dpi.getContribuableId();
		this.categorieImpotSource = dpi.getCategorieImpotSource();
		this.modeCommunication = dpi.getModeCommunication();
		this.sansSommation = dpi.getSansRappel();
		this.sansListeRecapitulative = dpi.getSansListeRecapitulative();

		final Periodicite periodicite = dpi.getDernierePeriodicite();
		if (periodicite != null) {
			this.periodiciteCourante = periodicite.getPeriodiciteDecompte();
			this.periodeDecompte = periodicite.getPeriodeDecompte();
		}

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

	public Boolean getSansSommation() {
		return sansSommation;
	}

	public void setSansSommation(Boolean sansSommation) {
		this.sansSommation = sansSommation;
	}

	public Boolean getSansListeRecapitulative() {
		return sansListeRecapitulative;
	}

	public void setSansListeRecapitulative(Boolean sansListeRecapitulative) {
		this.sansListeRecapitulative = sansListeRecapitulative;
	}

	public PeriodiciteDecompte getPeriodiciteCourante() {
		return periodiciteCourante;
	}

	public void setPeriodiciteCourante(PeriodiciteDecompte periodiciteCourante) {
		this.periodiciteCourante = periodiciteCourante;
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
}
