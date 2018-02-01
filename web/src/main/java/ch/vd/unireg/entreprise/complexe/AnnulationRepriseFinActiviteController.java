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
import ch.vd.unireg.tiers.ForFiscalPrincipalPM;
import ch.vd.unireg.tiers.TiersCriteria;
import ch.vd.unireg.tiers.view.TiersCriteriaView;
import ch.vd.unireg.transaction.TransactionHelper;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.TypeEtatEntreprise;

/**
 * Classe de base des contrôleurs autour des processus d'annulation de fin d'activité et de reprise d'activité
 * partielle
 */
public abstract class AnnulationRepriseFinActiviteController extends AbstractProcessusComplexeRechercheController {

	private final String processDescription;
	private final String viewPath;

	protected AnnulationRepriseFinActiviteController(String processDescription,
	                                                 String viewPath) {
		this.processDescription = processDescription;
		this.viewPath = viewPath;
	}

	@Override
	protected void checkDroitAcces() throws AccessDeniedException {
		checkAnyGranted(String.format("Vous ne possédez aucun droit IfoSec pour l'accès au processus complexe %s d'entreprise.", processDescription),
		                Role.FIN_ACTIVITE_ENTREPRISE);
	}

	@Override
	protected void fillCriteriaWithImplicitValues(TiersCriteriaView criteria) {
		criteria.setTiersActif(Boolean.FALSE);
		criteria.setTypeTiersImperatif(TiersCriteria.TypeTiers.ENTREPRISE);
		criteria.setEtatsEntrepriseInterdits(EnumSet.of(TypeEtatEntreprise.ABSORBEE));
		criteria.setMotifFermetureDernierForPrincipal(MotifFor.FIN_EXPLOITATION);
	}

	@Override
	protected String getSearchResultViewPath() {
		return String.format("entreprise/%s-finactivite/list", viewPath);
	}

	@RequestMapping(value = "/start.do", method = RequestMethod.GET)
	public String showStart(final Model model, @RequestParam("id") final long idEntreprise) {
		checkDroitAcces();
		controllerUtils.checkAccesDossierEnEcriture(idEntreprise);

		return doInReadOnlyTransaction(new TransactionCallback<String>() {
			@Override
			public String doInTransaction(TransactionStatus status) {
				final Entreprise entreprise = getTiers(Entreprise.class, idEntreprise);
				final ForFiscalPrincipalPM dernierFor = entreprise.getDernierForFiscalPrincipal();
				if (dernierFor == null || dernierFor.getMotifFermeture() != MotifFor.FIN_EXPLOITATION) {
					Flash.error("Le dernier for principal de l'entreprise sélectionnée n'est plus fermé pour motif 'Cessation d'activité'.");
					return "redirect:list.do";
				}

				final FinActiviteView view = new FinActiviteView(idEntreprise);
				view.setDateFinActivite(dernierFor.getDateFin());
				return showStart(model, view);
			}
		});
	}

	private String showStart(Model model, FinActiviteView view) {
		model.addAttribute(ACTION_COMMAND, view);
		return String.format("entreprise/%s-finactivite/start", viewPath);
	}

	@RequestMapping(value = "/start.do", method = RequestMethod.POST)
	public String doFinActivite(Model model, @Valid @ModelAttribute(value = ACTION_COMMAND) final FinActiviteView view, BindingResult bindingResult) throws Exception {
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
	protected abstract void doJob(Entreprise entreprise, FinActiviteView view) throws MetierServiceException;

}
