package ch.vd.uniregctb.mandataire;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.collections4.Closure;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.collections4.PredicateUtils;
import org.apache.commons.collections4.Transformer;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
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
import ch.vd.unireg.common.NomPrenom;
import ch.vd.unireg.interfaces.infra.data.GenreImpotMandataire;
import ch.vd.uniregctb.adresse.AdresseEnvoiDetaillee;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.AdresseMandataire;
import ch.vd.uniregctb.adresse.AdresseMandataireAdapter;
import ch.vd.uniregctb.adresse.AdresseMandataireSuisse;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.adresse.TypeAdresseFiscale;
import ch.vd.uniregctb.common.ActionException;
import ch.vd.uniregctb.common.ControllerUtils;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.common.TiersNotFoundException;
import ch.vd.uniregctb.entreprise.complexe.SearchTiersComponent;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.iban.IbanHelper;
import ch.vd.uniregctb.iban.IbanValidator;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityHelper;
import ch.vd.uniregctb.security.SecurityProviderInterface;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.CoordonneesFinancieres;
import ch.vd.uniregctb.tiers.Mandat;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersCriteria;
import ch.vd.uniregctb.tiers.TiersMapHelper;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.tiers.validator.TiersCriteriaValidator;
import ch.vd.uniregctb.tiers.view.TiersCriteriaView;
import ch.vd.uniregctb.transaction.TransactionHelper;
import ch.vd.uniregctb.type.TypeMandat;
import ch.vd.uniregctb.utils.RegDateEditor;

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

	@Override
	public void afterPropertiesSet() throws Exception {
		this.searchTiersComponent = new SearchTiersComponent(tiersService, messageSource, tiersMapHelper, "AddMandataireCriteria", "tiers/edition/mandataire/add", new SearchTiersComponent.TiersCriteriaFiller() {
			@Override
			public void fill(TiersCriteriaView criteria) {
				criteria.setTypesTiersImperatifs(EnumSet.of(TiersCriteria.TypeTiers.PERSONNE_PHYSIQUE, TiersCriteria.TypeTiers.ENTREPRISE, TiersCriteria.TypeTiers.ETABLISSEMENT_SECONDAIRE));
			}
		});
	}

	@InitBinder(value = SearchTiersComponent.COMMAND)
	protected void initCommandBinder(WebDataBinder binder) {
		binder.setValidator(new TiersCriteriaValidator());
		binder.registerCustomEditor(RegDate.class, new RegDateEditor(true, true, false, RegDateHelper.StringFormat.DISPLAY));
	}

	@InitBinder(value = DONNEES_MANDAT)
	protected void initDonneesMandatBinder(WebDataBinder binder) {
		binder.setValidator(new AddMandatViewValidator(ibanValidator, buildSetOfCodesGenreImpot()));
		binder.registerCustomEditor(RegDate.class, new RegDateEditor(true, true, false, RegDateHelper.StringFormat.DISPLAY));
	}

	@InitBinder(value = MANDAT_EDIT)
	protected void initEditMandatBinder(WebDataBinder binder) {
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

	@Nullable
	private static String buildNomPrenomPersonneContact(String prenom, String nom) {
		final NomPrenom nomPrenom = new NomPrenom(nom, prenom);
		return StringUtils.trimToNull(nomPrenom.getNomPrenom());
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
		return transactionHelper.doInTransaction(true, new TransactionCallback<AdresseMandatView>() {
			@Override
			public AdresseMandatView doInTransaction(TransactionStatus status) {
				final Mandat mandat = hibernateTemplate.get(Mandat.class, idMandat);
				if (mandat == null || (mandat.getTypeMandat() != TypeMandat.GENERAL && mandat.getTypeMandat() != TypeMandat.SPECIAL)) {
					return null;
				}

				final String nomPrenomContact = buildNomPrenomPersonneContact(mandat.getPrenomPersonneContact(), mandat.getNomPersonneContact());
				final String noTelContact = mandat.getNoTelephoneContact();

				final Tiers mandataire;
				try {
					mandataire = getMandataire(mandat);
				}
				catch (TiersNotFoundException e) {
					return new AdresseMandatView(nomPrenomContact, noTelContact, (List<String>) null);
				}

				try {
					final AdresseEnvoiDetaillee adresse = adresseService.getAdresseEnvoi(mandataire, mandat.getDateFin(), TypeAdresseFiscale.REPRESENTATION, false);
					return new AdresseMandatView(nomPrenomContact, noTelContact, Arrays.asList(adresse.getLignes().asTexte()));
				}
				catch (AdresseException e) {
					LOGGER.error("Problème à la détermination de l'adresse de représentation du tiers " + mandataire.getNumero(), e);
					return new AdresseMandatView(nomPrenomContact, noTelContact, e.getMessage());
				}
			}
		});
	}

	@RequestMapping(value = "/adresse-representation.do", method = RequestMethod.GET)
	@ResponseBody
	public LignesAdressesView getAdresseRepresentationTiers(@RequestParam("idTiers") final long idTiers) {
		return transactionHelper.doInTransaction(true, new TransactionCallback<LignesAdressesView>() {
			@Override
			public LignesAdressesView doInTransaction(TransactionStatus status) {
				final Tiers tiers = hibernateTemplate.get(Tiers.class, idTiers);
				if (tiers == null) {
					return null;
				}

				try {
					final AdresseEnvoiDetaillee adresse = adresseService.getAdresseEnvoi(tiers, null, TypeAdresseFiscale.REPRESENTATION, false);
					return new LignesAdressesView(Arrays.asList(adresse.getLignes().asTexte()));
				}
				catch (AdresseException e) {
					LOGGER.error("Problème à la détermination de l'adresse de représentation du tiers " + tiers.getNumero(), e);
					return null;
				}
			}
		});
	}

	@RequestMapping(value = "/adresse-mandataire.do", method = RequestMethod.GET)
	@ResponseBody
	public AdresseMandatView getAdresseMandataire(@RequestParam("idAdresse") final long idAdresse) {
		return transactionHelper.doInTransaction(true, new TransactionCallback<AdresseMandatView>() {
			@Override
			public AdresseMandatView doInTransaction(TransactionStatus status) {
				final AdresseMandataire mandat = hibernateTemplate.get(AdresseMandataire.class, idAdresse);
				if (mandat == null) {
					return null;
				}

				final String nomPrenomContact = buildNomPrenomPersonneContact(mandat.getPrenomPersonneContact(), mandat.getNomPersonneContact());
				final String noTelContact = mandat.getNoTelephoneContact();

				try {
					final AdresseMandataireAdapter adapter = new AdresseMandataireAdapter(mandat, infraService);
					final AdresseEnvoiDetaillee adresse = adresseService.buildAdresseEnvoi(adapter.getSource().getTiers(), adapter, mandat.getDateFin());
					return new AdresseMandatView(nomPrenomContact, noTelContact, Arrays.asList(adresse.getLignes().asTexte()));
				}
				catch (AdresseException e) {
					LOGGER.error("Problème à la construction de l'adresse mandataire " + mandat.getId(), e);
					return new AdresseMandatView(nomPrenomContact, noTelContact, e.getMessage());
				}
			}
		});
	}

	private static <T, U> void forEach(@Nullable Collection<T> source,
	                                   Transformer<? super T, ? extends U> mapper,
	                                   Closure<? super U> job) {

		if (source != null && !source.isEmpty()) {
			for (T elt : source) {
				final U transformee = mapper.transform(elt);
				if (transformee != null) {
					job.execute(transformee);
				}
			}
		}
	}

	private static <T> void forEach(@Nullable Collection<T> source, final Predicate<? super T> filter, Closure<? super T> job) {
		forEach(source, new Transformer<T, T>() {
			@Override
			public T transform(T input) {
				return filter.evaluate(input) ? input : null;
			}
		}, job);
	}

	private static void doForAllMandatsOfType(final Set<TypeMandat> targetTypes, Contribuable mandant, Closure<? super Mandat> job) {
		final Set<RapportEntreTiers> rets = mandant.getRapportsSujet();
		if (rets != null && !rets.isEmpty()) {
			final Transformer<RapportEntreTiers, Mandat> transf = new Transformer<RapportEntreTiers, Mandat>() {
				@Override
				public Mandat transform(RapportEntreTiers input) {
					return input instanceof Mandat && targetTypes.contains(((Mandat) input).getTypeMandat()) ? (Mandat) input : null;
				}
			};
			forEach(rets, transf, job);
		}
	}

	private static void doForAllAdressesMandataires(Contribuable mandant, Closure<? super AdresseMandataire> job) {
		final Set<AdresseMandataire> adresses = mandant.getAdressesMandataires();
		if (adresses != null && !adresses.isEmpty()) {
			forEach(adresses, PredicateUtils.<AdresseMandataire>truePredicate(), job);
		}
	}

	@RequestMapping(value = "/courrier/edit-list.do", method = RequestMethod.GET)
	public String editMandatairesCourrier(final Model model, @RequestParam("ctbId") final long idMandant) {
		checkDroitAccesMandatairesCourrier();
		return transactionHelper.doInTransaction(true, new TransactionCallback<String>() {
			@Override
			public String doInTransaction(TransactionStatus status) {
				checkAccessDossierEnEcriture(idMandant);

				final Contribuable mandant = hibernateTemplate.get(Contribuable.class, idMandant);
				if (mandant == null) {
					throw new TiersNotFoundException(idMandant);
				}

				final Map<TypeMandat, Boolean> modifiableMap = buildModifiableMap();

				final List<MandataireCourrierEditView> mandats = new LinkedList<>();
				doForAllMandatsOfType(EnumSet.of(TypeMandat.GENERAL, TypeMandat.SPECIAL), mandant, new Closure<Mandat>() {
					@Override
					public void execute(Mandat mandat) {
						mandats.add(new MandataireCourrierEditView(mandat, tiersService, infraService, modifiableMap.get(mandat.getTypeMandat())));
					}
				});
				doForAllAdressesMandataires(mandant, new Closure<AdresseMandataire>() {
					@Override
					public void execute(AdresseMandataire adresse) {
						mandats.add(new MandataireCourrierEditView(adresse, infraService, modifiableMap.get(adresse.getTypeMandat())));
					}
				});
				Collections.sort(mandats, MandataireViewHelper.COURRIER_COMPARATOR);

				model.addAttribute(MANDATS, mandats);
				model.addAttribute(ID_MANDANT, idMandant);
				return "tiers/edition/mandataire/courrier";
			}
		});
	}

	@RequestMapping(value = "/perception/edit-list.do", method = RequestMethod.GET)
	public String editMandatairesPerception(final Model model, @RequestParam("ctbId") final long idMandant) {
		checkDroitAccesMandatairesPerception();
		return transactionHelper.doInTransaction(true, new TransactionCallback<String>() {
			@Override
			public String doInTransaction(TransactionStatus status) {
				checkAccessDossierEnEcriture(idMandant);

				final Contribuable mandant = hibernateTemplate.get(Contribuable.class, idMandant);
				if (mandant == null) {
					throw new TiersNotFoundException(idMandant);
				}

				final Map<TypeMandat, Boolean> modifiableMap = buildModifiableMap();

				final List<MandatairePerceptionEditView> mandats = new LinkedList<>();
				doForAllMandatsOfType(EnumSet.of(TypeMandat.TIERS), mandant, new Closure<Mandat>() {
					@Override
					public void execute(Mandat mandat) {
						mandats.add(new MandatairePerceptionEditView(mandat, tiersService, modifiableMap.get(mandat.getTypeMandat())));
					}
				});
				Collections.sort(mandats, MandataireViewHelper.BASIC_COMPARATOR);

				model.addAttribute(MANDATS, mandats);
				model.addAttribute(ID_MANDANT, idMandant);
				return "tiers/edition/mandataire/perception";
			}
		});
	}

	@RequestMapping(value = "/courrier/ajouter-list.do", method = RequestMethod.GET)
	public String ajouterMandatCourrier(Model model, HttpSession session, @RequestParam(ID_MANDANT) long idMandant) {
		checkDroitAccesMandatairesCourrier();
		checkAccessDossierEnEcriture(idMandant);
		fillModelAvecDonneesAjoutMandatCourrier(model, new AddMandatView(idMandant));
		return searchTiersComponent.showFormulaireRecherche(model, session);
	}

	private void fillModelAvecDonneesBaseAjoutMandat(Model model, AddMandatView view) {
		model.addAttribute(ID_MANDANT, view.getIdMandant());
		model.addAttribute(ID_MANDATAIRE, view.getIdTiersMandataire());
		model.addAttribute(DONNEES_MANDAT, view);
		model.addAttribute(GENRES_IMPOT_ALLOWED, buildMapGenresImpotMandataire());
		model.addAttribute(TEXTES_CASE_POSTALE_ALLOWED, tiersMapHelper.getMapTexteCasePostale());
	}

	private void fillModelAvecDonneesAjoutMandatCourrier(Model model, AddMandatView view) {
		fillModelAvecDonneesBaseAjoutMandat(model, view);
		model.addAttribute(MODE, MODE_COURRIER);
		model.addAttribute(TYPES_MANDATS_ALLOWED, getTypesMandatAutorises(EnumSet.of(TypeMandat.GENERAL, TypeMandat.SPECIAL)));
	}

	@RequestMapping(value = "/perception/ajouter-list.do", method = RequestMethod.GET)
	public String ajouterMandatPerception(Model model, HttpSession session, @RequestParam(ID_MANDANT) long idMandant) {
		checkDroitAccesMandatairesPerception();
		checkAccessDossierEnEcriture(idMandant);
		fillModelAvecDonneesAjoutMandatPerception(model, new AddMandatView(idMandant));
		return searchTiersComponent.showFormulaireRecherche(model, session);
	}

	private void fillModelAvecDonneesAjoutMandatPerception(Model model, AddMandatView view) {
		fillModelAvecDonneesBaseAjoutMandat(model, view);
		model.addAttribute(MODE, MODE_PERCEPTION);
		model.addAttribute(TYPES_MANDATS_ALLOWED, getTypesMandatAutorises(EnumSet.of(TypeMandat.TIERS)));
	}

	/**
	 * @return Map de clé = code de régime, et valeur = libellé associé (l'itérateur sur la map donne les entrées dans l'ordre alphabétique des libellés
	 */
	private Map<String, String> buildMapGenresImpotMandataire() {
		final List<GenreImpotMandataire> gims = infraService.getGenresImpotMandataires();
		final Map<String, String> map = new HashMap<>(gims.size());
		for (GenreImpotMandataire gim : gims) {
			map.put(gim.getCode(), gim.getLibelle());
		}

		// on va trier par ordre alphabétique des libellés
		final List<Map.Entry<String, String>> flatMap = new ArrayList<>(map.entrySet());
		Collections.sort(flatMap, new Comparator<Map.Entry<String, String>>() {
			@Override
			public int compare(Map.Entry<String, String> o1, Map.Entry<String, String> o2) {
				return o1.getValue().compareTo(o2.getValue());      // ordre alphabétique des valeurs
			}
		});
		final Map<String, String> sortedMap = new LinkedHashMap<>(map.size());
		for (Map.Entry<String, String> entry : flatMap) {
			sortedMap.put(entry.getKey(), entry.getValue());
		}

		return sortedMap;
	}

	@RequestMapping(value = "/courrier/ajouter-list.do", method = RequestMethod.POST)
	public String doSearchTiersMandataireCourrier(@Valid @ModelAttribute(value = SearchTiersComponent.COMMAND) TiersCriteriaView view,
	                                              BindingResult bindingResult,
	                                              HttpSession session,
	                                              Model model,
	                                              @RequestParam(ID_MANDANT) long idMandant) {
		checkDroitAccesMandatairesCourrier();
		fillModelAvecDonneesAjoutMandatCourrier(model, new AddMandatView(idMandant));       // en cas d'erreur dans le formulaire de recherche, il faut ré-afficher la page... correctement
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
		fillModelAvecDonneesAjoutMandatPerception(model, new AddMandatView(idMandant));       // en cas d'erreur dans le formulaire de recherche, il faut ré-afficher la page... correctement
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

	private Map<TypeMandat, String> getTypesMandatAutorises(Set<TypeMandat> typesPossibles) {
		final Map<TypeMandat, Boolean> mapModifiables = buildModifiableMap();
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
			fillModelAvecDonneesAjoutMandatCourrier(model, view);
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
						mandat = Mandat.tiers(view.getDateDebut(), view.getDateFin(), mandant, mandataire, new CoordonneesFinancieres(IbanHelper.normalize(view.getIban()), null));
						break;
					default:
						throw new ActionException("Type de mandat invalide (" + view.getTypeMandat() + ") !");
					}

					if (mandat.getTypeMandat() != TypeMandat.TIERS) {
						mandat.setNomPersonneContact(view.getNomPersonneContact());
						mandat.setNoTelephoneContact(view.getNoTelContact());
						mandat.setPrenomPersonneContact(view.getPrenomPersonneContact());
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
					adresse.setNomDestinataire(view.getRaisonSociale());
					adresse.setNomPersonneContact(view.getNomPersonneContact());
					adresse.setNoTelephoneContact(view.getNoTelContact());
					adresse.setNpaCasePostale(view.getAdresse().getNpaCasePostale());
					adresse.setNumeroCasePostale(view.getAdresse().getNumeroCasePostale());
					adresse.setNumeroMaison(view.getAdresse().getNumeroMaison());
					adresse.setNumeroOrdrePoste(Integer.parseInt(view.getAdresse().getNumeroOrdrePoste()));
					adresse.setNumeroRue(view.getAdresse().getNumeroRue());
					adresse.setPrenomPersonneContact(view.getPrenomPersonneContact());
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
		fillModelAvecDonneesAjoutMandatCourrier(model, new AddMandatView(idMandant, idMandataire));
		return "tiers/edition/mandataire/add-avec-referent";
	}

	@RequestMapping(value = "/perception/ajouter-mandataire-choisi.do", method = RequestMethod.GET)
	public String addPerceptionAvecTiersMandataire(Model model, @RequestParam(ID_MANDANT) long idMandant, @RequestParam(ID_MANDATAIRE) long idMandataire) {
		fillModelAvecDonneesAjoutMandatPerception(model, new AddMandatView(idMandant, idMandataire));
		return "tiers/edition/mandataire/add-avec-referent";
	}

	@RequestMapping(value = "/courrier/ajouter-tiers-mandataire.do", method = RequestMethod.POST)
	public String addMandatCourrierSurTiersIdentifie(@Valid @ModelAttribute(value = DONNEES_MANDAT) AddMandatView view,
	                                                 BindingResult bindingResult,
	                                                 Model model) {
		checkDroitAccesMandatairesCourrier();
		if (bindingResult.hasErrors()) {
			fillModelAvecDonneesAjoutMandatCourrier(model, view);
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
			fillModelAvecDonneesAjoutMandatPerception(model, view);
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
		return transactionHelper.doInTransaction(false, new TransactionCallback<String>() {
			@Override
			public String doInTransaction(TransactionStatus status) {
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
			}
		});
	}

	@RequestMapping(value = "/annuler-adresse.do", method = RequestMethod.POST)
	public String annulerAdresse(@RequestParam("idAdresse") final long idAdresse) {
		return transactionHelper.doInTransaction(false, new TransactionCallback<String>() {
			@Override
			public String doInTransaction(TransactionStatus status) {
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
			}
		});
	}

	@RequestMapping(value = "/editer-mandat.do", method = RequestMethod.GET)
	public String showEditerMandat(final Model model, @RequestParam("idMandat") final long idMandat) {
		return transactionHelper.doInTransaction(true, new TransactionCallback<String>() {
			@Override
			public String doInTransaction(TransactionStatus status) {
				final Mandat mandat = hibernateTemplate.get(Mandat.class, idMandat);
				if (mandat == null) {
					throw new ObjectNotFoundException("Aucun mandat trouvé avec l'identifiant " + idMandat);
				}

				checkDroitAccesMandat(mandat.getTypeMandat());
				checkAccessDossierEnEcriture(mandat.getSujetId());

				return showEditerMandat(model, mandat, null);
			}
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
		return transactionHelper.doInTransaction(false, new TransactionCallback<String>() {
			@Override
			public String doInTransaction(TransactionStatus status) {
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
					final String oldIban = mandat.getCoordonneesFinancieres() != null ? IbanHelper.normalize(mandat.getCoordonneesFinancieres().getIban()) : null;
					final String newIban = IbanHelper.normalize(view.getIban());
					donneesModifiees = !Objects.equals(oldIban, newIban);
				}
				else {
					// la date de fin, les coordonnées de contact et le flag de copie des courriers peuvent être modifiés
					final boolean copyFlagModifie = (mandat.getWithCopy() != null && mandat.getWithCopy()) != view.isWithCopy();
					final boolean prenomContactModifie = !Objects.equals(mandat.getPrenomPersonneContact(), view.getPrenomPersonneContact());
					final boolean nomContactModifie = !Objects.equals(mandat.getNomPersonneContact(), view.getNomPersonneContact());
					final boolean noTelContactModifie = !Objects.equals(mandat.getNoTelephoneContact(), view.getNoTelContact());
					donneesModifiees = copyFlagModifie || prenomContactModifie || nomContactModifie || noTelContactModifie;
				}

				// si les données ont été modifiées, on procède par annulation du mandat précedent et ré-ouverture d'un autre
				if (donneesModifiees) {
					final Mandat copy = (Mandat) mandat.duplicate();
					mandat.setAnnule(true);
					if (dateFinApparue) {
						copy.setDateFin(view.getDateFin());
					}
					if (copy.getTypeMandat() == TypeMandat.TIERS) {
						copy.setCoordonneesFinancieres(new CoordonneesFinancieres(IbanHelper.normalize(view.getIban()), null));
					}
					else {
						copy.setWithCopy(view.isWithCopy());
						copy.setNomPersonneContact(view.getNomPersonneContact());
						copy.setPrenomPersonneContact(view.getPrenomPersonneContact());
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
			}
		});
	}

	@RequestMapping(value = "/editer-adresse.do", method = RequestMethod.GET)
	public String showEditerAdresseMandataire(final Model model, @RequestParam("idAdresse") final long idAdresse) {
		return transactionHelper.doInTransaction(true, new TransactionCallback<String>() {
			@Override
			public String doInTransaction(TransactionStatus status) {
				final AdresseMandataire adresse = hibernateTemplate.get(AdresseMandataire.class, idAdresse);
				if (adresse == null) {
					throw new ObjectNotFoundException("Aucune adresse mandataire trouvée avec l'identifiant " + idAdresse);
				}

				checkDroitAccesMandat(adresse.getTypeMandat());
				checkAccessDossierEnEcriture(adresse.getMandant().getNumero());

				return showEditerAdresseMandataire(model, adresse, null);
			}
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
		return transactionHelper.doInTransaction(false, new TransactionCallback<String>() {
			@Override
			public String doInTransaction(TransactionStatus status) {
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
				final boolean prenomContactModifie = !Objects.equals(adresse.getPrenomPersonneContact(), view.getPrenomPersonneContact());
				final boolean nomContactModifie = !Objects.equals(adresse.getNomPersonneContact(), view.getNomPersonneContact());
				final boolean noTelContactModifie = !Objects.equals(adresse.getNoTelephoneContact(), view.getNoTelContact());
				final boolean donneesModifiees = copyFlagModifie || prenomContactModifie || nomContactModifie || noTelContactModifie;

				// si les données ont été modifiées, on procède par annulation du mandat précedent et ré-ouverture d'un autre
				if (donneesModifiees) {
					final AdresseMandataire copy = adresse.duplicate();
					adresse.setAnnule(true);
					if (dateFinApparue) {
						copy.setDateFin(view.getDateFin());
					}
					copy.setWithCopy(view.isWithCopy());
					copy.setNomPersonneContact(view.getNomPersonneContact());
					copy.setPrenomPersonneContact(view.getPrenomPersonneContact());
					copy.setNoTelephoneContact(view.getNoTelContact());
					tiersService.addMandat(mandant, copy);
				}
				else if (dateFinApparue) {
					adresse.setDateFin(view.getDateFin());
				}

				return "redirect:/mandataire/courrier/edit-list.do?ctbId=" + mandant.getNumero();
			}
		});
	}

}
