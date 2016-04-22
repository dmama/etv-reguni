package ch.vd.uniregctb.entreprise.complexe;

import javax.validation.Valid;
import java.util.EnumSet;
import java.util.Set;

import org.springframework.stereotype.Controller;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import ch.vd.uniregctb.common.Flash;
import ch.vd.uniregctb.metier.MetierServiceException;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.ForFiscalPrincipalPM;
import ch.vd.uniregctb.tiers.TiersCriteria;
import ch.vd.uniregctb.tiers.view.TiersCriteriaView;
import ch.vd.uniregctb.type.MotifFor;

@Controller
@RequestMapping("/processuscomplexe/annulation/demenagement")
public class AnnulationDemenagementSiegeController extends AbstractProcessusComplexeRechercheController {

	public static final String CRITERIA_NAME = "AnnulationDemenagementSiegeCriteria";

	private static final String DEBUT_SIEGE_ACTUEL = "dateDebutSiegeActuel";
	private static final String TYPE_AUTORITE_SIEGE_ACTUEL = "typeAutoriteFiscaleSiegeActuel";
	private static final String OFS_SIEGE_ACTUEL = "noOfsSiegeActuel";

	private static final String DEBUT_SIEGE_PRECEDENT = "dateDebutSiegePrecedent";
	private static final String TYPE_AUTORITE_SIEGE_PRECEDENT = "typeAutoriteFiscaleSiegePrecedent";
	private static final String OFS_SIEGE_PRECEDENT = "noOfsSiegePrecedent";

	@Override
	protected void checkDroitAcces() throws AccessDeniedException {
		checkAnyGranted("Vous ne possédez aucun droit IfoSec pour l'accès au processus complexe d'annulation de déménagement de siège d'entreprise.",
		                Role.DEMENAGEMENT_SIEGE_ENTREPRISE);
	}

	@Override
	protected String getSearchCriteriaSessionName() {
		return CRITERIA_NAME;
	}

	@Override
	protected void fillCriteriaWithImplicitValues(TiersCriteriaView criteria) {
		criteria.setTiersActif(Boolean.TRUE);
		criteria.setConnuAuCivil(Boolean.FALSE);
		criteria.setTypeTiersImperatif(TiersCriteria.TypeTiers.ENTREPRISE);
	}

	@Override
	protected String getSearchResultViewPath() {
		return "entreprise/annulation-demenagement/list";
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
				if (dernierFor == null || dernierFor.getDateFin() != null) {
					Flash.error("Le dernier for fiscal principal de cette entreprise est fermé.");
					return "redirect:list.do";
				}

				return showStart(model, new AnnulationDemenagementSiegeView(idEntreprise, dernierFor.getDateDebut()));
			}
		});
	}

	private String showStart(final Model model, final AnnulationDemenagementSiegeView view) {
		model.addAttribute(SearchTiersComponent.COMMAND, view);
		return doInReadOnlyTransaction(new TransactionCallback<String>() {
			@Override
			public String doInTransaction(TransactionStatus status) {
				final Entreprise entreprise = getTiers(Entreprise.class, view.getIdEntreprise());
				if (entreprise.isConnueAuCivil()) {
					Flash.error("Cette opération n'est pas disponible pour les entreprises avec un lien vers RCEnt.");
					return "redirect:list.do";
				}

				final ForFiscalPrincipalPM forActuel = entreprise.getDernierForFiscalPrincipal();
				if (forActuel == null || forActuel.getDateFin() != null) {
					Flash.error("Le dernier for fiscal principal de cette entreprise est fermé.");
					return "redirect:list.do";
				}

				final Set<MotifFor> motifsDemenagement = EnumSet.of(MotifFor.DEMENAGEMENT_VD, MotifFor.ARRIVEE_HC, MotifFor.ARRIVEE_HS, MotifFor.DEPART_HC, MotifFor.DEPART_HS);
				if (!motifsDemenagement.contains(forActuel.getMotifOuverture())) {
					Flash.error("Le for principal actif de cette entreprise n'a pas été ouvert pour un motif correspondant à un déménagement de siège.");
					return "redirect:list.do";
				}

				model.addAttribute(DEBUT_SIEGE_ACTUEL, forActuel.getDateDebut());
				model.addAttribute(TYPE_AUTORITE_SIEGE_ACTUEL, forActuel.getTypeAutoriteFiscale());
				model.addAttribute(OFS_SIEGE_ACTUEL, forActuel.getNumeroOfsAutoriteFiscale());

				final ForFiscalPrincipalPM forPrecedent = entreprise.getDernierForFiscalPrincipalAvant(forActuel.getDateDebut().getOneDayBefore());
				if (forPrecedent != null) {
					model.addAttribute(DEBUT_SIEGE_PRECEDENT, forPrecedent.getDateDebut());
					model.addAttribute(TYPE_AUTORITE_SIEGE_PRECEDENT, forPrecedent.getTypeAutoriteFiscale());
					model.addAttribute(OFS_SIEGE_PRECEDENT, forPrecedent.getNumeroOfsAutoriteFiscale());
				}
				else {
					Flash.error("Cette entreprise ne possède qu'un seul for principal non-annulé (pas de déménagement de siège à annuler).");
					return "redirect:list.do";
				}

				return "entreprise/annulation-demenagement/start";
			}
		});
	}

	@RequestMapping(value = "/start.do", method = RequestMethod.POST)
	public String doDemenagement(Model model, @Valid @ModelAttribute(value = SearchTiersComponent.COMMAND) final AnnulationDemenagementSiegeView view, BindingResult bindingResult) throws Exception {
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
				if (entreprise.isConnueAuCivil()) {
					throw new MetierServiceException("Cette opération n'est pas disponible pour les entreprises avec un lien vers RCEnt.");
				}
				metierService.annuleDemenagement(entreprise, view.getDateDebutSiegeActuel());
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
					Flash.warning("Cette entreprise est actuellement sous l'influence d'une décision ACI. Veuillez en vérifier la pertinence après cette opération.");
				}
			}
		});

		return "redirect:/tiers/visu.do?id=" + view.getIdEntreprise();
	}
}
