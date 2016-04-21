package ch.vd.uniregctb.entreprise.complexe;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.util.EnumSet;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import ch.vd.uniregctb.common.Flash;
import ch.vd.uniregctb.common.NumeroIDEHelper;
import ch.vd.uniregctb.metier.MetierServiceException;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.ForFiscalPrincipalPM;
import ch.vd.uniregctb.tiers.TiersCriteria;
import ch.vd.uniregctb.tiers.view.TiersCriteriaView;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeEtatEntreprise;

/**
 * Classe de base des contrôleurs autour des processus d'annulation de fin d'activité et de reprise d'activité
 * partielle
 */
public abstract class AnnulationRepriseFinActiviteController extends AbstractProcessusComplexeController {

	private static final String TYPES_RECHERCHE_NOM_ENUM = "typesRechercheNom";
	private static final String TYPES_RECHERCHE_FJ_ENUM = "formesJuridiquesEnum";
	private static final String TYPES_RECHERCHE_CAT_ENUM = "categoriesEntreprisesEnum";

	private static final String LIST = "list";
	private static final String COMMAND = "command";
	private static final String ERROR_MESSAGE = "errorMessage";

	private final String criteriaSessionName;
	private final String processDescription;
	private final String viewPath;

	protected AnnulationRepriseFinActiviteController(String criteriaSessionName,
	                                                 String processDescription,
	                                                 String viewPath) {
		this.criteriaSessionName = criteriaSessionName;
		this.processDescription = processDescription;
		this.viewPath = viewPath;
	}

	private void checkDroitAcces() throws AccessDeniedException {
		checkAnyGranted(String.format("Vous ne possédez aucun droit IfoSec pour l'accès au processus complexe %s d'entreprise.", processDescription),
		                Role.FIN_ACTIVITE_ENTREPRISE);
	}

	@RequestMapping(value = "/list.do", method = RequestMethod.GET)
	public String showFormulaireRecherche(Model model, HttpSession session) {
		checkDroitAcces();
		final TiersCriteriaView criteria = (TiersCriteriaView) session.getAttribute(criteriaSessionName);
		return showRecherche(model, criteria, false);
	}

	@RequestMapping(value = "/reset-search.do", method = RequestMethod.GET)
	public String resetCriteresRecherche(HttpSession session) {
		checkDroitAcces();
		session.removeAttribute(criteriaSessionName);
		return "redirect:list.do";
	}

	@RequestMapping(value = "/list.do", method = RequestMethod.POST)
	public String doRecherche(@Valid @ModelAttribute(value = COMMAND) TiersCriteriaView view, BindingResult bindingResult, HttpSession session, Model model) {
		checkDroitAcces();
		if (bindingResult.hasErrors()) {
			return showRecherche(model, view, true);
		}
		else {
			session.setAttribute(criteriaSessionName, view);
		}
		return "redirect:list.do";
	}

	private String showRecherche(Model model, @Nullable TiersCriteriaView criteria, boolean error) {
		if (criteria == null) {
			criteria = new TiersCriteriaView();
			criteria.setTypeRechercheDuNom(TiersCriteria.TypeRecherche.EST_EXACTEMENT);
		}
		else if (!error) {
			// lancement de la recherche selon les critères donnés

			// reformattage du numéro IDE
			if (StringUtils.isNotBlank(criteria.getNumeroIDE())) {
				criteria.setNumeroIDE(NumeroIDEHelper.normalize(criteria.getNumeroIDE()));
			}

			criteria.setTiersActif(Boolean.FALSE);
			criteria.setTypeTiersImperatif(TiersCriteria.TypeTiers.ENTREPRISE);
			criteria.setEtatsEntrepriseInterdits(EnumSet.of(TypeEtatEntreprise.ABSORBEE));
			criteria.setMotifFermetureDernierForPrincipal(MotifFor.CESSATION_ACTIVITE);
			model.addAttribute(LIST, searchTiers(criteria, model, ERROR_MESSAGE));
		}

		model.addAttribute(COMMAND, criteria);
		model.addAttribute(TYPES_RECHERCHE_NOM_ENUM, tiersMapHelper.getMapTypeRechercheNom());
		model.addAttribute(TYPES_RECHERCHE_FJ_ENUM, tiersMapHelper.getMapFormeJuridiqueEntreprise());
		model.addAttribute(TYPES_RECHERCHE_CAT_ENUM, tiersMapHelper.getMapCategoriesEntreprise());
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
				if (dernierFor == null || dernierFor.getMotifFermeture() != MotifFor.CESSATION_ACTIVITE) {
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
		model.addAttribute(COMMAND, view);
		return String.format("entreprise/%s-finactivite/start", viewPath);
	}

	@RequestMapping(value = "/start.do", method = RequestMethod.POST)
	public String doFinActivite(Model model, @Valid @ModelAttribute(value = COMMAND) final FinActiviteView view, BindingResult bindingResult) throws Exception {
		checkDroitAcces();
		controllerUtils.checkAccesDossierEnEcriture(view.getIdEntreprise());
		if (bindingResult.hasErrors()) {
			return showStart(model, view);
		}
		controllerUtils.checkTraitementContribuableAvecDecisionAci(view.getIdEntreprise());

		doInTransaction(new MetierServiceExceptionAwareWithoutResultCallback() {
			@Override
			protected void doExecute(TransactionStatus status) throws MetierServiceException {
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
