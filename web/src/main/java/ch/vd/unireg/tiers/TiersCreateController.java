package ch.vd.unireg.tiers;

import javax.validation.Valid;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.DelegatingValidator;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.common.LiteralStringHelper;
import ch.vd.unireg.common.NumeroIDEHelper;
import ch.vd.unireg.complements.ComplementsEditCommunicationsView;
import ch.vd.unireg.complements.ComplementsEditCoordonneesFinancieresView;
import ch.vd.unireg.declaration.Periodicite;
import ch.vd.unireg.iban.IbanHelper;
import ch.vd.unireg.iban.IbanValidator;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.data.TypeRegimeFiscal;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.metier.AjustementForsSecondairesResult;
import ch.vd.unireg.metier.MetierServicePM;
import ch.vd.unireg.regimefiscal.RegimeFiscalService;
import ch.vd.unireg.security.AccessDeniedException;
import ch.vd.unireg.security.Role;
import ch.vd.unireg.security.SecurityHelper;
import ch.vd.unireg.security.SecurityProviderInterface;
import ch.vd.unireg.tiers.validator.CreateAutreCommunauteViewValidator;
import ch.vd.unireg.tiers.validator.CreateDebiteurViewValidator;
import ch.vd.unireg.tiers.validator.CreateEntrepriseViewValidator;
import ch.vd.unireg.tiers.validator.CreateEtablissementViewValidator;
import ch.vd.unireg.tiers.validator.CreateNonHabitantViewValidator;
import ch.vd.unireg.tiers.view.AutreCommunauteCivilView;
import ch.vd.unireg.tiers.view.CreateAutreCommunauteView;
import ch.vd.unireg.tiers.view.CreateDebiteurView;
import ch.vd.unireg.tiers.view.CreateEntrepriseView;
import ch.vd.unireg.tiers.view.CreateEtablissementView;
import ch.vd.unireg.tiers.view.CreateNonHabitantView;
import ch.vd.unireg.tiers.view.DebiteurFiscalView;
import ch.vd.unireg.tiers.view.EntrepriseCivilView;
import ch.vd.unireg.tiers.view.EtablissementCivilView;
import ch.vd.unireg.tiers.view.IdentificationPersonneView;
import ch.vd.unireg.tiers.view.NonHabitantCivilView;
import ch.vd.unireg.type.CategorieEntreprise;
import ch.vd.unireg.type.DayMonth;
import ch.vd.unireg.type.FormeJuridiqueEntreprise;
import ch.vd.unireg.type.GenreImpot;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.PeriodeDecompte;
import ch.vd.unireg.type.PeriodiciteDecompte;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.type.TypeEtatEntreprise;
import ch.vd.unireg.type.TypeGenerationEtatEntreprise;
import ch.vd.unireg.utils.RegDateEditor;

@Controller
@RequestMapping(value = "/tiers")
public class TiersCreateController {

	private static final String DATA = "data";
	private static final String SEXE_MAP_NAME = "sexes";
	private static final String CATEGORIE_ETRANGER_MAP_NAME = "categoriesEtrangers";
	private static final String FORME_JURIDIQUE_MAP_NAME = "formesJuridiques";
	private static final String FORME_JURIDIQUE_ENTREPRISE_MAP_NAME = "formesJuridiquesEntreprise";
	private static final String CTB_ASSOCIE = "ctbAssocie";
	private static final String CATEGORIE_IMPOT_SOURCE_MAP_NAME = "categoriesImpotSource";
	private static final String MODE_COMMUNICATION_MAP_NAME = "modesCommunication";
	private static final String PERIODE_DECOMPTE_MAP_NAME = "periodesDecompte";
	private static final String PERIODICITE_DECOMPTE_MAP_NAME = "periodicitesDecompte";

	private SecurityProviderInterface securityProvider;
	private TiersMapHelper tiersMapHelper;
	private TiersDAO tiersDAO;
	private TiersService tiersService;
	private IbanValidator ibanValidator;
	private ServiceInfrastructureService infraService;
	private MetierServicePM metierServicePM;
	private RegimeFiscalService regimeFiscalService;

	public void setSecurityProvider(SecurityProviderInterface securityProvider) {
		this.securityProvider = securityProvider;
	}

	public void setTiersMapHelper(TiersMapHelper tiersMapHelper) {
		this.tiersMapHelper = tiersMapHelper;
	}

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setIbanValidator(IbanValidator ibanValidator) {
		this.ibanValidator = ibanValidator;
	}

	public void setInfraService(ServiceInfrastructureService infraService) {
		this.infraService = infraService;
	}

	public void setMetierServicePM(MetierServicePM metierServicePM) {
		this.metierServicePM = metierServicePM;
	}

	public void setRegimeFiscalService(RegimeFiscalService regimeFiscalService) {
		this.regimeFiscalService = regimeFiscalService;
	}

	@InitBinder
	public void initBinder(WebDataBinder binder) {
		binder.setValidator(new TiersCreationValidator(ibanValidator));
		binder.registerCustomEditor(RegDate.class, new RegDateEditor(true, false, false));
		binder.registerCustomEditor(RegDate.class, "civil.dateNaissance", new RegDateEditor(true, true, false));      // la date de naissance doit supporter les dates partielles
	}

	private static class TiersCreationValidator extends DelegatingValidator {
		private TiersCreationValidator(IbanValidator ibanValidator) {
			addSubValidator(CreateNonHabitantView.class, new CreateNonHabitantViewValidator(ibanValidator));
			addSubValidator(CreateAutreCommunauteView.class, new CreateAutreCommunauteViewValidator(ibanValidator));
			addSubValidator(CreateEntrepriseView.class, new CreateEntrepriseViewValidator(ibanValidator));
			addSubValidator(CreateDebiteurView.class, new CreateDebiteurViewValidator(ibanValidator));
			addSubValidator(CreateEtablissementView.class, new CreateEtablissementViewValidator(ibanValidator));
		}
	}

	@RequestMapping(value = "/nonhabitant/create.do", method = RequestMethod.GET)
	public String createNonHabitant(Model model) {
		if (!SecurityHelper.isGranted(securityProvider, Role.CREATE_NONHAB)) {
			throw new AccessDeniedException("Vous ne possédez pas les droits d'accès suffisants à la création d'un inconnu au contrôle des habitants.");
		}
		return showCreateNonHabitant(model, new CreateNonHabitantView());
	}

	private String showCreateNonHabitant(Model model, CreateNonHabitantView view) {
		model.addAttribute(DATA, view);
		model.addAttribute(SEXE_MAP_NAME, tiersMapHelper.getMapSexe());
		model.addAttribute(CATEGORIE_ETRANGER_MAP_NAME, tiersMapHelper.getMapCategorieEtranger());
		return "tiers/edition/creation-non-habitant";
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/nonhabitant/create.do", method = RequestMethod.POST)
	public String doCreateNonHabitant(Model model, @Valid @ModelAttribute(DATA) CreateNonHabitantView view, BindingResult result) {
		if (!SecurityHelper.isGranted(securityProvider, Role.CREATE_NONHAB)) {
			throw new AccessDeniedException("Vous ne possédez pas les droits d'accès suffisants à la création d'un inconnu au contrôle des habitants.");
		}
		if (result.hasErrors()) {
			return showCreateNonHabitant(model, view);
		}

		final PersonnePhysique pp = new PersonnePhysique(false);

		final NonHabitantCivilView civilView = view.getCivil();
		pp.setNom(civilView.getNom());
		pp.setNomNaissance(civilView.getNomNaissance());
		pp.setPrenomUsuel(civilView.getPrenomUsuel());
		pp.setTousPrenoms(civilView.getTousPrenoms());
		pp.setNumeroAssureSocial(FormatNumeroHelper.removeSpaceAndDash(civilView.getNumeroAssureSocial()));
		pp.setSexe(civilView.getSexe());
		pp.setDateNaissance(civilView.getDateNaissance());
		pp.setDateDeces(civilView.getDateDeces());
		pp.setCategorieEtranger(civilView.getCategorieEtranger());
		pp.setDateDebutValiditeAutorisation(civilView.getDateDebutValiditeAutorisation());
		pp.setNumeroOfsNationalite(civilView.getNumeroOfsNationalite());
		if (civilView.getOfsCommuneOrigine() != null) {
			final Commune commune = infraService.getCommuneByNumeroOfs(civilView.getOfsCommuneOrigine(), null);
			pp.setOrigine(new OriginePersonnePhysique(civilView.getNewLibelleCommuneOrigine(), commune.getSigleCanton()));
		}
		else {
			pp.setOrigine(null);
		}
		pp.setPrenomsPere(civilView.getPrenomsPere());
		pp.setNomPere(civilView.getNomPere());
		pp.setPrenomsMere(civilView.getPrenomsMere());
		pp.setNomMere(civilView.getNomMere());

		final IdentificationPersonneView ipView = civilView.getIdentificationPersonne();
		tiersService.setIdentifiantsPersonne(pp, ipView.getAncienNumAVS(), ipView.getNumRegistreEtranger());

		setComplementCommunication(pp, view.getComplementCommunication());
		setComplementCoordFinanciere(pp, view.getComplementCoordFinanciere());

		final PersonnePhysique saved = (PersonnePhysique) tiersDAO.save(pp);
		return "redirect:/tiers/visu.do?id=" + saved.getNumero();
	}

	@RequestMapping(value = "/entreprise/create.do", method = RequestMethod.GET)
	public String createEntreprise(Model model) {
		if (!SecurityHelper.isGranted(securityProvider, Role.CREATE_ENTREPRISE)) {
			throw new AccessDeniedException("Vous ne possédez pas les droits d'accès suffisants à la création d'une entreprise.");
		}
		return showCreateEntreprise(model, new CreateEntrepriseView());
	}

	private String showCreateEntreprise(Model model, CreateEntrepriseView view) {
		model.addAttribute(DATA, view);

		final Set<FormeJuridiqueEntreprise> all = EnumSet.noneOf(FormeJuridiqueEntreprise.class);
		all.addAll(ALLOWED_EVERYWHERE);
		all.addAll(ONLY_HC_HS_ALLOWED);
		model.addAttribute(FORME_JURIDIQUE_ENTREPRISE_MAP_NAME, tiersMapHelper.getMapFormesJuridiquesEntrepriseChoisies(all));

		return "tiers/edition/creation-entreprise";
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/entreprise/create.do", method = RequestMethod.POST)
	public String doCreateEntreprise(Model model, @Valid @ModelAttribute(DATA) CreateEntrepriseView view, BindingResult result) throws TiersException {
		if (!SecurityHelper.isGranted(securityProvider, Role.CREATE_ENTREPRISE)) {
			throw new AccessDeniedException("Vous ne possédez pas les droits d'accès suffisants à la création d'une entreprise.");
		}
		if (result.hasErrors()) {
			return showCreateEntreprise(model, view);
		}

		final Entreprise entreprise = (Entreprise) tiersDAO.save(new Entreprise());
		final EntrepriseCivilView civilView = view.getCivil();

		// Numéro IDE
		final String normalizedIde = NumeroIDEHelper.normalize(civilView.getNumeroIde());
		if (StringUtils.isNotBlank(normalizedIde)) {
			final IdentificationEntreprise ident = new IdentificationEntreprise();
			ident.setNumeroIde(normalizedIde);
			entreprise.addIdentificationEntreprise(ident);
		}

		// Compléments
		setComplementCommunication(entreprise, view.getComplementCommunication());
		setComplementCoordFinanciere(entreprise, view.getComplementCoordFinanciere());

		final RegDate dateOuverture = civilView.getDateOuverture();
		final RegDate dateDebutExerciceCommercial = civilView.getTypeDateDebutExerciceCommercial() == EntrepriseCivilView.TypeDefautDate.DEFAULT
				? RegDate.get(dateOuverture.year(), 1, 1)
				: civilView.getDateDebutExerciceCommercial();
		final RegDate dateFondation = civilView.getTypeDateFondation() == EntrepriseCivilView.TypeDefautDate.DEFAULT
				? dateOuverture
				: civilView.getDateFondation();

		// Forme juridique
		final FormeJuridiqueEntreprise formeJuridique = civilView.getFormeJuridique();
		tiersService.addFormeJuridiqueFiscale(entreprise, formeJuridique, dateOuverture, null);

		// Capital
		if (civilView.getCapitalLibere() != null) {
			tiersService.addCapitalFiscal(entreprise, civilView.getCapitalLibere(), civilView.getDevise(), dateOuverture, null);
		}

		// Raison sociale
		tiersService.addRaisonSocialeFiscale(entreprise, LiteralStringHelper.stripExtraSpacesAndBlanks(civilView.getRaisonSociale()), dateOuverture, null);

		// Siège (établissement)
		final Etablissement etablissementPrincipal;
		{
			final Etablissement etablissement = new Etablissement();
			etablissement.setRaisonSociale(LiteralStringHelper.stripExtraSpacesAndBlanks(civilView.getRaisonSociale()));
			etablissementPrincipal = (Etablissement) tiersDAO.save(etablissement);
		}
		tiersService.addDomicileEtablissement(etablissementPrincipal, civilView.getTypeAutoriteFiscale(), civilView.getNumeroOfsSiege(), dateOuverture, null);

		// Rapport entre entreprise et établissement principal
		tiersService.addActiviteEconomique(etablissementPrincipal, entreprise, dateFondation, true);

		// Régimes fiscaux + For principal (différents en fonction de la forme juridique/catégorie entreprise)
		final TypeRegimeFiscal typeRegimeFiscalParDefaut = regimeFiscalService.getTypeRegimeFiscalParDefaut(formeJuridique);

		// Calcul de la date d'ouverture fiscale
		final boolean vd = civilView.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD;
		final RegDate dateOuvertureFiscale = vd && civilView.isInscriteRC() ? dateOuverture.getOneDayAfter() : dateOuverture; // SIFISC-25054 - Appliquer la règle jour + 1 pour les entités inscrites au RC VD, pas pour les autres. (supplante SIFISC-22478)

		// Ajout des régimes fiscaux
		tiersService.addRegimeFiscal(entreprise, RegimeFiscal.Portee.CH, typeRegimeFiscalParDefaut, dateOuvertureFiscale, null);
		tiersService.addRegimeFiscal(entreprise, RegimeFiscal.Portee.VD, typeRegimeFiscalParDefaut, dateOuvertureFiscale, null);

		final CategorieEntreprise categorieEntreprise = typeRegimeFiscalParDefaut.getCategorie();

		final Set<CategorieEntreprise> isPMOrIndet = EnumSet.of(CategorieEntreprise.PM, CategorieEntreprise.APM, CategorieEntreprise.INDET);
		if (isPMOrIndet.contains(categorieEntreprise)) {
			// Bouclement et premier exercice commercial
			entreprise.setDateDebutPremierExerciceCommercial(dateDebutExerciceCommercial);
			final RegDate dateBouclement = dateDebutExerciceCommercial.addYears(1).getOneDayBefore();
			final Bouclement bouclement = new Bouclement();
			bouclement.setAncrage(DayMonth.get(dateBouclement));
			bouclement.setDateDebut(dateBouclement.addDays(- dateBouclement.day() + 1));        // = le premier jour du mois du bouclement
			bouclement.setPeriodeMois(12);
			entreprise.addBouclement(bouclement);

			// For principal
			final MotifFor motifOuverture = vd ? MotifFor.DEBUT_EXPLOITATION : null;
			tiersService.addForPrincipal(entreprise, dateOuvertureFiscale, motifOuverture, null, null, MotifRattachement.DOMICILE, civilView.getNumeroOfsSiege(), civilView.getTypeAutoriteFiscale(), GenreImpot.BENEFICE_CAPITAL);
		}
		else if (categorieEntreprise == CategorieEntreprise.SP) {
			// Pas de régime fiscal (et donc pas de bouclement)
			// For principal
			tiersService.addForPrincipal(entreprise, dateOuverture, null, null, null, MotifRattachement.DOMICILE, civilView.getNumeroOfsSiege(), civilView.getTypeAutoriteFiscale(), GenreImpot.REVENU_FORTUNE);
		}

		// Etat fiscal
		final TypeEtatEntreprise typeEtatEntreprise = civilView.isInscriteRC() ? TypeEtatEntreprise.INSCRITE_RC : TypeEtatEntreprise.FONDEE;
		tiersService.changeEtatEntreprise(typeEtatEntreprise, entreprise, dateFondation, TypeGenerationEtatEntreprise.MANUELLE);

		return "redirect:/tiers/visu.do?id=" + entreprise.getNumero();
	}

	private static final Set<FormeJuridiqueEntreprise> ALLOWED_EVERYWHERE = EnumSet.of(
//			FormeJuridiqueEntreprise.EI,            // autorisées dans SITI, apparemment, mais sans catégorie d'entreprise intéressante pour nous (PP) -> éliminée pour le moment
			FormeJuridiqueEntreprise.ASSOCIATION,
			FormeJuridiqueEntreprise.FONDATION,
			FormeJuridiqueEntreprise.ADM_CH,
			FormeJuridiqueEntreprise.ADM_CO,
			FormeJuridiqueEntreprise.ADM_CT,
			FormeJuridiqueEntreprise.ADM_DI,
			FormeJuridiqueEntreprise.CORP_DP_ADM,
//			FormeJuridiqueEntreprise.ENT_CH,        // autorisées dans SITI, apparemment, mais sans catégorie d'entreprise intéressante pour nous (AUTRE) -> éliminée pour le moment
//			FormeJuridiqueEntreprise.ENT_CO,        // autorisées dans SITI, apparemment, mais sans catégorie d'entreprise intéressante pour nous (AUTRE) -> éliminée pour le moment
//			FormeJuridiqueEntreprise.ENT_CT,        // autorisées dans SITI, apparemment, mais sans catégorie d'entreprise intéressante pour nous (AUTRE) -> éliminée pour le moment
//			FormeJuridiqueEntreprise.ENT_DI,        // autorisées dans SITI, apparemment, mais sans catégorie d'entreprise intéressante pour nous (AUTRE) -> éliminée pour le moment
			FormeJuridiqueEntreprise.CORP_DP_ENT);
//			FormeJuridiqueEntreprise.SS,            // autorisées dans SITI, apparemment, mais sans catégorie d'entreprise intéressante pour nous (AUTRE) -> éliminée pour le moment
//			FormeJuridiqueEntreprise.FILIALE_HS_NIRC);  // autorisées dans SITI, apparemment, mais sans catégorie d'entreprise intéressante pour nous (AUTRE) -> éliminée pour le moment

	private static final Set<FormeJuridiqueEntreprise> ONLY_HC_HS_ALLOWED = EnumSet.of(
			FormeJuridiqueEntreprise.SNC,
			FormeJuridiqueEntreprise.SC,
			FormeJuridiqueEntreprise.SCA,
			FormeJuridiqueEntreprise.SA,
			FormeJuridiqueEntreprise.SARL,
			FormeJuridiqueEntreprise.SCOOP,
//			FormeJuridiqueEntreprise.FILIALE_HS_RC, // autorisées dans SITI, apparemment, mais sans catégorie d'entreprise intéressante pour nous (AUTRE) -> éliminée pour le moment
//			FormeJuridiqueEntreprise.PARTICULIER,   // autorisées dans SITI, apparemment, mais sans catégorie d'entreprise intéressante pour nous (AUTRE) -> éliminée pour le moment
			FormeJuridiqueEntreprise.SCPC,
			FormeJuridiqueEntreprise.SICAV,
			FormeJuridiqueEntreprise.SICAF,
			FormeJuridiqueEntreprise.IDP);
//			FormeJuridiqueEntreprise.PNC,           // autorisées dans SITI, apparemment, mais sans catégorie d'entreprise intéressante pour nous (AUTRE) -> éliminée pour le moment
//			FormeJuridiqueEntreprise.INDIVISION,    // autorisées dans SITI, apparemment, mais sans catégorie d'entreprise intéressante pour nous (AUTRE) -> éliminée pour le moment
//			FormeJuridiqueEntreprise.FILIALE_CH_RC, // autorisées dans SITI, apparemment, mais sans catégorie d'entreprise intéressante pour nous (AUTRE) -> éliminée pour le moment
//			FormeJuridiqueEntreprise.ENT_PUBLIQUE_HS,
//			FormeJuridiqueEntreprise.ADM_PUBLIQUE_HS,
//			FormeJuridiqueEntreprise.ORG_INTERNAT,
//			FormeJuridiqueEntreprise.ENT_HS);

	@ResponseBody
	@RequestMapping(value = "/entreprise/types-autorite-fiscale.do", method = RequestMethod.GET)
	public Map<TypeAutoriteFiscale, String> getTypesAutoriteFiscaleAutorises(@RequestParam("fj") FormeJuridiqueEntreprise fj) {
		if (ALLOWED_EVERYWHERE.contains(fj)) {
			return tiersMapHelper.getMapTypeAutoriteFiscale();
		}
		else if (ONLY_HC_HS_ALLOWED.contains(fj)) {
			return tiersMapHelper.getMapTypeAutoriteFiscaleEntreprise();
		}
		else {
			return Collections.emptyMap();
		}
	}

	@RequestMapping(value = "/autrecommunaute/create.do", method = RequestMethod.GET)
	public String createAutreCommunaute(Model model) {
		if (!SecurityHelper.isGranted(securityProvider, Role.CREATE_AC)) {
			throw new AccessDeniedException("Vous ne possédez pas les droits d'accès suffisants à la création d'un inconnu PM.");
		}
		return showCreateAutreCommunaute(model, new CreateAutreCommunauteView());
	}

	private String showCreateAutreCommunaute(Model model, CreateAutreCommunauteView view) {
		model.addAttribute(DATA, view);
		model.addAttribute(FORME_JURIDIQUE_MAP_NAME, tiersMapHelper.getMapFormeJuridique());
		return "tiers/edition/creation-autre-communaute";
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/autrecommunaute/create.do", method = RequestMethod.POST)
	public String doCreateAutreCommunaute(Model model, @Valid @ModelAttribute(DATA) CreateAutreCommunauteView view, BindingResult results) throws TiersException {
		if (!SecurityHelper.isGranted(securityProvider, Role.CREATE_AC)) {
			throw new AccessDeniedException("Vous ne possédez pas les droits d'accès suffisants à la création d'un inconnu PM.");
		}
		if (results.hasErrors()) {
			return showCreateAutreCommunaute(model, view);
		}

		final AutreCommunaute ac = new AutreCommunaute();

		final AutreCommunauteCivilView civilView = view.getCivil();
		ac.setFormeJuridique(civilView.getFormeJuridique());
		ac.setNom(civilView.getNom());
		tiersService.setIdentifiantEntreprise(ac, StringUtils.trimToNull(civilView.getIde()));

		setComplementCommunication(ac, view.getComplementCommunication());
		setComplementCoordFinanciere(ac, view.getComplementCoordFinanciere());

		final AutreCommunaute saved = (AutreCommunaute) tiersDAO.save(ac);
		return "redirect:/tiers/visu.do?id=" + saved.getNumero();
	}

	@RequestMapping(value = "/debiteur/create.do", method = RequestMethod.GET)
	public String createDebiteurPrestationImposable(Model model, @RequestParam(value = "numeroCtbAss") long noCtbAssocie) {
		if (!SecurityHelper.isGranted(securityProvider, Role.CREATE_DPI)) {
			throw new AccessDeniedException("Vous ne possédez pas les droits d'accès suffisants à la création d'un débiteur de prestation imposable.");
		}
		return showCreateDebiteurPrestationImposable(model, noCtbAssocie, new CreateDebiteurView());
	}

	private String showCreateDebiteurPrestationImposable(Model model, long noCtbAssocie, CreateDebiteurView view) {
		model.addAttribute(DATA, view);
		model.addAttribute(CTB_ASSOCIE, noCtbAssocie);
		model.addAttribute(MODE_COMMUNICATION_MAP_NAME, tiersMapHelper.getMapModeCommunication());
		model.addAttribute(CATEGORIE_IMPOT_SOURCE_MAP_NAME, tiersMapHelper.getMapCategorieImpotSource());
		model.addAttribute(PERIODICITE_DECOMPTE_MAP_NAME, tiersMapHelper.getMapPeriodiciteDecompte());
		model.addAttribute(PERIODE_DECOMPTE_MAP_NAME, tiersMapHelper.getPeriodeDecomptes());
		return "tiers/edition/creation-debiteur";
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/debiteur/create.do", method = RequestMethod.POST)
	public String doCreateDebiteurPrestationImposable(Model model, @RequestParam(value = "numeroCtbAss") long noCtbAssocie, @Valid @ModelAttribute(DATA) CreateDebiteurView view, BindingResult results) {
		if (!SecurityHelper.isGranted(securityProvider, Role.CREATE_DPI)) {
			throw new AccessDeniedException("Vous ne possédez pas les droits d'accès suffisants à la création d'un débiteur de prestation imposable.");
		}
		if (results.hasErrors()) {
			return showCreateDebiteurPrestationImposable(model, noCtbAssocie, view);
		}

		final DebiteurPrestationImposable dpi = new DebiteurPrestationImposable();

		final DebiteurFiscalView fiscalView = view.getFiscal();
		dpi.setCategorieImpotSource(fiscalView.getCategorieImpotSource());
		dpi.setModeCommunication(fiscalView.getModeCommunication());

		final PeriodiciteDecompte periodicite = fiscalView.getPeriodiciteDecompte();
		final PeriodeDecompte periode = periodicite == PeriodiciteDecompte.UNIQUE ? fiscalView.getPeriodeDecompte() : null;
		dpi.setPeriodicites(new HashSet<>(Collections.singletonList(new Periodicite(periodicite, periode, RegDate.get(RegDate.get().year(), 1, 1), null))));

		setComplementCommunication(dpi, view.getComplementCommunication());
		setComplementCoordFinanciere(dpi, view.getComplementCoordFinanciere());

		final DebiteurPrestationImposable saved = (DebiteurPrestationImposable) tiersDAO.save(dpi);
		final Contribuable ctbAss = (Contribuable) tiersDAO.get(noCtbAssocie);
		tiersService.addContactImpotSource(saved, ctbAss);

		return "redirect:/tiers/visu.do?id=" + saved.getNumero();
	}

	@Transactional(readOnly = true)
	@RequestMapping(value = "/etablissement/create.do", method = RequestMethod.GET)
	public String createEtablissement(Model model, @RequestParam(value = "numeroCtbAss") long noCtbAssocie) {
		if (!SecurityHelper.isGranted(securityProvider, Role.ETABLISSEMENTS)) {
			throw new AccessDeniedException("Vous ne possédez pas les droits d'accès suffisants à la création d'un établissement.");
		}

		Entreprise entreprise = (Entreprise) tiersDAO.get(noCtbAssocie);
		// On récupère la dernière raison sociale de l'entreprise pour la raison sociale par défaut de l'établissement
		String raisonSociale = tiersService.getDerniereRaisonSociale(entreprise);

		return showCreateEtablissement(model, noCtbAssocie, new CreateEtablissementView(raisonSociale));
	}

	private String showCreateEtablissement(Model model, long noCtbAssocie, CreateEtablissementView view) {
		model.addAttribute(DATA, view);
		model.addAttribute(CTB_ASSOCIE, noCtbAssocie);
		return "tiers/edition/creation-etablissement";
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/etablissement/create.do", method = RequestMethod.POST)
	public String doCreateEtablissement(Model model, @RequestParam(value = "numeroCtbAss") long noCtbAssocie, @Valid @ModelAttribute(DATA) CreateEtablissementView view, BindingResult results) throws Exception {
		if (!SecurityHelper.isGranted(securityProvider, Role.ETABLISSEMENTS)) {
			throw new AccessDeniedException("Vous ne possédez pas les droits d'accès suffisants à la création d'un établissement.");
		}
		if (results.hasErrors()) {
			return showCreateEtablissement(model, noCtbAssocie, view);
		}

		final Etablissement etablissement = new Etablissement();

		final EtablissementCivilView civilView = view.getCivil();
		etablissement.setRaisonSociale(LiteralStringHelper.stripExtraSpacesAndBlanks(civilView.getRaisonSociale()));
		etablissement.setEnseigne(LiteralStringHelper.stripExtraSpacesAndBlanks(civilView.getNomEnseigne()));
		etablissement.addDomicile(new DomicileEtablissement(civilView.getDateDebut(), civilView.getDateFin(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, civilView.getNoOfsCommune(), etablissement));

		final String normalizedIde = NumeroIDEHelper.normalize(civilView.getNumeroIDE());
		if (StringUtils.isNotBlank(normalizedIde)) {
			IdentificationEntreprise identification = new IdentificationEntreprise();
			identification.setNumeroIde(normalizedIde);
			etablissement.addIdentificationEntreprise(identification);
		}

		setComplementCommunication(etablissement, view.getComplementCommunication());
		setComplementCoordFinanciere(etablissement, view.getComplementCoordFinanciere());

		final Etablissement saved = (Etablissement) tiersDAO.save(etablissement);
		final Entreprise entreprise = (Entreprise) tiersDAO.get(noCtbAssocie);
		final RapportEntreTiers activiteEconomique = tiersService.addActiviteEconomique(saved, entreprise, civilView.getDateDebut(), false);
		if (civilView.getDateFin() != null && civilView.getDateFin().isAfterOrEqual(civilView.getDateDebut())) {
			tiersService.closeRapportEntreTiers(activiteEconomique, civilView.getDateFin());
		}

		// Calcul des éléments fiscaux
		final AjustementForsSecondairesResult ajustementForsSecondaires = metierServicePM.calculAjustementForsSecondairesPourEtablissementsVD(entreprise);

		for (ForFiscalSecondaire forAAnnuler : ajustementForsSecondaires.getAAnnuler()) {
			tiersService.annuleForFiscal(forAAnnuler);
		}

		for (AjustementForsSecondairesResult.ForAFermer forAFermer : ajustementForsSecondaires.getAFermer()) {
			tiersService.closeForFiscalSecondaire(entreprise, forAFermer.getForFiscal(), forAFermer.getDateFermeture(), MotifFor.FIN_EXPLOITATION);
		}

		for (ForFiscalSecondaire forACreer : ajustementForsSecondaires.getACreer()) {
			final Commune commune = infraService.getCommuneByNumeroOfs(forACreer.getNumeroOfsAutoriteFiscale(), forACreer.getDateDebut());
			if (!commune.isPrincipale()) {
				Assert.notNull(forACreer.getMotifOuverture(), "Le motif d'ouverture est obligatoire sur un for secondaire dans le canton");
				tiersService.addForSecondaire(entreprise, forACreer.getDateDebut(), forACreer.getDateFin(), forACreer.getMotifRattachement(), forACreer.getNumeroOfsAutoriteFiscale(),
				                              forACreer.getTypeAutoriteFiscale(),
				                              forACreer.getMotifOuverture(), forACreer.getMotifFermeture(), GenreImpot.BENEFICE_CAPITAL);
			}
		}

		return "redirect:/tiers/visu.do?id=" + saved.getNumero();
	}

	private void setComplementCoordFinanciere(@NotNull Tiers tiers, @NotNull ComplementsEditCoordonneesFinancieresView view) {
		final String iban = IbanHelper.normalize(view.getIban());
		final String bicSwift = StringUtils.trimToNull(FormatNumeroHelper.removeSpaceAndDash(view.getAdresseBicSwift()));
		final String titulaire = view.getTitulaireCompteBancaire();
		if (StringUtils.isNotBlank(titulaire) || StringUtils.isNotBlank(iban) || StringUtils.isNotBlank(bicSwift)) {
			tiers.addCoordonneesFinancieres(new CoordonneesFinancieres(titulaire, iban, bicSwift));
		}
	}

	private void setComplementCommunication(Tiers tiers, ComplementsEditCommunicationsView cpltCommView) {
		tiers.setPersonneContact(cpltCommView.getPersonneContact());
		tiers.setComplementNom(cpltCommView.getComplementNom());
		tiers.setNumeroTelephonePrive(cpltCommView.getNumeroTelephonePrive());
		tiers.setNumeroTelephonePortable(cpltCommView.getNumeroTelephonePortable());
		tiers.setNumeroTelephoneProfessionnel(cpltCommView.getNumeroTelephoneProfessionnel());
		tiers.setNumeroTelecopie(cpltCommView.getNumeroTelecopie());
		tiers.setAdresseCourrierElectronique(cpltCommView.getAdresseCourrierElectronique());
	}

}
