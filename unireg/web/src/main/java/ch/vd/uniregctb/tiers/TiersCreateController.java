package ch.vd.uniregctb.tiers;

import javax.validation.Valid;
import java.util.Arrays;
import java.util.HashSet;

import org.apache.commons.lang3.StringUtils;
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

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.DelegatingValidator;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.complements.ComplementsEditCommunicationsView;
import ch.vd.uniregctb.complements.ComplementsEditCoordonneesFinancieresView;
import ch.vd.uniregctb.declaration.Periodicite;
import ch.vd.uniregctb.iban.IbanValidator;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityHelper;
import ch.vd.uniregctb.security.SecurityProviderInterface;
import ch.vd.uniregctb.tiers.validator.CreateAutreCommunauteViewValidator;
import ch.vd.uniregctb.tiers.validator.CreateDebiteurViewValidator;
import ch.vd.uniregctb.tiers.validator.CreateNonHabitantViewValidator;
import ch.vd.uniregctb.tiers.view.AutreCommunauteCivilView;
import ch.vd.uniregctb.tiers.view.CreateAutreCommunauteView;
import ch.vd.uniregctb.tiers.view.CreateDebiteurView;
import ch.vd.uniregctb.tiers.view.CreateNonHabitantView;
import ch.vd.uniregctb.tiers.view.DebiteurFiscalView;
import ch.vd.uniregctb.tiers.view.IdentificationPersonneView;
import ch.vd.uniregctb.tiers.view.NonHabitantCivilView;
import ch.vd.uniregctb.type.PeriodeDecompte;
import ch.vd.uniregctb.type.PeriodiciteDecompte;
import ch.vd.uniregctb.utils.RegDateEditor;

@Controller
@RequestMapping(value = "/tiers")
public class TiersCreateController {

	private static final String DATA = "data";
	private static final String SEXE_MAP_NAME = "sexes";
	private static final String CATEGORIE_ETRANGER_MAP_NAME = "categoriesEtrangers";
	private static final String FORME_JURIDIQUE_MAP_NAME = "formesJuridiques";
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
			addSubValidator(CreateDebiteurView.class, new CreateDebiteurViewValidator(ibanValidator));
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
		pp.setPrenom(civilView.getPrenom());
		pp.setNumeroAssureSocial(FormatNumeroHelper.removeSpaceAndDash(civilView.getNumeroAssureSocial()));
		pp.setSexe(civilView.getSexe());
		pp.setDateNaissance(civilView.getDateNaissance());
		pp.setDateDeces(civilView.getDateDeces());
		pp.setCategorieEtranger(civilView.getCategorieEtranger());
		pp.setDateDebutValiditeAutorisation(civilView.getDateDebutValiditeAutorisation());
		pp.setNumeroOfsNationalite(civilView.getNumeroOfsNationalite());
		pp.setLibelleCommuneOrigine(civilView.getLibelleCommuneOrigine());
		pp.setPrenomsPere(civilView.getPrenomsPere());
		pp.setNomPere(civilView.getNomPere());
		pp.setPrenomsMere(civilView.getPrenomsMere());
		pp.setNomMere(civilView.getNomMere());

		final IdentificationPersonneView ipView = civilView.getIdentificationPersonne();
		tiersService.setIdentifiantsPersonne(pp, ipView.getAncienNumAVS(), ipView.getNumRegistreEtranger());

		final ComplementsEditCommunicationsView cpltCommView = view.getComplementCommunication();
		pp.setPersonneContact(cpltCommView.getPersonneContact());
		pp.setComplementNom(cpltCommView.getComplementNom());
		pp.setNumeroTelephonePrive(cpltCommView.getNumeroTelephonePrive());
		pp.setNumeroTelephonePortable(cpltCommView.getNumeroTelephonePortable());
		pp.setNumeroTelephoneProfessionnel(cpltCommView.getNumeroTelephoneProfessionnel());
		pp.setNumeroTelecopie(cpltCommView.getNumeroTelecopie());
		pp.setAdresseCourrierElectronique(cpltCommView.getAdresseCourrierElectronique());

		final ComplementsEditCoordonneesFinancieresView cpltCoordFinView = view.getComplementCoordFinanciere();
		pp.setNumeroCompteBancaire(cpltCoordFinView.getIban());
		pp.setTitulaireCompteBancaire(cpltCoordFinView.getTitulaireCompteBancaire());
		pp.setAdresseBicSwift(cpltCoordFinView.getAdresseBicSwift());

		final PersonnePhysique saved = (PersonnePhysique) tiersDAO.save(pp);
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
	public String doCreateAutreCommunaute(Model model, @Valid @ModelAttribute(DATA) CreateAutreCommunauteView view, BindingResult results) {
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

		final ComplementsEditCommunicationsView cpltCommView = view.getComplementCommunication();
		ac.setPersonneContact(cpltCommView.getPersonneContact());
		ac.setComplementNom(cpltCommView.getComplementNom());
		ac.setNumeroTelephonePrive(cpltCommView.getNumeroTelephonePrive());
		ac.setNumeroTelephonePortable(cpltCommView.getNumeroTelephonePortable());
		ac.setNumeroTelephoneProfessionnel(cpltCommView.getNumeroTelephoneProfessionnel());
		ac.setNumeroTelecopie(cpltCommView.getNumeroTelecopie());
		ac.setAdresseCourrierElectronique(cpltCommView.getAdresseCourrierElectronique());

		final ComplementsEditCoordonneesFinancieresView cpltCoordFinView = view.getComplementCoordFinanciere();
		ac.setNumeroCompteBancaire(cpltCoordFinView.getIban());
		ac.setTitulaireCompteBancaire(cpltCoordFinView.getTitulaireCompteBancaire());
		ac.setAdresseBicSwift(cpltCoordFinView.getAdresseBicSwift());

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

		final ComplementsEditCommunicationsView cpltCommView = view.getComplementCommunication();
		dpi.setPersonneContact(cpltCommView.getPersonneContact());
		dpi.setComplementNom(cpltCommView.getComplementNom());
		dpi.setNumeroTelephonePrive(cpltCommView.getNumeroTelephonePrive());
		dpi.setNumeroTelephonePortable(cpltCommView.getNumeroTelephonePortable());
		dpi.setNumeroTelephoneProfessionnel(cpltCommView.getNumeroTelephoneProfessionnel());
		dpi.setNumeroTelecopie(cpltCommView.getNumeroTelecopie());
		dpi.setAdresseCourrierElectronique(cpltCommView.getAdresseCourrierElectronique());

		final ComplementsEditCoordonneesFinancieresView cpltCoordFinView = view.getComplementCoordFinanciere();
		dpi.setNumeroCompteBancaire(cpltCoordFinView.getIban());
		dpi.setTitulaireCompteBancaire(cpltCoordFinView.getTitulaireCompteBancaire());
		dpi.setAdresseBicSwift(cpltCoordFinView.getAdresseBicSwift());

		final DebiteurPrestationImposable saved = (DebiteurPrestationImposable) tiersDAO.save(dpi);
		final Contribuable ctbAss = (Contribuable) tiersDAO.get(noCtbAssocie);
		tiersService.addContactImpotSource(saved, ctbAss);

		return "redirect:/tiers/visu.do?id=" + saved.getNumero();
	}

}
