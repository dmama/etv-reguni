package ch.vd.unireg.di;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.validation.ValidationException;
import ch.vd.unireg.declaration.DeclarationImpotOrdinaire;
import ch.vd.unireg.declaration.DeclarationImpotOrdinaireDAO;
import ch.vd.unireg.declaration.DelaiDeclaration;
import ch.vd.unireg.declaration.DelaiDeclarationDAO;
import ch.vd.unireg.declaration.EtatDeclaration;
import ch.vd.unireg.declaration.EtatDeclarationEmise;
import ch.vd.unireg.declaration.EtatDeclarationSommee;
import ch.vd.unireg.di.manager.DeclarationImpotEditManager;
import ch.vd.unireg.di.view.AjouterDelaiDeclarationView;
import ch.vd.unireg.di.view.AjouterEtatDeclarationView;
import ch.vd.unireg.di.view.DeclarationImpotListView;
import ch.vd.unireg.di.view.EditerDeclarationImpotView;
import ch.vd.unireg.di.view.ImprimerDuplicataDeclarationImpotView;
import ch.vd.unireg.di.view.ImprimerNouvelleDeclarationImpotView;
import ch.vd.unireg.di.view.ModifierDemandeDelaiDeclarationView;
import ch.vd.unireg.di.view.NouvelleDemandeDelaiDeclarationView;
import ch.vd.unireg.metier.bouclement.ExerciceCommercial;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersDAO;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.type.EtatDelaiDocumentFiscal;
import ch.vd.unireg.type.TypeDocument;
import ch.vd.unireg.type.TypeEtatDocumentFiscal;
import ch.vd.unireg.utils.ValidatorUtils;

public class DeclarationImpotControllerValidator implements Validator {

	private TiersDAO tiersDAO;
	private TiersService tiersService;
	private DeclarationImpotOrdinaireDAO diDAO;
	private DelaiDeclarationDAO delaiDeclarationDAO;
	private DeclarationImpotEditManager manager;

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
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

		// [SIFISC-18086] blindage en cas de mauvais format de saisie, pour éviter le double message d'erreur
		if (view.getDateDebutPeriodeImposition() == null) {
			if (!errors.hasFieldErrors("dateDebutPeriodeImposition")) {
				errors.rejectValue("dateDebutPeriodeImposition", "error.date.debut.vide");
			}
		}
		if (view.getDateFinPeriodeImposition() == null) {
			if (!errors.hasFieldErrors("dateFinPeriodeImposition")) {
				errors.rejectValue("dateFinPeriodeImposition", "error.date.fin.vide");
			}
		}
		else if (view.getDateFinPeriodeImposition().year() != view.getPeriodeFiscale()) {
			errors.rejectValue("dateFinPeriodeImposition", "error.date.fin.pas.dans.periode.fiscale");
		}
		else if (view.getDateDebutPeriodeImposition() != null) {
			// cas spécial pour les entreprise, la période d'imposition choisie pour la DI
			// ne doit pas être à cheval sur plusieurs exercices commerciaux
			if (ctb instanceof Entreprise) {
				final ExerciceCommercial exerciceDebut = tiersService.getExerciceCommercialAt((Entreprise) ctb, view.getDateDebutPeriodeImposition());
				if (exerciceDebut == null || !exerciceDebut.isValidAt(view.getDateFinPeriodeImposition())) {
					errors.rejectValue("dateFinPeriodeImposition", "error.declaration.cheval.plusieurs.exercices.commerciaux");
				}
			}

			if (view.getDateFinPeriodeImposition().year() == RegDate.get().year()) {
				// si la période est ouverte les dates sont libres... dans la limite des valeurs raisonnables
				if (view.getDateDebutPeriodeImposition().isAfter(view.getDateFinPeriodeImposition())) {
					errors.rejectValue("dateFinPeriodeImposition", "error.date.fin.avant.debut");
				}
				else if (ctb instanceof ContribuableImpositionPersonnesPhysiques && view.getDateDebutPeriodeImposition().year() != view.getDateFinPeriodeImposition().year()) {
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
		}

		final RegDate delaiAccorde = view.getDelaiAccorde();
		if (delaiAccorde == null) {
			// [SIFISC-18086] blindage en cas de mauvais format de saisie, pour éviter le double message d'erreur
			if (!errors.hasFieldErrors("delaiAccorde")) {
				errors.rejectValue("delaiAccorde", "error.delai.accorde.vide");
			}
		}

		// [SIFISC-17773] Le délai maximal de 6 mois n'est valide que pour les DI PP
		// [SIFISC-24587] Limitation du délai maximal à 6 mois supprimée pour les PP aussi
		else if (delaiAccorde.isBefore(RegDate.get())) {
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
			// [SIFISC-18086] blindage en cas de mauvais format de saisie, pour éviter le double message d'erreur
			if (!errors.hasFieldErrors("dateRetour")) {
				errors.rejectValue("dateRetour", "error.date.retour.vide");
			}
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
		EtatDeclaration emis = di.getDernierEtatDeclarationOfType(TypeEtatDocumentFiscal.EMIS);
		EtatDeclaration sommee = di.getDernierEtatDeclarationOfType(TypeEtatDocumentFiscal.SOMME);
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

		// [SIFISC-18086] blindage en cas de mauvais format de saisie, pour éviter le double message d'erreur
		if (!errors.hasFieldErrors("delaiAccordeAu")) {
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

		// [SIFISC-18086] blindage en cas de mauvais format de saisie, pour éviter le double message d'erreur
		if (!errors.hasFieldErrors("dateDemande")) {
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

		if (view.getDecision() == EtatDelaiDocumentFiscal.ACCORDE) {
			// [SIFISC-18086] blindage en cas de mauvais format de saisie, pour éviter le double message d'erreur
			if (!errors.hasFieldErrors("delaiAccordeAu")) {
				if (view.getDelaiAccordeAu() == null) {
					errors.rejectValue("delaiAccordeAu", "error.delai.accorde.vide");
				}
				else {
					final RegDate ancienDelaiAccorde = di.getDelaiAccordeAu();
					if (view.getDelaiAccordeAu().isBefore(RegDate.get()) || (ancienDelaiAccorde != null && view.getDelaiAccordeAu().isBeforeOrEqual(ancienDelaiAccorde))) {
						errors.rejectValue("delaiAccordeAu", "error.delai.accorde.invalide");
					}
				}
			}
		}
		else if (view.getDecision() == null) {
			errors.rejectValue("decision", "error.decision.obligatoire");
		}

		if (view.getDecision() != EtatDelaiDocumentFiscal.DEMANDE) {
			if (view.getTypeImpression() == null) {
				errors.rejectValue("decision", "error.type.impression.obligatoire");
			}
		}

		// [SIFISC-18086] blindage en cas de mauvais format de saisie, pour éviter le double message d'erreur
		if (!errors.hasFieldErrors("dateDemande")) {
			if (view.getDateDemande() == null) {
				errors.rejectValue("dateDemande", "error.date.demande.vide");
			}
			else if (view.getDateDemande().isAfter(RegDate.get())) {
				if (!ValidatorUtils.alreadyHasErrorOnField(errors, "dateDemande")) {
					errors.rejectValue("dateDemande", "error.date.demande.future");
				}
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

		if (view.getDecision() == EtatDelaiDocumentFiscal.ACCORDE) {
			// [SIFISC-18086] blindage en cas de mauvais format de saisie, pour éviter le double message d'erreur
			if (!errors.hasFieldErrors("delaiAccordeAu")) {
				if (view.getDelaiAccordeAu() == null) {
					errors.rejectValue("delaiAccordeAu", "error.delai.accorde.vide");
				}
				else {
					final RegDate ancienDelaiAccorde = delai.getDeclaration().getDelaiAccordeAu();
					if (view.getDelaiAccordeAu().isBefore(RegDate.get()) || (ancienDelaiAccorde != null && view.getDelaiAccordeAu().isBeforeOrEqual(ancienDelaiAccorde))) {
						errors.rejectValue("delaiAccordeAu", "error.delai.accorde.invalide");
					}
				}
			}
		}
		else if (view.getDecision() == null) {
			errors.rejectValue("decision", "error.decision.obligatoire");
		}

		if (view.getDecision() != EtatDelaiDocumentFiscal.DEMANDE) {
			if (view.getTypeImpression() == null) {
				errors.rejectValue("decision", "error.type.impression.obligatoire");
			}
		}

		// [SIFISC-18086] blindage en cas de mauvais format de saisie, pour éviter le double message d'erreur
		if (!errors.hasFieldErrors("dateDemande")) {
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
}
