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
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.common.ActionException;
import ch.vd.unireg.common.Flash;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.common.ObjectNotFoundException;
import ch.vd.unireg.indexer.IndexerException;
import ch.vd.unireg.security.AccessDeniedException;
import ch.vd.unireg.security.Role;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.EtatEntreprise;
import ch.vd.unireg.tiers.TiersCriteria;
import ch.vd.unireg.tiers.TiersIndexedDataView;
import ch.vd.unireg.tiers.view.TiersCriteriaView;
import ch.vd.unireg.type.TypeEtatEntreprise;
import ch.vd.unireg.utils.WebContextUtils;

@Controller
@RequestMapping("/processuscomplexe/fusion")
public class FusionEntreprisesController extends AbstractProcessusComplexeController implements InitializingBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(FusionEntreprisesController.class);

	public static final String CRITERIA_NAME_ABSORBANTE = "FusionEntreprisesCriteriaAbsorbante";
	public static final String CRITERIA_NAME_ABSORBEE = "FusionEntreprisesCriteriaAbsorbee";
	public static final String FUSION_NAME = "FusionEntreprises";

	private static final String FUSION = "fusion";

	private SearchTiersComponent searchAbsorbanteComponent;
	private SearchTiersComponent searchAbsorbeeComponent;

	private void checkDroitAcces() throws AccessDeniedException {
		checkAnyGranted("Vous ne possédez pas les droits d'accès au processus complexe de fusion d'entreprises.",
		                Role.FUSION_ENTREPRISES);
	}

	@Nullable
	private static FusionEntreprisesSessionData getSessionData(HttpSession session) {
		return (FusionEntreprisesSessionData) session.getAttribute(FUSION_NAME);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		this.searchAbsorbanteComponent = buildSearchComponent(CRITERIA_NAME_ABSORBANTE, "entreprise/fusion/list-absorbante",
		                                                      data -> {
			                                                      data.setTypeTiersImperatif(TiersCriteria.TypeTiers.ENTREPRISE);
			                                                      data.setEtatsEntrepriseInterdits(EnumSet.of(TypeEtatEntreprise.ABSORBEE, TypeEtatEntreprise.DISSOUTE, TypeEtatEntreprise.RADIEE_RC));
		                                                      });

		this.searchAbsorbeeComponent = buildSearchComponent(CRITERIA_NAME_ABSORBEE, "entreprise/fusion/list-absorbees",
		                                                    this::fillCriteresImperatifsPourEntrepriseAbsorbee,
		                                                    (model, session) -> {
			                                                    final FusionEntreprisesSessionData sessionData = getSessionData(session);
			                                                    if (sessionData == null) {
				                                                    Flash.warning("La session a été invalidée. Veuillez recommencer votre saisie.");
				                                                    throw new SearchTiersComponent.RedirectException("../absorbante/list.do");
			                                                    }
			                                                    model.addAttribute(FUSION, sessionData);
		                                                    },
		                                                    (SearchTiersComponent.TiersSearchAdapter<SelectionEntrepriseView>) (result, session) -> {
			                                                    final FusionEntreprisesSessionData sessionData = getSessionData(session);
			                                                    return FusionEntreprisesController.this.adapteSearchResults(result, sessionData);
		                                                    });
	}

	@RequestMapping(value = "/absorbante/list.do", method = RequestMethod.GET)
	public String showFormulaireRechercheAbsorbante(Model model, HttpSession session) {
		checkDroitAcces();

		// quand on arrive sur l'écran, on efface les données de la fusion précédente, au cas où il y en aurait encore...
		session.removeAttribute(FUSION_NAME);
		return searchAbsorbanteComponent.showFormulaireRecherche(model, session);
	}

	@RequestMapping(value = "/absorbante/reset-search.do", method = RequestMethod.GET)
	public String resetCriteresRechercheAbsorbante(HttpSession session) {
		checkDroitAcces();
		return searchAbsorbanteComponent.resetCriteresRecherche(session, "list.do");
	}

	@RequestMapping(value = "/absorbante/list.do", method = RequestMethod.POST)
	public String doRechercheAbsorbante(@Valid @ModelAttribute(value = SearchTiersComponent.COMMAND) TiersCriteriaView view, BindingResult bindingResult, HttpSession session, Model model) {
		checkDroitAcces();
		return searchAbsorbanteComponent.doRecherche(view, bindingResult, session, model, "list.do");
	}

	@RequestMapping(value = "/choix-dates.do", method = RequestMethod.GET)
	public String showStart(Model model, @RequestParam("absorbante") long idEntreprise) {
		checkDroitAcces();
		controllerUtils.checkAccesDossierEnEcriture(idEntreprise);
		return showStart(model, new FusionEntreprisesView(idEntreprise));
	}

	@RequestMapping(value = "/retour-choix-dates.do", method = RequestMethod.GET)
	public String showRetourStart(Model model, HttpSession session) {
		checkDroitAcces();

		final FusionEntreprisesSessionData sessionData = getSessionData(session);
		if (sessionData == null) {
			Flash.warning("La session a été invalidée. Veuillez recommencer votre saisie.");
			return "redirect:absorbante/list.do";
		}

		controllerUtils.checkAccesDossierEnEcriture(sessionData.getIdEntrepriseAbsorbante());
		return showStart(model, new FusionEntreprisesView(sessionData.getIdEntrepriseAbsorbante(),
		                                                  sessionData.getDateContratFusion(),
		                                                  sessionData.getDateBilanFusion()));
	}

	private String showStart(Model model, FusionEntreprisesView view) {
		model.addAttribute(ACTION_COMMAND, view);
		return "entreprise/fusion/choix-dates";
	}

	@RequestMapping(value = "/choix-dates.do", method = RequestMethod.POST)
	public String doSetDates(Model model, @Valid @ModelAttribute(value = ACTION_COMMAND) final FusionEntreprisesView view, BindingResult bindingResult, HttpSession session) throws Exception {
		checkDroitAcces();
		controllerUtils.checkAccesDossierEnEcriture(view.getIdEntrepriseAbsorbante());
		if (bindingResult.hasErrors()) {
			return showStart(model, view);
		}

		// plus de 6 mois entre les deux dates -> warning
		final RegDate dateContratFusion = view.getDateContratFusion();
		final RegDate dateBilanFusion = view.getDateBilanFusion();
		if ((dateBilanFusion.isBefore(dateContratFusion) && dateBilanFusion.addMonths(6).isBefore(dateContratFusion)) || dateContratFusion.isBefore(dateBilanFusion) && dateContratFusion.addMonths(6).isBefore(dateBilanFusion)) {
			Flash.warning("Les dates de contrat et de bilan de fusion sont séparées de plus de 6 mois.");
		}

		// on conserve les données de la fusion en session
		final FusionEntreprisesSessionData newSessionData = new FusionEntreprisesSessionData(view.getIdEntrepriseAbsorbante(), dateContratFusion, dateBilanFusion);
		final FusionEntreprisesSessionData oldSessionData = getSessionData(session);
		if (oldSessionData != null && newSessionData.getIdEntrepriseAbsorbante() == oldSessionData.getIdEntrepriseAbsorbante()) {
			for (FusionEntreprisesSessionData.EntrepriseAbsorbee oldAbsorbee : oldSessionData.getEntreprisesAbsorbees()) {
				newSessionData.addEntrepriseAbsorbee(oldAbsorbee);
			}
		}
		session.setAttribute(FUSION_NAME, newSessionData);

		return "redirect:/processuscomplexe/fusion/absorbees/list.do";
	}

	@RequestMapping(value = "/absorbees/list.do", method = RequestMethod.GET)
	public String showFormulaireRechercheAbsorbee(Model model, HttpSession session) {
		checkDroitAcces();
		return searchAbsorbeeComponent.showFormulaireRecherche(model, session);
	}

	@RequestMapping(value = "/absorbees/reset-search.do", method = RequestMethod.GET)
	public String resetCriteresRechercheAbsorbee(HttpSession session) {
		checkDroitAcces();
		return searchAbsorbeeComponent.resetCriteresRecherche(session, "list.do?searched=true");
	}

	private void fillCriteresImperatifsPourEntrepriseAbsorbee(TiersCriteriaView criteria) {
		criteria.setTypeTiersImperatif(TiersCriteria.TypeTiers.ENTREPRISE);
		criteria.setEtatsEntrepriseInterdits(EnumSet.of(TypeEtatEntreprise.ABSORBEE));
	}

	private List<SelectionEntrepriseView> adapteSearchResults(final List<TiersIndexedDataView> searchResults, @NotNull final FusionEntreprisesSessionData sessionData) {
		final List<SelectionEntrepriseView> results = new ArrayList<>(searchResults.size());
		final Set<Long> idsEntreprisesDejaAbsorbees = sessionData.getIdsEntreprisesAbsorbees();
		final RegDate dateSeuil = RegDateHelper.minimum(sessionData.getDateBilanFusion(), sessionData.getDateContratFusion(), NullDateBehavior.LATEST);
		if (!searchResults.isEmpty()) {
			doInReadOnlyTransaction(status -> {
				for (TiersIndexedDataView searchResult : searchResults) {
					final String explicationNonSelectionnable;

					// si l'entreprise est déjà sélectionnée comme entreprise absorbante, on ne peut pas la reprendre dans les absorbées
					if (sessionData.getIdEntrepriseAbsorbante() == searchResult.getNumero()) {
						explicationNonSelectionnable = messageSource.getMessage("label.fusion.entreprise.deja.utilisee.comme.absorbante", null, WebContextUtils.getDefaultLocale());
					}
					// si l'entreprise est déjà sélectionnée dans les entreprises absorbées, on ne peut pas la choisir à nouveau
					else if (idsEntreprisesDejaAbsorbees.contains(searchResult.getNumero())) {
						explicationNonSelectionnable = messageSource.getMessage("label.fusion.entreprise.deja.utilisee.comme.absorbee", null, WebContextUtils.getDefaultLocale());
					}
					else {
						final Entreprise entreprise = getTiers(Entreprise.class, searchResult.getNumero());
						final List<EtatEntreprise> etats = entreprise.getEtatsNonAnnulesTries();
						final TypeEtatEntreprise etatAvantAbsorption = etats.stream()
								.filter(etat -> RegDateHelper.isBeforeOrEqual(etat.getDateObtention(), dateSeuil, NullDateBehavior.EARLIEST))
								.reduce((a, b) -> b)            // on veut le dernier avant la date seuil (pas la peine de re-trier, puisque la liste de départ l'était déjà)
								.map(EtatEntreprise::getType)
								.orElse(null);

						if (etatAvantAbsorption == TypeEtatEntreprise.DISSOUTE) {
							explicationNonSelectionnable = messageSource.getMessage("label.fusion.entreprise.dissoute.avant.dates.fusion", null, WebContextUtils.getDefaultLocale());
						}
						else if (etatAvantAbsorption == TypeEtatEntreprise.RADIEE_RC) {
							explicationNonSelectionnable = messageSource.getMessage("label.fusion.entreprise.radiee.rc.avant.dates.fusion", null, WebContextUtils.getDefaultLocale());
						}
						else {
							explicationNonSelectionnable = null;
						}
					}

					final SelectionEntrepriseView view = new SelectionEntrepriseView(searchResult, explicationNonSelectionnable);
					results.add(view);
				}
				return null;
			});
		}
		return results;
	}

	@RequestMapping(value = "/absorbees/add.do", method = RequestMethod.POST)
	public String addAbsorbee(@RequestParam("id") long idAbsorbee, HttpSession session) {
		checkDroitAcces();

		// on vérifie qu'on a bien les droits sur l'entreprise rajoutée
		try {
			controllerUtils.checkAccesDossierEnEcriture(idAbsorbee);
			controllerUtils.checkTraitementContribuableAvecDecisionAci(idAbsorbee);
		}
		catch (AccessDeniedException | ObjectNotFoundException e) {
			Flash.error(e.getMessage());
			return "redirect:list.do";
		}

		// on va chercher à nouveau dans l'indexeur les données de cette entreprise
		final TiersCriteriaView criteria = new TiersCriteriaView();
		fillCriteresImperatifsPourEntrepriseAbsorbee(criteria);
		criteria.setNumero(idAbsorbee);

		try {
			final List<TiersIndexedDataView> resultats = searchAbsorbeeComponent._searchTiers(criteria);
			if (resultats.isEmpty()) {
				Flash.error("Impossible de récupérer les données du tiers " + FormatNumeroHelper.numeroCTBToDisplay(idAbsorbee));
				return "redirect:list.do";
			}

			// ajout de la donnée
			final FusionEntreprisesSessionData sessionData = getSessionData(session);
			if (sessionData == null) {
				Flash.warning("La session a été invalidée. Veuillez recommencer votre saisie.");
				return "redirect:../absorbante/list.do";
			}

			sessionData.addEntrepriseAbsorbee(new FusionEntreprisesSessionData.EntrepriseAbsorbee(resultats.get(0)));
			return "redirect:list.do";
		}
		catch (IndexerException e) {
			LOGGER.error(e.getMessage(), e);
			Flash.error("Impossible de récupérer les données du tiers " + FormatNumeroHelper.numeroCTBToDisplay(idAbsorbee));
			return "redirect:list.do";
		}
	}

	@RequestMapping(value = "/absorbees/remove.do", method = RequestMethod.POST)
	public String removeAbsorbee(@RequestParam("id") long idAbsorbee, HttpSession session) {
		checkDroitAcces();

		// retrait de la donnée
		final FusionEntreprisesSessionData sessionData = getSessionData(session);
		if (sessionData == null) {
			Flash.warning("La session a été invalidée. Veuillez recommencer votre saisie.");
			return "redirect:../absorbante/list.do";
		}

		sessionData.removeEntrepriseAbsorbee(idAbsorbee);
		return "redirect:list.do";
	}

	@RequestMapping(value = "/absorbees/list.do", method = RequestMethod.POST)
	public String doRechercheAbsorbee(@Valid @ModelAttribute(value = SearchTiersComponent.COMMAND) TiersCriteriaView view, BindingResult bindingResult, HttpSession session, Model model) {
		checkDroitAcces();
		return searchAbsorbeeComponent.doRecherche(view, bindingResult, session, model, "list.do?searched=true");
	}

	@RequestMapping(value = "/fusionner.do", method = RequestMethod.POST)
	public String fusionner(HttpSession session) {
		checkDroitAcces();

		// récupération des données de fusion
		final FusionEntreprisesSessionData sessionData = getSessionData(session);
		if (sessionData == null) {
			Flash.warning("La session a été invalidée. Veuillez recommencer votre saisie.");
			return "redirect:absorbante/list.do";
		}
		else if (sessionData.getIdsEntreprisesAbsorbees().isEmpty()) {
			throw new ActionException("Veuillez sélectionner au moins une entreprise absorbée.");
		}

		// on récupère les données des entreprises et on envoie tout ça dans le moteur
		doInTransaction(status -> {
			final Entreprise absorbante = getTiers(Entreprise.class, sessionData.getIdEntrepriseAbsorbante());
			final Set<Long> idsEntreprisesAbsorbees = sessionData.getIdsEntreprisesAbsorbees();
			final List<Entreprise> absorbees = new ArrayList<>(idsEntreprisesAbsorbees.size());
			for (Long id : idsEntreprisesAbsorbees) {
				absorbees.add(getTiers(Entreprise.class, id));
			}
			metierService.fusionne(absorbante, absorbees, sessionData.getDateContratFusion(), sessionData.getDateBilanFusion());
		});

		// quand le boulot est terminé (correctement), on efface les données de la session
		session.removeAttribute(FUSION_NAME);

		// ... et on termine dans le dossier de l'entreprise absorbante
		return "redirect:/tiers/visu.do?id=" + sessionData.getIdEntrepriseAbsorbante();
	}
}
