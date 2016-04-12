package ch.vd.uniregctb.tiers;

import javax.validation.Valid;
import java.util.Arrays;
import java.util.HashSet;

import org.apache.commons.lang3.StringUtils;
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

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.data.TypeRegimeFiscal;
import ch.vd.uniregctb.common.BouclementHelper;
import ch.vd.uniregctb.common.DelegatingValidator;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.common.NumeroIDEHelper;
import ch.vd.uniregctb.complements.ComplementsEditCommunicationsView;
import ch.vd.uniregctb.complements.ComplementsEditCoordonneesFinancieresView;
import ch.vd.uniregctb.declaration.Periodicite;
import ch.vd.uniregctb.iban.IbanHelper;
import ch.vd.uniregctb.iban.IbanValidator;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.metier.AjustementForsSecondairesResult;
import ch.vd.uniregctb.metier.MetierServicePM;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityHelper;
import ch.vd.uniregctb.security.SecurityProviderInterface;
import ch.vd.uniregctb.tiers.validator.CreateAutreCommunauteViewValidator;
import ch.vd.uniregctb.tiers.validator.CreateDebiteurViewValidator;
import ch.vd.uniregctb.tiers.validator.CreateEntrepriseViewValidator;
import ch.vd.uniregctb.tiers.validator.CreateEtablissementViewValidator;
import ch.vd.uniregctb.tiers.validator.CreateNonHabitantViewValidator;
import ch.vd.uniregctb.tiers.view.AutreCommunauteCivilView;
import ch.vd.uniregctb.tiers.view.CreateAutreCommunauteView;
import ch.vd.uniregctb.tiers.view.CreateDebiteurView;
import ch.vd.uniregctb.tiers.view.CreateEntrepriseView;
import ch.vd.uniregctb.tiers.view.CreateEtablissementView;
import ch.vd.uniregctb.tiers.view.CreateNonHabitantView;
import ch.vd.uniregctb.tiers.view.DebiteurFiscalView;
import ch.vd.uniregctb.tiers.view.EntrepriseCivilView;
import ch.vd.uniregctb.tiers.view.EtablissementCivilView;
import ch.vd.uniregctb.tiers.view.IdentificationPersonneView;
import ch.vd.uniregctb.tiers.view.NonHabitantCivilView;
import ch.vd.uniregctb.type.CategorieEntreprise;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.PeriodeDecompte;
import ch.vd.uniregctb.type.PeriodiciteDecompte;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeEtatEntreprise;
import ch.vd.uniregctb.type.TypeGenerationEtatEntreprise;
import ch.vd.uniregctb.utils.RegDateEditor;

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
	private static final String TYPES_AUTORITES_FISCALES = "typesAutoritesFiscales";

	private SecurityProviderInterface securityProvider;
	private TiersMapHelper tiersMapHelper;
	private TiersDAO tiersDAO;
	private TiersService tiersService;
	private IbanValidator ibanValidator;
	private ServiceInfrastructureService infraService;
	private MetierServicePM metierServicePM;

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
		model.addAttribute(TYPES_AUTORITES_FISCALES, tiersMapHelper.getMapTypeAutoriteFiscaleEntreprise());
		model.addAttribute(FORME_JURIDIQUE_ENTREPRISE_MAP_NAME, tiersMapHelper.getMapFormeJuridiqueEntrepriseEditableFiscalement());
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

		final Entreprise entreprise = new Entreprise();
		final EntrepriseCivilView civilView = view.getCivil();

		// Numéro IDE
		final String normalizedIde = NumeroIDEHelper.normalize(civilView.getNumeroIde());
		if (StringUtils.isNotBlank(normalizedIde)) {
			IdentificationEntreprise ident = new IdentificationEntreprise();
			ident.setNumeroIde(normalizedIde);
			entreprise.addIdentificationEntreprise(ident);
		}

		// Compléments
		setComplementCommunication(entreprise, view.getComplementCommunication());
		setComplementCoordFinanciere(entreprise, view.getComplementCoordFinanciere());

		final Entreprise saved = (Entreprise) tiersDAO.save(entreprise);

		// Forme juridique
		tiersService.addFormeJuridiqueFiscale(saved, civilView.getFormeJuridique(), civilView.getDateCreation(), null);

		// Capital
		if (civilView.getCapitalLibere() != null) {
			tiersService.addCapitalFiscal(saved, civilView.getCapitalLibere(), civilView.getDevise(), civilView.getDateCreation(), null);
		}

		// Raison sociale
		tiersService.addRaisonSocialeFiscale(saved, civilView.getRaisonSociale(), civilView.getDateCreation(), null);

		// Siège (établissement)
		Etablissement etablissement = new Etablissement();
		etablissement.setRaisonSociale(civilView.getRaisonSociale());
		final Etablissement savedEtablissement = (Etablissement) tiersDAO.save(etablissement);
		tiersService.addDomicileEtablissement(savedEtablissement, civilView.getTypeAutoriteFiscale(), civilView.getNumeroOfsSiege(), civilView.getDateCreation(), null);

		// Rapport entre entreprise et établissement principal
		tiersService.addActiviteEconomique(savedEtablissement, saved, civilView.getDateCreation(), true);

		// Régimes fiscaux + For principal (différents en fonction de la forme juridique/catégorie entreprise)
		CategorieEntreprise categorieEntreprise = CategorieEntrepriseHelper.map(civilView.getFormeJuridique());
		if (categorieEntreprise == CategorieEntreprise.PM || categorieEntreprise == CategorieEntreprise.DPPM
			|| categorieEntreprise == CategorieEntreprise.APM || categorieEntreprise == CategorieEntreprise.DPAPM) {

			// Récupération du type de régime fiscal
			TypeRegimeFiscal trf = tiersService.getTypeRegimeFiscalParDefault(categorieEntreprise);

			tiersService.addRegimeFiscal(saved, RegimeFiscal.Portee.CH, trf, civilView.getDateCreation(), null);
			tiersService.addRegimeFiscal(saved, RegimeFiscal.Portee.VD, trf, civilView.getDateCreation(), null);

			// Bouclement et premier exercice commercial
			Bouclement bouclement = BouclementHelper.createBouclement3112SelonSemestre(civilView.getDateCreation());
			saved.addBouclement(bouclement);
			saved.setDateDebutPremierExerciceCommercial(RegDate.get(civilView.getDateCreation().year(), 1, 1));

			// For principal
			tiersService.addForPrincipal(saved, civilView.getDateCreation(), MotifFor.DEBUT_EXPLOITATION, null, null, MotifRattachement.DOMICILE, civilView.getNumeroOfsSiege(), civilView.getTypeAutoriteFiscale(), GenreImpot.BENEFICE_CAPITAL);

		} else if (categorieEntreprise == CategorieEntreprise.SP) {
			// Pas de régime fiscal (et donc pas de bouclement)
			// For principal
			tiersService.addForPrincipal(saved, civilView.getDateCreation(), MotifFor.DEBUT_EXPLOITATION, null, null, MotifRattachement.DOMICILE, civilView.getNumeroOfsSiege(), civilView.getTypeAutoriteFiscale(), GenreImpot.REVENU_FORTUNE);
		}

		// Etat fiscal
		TypeEtatEntreprise typeEtatEntreprise = (civilView.isInscriteRC()) ? TypeEtatEntreprise.INSCRITE_RC : TypeEtatEntreprise.FONDEE;
		tiersService.changeEtatEntreprise(typeEtatEntreprise, saved, civilView.getDateCreation(), TypeGenerationEtatEntreprise.MANUELLE);

		return "redirect:/tiers/visu.do?id=" + saved.getNumero();
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
		dpi.setPeriodicites(new HashSet<>(Arrays.asList(new Periodicite(periodicite, periode, RegDate.get(RegDate.get().year(), 1, 1), null))));

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
		String raisonSociale = tiersService.getRaisonSociale(entreprise);

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
		etablissement.setRaisonSociale(civilView.getRaisonSociale());
		etablissement.setEnseigne(civilView.getNomEnseigne());
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
		tiersService.addActiviteEconomique(saved, entreprise, civilView.getDateDebut(), false);

		// Calcul des éléments fiscaux
		final AjustementForsSecondairesResult ajustementForsSecondaires = metierServicePM.calculAjustementForsSecondairesPourEtablissementsVD(entreprise, null);

		for (ForFiscalSecondaire forAAnnuler : ajustementForsSecondaires.getAAnnuler()) {
			tiersService.annuleForFiscal(forAAnnuler);
		}

		for (AjustementForsSecondairesResult.ForAFermer forAFermer : ajustementForsSecondaires.getAFermer()) {
			tiersService.closeForFiscalSecondaire(entreprise, forAFermer.getForFiscal(), forAFermer.getDateFermeture(), MotifFor.FIN_EXPLOITATION);
		}

		for (ForFiscalSecondaire forACreer : ajustementForsSecondaires.getACreer()) {
			final Commune commune = infraService.getCommuneByNumeroOfs(forACreer.getNumeroOfsAutoriteFiscale(), forACreer.getDateDebut());
			if (!commune.isPrincipale()) {
				Assert.notNull(forACreer.getMotifOuverture(), "Le motif d'ouverture est obligatoire sur un for secondaire dans le canton"); // TODO: is it?
				tiersService.addForSecondaire(entreprise, forACreer.getDateDebut(), forACreer.getDateFin(), forACreer.getMotifRattachement(), forACreer.getNumeroOfsAutoriteFiscale(),
				                              forACreer.getTypeAutoriteFiscale(),
				                              forACreer.getMotifOuverture(), forACreer.getMotifFermeture(), GenreImpot.BENEFICE_CAPITAL);
			}
		}

		return "redirect:/tiers/visu.do?id=" + saved.getNumero();
	}

	private void setComplementCoordFinanciere(Tiers tiers, ComplementsEditCoordonneesFinancieresView cpltCoordFinView) {
		final String iban = IbanHelper.normalize(cpltCoordFinView.getIban());
		final String bicSwift = StringUtils.trimToNull(FormatNumeroHelper.removeSpaceAndDash(cpltCoordFinView.getAdresseBicSwift()));
		if (iban != null || bicSwift != null) {
			tiers.setCoordonneesFinancieres(new CoordonneesFinancieres(iban, bicSwift));
		}
		else {
			tiers.setCoordonneesFinancieres(null);
		}
		tiers.setTitulaireCompteBancaire(cpltCoordFinView.getTitulaireCompteBancaire());
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
