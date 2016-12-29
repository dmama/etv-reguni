package ch.vd.uniregctb.entreprise.complexe;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.Flash;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.metier.MetierServiceException;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.TiersCriteria;
import ch.vd.uniregctb.tiers.view.TiersCriteriaView;
import ch.vd.uniregctb.transaction.TransactionHelper;

@Controller
@RequestMapping("/processuscomplexe/annulation/transfertpatrimoine")
public class AnnulationTransfertPatrimoineController extends AbstractProcessusComplexeRechercheController {

	public static final String CRITERIA_NAME = "AnnulationTransfertPatrimoineCriteria";

	private static final String DATES_TRANSFERT = "datesTransfert";

	@Override
	protected void checkDroitAcces() throws AccessDeniedException {
		checkAnyGranted("Vous ne possédez aucun droit IfoSec pour l'accès au processus complexe d'annulation de transfert de patrimoine.",
		                Role.TRANSFERT_PATRIMOINE_ENTREPRISE);
	}

	@Override
	protected String getSearchCriteriaSessionName() {
		return CRITERIA_NAME;
	}

	/**
	 * @param idEntreprise identifiant d'une entreprise considérée dans son rôle d'entreprise émettrice de patrimoine
	 * @return la liste (triée par ordre chronologique inverse) des dates de transfert passées
	 */
	@RequestMapping(value = "/dates-transfert.do", method = RequestMethod.GET)
	@ResponseBody
	public List<RegDate> getDatesTransfertExistantes(@RequestParam("idEntreprise") final long idEntreprise) {
		return doInReadOnlyTransaction(new TransactionCallback<List<RegDate>>() {
			@Override
			public List<RegDate> doInTransaction(TransactionStatus status) {
				final Entreprise emettrice = getTiers(Entreprise.class, idEntreprise);
				final List<RegDate> datesTransfert = new ArrayList<>(TransfertPatrimoineHelper.getTransferts(emettrice, tiersService).keySet());
				Collections.sort(datesTransfert, Collections.<RegDate>reverseOrder());
				return datesTransfert;
			}
		});
	}

	@Override
	protected void fillCriteriaWithImplicitValues(TiersCriteriaView criteria) {
		criteria.setTypeTiersImperatif(TiersCriteria.TypeTiers.ENTREPRISE);
		criteria.setCorporationTransferedPatrimony(Boolean.TRUE);
	}

	@Override
	protected String getSearchResultViewPath() {
		return "entreprise/annulation-transfertpatrimoine/list";
	}

	@RequestMapping(value = "/choix-date.do", method = RequestMethod.GET)
	public String showStart(Model model, @RequestParam("emettrice") long idEntreprise) {
		checkDroitAcces();
		controllerUtils.checkAccesDossierEnEcriture(idEntreprise);
		return showStart(model, new TransfertPatrimoineView(idEntreprise));
	}

	private String showStart(final Model model, final TransfertPatrimoineView view) {
		model.addAttribute(ACTION_COMMAND, view);

		final List<RegDate> datesTransfert = getDatesTransfertExistantes(view.getIdEntrepriseEmettrice());
		if (datesTransfert.isEmpty()) {
			Flash.error(String.format("Impossible de reconstituer des transferts de patrimoine passés depuis l'entreprise %s.", FormatNumeroHelper.numeroCTBToDisplay(view.getIdEntrepriseEmettrice())));
			return "redirect:list.do";
		}
		model.addAttribute(DATES_TRANSFERT, datesTransfert);
		return "entreprise/annulation-transfertpatrimoine/choix-date";
	}

	@RequestMapping(value = "/choix-date.do", method = RequestMethod.POST)
	public String doAnnulationScission(Model model, @Valid @ModelAttribute(value = ACTION_COMMAND) final TransfertPatrimoineView view, BindingResult bindingResult) {
		checkDroitAcces();
		if (bindingResult.hasErrors()) {
			return showStart(model, view);
		}

		doInTransaction(new TransactionHelper.ExceptionThrowingCallbackWithoutResult<MetierServiceException>() {
			@Override
			public void execute(TransactionStatus status) throws MetierServiceException {
				// récupération des données et vérification des droits d'accès
				final Entreprise emettrice = getTiers(Entreprise.class, view.getIdEntrepriseEmettrice());
				controllerUtils.checkAccesDossierEnEcriture(view.getIdEntrepriseEmettrice());
				controllerUtils.checkTraitementContribuableAvecDecisionAci(view.getIdEntrepriseEmettrice());

				final Map<RegDate, List<Entreprise>> transferts = TransfertPatrimoineHelper.getTransferts(emettrice, tiersService);
				final List<Entreprise> receptrices = transferts.get(view.getDateTransfert());
				if (receptrices == null || receptrices.isEmpty()) {
					throw new MetierServiceException("Aucune entreprise réceptrice du transfert de patrimoine trouvée!");
				}

				// petite boucle pour vérifier les droits d'accès sur les réceptrices aussi
				for (Entreprise resultante : receptrices) {
					controllerUtils.checkAccesDossierEnEcriture(resultante.getNumero());
					controllerUtils.checkTraitementContribuableAvecDecisionAci(resultante.getNumero());
				}

				// envoi de la sauce pour dé-tricoter tout ça
				metierService.annuleTransfertPatrimoine(emettrice, receptrices, view.getDateTransfert());
			}
		});

		return "redirect:/tiers/visu.do?id=" + view.getIdEntrepriseEmettrice();
	}
}