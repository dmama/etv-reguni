package ch.vd.unireg.entreprise.complexe;

import javax.validation.Valid;
import java.util.EnumSet;

import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

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

/**
 * Classe de base des controlleurs de révocation et d'annulation de faillite, deux processus
 * extrêmement proches
 */
public abstract class AnnulationRevocationFailliteController extends AbstractProcessusComplexeRechercheController {

	private final String processDescription;
	private final String viewPath;

	protected AnnulationRevocationFailliteController(String processDescription,
	                                                 String viewPath) {
		this.processDescription = processDescription;
		this.viewPath = viewPath;
	}

	@Override
	protected void checkDroitAcces() throws AccessDeniedException {
		checkAnyGranted(String.format("Vous ne possédez pas les droits d'accès au processus complexe %s de faillite d'entreprise.", processDescription),
		                Role.FAILLITE_ENTREPRISE);
	}

	@Override
	protected void fillCriteriaWithImplicitValues(TiersCriteriaView criteria) {
		criteria.setTiersActif(Boolean.FALSE);
		criteria.setTypeTiersImperatif(TiersCriteria.TypeTiers.ENTREPRISE);
		criteria.setEtatsEntrepriseInterdits(EnumSet.of(TypeEtatEntreprise.ABSORBEE, TypeEtatEntreprise.DISSOUTE));
		criteria.setEtatEntrepriseCourant(TypeEtatEntreprise.EN_FAILLITE);
	}

	@Override
	protected String getSearchResultViewPath() {
		return String.format("entreprise/%s-faillite/list", viewPath);
	}

	@RequestMapping(value = "/start.do", method = RequestMethod.GET)
	public String showStart(final Model model, @RequestParam("id") final long idEntreprise) {
		checkDroitAcces();
		controllerUtils.checkAccesDossierEnEcriture(idEntreprise);

		return doInReadOnlyTransaction(new TransactionCallback<String>() {
			@Override
			public String doInTransaction(TransactionStatus status) {
				final Entreprise entreprise = getTiers(Entreprise.class, idEntreprise);
				final EtatEntreprise etat = entreprise.getEtatActuel();
				if (etat == null || etat.getType() != TypeEtatEntreprise.EN_FAILLITE) {
					Flash.error("L'entreprise sélectionnée n'est pas/plus dans l'état 'En faillite'.", 4000);
					return "redirect:list.do";
				}

				final FailliteView view = new FailliteView(idEntreprise);
				view.setDatePrononceFaillite(etat.getDateObtention());
				return showStart(model, view);
			}
		});
	}

	private String showStart(Model model, FailliteView view) {
		model.addAttribute(ACTION_COMMAND, view);
		return String.format("entreprise/%s-faillite/start", viewPath);
	}

	@RequestMapping(value = "/start.do", method = RequestMethod.POST)
	public String doFaillite(Model model, @Valid @ModelAttribute(value = ACTION_COMMAND) final FailliteView view, BindingResult bindingResult) throws Exception {
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
				doJob(entreprise, view);
			}
		});

		return "redirect:/tiers/visu.do?id=" + view.getIdEntreprise();
	}

	/**
	 * Lance la véritable opération sur les données
	 * @param entreprise l'entreprise ciblée
	 * @param view données du formulaire
	 * @throws MetierServiceException en cas de souci
	 */
	protected abstract void doJob(Entreprise entreprise, FailliteView view) throws MetierServiceException;
}
