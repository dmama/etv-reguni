package ch.vd.uniregctb.entreprise.complexe;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.ActionException;
import ch.vd.uniregctb.common.Flash;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.metier.MetierServiceException;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.EtatEntreprise;
import ch.vd.uniregctb.tiers.TiersCriteria;
import ch.vd.uniregctb.tiers.TiersIndexedDataView;
import ch.vd.uniregctb.tiers.view.TiersCriteriaView;
import ch.vd.uniregctb.transaction.TransactionHelper;
import ch.vd.uniregctb.type.TypeEtatEntreprise;
import ch.vd.uniregctb.utils.WebContextUtils;

@Controller
@RequestMapping("/processuscomplexe/scission")
public class ScissionEntrepriseController extends AbstractProcessusComplexeController implements InitializingBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(ScissionEntrepriseController.class);

	public static final String CRITERIA_NAME_SCINDEE = "ScissionEntrepriseCriteriaScindee";
	public static final String CRITERIA_NAME_RESULTANTE = "ScissionEntrepriseCriteriaResultante";
	public static final String SCISSION_NAME = "ScissionEntreprise";

	private static final String SCISSION = "scission";

	private SearchTiersComponent searchScindeeComponent;
	private SearchTiersComponent searchResultanteComponent;

	private void checkDroitAcces() throws AccessDeniedException {
		checkAnyGranted("Vous ne possédez aucun droit IfoSec pour l'accès au processus complexe de scission d'entreprise.",
		                Role.SCISSION_ENTREPRISE);
	}

	@Nullable
	private static ScissionEntrepriseSessionData getSessionData(HttpSession session) {
		return (ScissionEntrepriseSessionData) session.getAttribute(SCISSION_NAME);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		this.searchScindeeComponent = buildSearchComponent(CRITERIA_NAME_SCINDEE, "entreprise/scission/list-scindee",
		                                                   new SearchTiersComponent.TiersCriteriaFiller() {
			                                                      @Override
			                                                      public void fill(TiersCriteriaView data) {
				                                                      data.setTypeTiersImperatif(TiersCriteria.TypeTiers.ENTREPRISE);
				                                                      data.setEtatsEntrepriseInterdits(EnumSet.of(TypeEtatEntreprise.ABSORBEE, TypeEtatEntreprise.DISSOUTE, TypeEtatEntreprise.RADIEE_RC));
			                                                      }
		                                                      });

		this.searchResultanteComponent = buildSearchComponent(CRITERIA_NAME_RESULTANTE, "entreprise/scission/list-resultantes",
		                                                      new SearchTiersComponent.TiersCriteriaFiller() {
			                                                      @Override
			                                                      public void fill(TiersCriteriaView data) {
				                                                      fillCriteresImperatifsPourEntrepriseResultante(data);
			                                                      }
		                                                      },
		                                                      new SearchTiersComponent.ModelFiller() {
			                                                      @Override
			                                                      public void fill(Model model, HttpSession session) throws SearchTiersComponent.RedirectException {
				                                                      final ScissionEntrepriseSessionData sessionData = getSessionData(session);
				                                                      if (sessionData == null) {
					                                                      Flash.warning("La session a été invalidée. Veuillez recommencer votre saisie.");
					                                                      throw new SearchTiersComponent.RedirectException("../scindee/list.do");
				                                                      }
				                                                      model.addAttribute(SCISSION, sessionData);
			                                                      }
		                                                      },
		                                                      new SearchTiersComponent.TiersSearchAdapter<SelectionEntrepriseView>() {
			                                                      @Override
			                                                      public List<SelectionEntrepriseView> adaptSearchResult(List<TiersIndexedDataView> result, HttpSession session) {
				                                                      final ScissionEntrepriseSessionData sessionData = getSessionData(session);
				                                                      return ScissionEntrepriseController.this.adapteSearchResults(result, sessionData);
			                                                      }
		                                                      });
	}

	@RequestMapping(value = "/scindee/list.do", method = RequestMethod.GET)
	public String showFormulaireRechercheScindee(Model model, HttpSession session) {
		checkDroitAcces();

		// quand on arrive sur l'écran, on efface les données de la scission précédente, au cas où il y en aurait encore...
		session.removeAttribute(SCISSION_NAME);
		return searchScindeeComponent.showFormulaireRecherche(model, session);
	}

	@RequestMapping(value = "/scindee/reset-search.do", method = RequestMethod.GET)
	public String resetCriteresRechercheScindee(HttpSession session) {
		checkDroitAcces();
		return searchScindeeComponent.resetCriteresRecherche(session, "list.do");
	}

	@RequestMapping(value = "/scindee/list.do", method = RequestMethod.POST)
	public String doRechercheScindee(@Valid @ModelAttribute(value = SearchTiersComponent.COMMAND) TiersCriteriaView view, BindingResult bindingResult, HttpSession session, Model model) {
		checkDroitAcces();
		return searchScindeeComponent.doRecherche(view, bindingResult, session, model, "list.do");
	}

	@RequestMapping(value = "/choix-date.do", method = RequestMethod.GET)
	public String showStart(Model model, @RequestParam("scindee") long idEntreprise) {
		checkDroitAcces();
		controllerUtils.checkAccesDossierEnEcriture(idEntreprise);
		return showStart(model, new ScissionEntrepriseView(idEntreprise));
	}

	@RequestMapping(value = "/retour-choix-date.do", method = RequestMethod.GET)
	public String showRetourStart(Model model, HttpSession session) {
		checkDroitAcces();

		final ScissionEntrepriseSessionData sessionData = getSessionData(session);
		if (sessionData == null) {
			Flash.warning("La session a été invalidée. Veuillez recommencer votre saisie.");
			return "redirect:scindee/list.do";
		}

		controllerUtils.checkAccesDossierEnEcriture(sessionData.getIdEntrepriseScindee());
		return showStart(model, new ScissionEntrepriseView(sessionData.getIdEntrepriseScindee(),
		                                                   sessionData.getDateContratScission()));
	}

	private String showStart(Model model, ScissionEntrepriseView view) {
		model.addAttribute(ACTION_COMMAND, view);
		return "entreprise/scission/choix-date";
	}

	@RequestMapping(value = "/choix-date.do", method = RequestMethod.POST)
	public String doSetDates(Model model, @Valid @ModelAttribute(value = ACTION_COMMAND) final ScissionEntrepriseView view, BindingResult bindingResult, HttpSession session) throws Exception {
		checkDroitAcces();
		controllerUtils.checkAccesDossierEnEcriture(view.getIdEntrepriseScindee());
		if (bindingResult.hasErrors()) {
			return showStart(model, view);
		}

		// on conserve les données de la scission en session
		final ScissionEntrepriseSessionData newSessionData = new ScissionEntrepriseSessionData(view.getIdEntrepriseScindee(), view.getDateContratScission());;
		final ScissionEntrepriseSessionData oldSessionData = getSessionData(session);
		if (oldSessionData != null && newSessionData.getIdEntrepriseScindee() == oldSessionData.getIdEntrepriseScindee()) {
			for (ScissionEntrepriseSessionData.EntrepriseResultante oldResultante : oldSessionData.getEntreprisesResultantes()) {
				newSessionData.addEntrepriseResultante(oldResultante);
			}
		}
		session.setAttribute(SCISSION_NAME, newSessionData);

		return "redirect:/processuscomplexe/scission/resultantes/list.do";
	}

	@RequestMapping(value = "/resultantes/list.do", method = RequestMethod.GET)
	public String showFormulaireRechercheResultante(Model model, HttpSession session) {
		checkDroitAcces();
		return searchResultanteComponent.showFormulaireRecherche(model, session);
	}

	@RequestMapping(value = "/resultantes/reset-search.do", method = RequestMethod.GET)
	public String resetCriteresRechercheResultante(HttpSession session) {
		checkDroitAcces();
		return searchResultanteComponent.resetCriteresRecherche(session, "list.do?searched=true");
	}

	private void fillCriteresImperatifsPourEntrepriseResultante(TiersCriteriaView criteria) {
		criteria.setTypeTiersImperatif(TiersCriteria.TypeTiers.ENTREPRISE);
		criteria.setEtatsEntrepriseInterdits(EnumSet.of(TypeEtatEntreprise.ABSORBEE, TypeEtatEntreprise.DISSOUTE, TypeEtatEntreprise.RADIEE_RC));
	}

	private List<SelectionEntrepriseView> adapteSearchResults(final List<TiersIndexedDataView> searchResults, @NotNull final ScissionEntrepriseSessionData sessionData) {
		final List<SelectionEntrepriseView> results = new ArrayList<>(searchResults.size());
		final Set<Long> idsEntreprisesDejaSelectionnees = sessionData.getIdsEntreprisesResultantes();
		if (!searchResults.isEmpty()) {
			doInReadOnlyTransaction(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) {
					for (TiersIndexedDataView searchResult : searchResults) {

						// si l'entreprise est déjà sélectionnée comme entreprise d'origine, on ne peut pas la reprendre dans les résultantes
						final String explicationNonSelectionnable;
						if (sessionData.getIdEntrepriseScindee() == searchResult.getNumero()) {
							explicationNonSelectionnable = messageSource.getMessage("label.scission.entreprise.deja.utilisee.comme.origine", null, WebContextUtils.getDefaultLocale());
						}
						// si l'entreprise est déjà sélectionnée dans les entreprises résultantes, on ne peut pas la choisir à nouveau
						else if (idsEntreprisesDejaSelectionnees.contains(searchResult.getNumero())) {
							explicationNonSelectionnable = messageSource.getMessage("label.scission.entreprise.deja.utilisee.comme.resultante", null, WebContextUtils.getDefaultLocale());
						}
						else {
							explicationNonSelectionnable = null;
						}

						final SelectionEntrepriseView view = new SelectionEntrepriseView(searchResult, explicationNonSelectionnable);
						results.add(view);
					}
				}
			});
		}
		return results;
	}

	/**
	 * @param entreprise une entreprise
	 * @return la date d'obtention du premier état non-annulé, ou <code>null</code> s'il n'y a pas de tel état
	 */
	@Nullable
	private static RegDate getDateCreation(Entreprise entreprise) {
		final List<EtatEntreprise> etats = entreprise.getEtatsNonAnnulesTries();
		return etats == null || etats.isEmpty() ? null : etats.get(0).getDateObtention();
	}

	@RequestMapping(value = "/resultantes/add.do", method = RequestMethod.POST)
	public String addResultante(@RequestParam("id") long idEntreprise, HttpSession session) {
		checkDroitAcces();

		// on vérifie qu'on a bien les droits sur l'entreprise rajoutée
		try {
			controllerUtils.checkAccesDossierEnEcriture(idEntreprise);
			controllerUtils.checkTraitementContribuableAvecDecisionAci(idEntreprise);
		}
		catch (AccessDeniedException | ObjectNotFoundException e) {
			Flash.error(e.getMessage());
			return "redirect:list.do";
		}

		// on va chercher à nouveau dans l'indexeur les données de cette entreprise
		final TiersCriteriaView criteria = new TiersCriteriaView();
		fillCriteresImperatifsPourEntrepriseResultante(criteria);
		criteria.setNumero(idEntreprise);

		try {
			final List<TiersIndexedDataView> resultats = searchResultanteComponent._searchTiers(criteria);
			if (resultats.isEmpty()) {
				Flash.error("Impossible de récupérer les données du tiers " + FormatNumeroHelper.numeroCTBToDisplay(idEntreprise));
				return "redirect:list.do";
			}

			// ajout de la donnée
			final ScissionEntrepriseSessionData sessionData = getSessionData(session);
			if (sessionData == null) {
				Flash.warning("La session a été invalidée. Veuillez recommencer votre saisie.");
				return "redirect:../scindee/list.do";
			}

			sessionData.addEntrepriseResultante(new ScissionEntrepriseSessionData.EntrepriseResultante(resultats.get(0)));
			return "redirect:list.do";
		}
		catch (IndexerException e) {
			LOGGER.error(e.getMessage(), e);
			Flash.error("Impossible de récupérer les données du tiers " + FormatNumeroHelper.numeroCTBToDisplay(idEntreprise));
			return "redirect:list.do";
		}
	}

	@RequestMapping(value = "/resultantes/remove.do", method = RequestMethod.POST)
	public String removeResultante(@RequestParam("id") long idEntreprise, HttpSession session) {
		checkDroitAcces();

		// retrait de la donnée
		final ScissionEntrepriseSessionData sessionData = getSessionData(session);
		if (sessionData == null) {
			Flash.warning("La session a été invalidée. Veuillez recommencer votre saisie.");
			return "redirect:../scindee/list.do";
		}

		sessionData.removeEntrepriseResultante(idEntreprise);
		return "redirect:list.do";
	}

	@RequestMapping(value = "/resultantes/list.do", method = RequestMethod.POST)
	public String doRechercheResultante(@Valid @ModelAttribute(value = SearchTiersComponent.COMMAND) TiersCriteriaView view, BindingResult bindingResult, HttpSession session, Model model) {
		checkDroitAcces();
		return searchResultanteComponent.doRecherche(view, bindingResult, session, model, "list.do?searched=true");
	}

	@RequestMapping(value = "/scinder.do", method = RequestMethod.POST)
	public String scinder(HttpSession session) {
		checkDroitAcces();

		// récupération des données de scission
		final ScissionEntrepriseSessionData sessionData = getSessionData(session);
		if (sessionData == null) {
			Flash.warning("La session a été invalidée. Veuillez recommencer votre saisie.");
			return "redirect:scindee/list.do";
		}
		else if (sessionData.getIdsEntreprisesResultantes().isEmpty()) {
			throw new ActionException("Veuillez sélectionner au moins une entreprise résultante.");
		}

		// on récupère les données des entreprises et on envoie tout ça dans le moteur
		doInTransaction(new TransactionHelper.ExceptionThrowingCallbackWithoutResult<MetierServiceException>() {
			@Override
			public void execute(TransactionStatus status) throws MetierServiceException {
				final Entreprise scindee = getTiers(Entreprise.class, sessionData.getIdEntrepriseScindee());
				final Set<Long> idsEntreprisesResultantes = sessionData.getIdsEntreprisesResultantes();
				final List<Entreprise> resultantes = new ArrayList<>(idsEntreprisesResultantes.size());
				for (Long id : idsEntreprisesResultantes) {
					resultantes.add(getTiers(Entreprise.class, id));
				}
				metierService.scinde(scindee, resultantes, sessionData.getDateContratScission());
			}
		});

		// quand le boulot est terminé (correctement), on efface les données de la session
		session.removeAttribute(SCISSION_NAME);

		// ... et on termine dans le dossier de l'entreprise scindée
		return "redirect:/tiers/visu.do?id=" + sessionData.getIdEntrepriseScindee();
	}
}
