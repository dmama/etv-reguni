package ch.vd.uniregctb.di;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.validation.ValidationException;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaireDAO;
import ch.vd.uniregctb.declaration.DelaiDeclaration;
import ch.vd.uniregctb.declaration.DelaiDeclarationDAO;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclarationEmise;
import ch.vd.uniregctb.declaration.EtatDeclarationSommee;
import ch.vd.uniregctb.di.manager.DeclarationImpotEditManager;
import ch.vd.uniregctb.di.view.AjouterDelaiDeclarationView;
import ch.vd.uniregctb.di.view.AjouterEtatDeclarationView;
import ch.vd.uniregctb.di.view.DeclarationImpotListView;
import ch.vd.uniregctb.di.view.EditerDeclarationImpotView;
import ch.vd.uniregctb.di.view.ImprimerDuplicataDeclarationImpotView;
import ch.vd.uniregctb.di.view.ImprimerNouvelleDeclarationImpotView;
import ch.vd.uniregctb.di.view.ModifierDemandeDelaiDeclarationView;
import ch.vd.uniregctb.di.view.NouvelleDemandeDelaiDeclarationView;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.type.EtatDelaiDeclaration;
import ch.vd.uniregctb.type.TypeDocument;
import ch.vd.uniregctb.type.TypeEtatDeclaration;
import ch.vd.uniregctb.utils.ValidatorUtils;

public class DeclarationImpotControllerValidator implements Validator {

	private TiersDAO tiersDAO;
	private DeclarationImpotOrdinaireDAO diDAO;
	private DelaiDeclarationDAO delaiDeclarationDAO;
	private DeclarationImpotEditManager manager;

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	public void setDiDAO(DeclarationImpotOrdinaireDAO diDAO) {
		this.diDAO = diDAO;
	}

	public void setDelaiDeclarationDAO(DelaiDeclarationDAO delaiDeclarationDAO) {
		this.delaiDeclarationDAO = delaiDeclarationDAO;
	}

	public void setManager(DeclarationImpotEditManager manager) {
		this.manager = manager;
	}

	@Override
	public boolean supports(Class<?> clazz) {
		return ImprimerNouvelleDeclarationImpotView.class.equals(clazz) || EditerDeclarationImpotView.class.equals(clazz)
				|| DeclarationImpotListView.class.equals(clazz) || ImprimerDuplicataDeclarationImpotView.class.equals(clazz)
				|| AjouterDelaiDeclarationView.class.equals(clazz) || AjouterEtatDeclarationView.class.equals(clazz)
				|| NouvelleDemandeDelaiDeclarationView.class.equals(clazz) || ModifierDemandeDelaiDeclarationView.class.equals(clazz);
	}

	@Override
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public void validate(Object target, Errors errors) {
		if (target instanceof ImprimerNouvelleDeclarationImpotView) {
			validateImprimerNouvelleDI((ImprimerNouvelleDeclarationImpotView) target, errors);
		}
		else if (target instanceof AjouterEtatDeclarationView) {
			validateAjouterEtatDI((AjouterEtatDeclarationView)target, errors);
		}
		else if (target instanceof ImprimerDuplicataDeclarationImpotView) {
			valideImprimerDuplicataDI((ImprimerDuplicataDeclarationImpotView) target, errors);
		}
		else if (target instanceof AjouterDelaiDeclarationView) {
			valideAjoutDelaiDeclaration((AjouterDelaiDeclarationView) target, errors);
		}
		else if (target instanceof NouvelleDemandeDelaiDeclarationView) {
			valideNouvelleDemandeDelaiDeclaration((NouvelleDemandeDelaiDeclarationView) target, errors);
		}
		else if (target instanceof ModifierDemandeDelaiDeclarationView) {
			valideModifierDemandeDelaiDeclaration((ModifierDemandeDelaiDeclarationView) target, errors);
		}
	}

	private void validateImprimerNouvelleDI(ImprimerNouvelleDeclarationImpotView view, Errors errors) {

		if (errors.hasErrors()) {
			// S'il y a déjà des erreurs de saisie dans le formulaire pas la peine d'aller plus loin dans
			// la validation (effets de bord indesirables)
			return;
		}

		// Vérifie que les paramètres reçus sont valides

		final Tiers tiers = tiersDAO.get(view.getTiersId());
		if (tiers == null) {
			errors.reject("error.tiers.inexistant");
			return;
		}

		if (!(tiers instanceof Contribuable)) {
			errors.reject("error.tiers.doit.etre.contribuable");
			return;
		}

		final Contribuable ctb = (Contribuable) tiers;

		if (view.getDateDebutPeriodeImposition() == null) {
			errors.rejectValue("dateDebutPeriodeImposition", "error.date.debut.vide");
		}
		else if (view.getDateFinPeriodeImposition() == null) {
			errors.rejectValue("dateFinPeriodeImposition", "error.date.fin.vide");
		}
		else if (view.getDateDebutPeriodeImposition().year() == RegDate.get().year()) {
			// si la période est ouverte les dates sont libres... dans la limite des valeurs raisonnables
			if (view.getDateDebutPeriodeImposition().isAfter(view.getDateFinPeriodeImposition())) {
				errors.rejectValue("dateFinPeriodeImposition", "error.date.fin.avant.debut");
			}
			else if (view.getDateDebutPeriodeImposition().year() != view.getDateFinPeriodeImposition().year()) {
				errors.rejectValue("dateFinPeriodeImposition", "error.declaration.cheval.plusieurs.annees");
			}
		}
		else {
			try {
				manager.checkRangeDi(ctb, new DateRangeHelper.Range(view.getDateDebutPeriodeImposition(), view.getDateFinPeriodeImposition()));
			}
			catch (ValidationException e) {
				errors.reject(e.getMessage());
			}
		}

		final RegDate delaiAccorde = view.getDelaiAccorde();
		if (delaiAccorde == null) {
			errors.rejectValue("delaiAccorde", "error.delai.accorde.vide");
		}
		else if (delaiAccorde.isBefore(RegDate.get()) || delaiAccorde.isAfter(RegDate.get().addMonths(6))) {
			errors.rejectValue("delaiAccorde", "error.delai.accorde.invalide");
		}
	}

	private void validateAjouterEtatDI(AjouterEtatDeclarationView view, Errors errors) {

		// Vérifie que les paramètres reçus sont valides

		final DeclarationImpotOrdinaire di = diDAO.get(view.getId());
		if (di == null) {
			errors.reject("error.di.inexistante");
			return;
		}

		final RegDate dateRetour = view.getDateRetour();
		if (dateRetour == null) {
			errors.rejectValue("dateRetour", "error.date.retour.vide");
			return;
		}

		if (dateRetour.isAfter(RegDate.get())) {
			errors.rejectValue("dateRetour", "error.date.retour.future");
		}

		final EtatDeclaration dernierEtat = getDernierEtatEmisOuSommee(di);
		if (dateRetour.isBefore(dernierEtat.getDateObtention())) {
			if (dernierEtat instanceof EtatDeclarationSommee) {
				errors.rejectValue("dateRetour", "error.date.retour.anterieure.date.emission.sommation");
			}
			if (dernierEtat instanceof EtatDeclarationEmise) {
				errors.rejectValue("dateRetour", "error.date.retour.anterieure.date.emission");
			}
		}

		if (view.isTypeDocumentEditable()) {
			final TypeDocument typeDocument = view.getTypeDocument();
			if (typeDocument == null) {
				errors.rejectValue("typeDocument", "error.type.document.vide");
			}
			else if (di.getTypeDeclaration() != typeDocument) { // [SIFISC-7486] on ne vérifie le type de document que s'il est différent
				if (typeDocument != TypeDocument.DECLARATION_IMPOT_COMPLETE_LOCAL && typeDocument != TypeDocument.DECLARATION_IMPOT_VAUDTAX) {
					errors.rejectValue("typeDocument", "error.type.document.invalide");
				}
			}
		}
	}

	private static EtatDeclaration getDernierEtatEmisOuSommee(DeclarationImpotOrdinaire di) {
		EtatDeclaration emis = di.getDernierEtatOfType(TypeEtatDeclaration.EMISE);
		EtatDeclaration sommee = di.getDernierEtatOfType(TypeEtatDeclaration.SOMMEE);
		//On aura toujours un état émis sur une déclaration sinon bug
		if (sommee == null) {
			return emis;
		}
		else {
			return sommee;
		}
	}

	private void valideImprimerDuplicataDI(ImprimerDuplicataDeclarationImpotView view, Errors errors) {
		if (view.getIdDI() == null) {
			errors.reject("error.di.inexistante");
		}
	}

	private void valideAjoutDelaiDeclaration(AjouterDelaiDeclarationView view, Errors errors) {

		if (view.getIdDeclaration() == null) {
			errors.reject("error.di.inexistante");
			return;
		}

		final DeclarationImpotOrdinaire di = diDAO.get(view.getIdDeclaration());
		if (di == null) {
			errors.reject("error.di.inexistante");
			return;
		}

		if (view.getDelaiAccordeAu() == null) {
			ValidationUtils.rejectIfEmpty(errors, "delaiAccordeAu", "error.delai.accorde.vide");
		}
		else {
			final RegDate ancienDelaiAccorde = di.getDelaiAccordeAu();
			if (view.getDelaiAccordeAu().isBefore(RegDate.get()) || (ancienDelaiAccorde != null && view.getDelaiAccordeAu().isBeforeOrEqual(ancienDelaiAccorde))) {
				errors.rejectValue("delaiAccordeAu", "error.delai.accorde.invalide");
			}
		}

		if (view.getDateDemande() == null) {
			ValidationUtils.rejectIfEmpty(errors, "dateDemande", "error.date.demande.vide");
		}
		else if (view.getDateDemande().isAfter(RegDate.get())) {
			if (!ValidatorUtils.alreadyHasErrorOnField(errors, "dateDemande")) {
				errors.rejectValue("dateDemande", "error.date.demande.future");
			}
		}
	}

	private void valideNouvelleDemandeDelaiDeclaration(NouvelleDemandeDelaiDeclarationView view, Errors errors) {

		if (view.getIdDeclaration() == null) {
			errors.reject("error.di.inexistante");
			return;
		}

		final DeclarationImpotOrdinaire di = diDAO.get(view.getIdDeclaration());
		if (di == null) {
			errors.reject("error.di.inexistante");
			return;
		}

		if (view.getDecision() == EtatDelaiDeclaration.ACCORDE) {
			if (view.getDelaiAccordeAu() == null) {
				ValidationUtils.rejectIfEmpty(errors, "delaiAccordeAu", "error.delai.accorde.vide");
			}
			else {
				final RegDate ancienDelaiAccorde = di.getDelaiAccordeAu();
				if (view.getDelaiAccordeAu().isBefore(RegDate.get()) || (ancienDelaiAccorde != null && view.getDelaiAccordeAu().isBeforeOrEqual(ancienDelaiAccorde))) {
					errors.rejectValue("delaiAccordeAu", "error.delai.accorde.invalide");
				}
			}
		}
		else if (view.getDecision() == null) {
			errors.rejectValue("decision", "error.decision.obligatoire");
		}

		if (view.getDecision() != EtatDelaiDeclaration.DEMANDE) {
			if (view.getTypeImpression() == null) {
				errors.rejectValue("decision", "error.type.impression.obligatoire");
			}
		}

		if (view.getDateDemande() == null) {
			ValidationUtils.rejectIfEmpty(errors, "dateDemande", "error.date.demande.vide");
		}
		else if (view.getDateDemande().isAfter(RegDate.get())) {
			if (!ValidatorUtils.alreadyHasErrorOnField(errors, "dateDemande")) {
				errors.rejectValue("dateDemande", "error.date.demande.future");
			}
		}
	}

	private void valideModifierDemandeDelaiDeclaration(ModifierDemandeDelaiDeclarationView view, Errors errors) {

		if (view.getIdDelai() == null) {
			errors.reject("error.delai.inexistant");
			return;
		}

		final DelaiDeclaration delai = delaiDeclarationDAO.get(view.getIdDelai());
		if (delai == null) {
			errors.reject("error.delai.inexistant");
			return;
		}

		if (view.getDecision() == EtatDelaiDeclaration.ACCORDE) {
			if (view.getDelaiAccordeAu() == null) {
				ValidationUtils.rejectIfEmpty(errors, "delaiAccordeAu", "error.delai.accorde.vide");
			}
			else {
				final RegDate ancienDelaiAccorde = delai.getDeclaration().getDelaiAccordeAu();
				if (view.getDelaiAccordeAu().isBefore(RegDate.get()) || (ancienDelaiAccorde != null && view.getDelaiAccordeAu().isBeforeOrEqual(ancienDelaiAccorde))) {
					errors.rejectValue("delaiAccordeAu", "error.delai.accorde.invalide");
				}
			}
		}
		else if (view.getDecision() == null) {
			errors.rejectValue("decision", "error.decision.obligatoire");
		}

		if (view.getDecision() != EtatDelaiDeclaration.DEMANDE) {
			if (view.getTypeImpression() == null) {
				errors.rejectValue("decision", "error.type.impression.obligatoire");
			}
		}

		if (view.getDateDemande() == null) {
			ValidationUtils.rejectIfEmpty(errors, "dateDemande", "error.date.demande.vide");
		}
		else if (view.getDateDemande().isAfter(RegDate.get())) {
			if (!ValidatorUtils.alreadyHasErrorOnField(errors, "dateDemande")) {
				errors.rejectValue("dateDemande", "error.date.demande.future");
			}
		}
	}
}
