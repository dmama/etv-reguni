package ch.vd.uniregctb.entreprise.complexe;

import javax.validation.Valid;
import java.util.List;

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
import ch.vd.uniregctb.common.CollectionsUtils;
import ch.vd.uniregctb.common.Flash;
import ch.vd.uniregctb.metier.MetierServiceException;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.tiers.DomicileHisto;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.TiersCriteria;
import ch.vd.uniregctb.tiers.view.TiersCriteriaView;

@Controller
@RequestMapping("/processuscomplexe/demenagement")
public class DemenagementSiegeController extends AbstractProcessusComplexeRechercheController {

	public static final String CRITERIA_NAME = "DemenagementSiegeCriteria";

	private static final String TYPES_AUTORITE_FISCALE = "typesAutoriteFiscale";

	private static final String DEBUT_SIEGE_ACTUEL = "dateDebutSiegeActuel";
	private static final String TYPE_AUTORITE_SIEGE_ACTUEL = "typeAutoriteFiscaleSiegeActuel";
	private static final String OFS_SIEGE_ACTUEL = "noOfsSiegeActuel";

	@Override
	protected void checkDroitAcces() throws AccessDeniedException {
		checkAnyGranted("Vous ne possédez aucun droit IfoSec pour l'accès au processus complexe de déménagement de siège d'entreprise.",
		                Role.DEMENAGEMENT_SIEGE_ENTREPRISE);
	}

	@Override
	protected String getSearchCriteriaSessionName() {
		return CRITERIA_NAME;
	}

	@Override
	protected void fillCriteriaWithImplicitValues(TiersCriteriaView criteria) {
		criteria.setTiersActif(Boolean.TRUE);
		criteria.setTypeTiersImperatif(TiersCriteria.TypeTiers.ENTREPRISE);
	}

	@Override
	protected String getSearchResultViewPath() {
		return "entreprise/demenagement/list";
	}

	@RequestMapping(value = "/start.do", method = RequestMethod.GET)
	public String showStart(Model model, @RequestParam("id") long idEntreprise) {
		checkDroitAcces();
		controllerUtils.checkAccesDossierEnEcriture(idEntreprise);
		return showStart(model, new DemenagementSiegeView(idEntreprise));
	}

	private String showStart(final Model model, final DemenagementSiegeView view) {
		model.addAttribute(SearchTiersComponent.COMMAND, view);
		model.addAttribute(TYPES_AUTORITE_FISCALE, tiersMapHelper.getMapTypeAutoriteFiscale());
		doInReadOnlyTransaction(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = getTiers(Entreprise.class, view.getIdEntreprise());
				final List<DomicileHisto> sieges = tiersService.getSieges(entreprise, false);
				if (sieges != null && !sieges.isEmpty()) {
					final DomicileHisto dernierSiege = CollectionsUtils.getLastElement(sieges);

					// on essaie de récupérer la date de début du siège à cet endroit (même si c'est passé par des données
					// civiles et fiscales...)
					RegDate dateDebut = dernierSiege.getDateDebut();
					for (DomicileHisto domicile : CollectionsUtils.revertedOrder(sieges)) {
						if (domicile.getTypeAutoriteFiscale() != dernierSiege.getTypeAutoriteFiscale() ||
								domicile.getNumeroOfsAutoriteFiscale() == null ||
								!domicile.getNumeroOfsAutoriteFiscale().equals(dernierSiege.getNumeroOfsAutoriteFiscale())) {
							break;
						}
						dateDebut = domicile.getDateDebut();
					}

					model.addAttribute(DEBUT_SIEGE_ACTUEL, dateDebut);
					model.addAttribute(TYPE_AUTORITE_SIEGE_ACTUEL, dernierSiege.getTypeAutoriteFiscale());
					model.addAttribute(OFS_SIEGE_ACTUEL, dernierSiege.getNumeroOfsAutoriteFiscale());
				}
			}
		});
		return "entreprise/demenagement/start";
	}

	@RequestMapping(value = "/start.do", method = RequestMethod.POST)
	public String doDemenagement(Model model, @Valid @ModelAttribute(value = SearchTiersComponent.COMMAND) final DemenagementSiegeView view, BindingResult bindingResult) throws Exception {
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
				metierService.demenageSiege(entreprise, view.getDateDebutNouveauSiege(), view.getTypeAutoriteFiscale(), view.getNoAutoriteFiscale());
			}
		});

		// tout s'est bien passé... mais si le contribuable est sous le coup d'une décision ACI
		// (et que l'utilisateur courant a le droit de modification, ce qui est le cas si nous sommes
		// arrivés jusqu'ici), il faut lui laisser un petit mot...
		doInReadOnlyTransaction(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = getTiers(Entreprise.class, view.getIdEntreprise());
				if (tiersService.isSousInfluenceDecisions(entreprise)) {
					Flash.warning("Cette entreprise est actuellement sous l'influence d'une décision ACI. Veuillez en vérifier la pertinence après ce déménagement de siège.");
				}
			}
		});

		return "redirect:/tiers/visu.do?id=" + view.getIdEntreprise();
	}
}
