package ch.vd.unireg.entreprise.complexe;

import javax.validation.Valid;
import java.io.IOException;
import java.util.EnumSet;

import org.springframework.stereotype.Controller;
import org.springframework.transaction.TransactionStatus;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import ch.vd.registre.base.date.RegDate;
import ch.vd.shared.validation.ValidationService;
import ch.vd.unireg.common.EditiqueErrorHelper;
import ch.vd.unireg.common.Flash;
import ch.vd.unireg.common.RetourEditiqueControllerHelper;
import ch.vd.unireg.documentfiscal.AutreDocumentFiscalException;
import ch.vd.unireg.documentfiscal.AutreDocumentFiscalService;
import ch.vd.unireg.editique.EditiqueResultat;
import ch.vd.unireg.editique.EditiqueResultatErreur;
import ch.vd.unireg.editique.EditiqueResultatReroutageInbox;
import ch.vd.unireg.metier.MetierServiceException;
import ch.vd.unireg.security.AccessDeniedException;
import ch.vd.unireg.security.Role;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.TiersCriteria;
import ch.vd.unireg.tiers.view.TiersCriteriaView;
import ch.vd.unireg.transaction.TransactionHelper;
import ch.vd.unireg.type.TypeEtatEntreprise;

@Controller
@RequestMapping("/processuscomplexe/requisitionradiationrc")
public class RequisitionRadiationRCController extends AbstractProcessusComplexeRechercheController {

	private AutreDocumentFiscalService autreDocumentFiscalService;
	private ValidationService validationService;
	private RetourEditiqueControllerHelper retourEditiqueHelper;

	public static final String CRITERIA_NAME = "RequisitionRadiationRCCriteria";

	public void setAutreDocumentFiscalService(AutreDocumentFiscalService autreDocumentFiscalService) {
		this.autreDocumentFiscalService = autreDocumentFiscalService;
	}

	public void setValidationService(ValidationService validationService) {
		this.validationService = validationService;
	}

	public void setRetourEditiqueHelper(RetourEditiqueControllerHelper retourEditiqueHelper) {
		this.retourEditiqueHelper = retourEditiqueHelper;
	}

	@Override
	protected void checkDroitAcces() throws AccessDeniedException {
		checkAnyGranted("Vous ne possédez aucun droit IfoSec pour l'accès au processus complexe de réquisition de radiation du RC d'une entreprise.",
		                Role.REQUISITION_RADIATION_RC);
	}

	@Override
	protected String getSearchCriteriaSessionName() {
		return CRITERIA_NAME;
	}

	@Override
	protected void fillCriteriaWithImplicitValues(TiersCriteriaView criteria) {
		criteria.setTiersActif(Boolean.TRUE);
		criteria.setTypeTiersImperatif(TiersCriteria.TypeTiers.ENTREPRISE);
		criteria.setEtatsEntrepriseInterdits(EnumSet.of(TypeEtatEntreprise.RADIEE_RC, TypeEtatEntreprise.DISSOUTE));
	}

	@Override
	protected String getSearchResultViewPath() {
		return "entreprise/requisitionradiationrc/list";
	}

	@RequestMapping(value = "/start.do", method = RequestMethod.GET)
	public String showStart(Model model, @RequestParam("id") long idEntreprise) {
		checkDroitAcces();
		controllerUtils.checkAccesDossierEnEcriture(idEntreprise);
		return showStart(model, new RequisitionRadiationRCView(idEntreprise));
	}

	private String showStart(Model model, FinActiviteView view) {
		model.addAttribute(ACTION_COMMAND, view);
		return "entreprise/requisitionradiationrc/start";
	}

	@RequestMapping(value = "/start.do", method = RequestMethod.POST)
	public String doFinActivite(Model model, @Valid @ModelAttribute(value = ACTION_COMMAND) final RequisitionRadiationRCView view, BindingResult bindingResult) throws Exception {
		checkDroitAcces();
		controllerUtils.checkAccesDossierEnEcriture(view.getIdEntreprise());
		if (bindingResult.hasErrors()) {
			return showStart(model, view);
		}
		controllerUtils.checkTraitementContribuableAvecDecisionAci(view.getIdEntreprise());

		return doInTransaction(new TransactionHelper.ExceptionThrowingCallback<String, MetierServiceException>() {
			@Override
			public String execute(TransactionStatus status) throws MetierServiceException {
				final Entreprise entreprise = getTiers(Entreprise.class, view.getIdEntreprise());
				metierService.finActivite(entreprise, view.getDateFinActivite(), view.getRemarque());

				final String redirect = String.format("redirect:/tiers/visu.do?id=%d", view.getIdEntreprise());
				if (!validationService.validate(entreprise).hasErrors() && view.isImprimerDemandeBilanFinal()) {
					try {
						final EditiqueResultat editique = autreDocumentFiscalService.envoyerDemandeBilanFinalOnline(entreprise, RegDate.get(), view.getPeriodeFiscale(), view.getDateFinActivite());

						final RetourEditiqueControllerHelper.TraitementRetourEditique<EditiqueResultatReroutageInbox> inbox =
								new RetourEditiqueControllerHelper.TraitementRetourEditique<EditiqueResultatReroutageInbox>() {
									@Override
									public String doJob(EditiqueResultatReroutageInbox resultat) {
										return redirect;
									}
								};

						final RetourEditiqueControllerHelper.TraitementRetourEditique<EditiqueResultatErreur> erreur = new RetourEditiqueControllerHelper.TraitementRetourEditique<EditiqueResultatErreur>() {
							@Override
							public String doJob(EditiqueResultatErreur resultat) {
								Flash.error(EditiqueErrorHelper.getMessageErreurEditique(resultat));
								return redirect;
							}
						};

						return retourEditiqueHelper.traiteRetourEditiqueAfterRedirect(editique,
						                                                              "demandeBilanFinal",
						                                                              redirect,
						                                                              true,
						                                                              inbox,
						                                                              null,
						                                                              erreur);
					}
					catch (AutreDocumentFiscalException | IOException e) {
						throw new MetierServiceException(e);
					}
				}
				else {
					return redirect;
				}
			}
		});
	}
}
