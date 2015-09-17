package ch.vd.uniregctb.tiers.view;

import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.CollectionsUtils;
import ch.vd.uniregctb.declaration.Declaration;
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
	private RegDate dateDebutNouvellePeriodicite;
	private final PeriodiciteDecompte periodiciteActive;        // Attention, ce n'est pas réellement la périodicité active, mais peut représenter la dernière périodicité couverte par des LR
	private final RegDate dateDebutPeriodiciteActive;
	private PeriodeDecompte periodeDecompte;
	private final ComplementView complement;
	private Long logicielId;
	private boolean sansLREmises;

	public DebiteurEditView() {
		this.complement = new ComplementView();
		this.periodiciteActive = null;
		this.dateDebutPeriodiciteActive = null;
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

		final Periodicite periodiciteActive = dpi.getPeriodiciteAt(getDateReferencePourPeriodiciteActive(dpi));
		this.periodiciteActive = periodiciteActive != null ? periodiciteActive.getPeriodiciteDecompte() : null;
		this.dateDebutPeriodiciteActive = periodiciteActive != null ? periodiciteActive.getDateDebut() : null;
		this.complement = new ComplementView(dpi, ibanValidator);
	}

	private static RegDate getDateReferencePourPeriodiciteActive(DebiteurPrestationImposable dpi) {
		final List<Declaration> lrTriees = dpi.getDeclarationsSorted();
		if (lrTriees != null) {
			for (Declaration lr : CollectionsUtils.revertedOrder(lrTriees)) {
				if (!lr.isAnnule()) {
					return lr.getDateFin();
				}
			}
		}
		return RegDate.get();
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

	public RegDate getDateDebutPeriodiciteActive() {
		return dateDebutPeriodiciteActive;
	}
}
