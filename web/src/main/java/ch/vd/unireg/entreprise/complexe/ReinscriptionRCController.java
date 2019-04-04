package ch.vd.unireg.entreprise.complexe;

import javax.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.Flash;
import ch.vd.unireg.metier.MetierServiceException;
import ch.vd.unireg.security.AccessDeniedException;
import ch.vd.unireg.security.Role;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.EtatEntreprise;
import ch.vd.unireg.tiers.TiersCriteria;
import ch.vd.unireg.tiers.view.TiersCriteriaView;
import ch.vd.unireg.transaction.TransactionHelper;
import ch.vd.unireg.type.TypeEtatEntreprise;

@Controller
@RequestMapping(value = "/processuscomplexe/reinscriptionrc")
public class ReinscriptionRCController extends AbstractProcessusComplexeRechercheController {

	public static final String CRITERIA_NAME = "ReinscriptionRCCriteria";

	@Override
	protected void checkDroitAcces() throws AccessDeniedException {
		checkAnyGranted("Vous ne possédez pas les droits d'accès au processus complexe de ré-inscription au RC d'une entreprise radiée.",
		                Role.REINSCRIPTION_RC_ENTREPRISE);
	}

	@Override
	protected String getSearchCriteriaSessionName() {
		return CRITERIA_NAME;
	}

	@Override
	protected void fillCriteriaWithImplicitValues(TiersCriteriaView criteria) {
		criteria.setTypeTiersImperatif(TiersCriteria.TypeTiers.ENTREPRISE);
		criteria.setEtatEntrepriseCourant(TypeEtatEntreprise.RADIEE_RC);
	}

	@Override
	protected String getSearchResultViewPath() {
		return "entreprise/reinscriptionrc/list";
	}

	@RequestMapping(value = "/start.do", method = RequestMethod.GET)
	public String showStart(final Model model, @RequestParam("id") final long idEntreprise) {
		checkDroitAcces();
		controllerUtils.checkAccesDossierEnEcriture(idEntreprise);

		return doInReadOnlyTransaction(new TransactionCallback<String>() {
			@Override
			public String doInTransaction(TransactionStatus status) {
				final Entreprise entreprise = getTiers(Entreprise.class, idEntreprise);
				final EtatEntreprise etatRadie = entreprise.getEtatActuel();
				if (etatRadie == null || etatRadie.getType() != TypeEtatEntreprise.RADIEE_RC) {
					Flash.error("L'entreprise choisie n'est pas actuellement dans l'état 'Radiée du RC'");
					return "redirect:list.do";
				}
				final RegDate dateRadiation = etatRadie.getDateObtention();
				return showStart(model, new ReinscriptionRCView(idEntreprise, dateRadiation));
			}
		});
	}

	private String showStart(Model model, ReinscriptionRCView view) {
		model.addAttribute(ACTION_COMMAND, view);
		return "entreprise/reinscriptionrc/start";
	}

	@RequestMapping(value = "/start.do", method = RequestMethod.POST)
	public String doFaillite(Model model, @Valid @ModelAttribute(value = ACTION_COMMAND) final ReinscriptionRCView view, BindingResult bindingResult) throws Exception {
		checkDroitAcces();
		controllerUtils.checkAccesDossierEnEcriture(view.getIdEntreprise());
		if (bindingResult.hasErrors()) {
			return showStart(model, view);
		}
		controllerUtils.checkTraitementContribuableAvecDecisionAci(view.getIdEntreprise());

		doInTransaction(new TransactionHelper.ExceptionThrowingCallbackWithoutResult<MetierServiceException>() {
			@Override
			public void execute(TransactionStatus status) throws MetierServiceException {
				final Entreprise entreprise = getTiers(Entreprise.class, view.getIdEntreprise());
				metierService.reinscritRC(entreprise, view.getDateRadiationRC(), view.getRemarque());
			}
		});

		return "redirect:/tiers/visu.do?id=" + view.getIdEntreprise();
	}
}
