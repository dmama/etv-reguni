package ch.vd.unireg.param.view;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.ObjectNotFoundException;
import ch.vd.unireg.declaration.PeriodeFiscale;
import ch.vd.unireg.parametrage.ParametrePeriodeFiscaleEmolument;
import ch.vd.unireg.parametrage.ParametrePeriodeFiscalePP;
import ch.vd.unireg.type.TypeDocumentEmolument;

public class ParametrePeriodeFiscalePPEditView {

	private Long idPeriodeFiscale;
	private Integer anneePeriodeFiscale;
	private boolean codeControleSurSommationDI;

	private boolean emolumentSommationDI;
	private Integer montantEmolumentSommationDI;

	private RegDate sommationReglementaireVaud;
	private RegDate sommationEffectiveVaud;
	private RegDate finEnvoiMasseDIVaud;

	private RegDate sommationReglementaireHorsCanton;
	private RegDate sommationEffectiveHorsCanton;
	private RegDate finEnvoiMasseDIHorsCanton;

	private RegDate sommationReglementaireHorsSuisse;
	private RegDate sommationEffectiveHorsSuisse;
	private RegDate finEnvoiMasseDIHorsSuisse;

	private RegDate sommationReglementaireDepense;
	private RegDate sommationEffectiveDepense;
	private RegDate finEnvoiMasseDIDepense;

	private RegDate sommationReglementaireDiplomate;
	private RegDate sommationEffectiveDiplomate;
	private RegDate finEnvoiMasseDIDiplomate;

	public ParametrePeriodeFiscalePPEditView() {
	}

	public ParametrePeriodeFiscalePPEditView(@NotNull PeriodeFiscale pf) {
		this.idPeriodeFiscale = pf.getId();
		this.anneePeriodeFiscale = pf.getAnnee();
		this.codeControleSurSommationDI = pf.isShowCodeControleSommationDeclarationPP();

		final ParametrePeriodeFiscaleEmolument emolument = pf.getParametrePeriodeFiscaleEmolument(TypeDocumentEmolument.SOMMATION_DI_PP);
		this.montantEmolumentSommationDI = emolument != null ? emolument.getMontant() : null;
		this.emolumentSommationDI = emolument != null && emolument.getMontant() != null;

		this.finEnvoiMasseDIDepense = pf.getParametrePeriodeFiscalePPDepense().getDateFinEnvoiMasseDI();
		this.finEnvoiMasseDIDiplomate = pf.getParametrePeriodeFiscalePPDiplomateSuisse().getDateFinEnvoiMasseDI();
		this.finEnvoiMasseDIHorsCanton = pf.getParametrePeriodeFiscalePPHorsCanton().getDateFinEnvoiMasseDI();
		this.finEnvoiMasseDIHorsSuisse = pf.getParametrePeriodeFiscalePPHorsSuisse().getDateFinEnvoiMasseDI();
		this.finEnvoiMasseDIVaud = pf.getParametrePeriodeFiscalePPVaudoisOrdinaire().getDateFinEnvoiMasseDI();

		this.sommationEffectiveDepense = pf.getParametrePeriodeFiscalePPDepense().getTermeGeneralSommationEffectif();
		this.sommationEffectiveDiplomate = pf.getParametrePeriodeFiscalePPDiplomateSuisse().getTermeGeneralSommationEffectif();
		this.sommationEffectiveHorsCanton = pf.getParametrePeriodeFiscalePPHorsCanton().getTermeGeneralSommationEffectif();
		this.sommationEffectiveHorsSuisse = pf.getParametrePeriodeFiscalePPHorsSuisse().getTermeGeneralSommationEffectif();
		this.sommationEffectiveVaud = pf.getParametrePeriodeFiscalePPVaudoisOrdinaire().getTermeGeneralSommationEffectif();

		this.sommationReglementaireDepense = pf.getParametrePeriodeFiscalePPDepense().getTermeGeneralSommationReglementaire();
		this.sommationReglementaireDiplomate = pf.getParametrePeriodeFiscalePPDiplomateSuisse().getTermeGeneralSommationReglementaire();
		this.sommationReglementaireHorsCanton = pf.getParametrePeriodeFiscalePPHorsCanton().getTermeGeneralSommationReglementaire();
		this.sommationReglementaireHorsSuisse = pf.getParametrePeriodeFiscalePPHorsSuisse().getTermeGeneralSommationReglementaire();
		this.sommationReglementaireVaud = pf.getParametrePeriodeFiscalePPVaudoisOrdinaire().getTermeGeneralSommationReglementaire();
	}

	public Integer getAnneePeriodeFiscale() {
		return anneePeriodeFiscale;
	}

	public void setAnneePeriodeFiscale(Integer anneePeriodeFiscale) {
		this.anneePeriodeFiscale = anneePeriodeFiscale;
	}

	public boolean isEmolumentSommationDI() {
		return emolumentSommationDI;
	}

	public void setEmolumentSommationDI(boolean emolumentSommationDI) {
		this.emolumentSommationDI = emolumentSommationDI;
	}

	public Integer getMontantEmolumentSommationDI() {
		return montantEmolumentSommationDI;
	}

	public void setMontantEmolumentSommationDI(Integer montantEmolumentSommationDI) {
		this.montantEmolumentSommationDI = montantEmolumentSommationDI;
	}

	public RegDate getSommationReglementaireVaud() {
		return sommationReglementaireVaud;
	}

	public void setSommationReglementaireVaud(RegDate sommationReglementaireVaud) {
		this.sommationReglementaireVaud = sommationReglementaireVaud;
	}

	public RegDate getSommationEffectiveVaud() {
		return sommationEffectiveVaud;
	}

	public void setSommationEffectiveVaud(RegDate sommationEffectiveVaud) {
		this.sommationEffectiveVaud = sommationEffectiveVaud;
	}

	public RegDate getFinEnvoiMasseDIVaud() {
		return finEnvoiMasseDIVaud;
	}

	public void setFinEnvoiMasseDIVaud(RegDate finEnvoiMasseDIVaud) {
		this.finEnvoiMasseDIVaud = finEnvoiMasseDIVaud;
	}

	public RegDate getSommationReglementaireHorsCanton() {
		return sommationReglementaireHorsCanton;
	}

	public void setSommationReglementaireHorsCanton(RegDate sommationReglementaireHorsCanton) {
		this.sommationReglementaireHorsCanton = sommationReglementaireHorsCanton;
	}

	public RegDate getSommationEffectiveHorsCanton() {
		return sommationEffectiveHorsCanton;
	}

	public void setSommationEffectiveHorsCanton(RegDate sommationEffectiveHorsCanton) {
		this.sommationEffectiveHorsCanton = sommationEffectiveHorsCanton;
	}

	public RegDate getFinEnvoiMasseDIHorsCanton() {
		return finEnvoiMasseDIHorsCanton;
	}

	public void setFinEnvoiMasseDIHorsCanton(RegDate finEnvoiMasseDIHorsCanton) {
		this.finEnvoiMasseDIHorsCanton = finEnvoiMasseDIHorsCanton;
	}

	public RegDate getSommationReglementaireHorsSuisse() {
		return sommationReglementaireHorsSuisse;
	}

	public void setSommationReglementaireHorsSuisse(RegDate sommationReglementaireHorsSuisse) {
		this.sommationReglementaireHorsSuisse = sommationReglementaireHorsSuisse;
	}

	public RegDate getSommationEffectiveHorsSuisse() {
		return sommationEffectiveHorsSuisse;
	}

	public void setSommationEffectiveHorsSuisse(RegDate sommationEffectiveHorsSuisse) {
		this.sommationEffectiveHorsSuisse = sommationEffectiveHorsSuisse;
	}

	public RegDate getFinEnvoiMasseDIHorsSuisse() {
		return finEnvoiMasseDIHorsSuisse;
	}

	public void setFinEnvoiMasseDIHorsSuisse(RegDate finEnvoiMasseDIHorsSuisse) {
		this.finEnvoiMasseDIHorsSuisse = finEnvoiMasseDIHorsSuisse;
	}

	public RegDate getSommationReglementaireDepense() {
		return sommationReglementaireDepense;
	}

	public void setSommationReglementaireDepense(RegDate sommationReglementaireDepense) {
		this.sommationReglementaireDepense = sommationReglementaireDepense;
	}

	public RegDate getSommationEffectiveDepense() {
		return sommationEffectiveDepense;
	}

	public void setSommationEffectiveDepense(RegDate sommationEffectiveDepense) {
		this.sommationEffectiveDepense = sommationEffectiveDepense;
	}

	public RegDate getFinEnvoiMasseDIDepense() {
		return finEnvoiMasseDIDepense;
	}

	public void setFinEnvoiMasseDIDepense(RegDate finEnvoiMasseDIDepense) {
		this.finEnvoiMasseDIDepense = finEnvoiMasseDIDepense;
	}

	public Long getIdPeriodeFiscale() {
		return idPeriodeFiscale;
	}

	public void setIdPeriodeFiscale(Long idPeriodeFiscale) {
		this.idPeriodeFiscale = idPeriodeFiscale;
	}

	public RegDate getSommationReglementaireDiplomate() {
		return sommationReglementaireDiplomate;
	}

	public void setSommationReglementaireDiplomate(RegDate sommationReglementaireDiplomate) {
		this.sommationReglementaireDiplomate = sommationReglementaireDiplomate;
	}

	public RegDate getSommationEffectiveDiplomate() {
		return sommationEffectiveDiplomate;
	}

	public void setSommationEffectiveDiplomate(RegDate sommationEffectiveDiplomate) {
		this.sommationEffectiveDiplomate = sommationEffectiveDiplomate;
	}

	public RegDate getFinEnvoiMasseDIDiplomate() {
		return finEnvoiMasseDIDiplomate;
	}

	public void setFinEnvoiMasseDIDiplomate(RegDate finEnvoiMasseDIDiplomate) {
		this.finEnvoiMasseDIDiplomate = finEnvoiMasseDIDiplomate;
	}

	public boolean isCodeControleSurSommationDI() {
		return codeControleSurSommationDI;
	}

	public void setCodeControleSurSommationDI(boolean codeControleSurSommationDI) {
		this.codeControleSurSommationDI = codeControleSurSommationDI;
	}

	public void saveTo(@NotNull PeriodeFiscale pf) {
		pf.setShowCodeControleSommationDeclarationPP(this.isCodeControleSurSommationDI());

		final ParametrePeriodeFiscaleEmolument emolument = pf.getParametrePeriodeFiscaleEmolument(TypeDocumentEmolument.SOMMATION_DI_PP);
		if (this.isEmolumentSommationDI()) {
			if (emolument == null) {
				final ParametrePeriodeFiscaleEmolument param = new ParametrePeriodeFiscaleEmolument();
				param.setTypeDocument(TypeDocumentEmolument.SOMMATION_DI_PP);
				param.setMontant(montantEmolumentSommationDI);
				pf.addParametrePeriodeFiscale(param);
			}
			else {
				emolument.setMontant(this.getMontantEmolumentSommationDI());
			}
		}
		else if (emolument != null) {
			emolument.setMontant(null);
		}

		final ParametrePeriodeFiscalePP[] ppfs = new ParametrePeriodeFiscalePP[] {
				pf.getParametrePeriodeFiscalePPVaudoisOrdinaire(),
				pf.getParametrePeriodeFiscalePPHorsCanton(),
				pf.getParametrePeriodeFiscalePPHorsSuisse(),
				pf.getParametrePeriodeFiscalePPDepense(),
				pf.getParametrePeriodeFiscalePPDiplomateSuisse()
		};

		final RegDate[][] termes = new RegDate [][] {
				{sommationEffectiveVaud, sommationReglementaireVaud, finEnvoiMasseDIVaud},
				{sommationEffectiveHorsCanton, sommationReglementaireHorsCanton, finEnvoiMasseDIHorsCanton},
				{sommationEffectiveHorsSuisse, sommationReglementaireHorsSuisse, finEnvoiMasseDIHorsSuisse},
				{sommationEffectiveDepense, sommationReglementaireDepense, finEnvoiMasseDIDepense},
				{sommationEffectiveDiplomate, sommationReglementaireDiplomate, finEnvoiMasseDIDiplomate}
		};

		assert (ppfs.length == termes.length);

		// On verifie que tous les parametres de periode fiscale ne soient pas null
		for (ParametrePeriodeFiscalePP ppf : ppfs) {
			if (ppf == null) {
				String msgErr = "Impossible de retrouver tous les paramètres PP pour la période fiscale : " + this.getAnneePeriodeFiscale();
				throw new ObjectNotFoundException(msgErr);
			}
		}

		// On met à jour les parametres de periode fiscale
		for (int i = 0; i < ppfs.length; i++) {
			ppfs[i].setTermeGeneralSommationEffectif(termes[i][0]);
			ppfs[i].setTermeGeneralSommationReglementaire(termes[i][1]);
			ppfs[i].setDateFinEnvoiMasseDI(termes[i][2]);
		}
	}
}
