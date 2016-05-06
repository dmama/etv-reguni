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

@Controller
@RequestMapping("/processuscomplexe/annulation/scission")
public class AnnulationScissionEntrepriseController extends AbstractProcessusComplexeRechercheController {

	public static final String CRITERIA_NAME = "AnnulationScissionEntrepriseCriteria";

	private static final String DATES_CONTRAT = "datesContrat";

	@Override
	protected void checkDroitAcces() throws AccessDeniedException {
		checkAnyGranted("Vous ne possédez aucun droit IfoSec pour l'accès au processus complexe d'annulation de scission d'entreprise.",
		                Role.SCISSION_ENTREPRISE);
	}

	@Override
	protected String getSearchCriteriaSessionName() {
		return CRITERIA_NAME;
	}

	/**
	 * @param idEntreprise identifiant d'une entreprise considérée dans son rôle d'entreprise scindée
	 * @return la liste (triée par ordre chronologique inverse) des dates de contrats de scission passées
	 */
	@RequestMapping(value = "/dates-contrat.do", method = RequestMethod.GET)
	@ResponseBody
	public List<RegDate> getDatesContratScissionExistantes(@RequestParam("idEntreprise") final long idEntreprise) {
		return doInReadOnlyTransaction(new TransactionCallback<List<RegDate>>() {
			@Override
			public List<RegDate> doInTransaction(TransactionStatus status) {
				final Entreprise scindee = getTiers(Entreprise.class, idEntreprise);
				final List<RegDate> datesContrats = new ArrayList<>(ScissionEntrepriseHelper.getScissions(scindee, tiersService).keySet());
				Collections.sort(datesContrats, Collections.<RegDate>reverseOrder());
				return datesContrats;
			}
		});
	}

	@Override
	protected void fillCriteriaWithImplicitValues(TiersCriteriaView criteria) {
		criteria.setTypeTiersImperatif(TiersCriteria.TypeTiers.ENTREPRISE);
		criteria.setCorporationSplit(Boolean.TRUE);
	}

	@Override
	protected String getSearchResultViewPath() {
		return "entreprise/annulation-scission/list";
	}

	@RequestMapping(value = "/choix-date.do", method = RequestMethod.GET)
	public String showStart(Model model, @RequestParam("scindee") long idEntreprise) {
		checkDroitAcces();
		controllerUtils.checkAccesDossierEnEcriture(idEntreprise);
		return showStart(model, new ScissionEntrepriseView(idEntreprise));
	}

	private String showStart(final Model model, final ScissionEntrepriseView view) {
		model.addAttribute(SearchTiersComponent.COMMAND, view);

		final List<RegDate> datesContrat = getDatesContratScissionExistantes(view.getIdEntrepriseScindee());
		if (datesContrat.isEmpty()) {
			Flash.error(String.format("Impossible de reconstituer des scissions passées pour l'entreprise %s.", FormatNumeroHelper.numeroCTBToDisplay(view.getIdEntrepriseScindee())));
			return "redirect:list.do";
		}
		model.addAttribute(DATES_CONTRAT, datesContrat);
		return "entreprise/annulation-scission/choix-date";
	}

	@RequestMapping(value = "/choix-date.do", method = RequestMethod.POST)
	public String doAnnulationScission(Model model, @Valid @ModelAttribute(SearchTiersComponent.COMMAND) final ScissionEntrepriseView view, BindingResult bindingResult) {
		checkDroitAcces();
		if (bindingResult.hasErrors()) {
			return showStart(model, view);
		}

		doInTransaction(new MetierServiceExceptionAwareWithoutResultCallback() {
			@Override
			protected void doExecute(TransactionStatus status) throws MetierServiceException {
				// récupération des données et vérification des droits d'accès
				final Entreprise scindee = getTiers(Entreprise.class, view.getIdEntrepriseScindee());
				controllerUtils.checkAccesDossierEnEcriture(view.getIdEntrepriseScindee());
				controllerUtils.checkTraitementContribuableAvecDecisionAci(view.getIdEntrepriseScindee());

				final Map<RegDate, List<Entreprise>> scissions = ScissionEntrepriseHelper.getScissions(scindee, tiersService);
				final List<Entreprise> resultantes = scissions.get(view.getDateContratScission());
				if (resultantes == null || resultantes.isEmpty()) {
					throw new MetierServiceException("Aucune entreprise résultante de la scission trouvée!");
				}

				// petite boucle pour vérifier les droits d'accès sur les résultantes aussi
				for (Entreprise resultante : resultantes) {
					controllerUtils.checkAccesDossierEnEcriture(resultante.getNumero());
					controllerUtils.checkTraitementContribuableAvecDecisionAci(resultante.getNumero());
				}

				// envoi de la sauce pour dé-tricoter tout ça
				metierService.annuleScission(scindee, resultantes, view.getDateContratScission());
			}
		});

		return "redirect:/tiers/visu.do?id=" + view.getIdEntrepriseScindee();
	}
}