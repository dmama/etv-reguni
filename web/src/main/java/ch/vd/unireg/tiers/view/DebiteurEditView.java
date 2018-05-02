package ch.vd.unireg.tiers.view;

import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.CollectionsUtils;
import ch.vd.unireg.declaration.DeclarationImpotSource;
import ch.vd.unireg.declaration.Periodicite;
import ch.vd.unireg.tiers.DebiteurPrestationImposable;
import ch.vd.unireg.type.CategorieImpotSource;
import ch.vd.unireg.type.ModeCommunication;
import ch.vd.unireg.type.PeriodeDecompte;
import ch.vd.unireg.type.PeriodiciteDecompte;

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
	private Long logicielId;
	private boolean sansLREmises;

	public DebiteurEditView() {
		this.periodiciteActive = null;
		this.dateDebutPeriodiciteActive = null;
	}

	public DebiteurEditView(DebiteurPrestationImposable dpi) {
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
	}

	private static RegDate getDateReferencePourPeriodiciteActive(DebiteurPrestationImposable dpi) {
		final List<DeclarationImpotSource> lrs = dpi.getDeclarationsTriees(DeclarationImpotSource.class, false);
		return lrs.isEmpty() ? RegDate.get() : CollectionsUtils.getLastElement(lrs).getDateFin();
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
