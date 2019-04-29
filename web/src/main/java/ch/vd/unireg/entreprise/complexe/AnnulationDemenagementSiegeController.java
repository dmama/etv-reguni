package ch.vd.unireg.entreprise.complexe;

import javax.validation.Valid;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Controller;
import org.springframework.transaction.TransactionStatus;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.unireg.common.CollectionsUtils;
import ch.vd.unireg.common.Flash;
import ch.vd.unireg.metier.MetierServiceException;
import ch.vd.unireg.security.AccessDeniedException;
import ch.vd.unireg.security.Role;
import ch.vd.unireg.tiers.DomicileHisto;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.ForFiscalPrincipalPM;
import ch.vd.unireg.tiers.TiersCriteria;
import ch.vd.unireg.tiers.view.TiersCriteriaView;
import ch.vd.unireg.transaction.TransactionHelper;
import ch.vd.unireg.type.MotifFor;

@Controller
@RequestMapping("/processuscomplexe/annulation/demenagement")
public class AnnulationDemenagementSiegeController extends AbstractProcessusComplexeRechercheController {

	public static final String CRITERIA_NAME = "AnnulationDemenagementSiegeCriteria";

	private static final String CONNUE_CIVILEMENT = "entrepriseConnueAuRegistreCivil";

	private static final String DEBUT_SIEGE_ACTUEL = "dateDebutSiegeActuel";
	private static final String TYPE_AUTORITE_SIEGE_ACTUEL = "typeAutoriteFiscaleSiegeActuel";
	private static final String OFS_SIEGE_ACTUEL = "noOfsSiegeActuel";

	private static final String DEBUT_SIEGE_PRECEDENT = "dateDebutSiegePrecedent";
	private static final String TYPE_AUTORITE_SIEGE_PRECEDENT = "typeAutoriteFiscaleSiegePrecedent";
	private static final String OFS_SIEGE_PRECEDENT = "noOfsSiegePrecedent";

	private static final String DEBUT_FOR_PRINCIPAL_ACTUEL = "dateDebutForPrincipalActuel";
	private static final String OFS_FOR_PRINCIPAL_ACTUEL = "noOfsForPrincipalActuel";
	private static final String TYPE_AUTORITE_FOR_PRINCIPAL_ACTUEL = "typeAutoriteFiscaleForPrincipalActuel";

	private static final String DEBUT_FOR_PRINCIPAL_PRECEDENT = "dateDebutForPrincipalPrecedent";
	private static final String TYPE_AUTORITE_FOR_PRINCIPAL_PRECEDENT = "typeAutoriteFiscaleForPrincipalPrecedent";
	private static final String OFS_FOR_PRINCIPAL_PRECEDENT = "noOfsForPrincipalPrecedent";

	@Override
	protected void checkDroitAcces() throws AccessDeniedException {
		checkAnyGranted("Vous ne possédez pas les droits d'accès au processus complexe d'annulation de déménagement de siège d'entreprise.",
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
		return "entreprise/annulation-demenagement/list";
	}

	@RequestMapping(value = "/start.do", method = RequestMethod.GET)
	public String showStart(final Model model, @RequestParam("id") final long idEntreprise) {
		checkDroitAcces();
		controllerUtils.checkAccesDossierEnEcriture(idEntreprise);

		return doInReadOnlyTransaction(status -> {
			final Entreprise entreprise = getTiers(Entreprise.class, idEntreprise);
			final ForFiscalPrincipalPM dernierFor = entreprise.getDernierForFiscalPrincipal();
			if (dernierFor == null || dernierFor.getDateFin() != null) {
				Flash.error("Le dernier for fiscal principal de cette entreprise est fermé.");
				return "redirect:list.do";
			}
			;
			return showStart(model, new AnnulationDemenagementSiegeView(idEntreprise, dernierFor.getDateDebut()));
		});
	}

	private String showStart(final Model model, final AnnulationDemenagementSiegeView view) {
		model.addAttribute(ACTION_COMMAND, view);
		return doInReadOnlyTransaction(status -> {
			final Entreprise entreprise = getTiers(Entreprise.class, view.getIdEntreprise());
			model.addAttribute(CONNUE_CIVILEMENT, entreprise.isConnueAuCivil());

			// données de for fiscal principal

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

			model.addAttribute(DEBUT_FOR_PRINCIPAL_ACTUEL, forActuel.getDateDebut());
			model.addAttribute(TYPE_AUTORITE_FOR_PRINCIPAL_ACTUEL, forActuel.getTypeAutoriteFiscale());
			model.addAttribute(OFS_FOR_PRINCIPAL_ACTUEL, forActuel.getNumeroOfsAutoriteFiscale());

			final ForFiscalPrincipalPM forPrecedent = entreprise.getDernierForFiscalPrincipalAvant(forActuel.getDateDebut().getOneDayBefore());
			if (forPrecedent != null) {
				model.addAttribute(DEBUT_FOR_PRINCIPAL_PRECEDENT, forPrecedent.getDateDebut());
				model.addAttribute(TYPE_AUTORITE_FOR_PRINCIPAL_PRECEDENT, forPrecedent.getTypeAutoriteFiscale());
				model.addAttribute(OFS_FOR_PRINCIPAL_PRECEDENT, forPrecedent.getNumeroOfsAutoriteFiscale());
			}
			else {
				Flash.error("Cette entreprise ne possède qu'un seul for principal non-annulé (pas de déménagement de siège à annuler).");
				return "redirect:list.do";
			}

			// données de siege

			final List<DomicileHisto> sieges = tiersService.getSieges(entreprise, false);
			if (sieges == null || sieges.isEmpty()) {
				Flash.error("Cette entreprise ne possède aucun siège civil.");
				return "redirect:list.do";
			}
			final DomicileHisto dernierSiege = CollectionsUtils.getLastElement(sieges);
			if (dernierSiege.getDateFin() != null) {
				Flash.error("Le dernier siège de cette entreprise est fermé.");
				return "redirect:list.do";
			}

			model.addAttribute(DEBUT_SIEGE_ACTUEL, dernierSiege.getDateDebut());
			model.addAttribute(TYPE_AUTORITE_SIEGE_ACTUEL, dernierSiege.getTypeAutoriteFiscale());
			model.addAttribute(OFS_SIEGE_ACTUEL, dernierSiege.getNumeroOfsAutoriteFiscale());

			final DomicileHisto siegePrecedent = DateRangeHelper.rangeAt(sieges, dernierSiege.getDateDebut().getOneDayBefore());
			if (siegePrecedent != null) {
				model.addAttribute(DEBUT_SIEGE_PRECEDENT, siegePrecedent.getDateDebut());
				model.addAttribute(TYPE_AUTORITE_SIEGE_PRECEDENT, siegePrecedent.getTypeAutoriteFiscale());
				model.addAttribute(OFS_SIEGE_PRECEDENT, siegePrecedent.getNumeroOfsAutoriteFiscale());
			}
			else if (!entreprise.isConnueAuCivil()) {
				// n'avoir qu'un seul siège n'est une erreur que si les sièges sont fiscaux
				// (pour être cohérent avec le processus complexe de déménagement d'une entreprise connue au civil)
				Flash.error("Cette entreprise ne possède qu'un seul siège (pas de déménagement de siège à annuler).");
				return "redirect:list.do";
			}
			return "entreprise/annulation-demenagement/start";
		});
	}

	@RequestMapping(value = "/start.do", method = RequestMethod.POST)
	public String doDemenagement(Model model, @Valid @ModelAttribute(value = ACTION_COMMAND) final AnnulationDemenagementSiegeView view, BindingResult bindingResult) throws Exception {
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
				metierService.annuleDemenagement(entreprise, view.getDateDebutSiegeActuel());
			}
		});

		// tout s'est bien passé... mais si le contribuable est sous le coup d'une décision ACI
		// (et que l'utilisateur courant a le droit de modification, ce qui est le cas si nous sommes
		// arrivés jusqu'ici), il faut lui laisser un petit mot...
		doInReadOnlyTransaction(status -> {
			final Entreprise entreprise = getTiers(Entreprise.class, view.getIdEntreprise());
			if (tiersService.isSousInfluenceDecisions(entreprise)) {
				Flash.warning("Cette entreprise est actuellement sous l'influence d'une décision ACI. Veuillez en vérifier la pertinence après cette opération.");
			}
			return null;
		});

		return "redirect:/tiers/visu.do?id=" + view.getIdEntreprise();
	}
}
