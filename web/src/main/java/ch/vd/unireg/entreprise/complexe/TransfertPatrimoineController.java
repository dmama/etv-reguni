package ch.vd.unireg.entreprise.complexe;

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
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import ch.vd.unireg.common.ActionException;
import ch.vd.unireg.common.Flash;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.common.ObjectNotFoundException;
import ch.vd.unireg.indexer.IndexerException;
import ch.vd.unireg.metier.MetierServiceException;
import ch.vd.unireg.security.AccessDeniedException;
import ch.vd.unireg.security.Role;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.TiersCriteria;
import ch.vd.unireg.tiers.TiersIndexedDataView;
import ch.vd.unireg.tiers.view.TiersCriteriaView;
import ch.vd.unireg.transaction.TransactionHelper;
import ch.vd.unireg.type.TypeEtatEntreprise;
import ch.vd.unireg.utils.WebContextUtils;

@Controller
@RequestMapping("/processuscomplexe/transfertpatrimoine")
public class TransfertPatrimoineController extends AbstractProcessusComplexeController implements InitializingBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(TransfertPatrimoineController.class);

	public static final String CRITERIA_NAME_EMETTRICE = "TransfertPatrimoineEntrepriseCriteriaEmettrice";
	public static final String CRITERIA_NAME_RECEPTRICE = "TransfertPatrimoineEntrepriseCriteriaReceptrice";
	public static final String TRANSFERT_NAME = "TransfertPatrimoineEntreprise";

	private static final String TRANSFERT = "transfert";

	private SearchTiersComponent searchEmettriceComponent;
	private SearchTiersComponent searchReceptriceComponent;

	private void checkDroitAcces() throws AccessDeniedException {
		checkAnyGranted("Vous ne possédez pas les droits d'accès au processus complexe de transfert de patrimoine d'entreprise.",
		                Role.TRANSFERT_PATRIMOINE_ENTREPRISE);
	}

	@Nullable
	private static TransferPatrimoineSessionData getSessionData(HttpSession session) {
		return (TransferPatrimoineSessionData) session.getAttribute(TRANSFERT_NAME);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		this.searchEmettriceComponent = buildSearchComponent(CRITERIA_NAME_EMETTRICE, "entreprise/transfertpatrimoine/list-emettrice",
		                                                     new SearchTiersComponent.TiersCriteriaFiller() {
			                                                     @Override
			                                                     public void fill(TiersCriteriaView data) {
				                                                     data.setTypeTiersImperatif(TiersCriteria.TypeTiers.ENTREPRISE);
				                                                     data.setEtatsEntrepriseInterdits(EnumSet.of(TypeEtatEntreprise.ABSORBEE, TypeEtatEntreprise.DISSOUTE, TypeEtatEntreprise.RADIEE_RC, TypeEtatEntreprise.EN_FAILLITE));
			                                                     }
		                                                     });

		this.searchReceptriceComponent = buildSearchComponent(CRITERIA_NAME_RECEPTRICE, "entreprise/transfertpatrimoine/list-receptrices",
		                                                      new SearchTiersComponent.TiersCriteriaFiller() {
			                                                      @Override
			                                                      public void fill(TiersCriteriaView data) {
				                                                      fillCriteresImperatifsPourEntrepriseReceptrice(data);
			                                                      }
		                                                      },
		                                                      new SearchTiersComponent.ModelFiller() {
			                                                      @Override
			                                                      public void fill(Model model, HttpSession session) throws SearchTiersComponent.RedirectException {
				                                                      final TransferPatrimoineSessionData sessionData = getSessionData(session);
				                                                      if (sessionData == null) {
					                                                      Flash.warning("La session a été invalidée. Veuillez recommencer votre saisie.");
					                                                      throw new SearchTiersComponent.RedirectException("../emettrice/list.do");
				                                                      }
				                                                      model.addAttribute(TRANSFERT, sessionData);
			                                                      }
		                                                      },
		                                                      new SearchTiersComponent.TiersSearchAdapter<SelectionEntrepriseView>() {
			                                                      @Override
			                                                      public List<SelectionEntrepriseView> adaptSearchResult(List<TiersIndexedDataView> result, HttpSession session) {
				                                                      final TransferPatrimoineSessionData sessionData = getSessionData(session);
				                                                      return TransfertPatrimoineController.this.adapteSearchResults(result, sessionData);
			                                                      }
		                                                      });
	}

	@RequestMapping(value = "/emettrice/list.do", method = RequestMethod.GET)
	public String showFormulaireRechercheEmettrice(Model model, HttpSession session) {
		checkDroitAcces();

		// quand on arrive sur l'écran, on efface les données de la scission précédente, au cas où il y en aurait encore...
		session.removeAttribute(TRANSFERT_NAME);
		return searchEmettriceComponent.showFormulaireRecherche(model, session);
	}

	@RequestMapping(value = "/emettrice/reset-search.do", method = RequestMethod.GET)
	public String resetCriteresRechercheEmettrice(HttpSession session) {
		checkDroitAcces();
		return searchEmettriceComponent.resetCriteresRecherche(session, "list.do");
	}

	@RequestMapping(value = "/emettrice/list.do", method = RequestMethod.POST)
	public String doRechercheEmettrice(@Valid @ModelAttribute(value = SearchTiersComponent.COMMAND) TiersCriteriaView view, BindingResult bindingResult, HttpSession session, Model model) {
		checkDroitAcces();
		return searchEmettriceComponent.doRecherche(view, bindingResult, session, model, "list.do");
	}

	@RequestMapping(value = "/choix-date.do", method = RequestMethod.GET)
	public String showStart(Model model, @RequestParam("emettrice") long idEntreprise) {
		checkDroitAcces();
		controllerUtils.checkAccesDossierEnEcriture(idEntreprise);
		return showStart(model, new TransfertPatrimoineView(idEntreprise));
	}

	@RequestMapping(value = "/retour-choix-date.do", method = RequestMethod.GET)
	public String showRetourStart(Model model, HttpSession session) {
		checkDroitAcces();

		final TransferPatrimoineSessionData sessionData = getSessionData(session);
		if (sessionData == null) {
			Flash.warning("La session a été invalidée. Veuillez recommencer votre saisie.");
			return "redirect:emettrice/list.do";
		}

		controllerUtils.checkAccesDossierEnEcriture(sessionData.getIdEntrepriseEmettrice());
		return showStart(model, new TransfertPatrimoineView(sessionData.getIdEntrepriseEmettrice(),
		                                                    sessionData.getDateTransfert()));
	}

	private String showStart(Model model, TransfertPatrimoineView view) {
		model.addAttribute(ACTION_COMMAND, view);
		return "entreprise/transfertpatrimoine/choix-date";
	}

	@RequestMapping(value = "/choix-date.do", method = RequestMethod.POST)
	public String doSetDates(Model model, @Valid @ModelAttribute(value = ACTION_COMMAND) final TransfertPatrimoineView view, BindingResult bindingResult, HttpSession session) throws Exception {
		checkDroitAcces();
		controllerUtils.checkAccesDossierEnEcriture(view.getIdEntrepriseEmettrice());
		if (bindingResult.hasErrors()) {
			return showStart(model, view);
		}

		// on conserve les données du transfert en session
		final TransferPatrimoineSessionData newSessionData = new TransferPatrimoineSessionData(view.getIdEntrepriseEmettrice(), view.getDateTransfert());
		final TransferPatrimoineSessionData oldSessionData = getSessionData(session);
		if (oldSessionData != null && newSessionData.getIdEntrepriseEmettrice() == oldSessionData.getIdEntrepriseEmettrice()) {
			for (TransferPatrimoineSessionData.EntrepriseReceptrice oldReceptrice : oldSessionData.getEntreprisesReceptrices()) {
				newSessionData.addEntrepriseReceptrice(oldReceptrice);
			}
		}
		session.setAttribute(TRANSFERT_NAME, newSessionData);

		return "redirect:/processuscomplexe/transfertpatrimoine/receptrices/list.do";
	}

	@RequestMapping(value = "/receptrices/list.do", method = RequestMethod.GET)
	public String showFormulaireRechercheReceptrice(Model model, HttpSession session) {
		checkDroitAcces();
		return searchReceptriceComponent.showFormulaireRecherche(model, session);
	}

	@RequestMapping(value = "/receptrices/reset-search.do", method = RequestMethod.GET)
	public String resetCriteresRechercheReceptrice(HttpSession session) {
		checkDroitAcces();
		return searchReceptriceComponent.resetCriteresRecherche(session, "list.do?searched=true");
	}

	private void fillCriteresImperatifsPourEntrepriseReceptrice(TiersCriteriaView criteria) {
		criteria.setTypeTiersImperatif(TiersCriteria.TypeTiers.ENTREPRISE);
		criteria.setEtatsEntrepriseInterdits(EnumSet.of(TypeEtatEntreprise.ABSORBEE, TypeEtatEntreprise.DISSOUTE, TypeEtatEntreprise.RADIEE_RC, TypeEtatEntreprise.EN_FAILLITE));
	}

	private List<SelectionEntrepriseView> adapteSearchResults(final List<TiersIndexedDataView> searchResults, @NotNull final TransferPatrimoineSessionData sessionData) {
		final List<SelectionEntrepriseView> results = new ArrayList<>(searchResults.size());
		final Set<Long> idsEntreprisesDejaSelectionnees = sessionData.getIdsEntreprisesReceptrices();
		if (!searchResults.isEmpty()) {
			doInReadOnlyTransaction(status -> {
				for (TiersIndexedDataView searchResult : searchResults) {

					// si l'entreprise est déjà sélectionnée comme entreprise émettrice, on ne peut pas la reprendre dans les réceptrices
					final String explicationNonSelectionnable;
					if (sessionData.getIdEntrepriseEmettrice() == searchResult.getNumero()) {
						explicationNonSelectionnable = messageSource.getMessage("label.transfert.patrimoine.entreprise.deja.utilisee.comme.emettrice", null, WebContextUtils.getDefaultLocale());
					}
					// si l'entreprise est déjà sélectionnée dans les entreprises résultantes, on ne peut pas la choisir à nouveau
					else if (idsEntreprisesDejaSelectionnees.contains(searchResult.getNumero())) {
						explicationNonSelectionnable = messageSource.getMessage("label.transfert.patrimoine.entreprise.deja.utilisee.comme.receptrice", null, WebContextUtils.getDefaultLocale());
					}
					else {
						explicationNonSelectionnable = null;
					}

					final SelectionEntrepriseView view = new SelectionEntrepriseView(searchResult, explicationNonSelectionnable);
					results.add(view);
				}
				return null;
			});
		}
		return results;
	}

	@RequestMapping(value = "/receptrices/add.do", method = RequestMethod.POST)
	public String addReceptrice(@RequestParam("id") long idEntreprise, HttpSession session) {
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
		fillCriteresImperatifsPourEntrepriseReceptrice(criteria);
		criteria.setNumero(idEntreprise);

		try {
			final List<TiersIndexedDataView> resultats = searchReceptriceComponent._searchTiers(criteria);
			if (resultats.isEmpty()) {
				Flash.error("Impossible de récupérer les données du tiers " + FormatNumeroHelper.numeroCTBToDisplay(idEntreprise));
				return "redirect:list.do";
			}

			// ajout de la donnée
			final TransferPatrimoineSessionData sessionData = getSessionData(session);
			if (sessionData == null) {
				Flash.warning("La session a été invalidée. Veuillez recommencer votre saisie.");
				return "redirect:../emettrice/list.do";
			}

			sessionData.addEntrepriseReceptrice(new TransferPatrimoineSessionData.EntrepriseReceptrice(resultats.get(0)));
			return "redirect:list.do";
		}
		catch (IndexerException e) {
			LOGGER.error(e.getMessage(), e);
			Flash.error("Impossible de récupérer les données du tiers " + FormatNumeroHelper.numeroCTBToDisplay(idEntreprise));
			return "redirect:list.do";
		}
	}

	@RequestMapping(value = "/receptrices/remove.do", method = RequestMethod.POST)
	public String removeReceptrice(@RequestParam("id") long idEntreprise, HttpSession session) {
		checkDroitAcces();

		// retrait de la donnée
		final TransferPatrimoineSessionData sessionData = getSessionData(session);
		if (sessionData == null) {
			Flash.warning("La session a été invalidée. Veuillez recommencer votre saisie.");
			return "redirect:../emettrice/list.do";
		}

		sessionData.removeEntrepriseReceptrice(idEntreprise);
		return "redirect:list.do";
	}

	@RequestMapping(value = "/receptrices/list.do", method = RequestMethod.POST)
	public String doRechercheReceptrice(@Valid @ModelAttribute(value = SearchTiersComponent.COMMAND) TiersCriteriaView view, BindingResult bindingResult, HttpSession session, Model model) {
		checkDroitAcces();
		return searchReceptriceComponent.doRecherche(view, bindingResult, session, model, "list.do?searched=true");
	}

	@RequestMapping(value = "/transferer.do", method = RequestMethod.POST)
	public String transferer(HttpSession session) {
		checkDroitAcces();

		// récupération des données du transfert
		final TransferPatrimoineSessionData sessionData = getSessionData(session);
		if (sessionData == null) {
			Flash.warning("La session a été invalidée. Veuillez recommencer votre saisie.");
			return "redirect:emettrice/list.do";
		}
		else if (sessionData.getIdsEntreprisesReceptrices().isEmpty()) {
			throw new ActionException("Veuillez sélectionner au moins une entreprise réceptrice.");
		}

		// on récupère les données des entreprises et on envoie tout ça dans le moteur
		doInTransaction(new TransactionHelper.ExceptionThrowingCallbackWithoutResult<MetierServiceException>() {
			@Override
			public void execute(TransactionStatus status) throws MetierServiceException {
				final Entreprise emettrice = getTiers(Entreprise.class, sessionData.getIdEntrepriseEmettrice());
				final Set<Long> idsEntreprisesReceptrices = sessionData.getIdsEntreprisesReceptrices();
				final List<Entreprise> receptrices = new ArrayList<>(idsEntreprisesReceptrices.size());
				for (Long id : idsEntreprisesReceptrices) {
					receptrices.add(getTiers(Entreprise.class, id));
				}

				metierService.transferePatrimoine(emettrice, receptrices, sessionData.getDateTransfert());
			}
		});

		// quand le boulot est terminé (correctement), on efface les données de la session
		session.removeAttribute(TRANSFERT_NAME);

		// ... et on termine dans le dossier de l'entreprise émettrice
		return "redirect:/tiers/visu.do?id=" + sessionData.getIdEntrepriseEmettrice();
	}
}
