package ch.vd.unireg.tiers;

import javax.validation.Valid;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
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
import ch.vd.unireg.iban.IbanValidator;
import ch.vd.unireg.security.AccessDeniedException;
import ch.vd.unireg.security.Role;
import ch.vd.unireg.security.SecurityHelper;
import ch.vd.unireg.security.SecurityProviderInterface;
import ch.vd.unireg.tiers.validator.CreateAutreCommunauteViewValidator;
import ch.vd.unireg.tiers.validator.CreateDebiteurViewValidator;
import ch.vd.unireg.tiers.validator.CreateEntrepriseViewValidator;
import ch.vd.unireg.tiers.validator.CreateEtablissementViewValidator;
import ch.vd.unireg.tiers.validator.CreateNonHabitantViewValidator;
import ch.vd.unireg.tiers.view.CreateAutreCommunauteView;
import ch.vd.unireg.tiers.view.CreateDebiteurView;
import ch.vd.unireg.tiers.view.CreateEntrepriseView;
import ch.vd.unireg.tiers.view.CreateEtablissementView;
import ch.vd.unireg.tiers.view.CreateNonHabitantView;
import ch.vd.unireg.tiers.view.EntrepriseCivilView;
import ch.vd.unireg.type.FormeJuridiqueEntreprise;
import ch.vd.unireg.type.TypeAutoriteFiscale;
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

		final PersonnePhysique nonHabitant = tiersService.createNonHabitant(view.getCivil().getNom(),
		                                                                    view.getCivil().getNomNaissance(),
		                                                                    view.getCivil().getPrenomUsuel(),
		                                                                    view.getCivil().getTousPrenoms(),
		                                                                    view.getCivil().getNumeroAssureSocial(),
		                                                                    view.getCivil().getSexe(),
		                                                                    view.getCivil().getDateNaissance(),
		                                                                    view.getCivil().getDateDeces(),
		                                                                    view.getCivil().getCategorieEtranger(),
		                                                                    view.getCivil().getDateDebutValiditeAutorisation(),
		                                                                    view.getCivil().getNumeroOfsNationalite(),
		                                                                    view.getCivil().getOfsCommuneOrigine(),
		                                                                    view.getCivil().getNewLibelleCommuneOrigine(),
		                                                                    view.getCivil().getPrenomsPere(),
		                                                                    view.getCivil().getNomPere(),
		                                                                    view.getCivil().getPrenomsMere(),
		                                                                    view.getCivil().getNomMere(),
		                                                                    view.getCivil().getIdentificationPersonne().getAncienNumAVS(),
		                                                                    view.getCivil().getIdentificationPersonne().getNumRegistreEtranger(),
		                                                                    view.getComplementCommunication().getPersonneContact(),
		                                                                    view.getComplementCommunication().getComplementNom(),
		                                                                    view.getComplementCommunication().getNumeroTelephonePrive(),
		                                                                    view.getComplementCommunication().getNumeroTelephonePortable(),
		                                                                    view.getComplementCommunication().getNumeroTelephoneProfessionnel(),
		                                                                    view.getComplementCommunication().getNumeroTelecopie(),
		                                                                    view.getComplementCommunication().getAdresseCourrierElectronique(),
		                                                                    view.getComplementCoordFinanciere().getIban(),
		                                                                    view.getComplementCoordFinanciere().getAdresseBicSwift(),
		                                                                    view.getComplementCoordFinanciere().getTitulaireCompteBancaire());

		return "redirect:/tiers/visu.do?id=" + nonHabitant.getNumero();
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

		final EntrepriseCivilView civilView = view.getCivil();
		
		final RegDate dateDebutExerciceCommercial = civilView.getTypeDateDebutExerciceCommercial() == EntrepriseCivilView.TypeDefautDate.DEFAULT
				? RegDate.get(civilView.getDateOuverture().year(), 1, 1)
				: civilView.getDateDebutExerciceCommercial();

		final RegDate dateFondation = civilView.getTypeDateFondation() == EntrepriseCivilView.TypeDefautDate.DEFAULT
				? civilView.getDateOuverture()
				: civilView.getDateFondation();

		final Entreprise entreprise = tiersService.createEntreprise(civilView.getNumeroIde(),
		                                                            civilView.getDateOuverture(),
		                                                            dateDebutExerciceCommercial,
		                                                            dateFondation,
		                                                            civilView.getFormeJuridique(),
		                                                            civilView.getCapitalLibere(),
		                                                            civilView.getDevise(),
		                                                            civilView.getRaisonSociale(),
		                                                            civilView.getTypeAutoriteFiscale(),
		                                                            civilView.getNumeroOfsSiege(),
		                                                            civilView.isInscriteRC(),
		                                                            view.getComplementCommunication().getPersonneContact(),
		                                                            view.getComplementCommunication().getComplementNom(),
		                                                            view.getComplementCommunication().getNumeroTelephonePrive(),
		                                                            view.getComplementCommunication().getNumeroTelephonePortable(),
		                                                            view.getComplementCommunication().getNumeroTelephoneProfessionnel(),
		                                                            view.getComplementCommunication().getNumeroTelecopie(),
		                                                            view.getComplementCommunication().getAdresseCourrierElectronique(),
		                                                            view.getComplementCoordFinanciere().getIban(),
		                                                            view.getComplementCoordFinanciere().getAdresseBicSwift(),
		                                                            view.getComplementCoordFinanciere().getTitulaireCompteBancaire());


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

		final AutreCommunaute autreCommunaute = tiersService.createAutreCommunaute(view.getCivil().getNom(),
		                                                                           view.getCivil().getIde(),
		                                                                           view.getCivil().getFormeJuridique(),
		                                                                           view.getComplementCommunication().getPersonneContact(),
		                                                                           view.getComplementCommunication().getComplementNom(),
		                                                                           view.getComplementCommunication().getNumeroTelephonePrive(),
		                                                                           view.getComplementCommunication().getNumeroTelephonePortable(),
		                                                                           view.getComplementCommunication().getNumeroTelephoneProfessionnel(),
		                                                                           view.getComplementCommunication().getNumeroTelecopie(),
		                                                                           view.getComplementCommunication().getAdresseCourrierElectronique(),
		                                                                           view.getComplementCoordFinanciere().getIban(),
		                                                                           view.getComplementCoordFinanciere().getAdresseBicSwift(),
		                                                                           view.getComplementCoordFinanciere().getTitulaireCompteBancaire());
		
		return "redirect:/tiers/visu.do?id=" + autreCommunaute.getNumero();
	}

	@RequestMapping(value = "/debiteur/create.do", method = RequestMethod.GET)
	public String createDebiteurPrestationImposable(Model model, @RequestParam(value = "numeroCtbAss") long noCtbAssocie) {
		if (!SecurityHelper.isGranted(securityProvider, Role.CREATE_MODIF_DPI)) {
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
		if (!SecurityHelper.isGranted(securityProvider, Role.CREATE_MODIF_DPI)) {
			throw new AccessDeniedException("Vous ne possédez pas les droits d'accès suffisants à la création d'un débiteur de prestation imposable.");
		}
		if (results.hasErrors()) {
			return showCreateDebiteurPrestationImposable(model, noCtbAssocie, view);
		}

		final DebiteurPrestationImposable debiteur = tiersService.createDebiteur(noCtbAssocie,
		                                                                         view.getFiscal().getCategorieImpotSource(),
		                                                                         view.getFiscal().getModeCommunication(),
		                                                                         view.getFiscal().getPeriodiciteDecompte(),
		                                                                         view.getFiscal().getPeriodeDecompte(),
		                                                                         view.getComplementCommunication().getPersonneContact(),
		                                                                         view.getComplementCommunication().getComplementNom(),
		                                                                         view.getComplementCommunication().getNumeroTelephonePrive(),
		                                                                         view.getComplementCommunication().getNumeroTelephonePortable(),
		                                                                         view.getComplementCommunication().getNumeroTelephoneProfessionnel(),
		                                                                         view.getComplementCommunication().getNumeroTelecopie(),
		                                                                         view.getComplementCommunication().getAdresseCourrierElectronique(),
		                                                                         view.getComplementCoordFinanciere().getIban(),
		                                                                         view.getComplementCoordFinanciere().getAdresseBicSwift(),
		                                                                         view.getComplementCoordFinanciere().getTitulaireCompteBancaire());

		return "redirect:/tiers/visu.do?id=" + debiteur.getNumero();
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

		final Etablissement etablissement = tiersService.createEtablissement(noCtbAssocie,
		                                                                     view.getCivil().getRaisonSociale(),
		                                                                     view.getCivil().getNomEnseigne(),
		                                                                     view.getCivil().getDateDebut(),
		                                                                     view.getCivil().getDateFin(),
		                                                                     view.getCivil().getNoOfsCommune(),
		                                                                     view.getCivil().getNumeroIDE(),
		                                                                     view.getComplementCommunication().getPersonneContact(),
		                                                                     view.getComplementCommunication().getComplementNom(),
		                                                                     view.getComplementCommunication().getNumeroTelephonePrive(),
		                                                                     view.getComplementCommunication().getNumeroTelephonePortable(),
		                                                                     view.getComplementCommunication().getNumeroTelephoneProfessionnel(),
		                                                                     view.getComplementCommunication().getNumeroTelecopie(),
		                                                                     view.getComplementCommunication().getAdresseCourrierElectronique(),
		                                                                     view.getComplementCoordFinanciere().getIban(),
		                                                                     view.getComplementCoordFinanciere().getAdresseBicSwift(),
		                                                                     view.getComplementCoordFinanciere().getTitulaireCompteBancaire());

		return "redirect:/tiers/visu.do?id=" + etablissement.getNumero();
	}

}
