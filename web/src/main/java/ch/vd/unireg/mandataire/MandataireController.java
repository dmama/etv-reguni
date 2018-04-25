package ch.vd.unireg.mandataire;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
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
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.adresse.AdresseEnvoiDetaillee;
import ch.vd.unireg.adresse.AdresseException;
import ch.vd.unireg.adresse.AdresseMandataire;
import ch.vd.unireg.adresse.AdresseMandataireAdapter;
import ch.vd.unireg.adresse.AdresseMandataireSuisse;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.adresse.TypeAdresseFiscale;
import ch.vd.unireg.common.ActionException;
import ch.vd.unireg.common.ControllerUtils;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.common.MandatOuAssimile;
import ch.vd.unireg.common.ObjectNotFoundException;
import ch.vd.unireg.common.TiersNotFoundException;
import ch.vd.unireg.entreprise.complexe.SearchTiersComponent;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.iban.IbanHelper;
import ch.vd.unireg.iban.IbanValidator;
import ch.vd.unireg.interfaces.infra.data.GenreImpotMandataire;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.security.AccessDeniedException;
import ch.vd.unireg.security.Role;
import ch.vd.unireg.security.SecurityHelper;
import ch.vd.unireg.security.SecurityProviderInterface;
import ch.vd.unireg.tiers.CompteBancaire;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.Mandat;
import ch.vd.unireg.tiers.RapportEntreTiers;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersCriteria;
import ch.vd.unireg.tiers.TiersMapHelper;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.tiers.validator.TiersCriteriaValidator;
import ch.vd.unireg.tiers.view.TiersCriteriaView;
import ch.vd.unireg.transaction.TransactionHelper;
import ch.vd.unireg.type.TypeMandat;
import ch.vd.unireg.utils.RegDateEditor;

@Controller
@RequestMapping(value = "/mandataire")
public class MandataireController implements MessageSourceAware, InitializingBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(MandataireController.class);

	private static final String ID_MANDANT = "idMandant";
	private static final String ID_MANDATAIRE = "idMandataire";
	private static final String MANDAT = "mandat";
	private static final String MANDATS = "mandats";
	private static final String MANDAT_EDIT = "editMandat";
	private static final String TYPES_MANDATS_ALLOWED = "typesMandatAutorises";
	private static final String GENRES_IMPOT_ALLOWED = "genresImpotAutorises";
	private static final String TEXTES_CASE_POSTALE_ALLOWED = "textesCasePostale";
	private static final String DONNEES_MANDAT = "donneesMandat";
	private static final String FORCAGE_AVEC_SANS_TIERS = "forcageAvecSansTiers";   // valeurs : avec ou sans
	private static final String ACCES_MANDATAIRES = "accesMandataires";
	private static final String ADD_LIEN_COURRIER_AUTORISE = "addLienCourrierAutorise";     // booléen

	private static final String MODE = "mode";
	private static final String MODE_COURRIER = "courrier";
	private static final String MODE_PERCEPTION = "perception";

	private AdresseService adresseService;
	private TiersService tiersService;
	private ServiceInfrastructureService infraService;
	private TransactionHelper transactionHelper;
	private HibernateTemplate hibernateTemplate;
	private SecurityProviderInterface securityProvider;
	private ControllerUtils controllerUtils;
	private TiersMapHelper tiersMapHelper;
	private MessageSource messageSource;
	private SearchTiersComponent searchTiersComponent;
	private IbanValidator ibanValidator;
	private ConfigurationMandataire configurationMandataire;

	@Override
	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setInfraService(ServiceInfrastructureService infraService) {
		this.infraService = infraService;
	}

	public void setTransactionHelper(TransactionHelper transactionHelper) {
		this.transactionHelper = transactionHelper;
	}

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	public void setSecurityProvider(SecurityProviderInterface securityProvider) {
		this.securityProvider = securityProvider;
	}

	public void setControllerUtils(ControllerUtils controllerUtils) {
		this.controllerUtils = controllerUtils;
	}

	public void setTiersMapHelper(TiersMapHelper tiersMapHelper) {
		this.tiersMapHelper = tiersMapHelper;
	}

	public void setIbanValidator(IbanValidator ibanValidator) {
		this.ibanValidator = ibanValidator;
	}

	public void setConfigurationMandataire(ConfigurationMandataire configurationMandataire) {
		this.configurationMandataire = configurationMandataire;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		final Set<TiersCriteria.TypeTiers> typesImperatifs = EnumSet.of(TiersCriteria.TypeTiers.PERSONNE_PHYSIQUE, TiersCriteria.TypeTiers.ENTREPRISE, TiersCriteria.TypeTiers.ETABLISSEMENT_SECONDAIRE);
		this.searchTiersComponent = new SearchTiersComponent(tiersService, messageSource, tiersMapHelper,
		                                                     "AddMandataireCriteria", "tiers/edition/mandataire/add",
		                                                     criteria -> criteria.setTypesTiersImperatifs(typesImperatifs));
	}

	@InitBinder(value = SearchTiersComponent.COMMAND)
	public void initCommandBinder(WebDataBinder binder) {
		binder.setValidator(new TiersCriteriaValidator());
		binder.registerCustomEditor(RegDate.class, new RegDateEditor(true, true, false, RegDateHelper.StringFormat.DISPLAY));
	}

	@InitBinder(value = DONNEES_MANDAT)
	public void initDonneesMandatBinder(WebDataBinder binder) {
		binder.setValidator(new AddMandatViewValidator(ibanValidator, buildSetOfCodesGenreImpot()));
		binder.registerCustomEditor(RegDate.class, new RegDateEditor(true, true, false, RegDateHelper.StringFormat.DISPLAY));
	}

	@InitBinder(value = MANDAT_EDIT)
	public void initEditMandatBinder(WebDataBinder binder) {
		binder.setValidator(new EditMandatViewValidator(ibanValidator));
		binder.registerCustomEditor(RegDate.class, new RegDateEditor(true, true, false, RegDateHelper.StringFormat.DISPLAY));
	}

	private Set<String> buildSetOfCodesGenreImpot() {
		final List<GenreImpotMandataire> all = infraService.getGenresImpotMandataires();
		final Set<String> codes = new HashSet<>(all.size());
		for (GenreImpotMandataire gim : all) {
			codes.add(gim.getCode());
		}
		return codes;
	}

	private void checkDroitAccesMandatairesCourrier() {
		if (!SecurityHelper.isAnyGranted(securityProvider, Role.MODIF_MANDAT_GENERAL, Role.MODIF_MANDAT_SPECIAL)) {
			throw new AccessDeniedException("Vous ne possédez aucun droit IfoSec d'administration des mandats de types général et spécial.");
		}
	}

	private void checkDroitAccesMandatairesGeneraux() {
		if (!SecurityHelper.isAnyGranted(securityProvider, Role.MODIF_MANDAT_GENERAL)) {
			throw new AccessDeniedException("Vous ne possédez aucun droit IfoSec d'administration des mandats de type général.");
		}
	}

	private void checkDroitAccesMandatairesSpeciaux() {
		if (!SecurityHelper.isAnyGranted(securityProvider, Role.MODIF_MANDAT_SPECIAL)) {
			throw new AccessDeniedException("Vous ne possédez aucun droit IfoSec d'administration des mandats de type spécial.");
		}
	}

	private void checkDroitAccesMandatairesPerception() {
		if (!SecurityHelper.isAnyGranted(securityProvider, Role.MODIF_MANDAT_TIERS)) {
			throw new AccessDeniedException("Vous ne possédez aucun droit IfoSec d'administration des mandats de type tiers.");
		}
	}

	private void checkDroitAccesMandat(TypeMandat typeMandat) {
		switch (typeMandat) {
		case GENERAL:
			checkDroitAccesMandatairesGeneraux();
			break;
		case SPECIAL:
			checkDroitAccesMandatairesSpeciaux();
			break;
		case TIERS:
			checkDroitAccesMandatairesPerception();
			break;
		default:
			throw new IllegalArgumentException("Valeur de type de mandat non-supportée : " + typeMandat);
		}
	}

	private void checkAccessDossierEnEcriture(long noTiers) {
		controllerUtils.checkAccesDossierEnLecture(noTiers);
	}

	private Map<TypeMandat, Boolean> buildModifiableMap() {
		final Map<TypeMandat, Boolean> map = new EnumMap<>(TypeMandat.class);
		map.put(TypeMandat.GENERAL, SecurityHelper.isGranted(securityProvider, Role.MODIF_MANDAT_GENERAL));
		map.put(TypeMandat.SPECIAL, SecurityHelper.isGranted(securityProvider, Role.MODIF_MANDAT_SPECIAL));
		map.put(TypeMandat.TIERS, SecurityHelper.isGranted(securityProvider, Role.MODIF_MANDAT_TIERS));
		return map;
	}

	/**
	 * @param mandat un mandat
	 * @return le mandataire associé au mandat
	 * @throws TiersNotFoundException si un tel tiers n'existe pas
	 */
	@NotNull
	private Tiers getMandataire(Mandat mandat) {
		final long idMandataire = mandat.getObjetId();
		final Tiers mandataire = hibernateTemplate.get(Tiers.class, idMandataire);
		if (mandataire == null) {
			throw new TiersNotFoundException(idMandataire);
		}
		return mandataire;
	}

	@RequestMapping(value = "/adresse-de-mandat.do", method = RequestMethod.GET)
	@ResponseBody
	public AdresseMandatView getAdresseMandat(@RequestParam("idMandat") final long idMandat) {
		return transactionHelper.doInTransaction(true, status -> {
			final Mandat mandat = hibernateTemplate.get(Mandat.class, idMandat);
			if (mandat == null || (mandat.getTypeMandat() != TypeMandat.GENERAL && mandat.getTypeMandat() != TypeMandat.SPECIAL)) {
				return null;
			}

			final String personneContact = mandat.getPersonneContact();
			final String noTelContact = mandat.getNoTelephoneContact();

			final Tiers mandataire;
			try {
				mandataire = getMandataire(mandat);
			}
			catch (TiersNotFoundException e) {
				return new AdresseMandatView(personneContact, noTelContact, (List<String>) null);
			}

			try {
				final AdresseEnvoiDetaillee adresse = adresseService.getAdresseEnvoi(mandataire, mandat.getDateFin(), TypeAdresseFiscale.REPRESENTATION, false);
				return new AdresseMandatView(personneContact, noTelContact, Arrays.asList(adresse.getLignes()));
			}
			catch (AdresseException e) {
				LOGGER.error("Problème à la détermination de l'adresse de représentation du tiers " + mandataire.getNumero(), e);
				return new AdresseMandatView(personneContact, noTelContact, e.getMessage());
			}
		});
	}

	@RequestMapping(value = "/adresse-representation.do", method = RequestMethod.GET)
	@ResponseBody
	public LignesAdressesView getAdresseRepresentationTiers(@RequestParam("idTiers") final long idTiers) {
		return transactionHelper.doInTransaction(true, status -> {
			final Tiers tiers = hibernateTemplate.get(Tiers.class, idTiers);
			if (tiers == null) {
				return null;
			}

			try {
				final AdresseEnvoiDetaillee adresse = adresseService.getAdresseEnvoi(tiers, null, TypeAdresseFiscale.REPRESENTATION, false);
				return new LignesAdressesView(Arrays.asList(adresse.getLignes()));
			}
			catch (AdresseException e) {
				LOGGER.error("Problème à la détermination de l'adresse de représentation du tiers " + tiers.getNumero(), e);
				return null;
			}
		});
	}

	@RequestMapping(value = "/adresse-mandataire.do", method = RequestMethod.GET)
	@ResponseBody
	public AdresseMandatView getAdresseMandataire(@RequestParam("idAdresse") final long idAdresse) {
		return transactionHelper.doInTransaction(true, status -> {
			final AdresseMandataire mandat = hibernateTemplate.get(AdresseMandataire.class, idAdresse);
			if (mandat == null) {
				return null;
			}

			final String personneContact = mandat.getPersonneContact();
			final String noTelContact = mandat.getNoTelephoneContact();

			try {
				final AdresseMandataireAdapter adapter = new AdresseMandataireAdapter(mandat, infraService);
				final AdresseEnvoiDetaillee adresse = adresseService.buildAdresseEnvoi(adapter.getSource().getTiers(), adapter, mandat.getDateFin());
				return new AdresseMandatView(personneContact, noTelContact, Arrays.asList(adresse.getLignes()));
			}
			catch (AdresseException e) {
				LOGGER.error("Problème à la construction de l'adresse mandataire " + mandat.getId(), e);
				return new AdresseMandatView(personneContact, noTelContact, e.getMessage());
			}
		});
	}

	private static <T, U> void forEach(@Nullable Collection<T> source,
	                                   Predicate<? super T> filter,
	                                   Function<? super T, ? extends U> mapper,
	                                   Consumer<? super U> job) {
		if (source != null && !source.isEmpty()) {
			source.stream()
					.filter(filter)
					.map(mapper)
					.forEach(job);
		}
	}

	private static void doForAllMandatsOfType(final Set<TypeMandat> targetTypes, Contribuable mandant, Consumer<? super Mandat> job) {
		final Set<RapportEntreTiers> rets = mandant.getRapportsSujet();
		if (rets != null && !rets.isEmpty()) {
			forEach(rets,
			        ret -> ret instanceof Mandat && targetTypes.contains(((Mandat) ret).getTypeMandat()),
					Mandat.class::cast,
					job);
		}
	}

	private static void doForAllAdressesMandataires(Contribuable mandant, Consumer<? super AdresseMandataire> job) {
		final Set<AdresseMandataire> adresses = mandant.getAdressesMandataires();
		forEach(adresses, x -> true, Function.identity(), job);
	}

	private static void filtrerMapModifiablesAvecAccesMandataires(Map<TypeMandat, Boolean> modifiableMap, AccesMandatairesView acces) {
		if (!acces.hasGeneralInEdition()) {
			modifiableMap.put(TypeMandat.GENERAL, Boolean.FALSE);
		}
		if (!acces.hasSpecialInEdition()) {
			modifiableMap.put(TypeMandat.SPECIAL, Boolean.FALSE);
		}
		if (!acces.hasTiersPerceptionInEdition()) {
			modifiableMap.put(TypeMandat.TIERS, Boolean.FALSE);
		}
	}

	private AccesMandatairesView getAccesMandatairesView(long idMandant) {
		return transactionHelper.doInTransaction(true, status -> {
			final Tiers tiers = hibernateTemplate.get(Contribuable.class, idMandant);
			return getAccesMandatairesView(tiers);
		});
	}

	private AccesMandatairesView getAccesMandatairesView(Tiers tiers) {
		return new AccesMandatairesView(tiers, configurationMandataire, infraService);
	}

	@RequestMapping(value = "/courrier/edit-list.do", method = RequestMethod.GET)
	public String editMandatairesCourrier(final Model model, @RequestParam("ctbId") final long idMandant) {
		checkDroitAccesMandatairesCourrier();
		return transactionHelper.doInTransaction(true, status -> {
			checkAccessDossierEnEcriture(idMandant);

			final Contribuable mandant = hibernateTemplate.get(Contribuable.class, idMandant);
			if (mandant == null) {
				throw new TiersNotFoundException(idMandant);
			}

			final Map<TypeMandat, Boolean> modifiableMap = buildModifiableMap();
			final AccesMandatairesView accesMandataires = getAccesMandatairesView(mandant);
			filtrerMapModifiablesAvecAccesMandataires(modifiableMap, accesMandataires);

			final Predicate<MandatOuAssimile> isVisible = mandat -> (mandat.getTypeMandat() == TypeMandat.GENERAL && accesMandataires.hasGeneral()) || (mandat.getTypeMandat() == TypeMandat.SPECIAL && accesMandataires.hasSpecial(mandat.getCodeGenreImpot()));
			final Predicate<MandatOuAssimile> isEditable = mandat -> (mandat.getTypeMandat() == TypeMandat.GENERAL && accesMandataires.hasGeneralInEdition()) || (mandat.getTypeMandat() == TypeMandat.SPECIAL && accesMandataires.hasSpecialInEdition(mandat.getCodeGenreImpot()));

			final List<MandataireCourrierEditView> mandats = new LinkedList<>();
			doForAllMandatsOfType(EnumSet.of(TypeMandat.GENERAL, TypeMandat.SPECIAL),
			                      mandant,
			                      mandat -> {
				                      if (isVisible.test(mandat)) {
					                      final boolean modifiableSelonGenreImpot = isEditable.test(mandat);
					                      final boolean modifiableSelonDroits = modifiableMap.get(mandat.getTypeMandat());
					                      mandats.add(new MandataireCourrierEditView(mandat, tiersService, infraService, modifiableSelonDroits && modifiableSelonGenreImpot));
				                      }
			                      });
			doForAllAdressesMandataires(mandant,
			                            adresse -> {
				                            if (isVisible.test(adresse)) {
					                            final boolean modifiableSelonGenreImpot = isEditable.test(adresse);
					                            final boolean modifiableSelonDroits = modifiableMap.get(adresse.getTypeMandat());
					                            mandats.add(new MandataireCourrierEditView(adresse, infraService, modifiableSelonDroits && modifiableSelonGenreImpot));
				                            }
			                            });
			mandats.sort(MandataireViewHelper.COURRIER_COMPARATOR);

			model.addAttribute(MANDATS, mandats);
			model.addAttribute(ID_MANDANT, idMandant);
			model.addAttribute(ACCES_MANDATAIRES, accesMandataires);
			return "tiers/edition/mandataire/courrier";
		});
	}

	@RequestMapping(value = "/perception/edit-list.do", method = RequestMethod.GET)
	public String editMandatairesPerception(final Model model, @RequestParam("ctbId") final long idMandant) {
		checkDroitAccesMandatairesPerception();
		return transactionHelper.doInTransaction(true, status -> {
			checkAccessDossierEnEcriture(idMandant);

			final Contribuable mandant = hibernateTemplate.get(Contribuable.class, idMandant);
			if (mandant == null) {
				throw new TiersNotFoundException(idMandant);
			}

			final Map<TypeMandat, Boolean> modifiableMap = buildModifiableMap();
			final AccesMandatairesView accesMandataires = getAccesMandatairesView(mandant);
			filtrerMapModifiablesAvecAccesMandataires(modifiableMap, accesMandataires);

			final List<MandatairePerceptionEditView> mandats = new LinkedList<>();
			doForAllMandatsOfType(EnumSet.of(TypeMandat.TIERS),
			                      mandant,
			                      mandat -> {
				                      if (accesMandataires.hasTiersPerception()) {
					                      final boolean modifiableSelonTiers = accesMandataires.hasTiersPerceptionInEdition();
					                      final boolean modifiableSelonDroits = modifiableMap.get(mandat.getTypeMandat());
					                      mandats.add(new MandatairePerceptionEditView(mandat, tiersService, modifiableSelonTiers && modifiableSelonDroits));
				                      }
			                      });
			mandats.sort(MandataireViewHelper.BASIC_COMPARATOR);

			model.addAttribute(MANDATS, mandats);
			model.addAttribute(ID_MANDANT, idMandant);
			model.addAttribute(ACCES_MANDATAIRES, accesMandataires);
			return "tiers/edition/mandataire/perception";
		});
	}

	@RequestMapping(value = "/courrier/ajouter-list.do", method = RequestMethod.GET)
	public String ajouterMandatCourrier(Model model, HttpSession session, @RequestParam(ID_MANDANT) long idMandant) {
		checkDroitAccesMandatairesCourrier();
		checkAccessDossierEnEcriture(idMandant);
		fillModelAvecDonneesAjoutMandatCourrier(model, new AddMandatView(idMandant), getAccesMandatairesView(idMandant));
		return searchTiersComponent.showFormulaireRecherche(model, session);
	}

	private void fillModelAvecDonneesBaseAjoutMandat(Model model, AddMandatView view, AccesMandatairesView accesMandataires) {
		model.addAttribute(ID_MANDANT, view.getIdMandant());
		model.addAttribute(ID_MANDATAIRE, view.getIdTiersMandataire());
		model.addAttribute(DONNEES_MANDAT, view);
		model.addAttribute(GENRES_IMPOT_ALLOWED, buildMapGenresImpotMandataire(accesMandataires));
		model.addAttribute(TEXTES_CASE_POSTALE_ALLOWED, tiersMapHelper.getMapTexteCasePostale());
	}

	private void fillModelAvecDonneesAjoutMandatCourrier(Model model, AddMandatView view, AccesMandatairesView accesMandataires) {
		fillModelAvecDonneesBaseAjoutMandat(model, view, accesMandataires);
		model.addAttribute(MODE, MODE_COURRIER);
		model.addAttribute(TYPES_MANDATS_ALLOWED, getTypesMandatAutorises(EnumSet.of(TypeMandat.GENERAL, TypeMandat.SPECIAL), accesMandataires));
		model.addAttribute(ADD_LIEN_COURRIER_AUTORISE, configurationMandataire.isCreationRapportEntreTiersAutoriseePourMandatsCourrier());
	}

	@RequestMapping(value = "/perception/ajouter-list.do", method = RequestMethod.GET)
	public String ajouterMandatPerception(Model model, HttpSession session, @RequestParam(ID_MANDANT) long idMandant) {
		checkDroitAccesMandatairesPerception();
		checkAccessDossierEnEcriture(idMandant);
		fillModelAvecDonneesAjoutMandatPerception(model, new AddMandatView(idMandant), getAccesMandatairesView(idMandant));
		return searchTiersComponent.showFormulaireRecherche(model, session);
	}

	private void fillModelAvecDonneesAjoutMandatPerception(Model model, AddMandatView view, AccesMandatairesView accesMandataires) {
		fillModelAvecDonneesBaseAjoutMandat(model, view, accesMandataires);
		model.addAttribute(MODE, MODE_PERCEPTION);
		model.addAttribute(TYPES_MANDATS_ALLOWED, getTypesMandatAutorises(EnumSet.of(TypeMandat.TIERS), accesMandataires));
	}

	/**
	 * Seul les genres d'impôt autorisés à la modification sont exposés ici...
	 * @return Map de clé = code de régime, et valeur = libellé associé (l'itérateur sur la map donne les entrées dans l'ordre alphabétique des libellés
	 */
	private Map<String, String> buildMapGenresImpotMandataire(AccesMandatairesView accesMandataires) {
		final List<GenreImpotMandataire> gims = infraService.getGenresImpotMandataires();
		return gims.stream()
				.filter(gim -> accesMandataires.hasSpecialInEdition(gim.getCode()))
				.sorted(Comparator.comparing(GenreImpotMandataire::getLibelle))     // on va trier par ordre alphabétique des libellés
				.collect(Collectors.toMap(GenreImpotMandataire::getCode,
				                          GenreImpotMandataire::getLibelle,
				                          (l1, l2) -> l1,
				                          () -> new LinkedHashMap<>(gims.size())));
	}

	@RequestMapping(value = "/courrier/ajouter-list.do", method = RequestMethod.POST)
	public String doSearchTiersMandataireCourrier(@Valid @ModelAttribute(value = SearchTiersComponent.COMMAND) TiersCriteriaView view,
	                                              BindingResult bindingResult,
	                                              HttpSession session,
	                                              Model model,
	                                              @RequestParam(ID_MANDANT) long idMandant) {
		checkDroitAccesMandatairesCourrier();
		fillModelAvecDonneesAjoutMandatCourrier(model, new AddMandatView(idMandant), getAccesMandatairesView(idMandant));       // en cas d'erreur dans le formulaire de recherche, il faut ré-afficher la page... correctement
		fillModelForcageAvecSansTiers(model, true);
		return searchTiersComponent.doRecherche(view, bindingResult, session, model, "ajouter-list.do");
	}

	@RequestMapping(value = "/perception/ajouter-list.do", method = RequestMethod.POST)
	public String doSearchTiersMandatairePerception(@Valid @ModelAttribute(value = SearchTiersComponent.COMMAND) TiersCriteriaView view,
	                                                BindingResult bindingResult,
	                                                HttpSession session,
	                                                Model model,
	                                                @RequestParam(ID_MANDANT) long idMandant) {
		checkDroitAccesMandatairesPerception();
		fillModelAvecDonneesAjoutMandatPerception(model, new AddMandatView(idMandant), getAccesMandatairesView(idMandant));       // en cas d'erreur dans le formulaire de recherche, il faut ré-afficher la page... correctement
		return searchTiersComponent.doRecherche(view, bindingResult, session, model, "ajouter-list.do");
	}

	@RequestMapping(value = "/courrier/reset-search.do", method = RequestMethod.GET)
	public String doResetSearchTiersMandataireCourrier(HttpSession session, @RequestParam(ID_MANDANT) long idMandant) {
		checkDroitAccesMandatairesCourrier();
		return searchTiersComponent.resetCriteresRecherche(session, "ajouter-list.do?idMandant=" + idMandant);
	}

	@RequestMapping(value = "/perception/reset-search.do", method = RequestMethod.GET)
	public String doResetSearchTiersMandatairePerception(HttpSession session, @RequestParam(ID_MANDANT) long idMandant) {
		checkDroitAccesMandatairesPerception();
		return searchTiersComponent.resetCriteresRecherche(session, "ajouter-list.do?idMandant=" + idMandant);
	}

	private Map<TypeMandat, String> getTypesMandatAutorises(Set<TypeMandat> typesPossibles, AccesMandatairesView accesMandataires) {
		final Map<TypeMandat, Boolean> mapModifiables = buildModifiableMap();
		filtrerMapModifiablesAvecAccesMandataires(mapModifiables, accesMandataires);
		final Map<TypeMandat, String> typesMandat = tiersMapHelper.getTypesMandat();
		final Map<TypeMandat, String> copy = new LinkedHashMap<>(typesMandat.size());       // il faut absolument conserver l'ordre...
		for (Map.Entry<TypeMandat, String> entry : typesMandat.entrySet()) {
			final TypeMandat type = entry.getKey();
			if (typesPossibles.contains(type) && mapModifiables.get(type)) {
				copy.put(type, entry.getValue());
			}
		}
		return copy;
	}

	private void fillModelForcageAvecSansTiers(Model model, boolean avec) {
		model.addAttribute(FORCAGE_AVEC_SANS_TIERS, avec ? "avec" : "sans");
	}

	@RequestMapping(value = "/courrier/add-adresse.do", method = RequestMethod.POST)
	public String doAddAdresseCourrier(@Valid @ModelAttribute(value = DONNEES_MANDAT) AddMandatView view,
	                                   BindingResult bindingResult,
	                                   Model model, HttpSession session) {
		checkDroitAccesMandatairesCourrier();
		if (bindingResult.hasErrors()) {
			fillModelAvecDonneesAjoutMandatCourrier(model, view, getAccesMandatairesView(view.getIdMandant()));
			fillModelForcageAvecSansTiers(model, false);
			searchTiersComponent.fillModel(model, session, true);
			return "tiers/edition/mandataire/add";
		}

		// mode courrier... on ne devrait pas être ici avec un type de mandat tiers
		if (view.getTypeMandat() == TypeMandat.TIERS) {
			throw new ActionException("Type de mandat invalide !");
		}

		ajouterMandat(view);
		return "redirect:/mandataire/courrier/edit-list.do?ctbId=" + view.getIdMandant();
	}

	private void ajouterMandat(final AddMandatView view) {

		transactionHelper.doInTransaction(false, new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				checkAccessDossierEnEcriture(view.getIdMandant());

				final Contribuable mandant = hibernateTemplate.get(Contribuable.class, view.getIdMandant());
				if (mandant == null) {
					throw new ActionException("Le contribuable mandant " + FormatNumeroHelper.numeroCTBToDisplay(view.getIdMandant()) + " n'existe pas.");
				}

				if (view.getIdTiersMandataire() != null) {
					final Contribuable mandataire = hibernateTemplate.get(Contribuable.class, view.getIdTiersMandataire());
					if (mandataire == null) {
						throw new ActionException("Le contribuable mandataire " + FormatNumeroHelper.numeroCTBToDisplay(view.getIdTiersMandataire()) + " n'existe pas.");
					}

					// on va générer un vrai mandat...
					final Mandat mandat;
					switch (view.getTypeMandat()) {
					case GENERAL:
						mandat = Mandat.general(view.getDateDebut(), view.getDateFin(), mandant, mandataire, view.isWithCopy());
						break;
					case SPECIAL:
						mandat = Mandat.special(view.getDateDebut(), view.getDateFin(), mandant, mandataire, view.isWithCopy(), view.getCodeGenreImpot());
						break;
					case TIERS:
						mandat = Mandat.tiers(view.getDateDebut(), view.getDateFin(), mandant, mandataire, new CompteBancaire(IbanHelper.normalize(view.getIban()), null));
						break;
					default:
						throw new ActionException("Type de mandat invalide (" + view.getTypeMandat() + ") !");
					}

					if (mandat.getTypeMandat() != TypeMandat.TIERS) {
						mandat.setPersonneContact(view.getPersonneContact());
						mandat.setNoTelephoneContact(view.getNoTelContact());
					}
					tiersService.addMandat(mandant, mandat);
				}
				else {
					// on va générer une adresse de mandataire
					if (view.getTypeMandat() == TypeMandat.TIERS) {
						throw new ActionException("Tiers mandataire obligatoire pour les mandats tiers...");
					}

					final AdresseMandataireSuisse adresse = new AdresseMandataireSuisse();
					adresse.setCodeGenreImpot(view.getCodeGenreImpot());
					adresse.setComplement(view.getAdresse().getComplements());
					adresse.setDateDebut(view.getDateDebut());
					adresse.setDateFin(view.getDateFin());
					adresse.setCivilite(view.getCivilite());
					adresse.setNomDestinataire(view.getRaisonSociale());
					adresse.setNoTelephoneContact(view.getNoTelContact());
					adresse.setNpaCasePostale(view.getAdresse().getNpaCasePostale());
					adresse.setNumeroCasePostale(view.getAdresse().getNumeroCasePostale());
					adresse.setNumeroMaison(view.getAdresse().getNumeroMaison());
					adresse.setNumeroOrdrePoste(Integer.parseInt(view.getAdresse().getNumeroOrdrePoste()));
					adresse.setNumeroRue(view.getAdresse().getNumeroRue());
					adresse.setPersonneContact(view.getPersonneContact());
					adresse.setRue(view.getAdresse().getRue());
					adresse.setTexteCasePostale(view.getAdresse().getTexteCasePostale());
					adresse.setTypeMandat(view.getTypeMandat());
					adresse.setWithCopy(view.isWithCopy());

					tiersService.addMandat(mandant, adresse);
				}
			}
		});
	}

	@RequestMapping(value = "/courrier/ajouter-mandataire-choisi.do", method = RequestMethod.GET)
	public String addCourrierAvecTiersMandataire(Model model, @RequestParam(ID_MANDANT) long idMandant, @RequestParam(ID_MANDATAIRE) long idMandataire) {
		fillModelAvecDonneesAjoutMandatCourrier(model, new AddMandatView(idMandant, idMandataire), getAccesMandatairesView(idMandant));
		return "tiers/edition/mandataire/add-avec-referent";
	}

	@RequestMapping(value = "/perception/ajouter-mandataire-choisi.do", method = RequestMethod.GET)
	public String addPerceptionAvecTiersMandataire(Model model, @RequestParam(ID_MANDANT) long idMandant, @RequestParam(ID_MANDATAIRE) long idMandataire) {
		fillModelAvecDonneesAjoutMandatPerception(model, new AddMandatView(idMandant, idMandataire), getAccesMandatairesView(idMandant));
		return "tiers/edition/mandataire/add-avec-referent";
	}

	@RequestMapping(value = "/courrier/ajouter-tiers-mandataire.do", method = RequestMethod.POST)
	public String addMandatCourrierSurTiersIdentifie(@Valid @ModelAttribute(value = DONNEES_MANDAT) AddMandatView view,
	                                                 BindingResult bindingResult,
	                                                 Model model) {
		checkDroitAccesMandatairesCourrier();
		if (bindingResult.hasErrors()) {
			fillModelAvecDonneesAjoutMandatCourrier(model, view, getAccesMandatairesView(view.getIdMandant()));
			return "tiers/edition/mandataire/add-avec-referent";
		}

		// mode courrier... on ne devrait pas être ici avec un type de mandat tiers
		if (view.getTypeMandat() == TypeMandat.TIERS) {
			throw new ActionException("Type de mandat invalide !");
		}

		// pas d'identifiant de mandataire... on ne devrait pas être ici
		if (view.getIdTiersMandataire() == null) {
			throw new ActionException("Identifiant du tiers mandataire absent !");
		}

		ajouterMandat(view);
		return "redirect:/mandataire/courrier/edit-list.do?ctbId=" + view.getIdMandant();
	}

	@RequestMapping(value = "/perception/ajouter-tiers-mandataire.do", method = RequestMethod.POST)
	public String addMandatPerceptionSurTiersIdentifie(@Valid @ModelAttribute(value = DONNEES_MANDAT) AddMandatView view,
	                                                   BindingResult bindingResult,
	                                                   Model model) {
		checkDroitAccesMandatairesPerception();
		if (bindingResult.hasErrors()) {
			fillModelAvecDonneesAjoutMandatPerception(model, view, getAccesMandatairesView(view.getIdMandant()));
			return "tiers/edition/mandataire/add-avec-referent";
		}

		// mode perception... on ne devrait pas être ici avec un type de mandat autre que tiers
		if (view.getTypeMandat() != TypeMandat.TIERS) {
			throw new ActionException("Type de mandat invalide !");
		}

		// pas d'identifiant de mandataire... on ne devrait pas être ici
		if (view.getIdTiersMandataire() == null) {
			throw new ActionException("Identifiant du tiers mandataire absent !");
		}

		ajouterMandat(view);
		return "redirect:/mandataire/perception/edit-list.do?ctbId=" + view.getIdMandant();
	}

	@RequestMapping(value = "/annuler-mandat.do", method = RequestMethod.POST)
	public String annulerMandat(@RequestParam("idMandat") final long idMandat) {
		return transactionHelper.doInTransaction(false, status -> {
			final Mandat mandat = hibernateTemplate.get(Mandat.class, idMandat);
			if (mandat == null) {
				throw new ObjectNotFoundException("Aucun mandat ne correspond à l'identifiant " + idMandat);
			}

			// identifiant du mandant
			final long idMandant = mandat.getSujetId();

			// si mandat déjà annulé, on ne change rien
			if (!mandat.isAnnule()) {
				checkDroitAccesMandat(mandat.getTypeMandat());
				checkAccessDossierEnEcriture(idMandant);

				// on annule le mandat...
				mandat.setAnnule(true);
			}

			if (mandat.getTypeMandat() == TypeMandat.TIERS) {
				return "redirect:/mandataire/perception/edit-list.do?ctbId=" + idMandant;
			}
			else {
				return "redirect:/mandataire/courrier/edit-list.do?ctbId=" + idMandant;
			}
		});
	}

	@RequestMapping(value = "/annuler-adresse.do", method = RequestMethod.POST)
	public String annulerAdresse(@RequestParam("idAdresse") final long idAdresse) {
		return transactionHelper.doInTransaction(false, status -> {
			final AdresseMandataire mandat = hibernateTemplate.get(AdresseMandataire.class, idAdresse);
			if (mandat == null) {
				throw new ObjectNotFoundException("Aucune adresse mandataire ne correspond à l'identifiant " + idAdresse);
			}

			final Contribuable mandant = mandat.getMandant();

			// si mandat déjà annulé, on ne change rien
			if (!mandat.isAnnule()) {
				checkDroitAccesMandat(mandat.getTypeMandat());
				checkAccessDossierEnEcriture(mandant.getNumero());

				// on annule le mandat...
				mandat.setAnnule(true);
			}

			if (mandat.getTypeMandat() == TypeMandat.TIERS) {
				return "redirect:/mandataire/perception/edit-list.do?ctbId=" + mandant.getNumero();
			}
			else {
				return "redirect:/mandataire/courrier/edit-list.do?ctbId=" + mandant.getNumero();
			}
		});
	}

	@RequestMapping(value = "/editer-mandat.do", method = RequestMethod.GET)
	public String showEditerMandat(final Model model, @RequestParam("idMandat") final long idMandat) {
		return transactionHelper.doInTransaction(true, status -> {
			final Mandat mandat = hibernateTemplate.get(Mandat.class, idMandat);
			if (mandat == null) {
				throw new ObjectNotFoundException("Aucun mandat trouvé avec l'identifiant " + idMandat);
			}

			checkDroitAccesMandat(mandat.getTypeMandat());
			checkAccessDossierEnEcriture(mandat.getSujetId());

			return showEditerMandat(model, mandat, null);
		});
	}

	private String showEditerMandat(Model model, Mandat mandat, EditMandatView view) {
		// un peu de remplissage pour la vue d'édition du mandat
		model.addAttribute(ID_MANDANT, mandat.getSujetId());
		model.addAttribute(MANDAT, new MandatView(mandat, infraService));
		model.addAttribute(MANDAT_EDIT, view != null ? view : new EditMandatView(mandat));
		model.addAttribute(MODE, mandat.getTypeMandat() == TypeMandat.TIERS ? MODE_PERCEPTION : MODE_COURRIER);
		return "tiers/edition/mandataire/edit";
	}

	@RequestMapping(value = "/editer-mandat.do", method = RequestMethod.POST)
	public String doEditerMandat(@Valid @ModelAttribute(MANDAT_EDIT) final EditMandatView view, final BindingResult bindingResult, final Model model) {
		return transactionHelper.doInTransaction(false, status -> {
			final Mandat mandat = hibernateTemplate.get(Mandat.class, view.getIdMandat());
			if (mandat == null) {
				throw new ObjectNotFoundException("Aucun mandat trouvé avec l'identifiant " + view.getIdMandat());
			}

			if (bindingResult.hasErrors()) {
				return showEditerMandat(model, mandat, view);
			}

			checkDroitAccesMandat(mandat.getTypeMandat());
			checkAccessDossierEnEcriture(mandat.getSujetId());

			if (mandat.isAnnule()) {
				throw new ActionException("Le mandat est annulé, il n'est plus modifiable.");
			}

			final Contribuable mandant = hibernateTemplate.get(Contribuable.class, mandat.getSujetId());
			if (mandant == null) {
				throw new TiersNotFoundException(mandat.getSujetId());
			}

			// y a-t-il un autre changement que l'apparition de la date de fin ?
			final boolean dateFinApparue = mandat.getDateFin() == null && view.getDateFin() != null;
			final boolean donneesModifiees;
			if (mandat.getTypeMandat() == TypeMandat.TIERS) {
				// seuls la date de fin et l'IBAN peuvent être modifiés
				final String oldIban = mandat.getCompteBancaire() != null ? IbanHelper.normalize(mandat.getCompteBancaire().getIban()) : null;
				final String newIban = IbanHelper.normalize(view.getIban());
				donneesModifiees = !Objects.equals(oldIban, newIban);
			}
			else {
				// la date de fin, les coordonnées de contact et le flag de copie des courriers peuvent être modifiés
				final boolean copyFlagModifie = (mandat.getWithCopy() != null && mandat.getWithCopy()) != view.isWithCopy();
				final boolean personneContactModifiee = !Objects.equals(mandat.getPersonneContact(), view.getPersonneContact());
				final boolean noTelContactModifie = !Objects.equals(mandat.getNoTelephoneContact(), view.getNoTelContact());
				donneesModifiees = copyFlagModifie || personneContactModifiee || noTelContactModifie;
			}

			// si les données ont été modifiées, on procède par annulation du mandat précedent et ré-ouverture d'un autre
			if (donneesModifiees) {
				final Mandat copy = (Mandat) mandat.duplicate();
				mandat.setAnnule(true);
				if (dateFinApparue) {
					copy.setDateFin(view.getDateFin());
				}
				if (copy.getTypeMandat() == TypeMandat.TIERS) {
					copy.setCompteBancaire(new CompteBancaire(IbanHelper.normalize(view.getIban()), null));
				}
				else {
					copy.setWithCopy(view.isWithCopy());
					copy.setPersonneContact(view.getPersonneContact());
					copy.setNoTelephoneContact(view.getNoTelContact());
				}
				tiersService.addMandat(mandant, copy);
			}
			else if (dateFinApparue) {
				mandat.setDateFin(view.getDateFin());
			}

			if (mandat.getTypeMandat() == TypeMandat.TIERS) {
				return "redirect:/mandataire/perception/edit-list.do?ctbId=" + mandant.getNumero();
			}
			else {
				return "redirect:/mandataire/courrier/edit-list.do?ctbId=" + mandant.getNumero();
			}
		});
	}

	@RequestMapping(value = "/editer-adresse.do", method = RequestMethod.GET)
	public String showEditerAdresseMandataire(final Model model, @RequestParam("idAdresse") final long idAdresse) {
		return transactionHelper.doInTransaction(true, status -> {
			final AdresseMandataire adresse = hibernateTemplate.get(AdresseMandataire.class, idAdresse);
			if (adresse == null) {
				throw new ObjectNotFoundException("Aucune adresse mandataire trouvée avec l'identifiant " + idAdresse);
			}

			checkDroitAccesMandat(adresse.getTypeMandat());
			checkAccessDossierEnEcriture(adresse.getMandant().getNumero());

			return showEditerAdresseMandataire(model, adresse, null);
		});
	}

	private String showEditerAdresseMandataire(Model model, AdresseMandataire adresse, EditMandatView view) {
		// un peu de remplissage pour la vue d'édition du mandat
		model.addAttribute(ID_MANDANT, adresse.getMandant().getId());
		model.addAttribute(MANDAT, new MandatView(adresse, infraService));
		model.addAttribute(MANDAT_EDIT, view != null ? view : new EditMandatView(adresse));
		model.addAttribute(MODE, adresse.getTypeMandat() == TypeMandat.TIERS ? MODE_PERCEPTION : MODE_COURRIER);
		return "tiers/edition/mandataire/edit";
	}

	@RequestMapping(value = "/editer-adresse.do", method = RequestMethod.POST)
	public String doEditerAdresseMandataire(@Valid @ModelAttribute(MANDAT_EDIT) final EditMandatView view, final BindingResult bindingResult, final Model model) {
		return transactionHelper.doInTransaction(false, status -> {
			final AdresseMandataire adresse = hibernateTemplate.get(AdresseMandataire.class, view.getIdAdresse());
			if (adresse == null) {
				throw new ObjectNotFoundException("Aucune adresse mandataire trouvée avec l'identifiant " + view.getIdAdresse());
			}

			if (bindingResult.hasErrors()) {
				return showEditerAdresseMandataire(model, adresse, view);
			}

			final Contribuable mandant = adresse.getMandant();
			checkDroitAccesMandat(adresse.getTypeMandat());
			checkAccessDossierEnEcriture(mandant.getNumero());

			if (adresse.isAnnule()) {
				throw new ActionException("L'adresse mandataire est annulée, elle n'est plus modifiable.");
			}

			// y a-t-il un autre changement que l'apparition de la date de fin ?
			final boolean dateFinApparue = adresse.getDateFin() == null && view.getDateFin() != null;

			// la date de fin, les coordonnées de contact et le flag de copie des courriers peuvent être modifiés
			final boolean copyFlagModifie = adresse.isWithCopy() != view.isWithCopy();
			final boolean personneContactModifiee = !Objects.equals(adresse.getPersonneContact(), view.getPersonneContact());
			final boolean noTelContactModifie = !Objects.equals(adresse.getNoTelephoneContact(), view.getNoTelContact());
			final boolean donneesModifiees = copyFlagModifie || personneContactModifiee || noTelContactModifie;

			// si les données ont été modifiées, on procède par annulation du mandat précedent et ré-ouverture d'un autre
			if (donneesModifiees) {
				final AdresseMandataire copy = adresse.duplicate();
				adresse.setAnnule(true);
				if (dateFinApparue) {
					copy.setDateFin(view.getDateFin());
				}
				copy.setWithCopy(view.isWithCopy());
				copy.setPersonneContact(view.getPersonneContact());
				copy.setNoTelephoneContact(view.getNoTelContact());
				tiersService.addMandat(mandant, copy);
			}
			else if (dateFinApparue) {
				adresse.setDateFin(view.getDateFin());
			}

			return "redirect:/mandataire/courrier/edit-list.do?ctbId=" + mandant.getNumero();
		});
	}

}
