package ch.vd.unireg.tiers;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.common.DelegatingValidator;
import ch.vd.unireg.indexer.EmptySearchCriteriaException;
import ch.vd.unireg.indexer.IndexerException;
import ch.vd.unireg.indexer.TooManyResultsIndexerException;
import ch.vd.unireg.param.manager.ParamApplicationManager;
import ch.vd.unireg.param.validator.ParamApplicationValidator;
import ch.vd.unireg.param.view.ParamApplicationView;
import ch.vd.unireg.security.AccessDeniedException;
import ch.vd.unireg.security.Role;
import ch.vd.unireg.security.SecurityHelper;
import ch.vd.unireg.security.SecurityProviderInterface;
import ch.vd.unireg.tiers.validator.TiersCriteriaValidator;
import ch.vd.unireg.tiers.view.TiersCriteriaView;
import ch.vd.unireg.utils.RegDateEditor;
import ch.vd.unireg.utils.WebContextUtils;

@Controller
public class TiersListController implements MessageSourceAware {

	private static final Logger LOGGER = LoggerFactory.getLogger(TiersListController.class);

	private TiersSearchHelper helper;
	private SecurityProviderInterface securityProvider;
	private ParamApplicationManager paramApplicationManager;
	private MessageSource messageSource;

	public static final String NUMERO_PARAMETER_NAME = "numero";
	public static final String NOM_RAISON_PARAMETER_NAME = "nomRaison";
	public static final String LOCALITE_OU_PAYS_PARAMETER_NAME = "localiteOuPays";
	public static final String NO_OFS_FOR_PARAMETER_NAME = "noOfsFor";
	public static final String DATE_NAISSANCE_PARAMETER_NAME = "dateNaissance";
	public static final String NUMERO_ASSURE_SOCIAL_PARAMETER_NAME = "numeroAssureSocial";
	public static final String TYPE_RECHERCHE_PARAMETER_NAME = "typeRecherche";
	public static final String URL_RETOUR_PARAMETER_NAME = "urlRetour";
	public static final String FOR_PRINCIPAL_ACTIF_PARAMETER_NAME = "forPrincipalActif";

	/**
	 * Le nom de l'attribut utilise pour l'objet stockant les paramètres de l'application
	 */
	private static final String PARAMETRES_APP = "parametresApp";

	/**
	 * Le nom de l'objet de criteres de recherche stocké en session
	 */
	public static final String TIERS_CRITERIA_SESSION_NAME = "tiersCriteria";

	/**
	 * Le nom de l'URL de retour stockée en session
	 */
	public static final String URL_RETOUR_SESSION_NAME = "urlRetour";

	/**
	 * Le nom de l'attribut du modèle pour les résultats de recherche
	 */
	public static final String TIERS_LIST_ATTRIBUTE_NAME = "list";

	/**
	 * Le nom de l'attrbut du modèle pour les messages d'erreur
	 */
	public static final String ERROR_MESSAGE_ATTRIBUTE_NAME = "errorMessage";

	private static final String COMMAND_NAME = "command";

	public void setHelper(TiersSearchHelper helper) {
		this.helper = helper;
	}

	public void setSecurityProvider(SecurityProviderInterface securityProvider) {
		this.securityProvider = securityProvider;
	}

	public void setParamApplicationManager(ParamApplicationManager paramApplicationManager) {
		this.paramApplicationManager = paramApplicationManager;
	}

	@Override
	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	private static final class Validator extends DelegatingValidator {
		private Validator() {
			addSubValidator(TiersCriteriaView.class, new TiersCriteriaValidator());
			addSubValidator(ParamApplicationView.class, new ParamApplicationValidator());
		}
	}

	@InitBinder
	protected void initBinder(WebDataBinder binder) {
		binder.setValidator(new Validator());
		binder.registerCustomEditor(RegDate.class, new RegDateEditor(true, true, false, RegDateHelper.StringFormat.DISPLAY));
	}

	@RequestMapping(value = "/tiers/list.do", method = RequestMethod.GET)
	public String showSearchForm(Model model, HttpSession session,
	                             @RequestParam(value = NUMERO_PARAMETER_NAME, required = false) String numeroParam,
	                             @RequestParam(value = NOM_RAISON_PARAMETER_NAME, required = false) String nomRaisonParam,
	                             @RequestParam(value = LOCALITE_OU_PAYS_PARAMETER_NAME, required = false) String localitePaysParam,
	                             @RequestParam(value = NO_OFS_FOR_PARAMETER_NAME, required = false) String noOfsForParam,
	                             @RequestParam(value = DATE_NAISSANCE_PARAMETER_NAME, required = false) String dateNaissanceParam,
	                             @RequestParam(value = NUMERO_ASSURE_SOCIAL_PARAMETER_NAME, required = false) String numeroAssureSocialParam,
	                             @RequestParam(value = TYPE_RECHERCHE_PARAMETER_NAME, required = false) TiersCriteria.TypeTiers typeTiers,
	                             @RequestParam(value = URL_RETOUR_PARAMETER_NAME, required = false) String urlRetour,
	                             @RequestParam(value = FOR_PRINCIPAL_ACTIF_PARAMETER_NAME, required = false, defaultValue = "false") boolean seulementForPrincipalActif) throws Exception {

		if (!SecurityHelper.isAnyGranted(securityProvider, Role.VISU_ALL, Role.VISU_LIMITE)) {
			throw new AccessDeniedException("vous ne possédez pas les droits de consultation pour l'application Unireg");
		}

		RegDate dateNaissance = null;
		if (!StringUtils.isBlank(dateNaissanceParam)) {
			dateNaissance = RegDateHelper.displayStringToRegDate(dateNaissanceParam.trim(), true);
		}

		if (StringUtils.isNotBlank(urlRetour)) {
			session.setAttribute(URL_RETOUR_SESSION_NAME, StringUtils.trim(urlRetour));

			// [SIFISC-11341] le comportement avant SIFISC-10049 était qu'en cas de urlRetour demandée, le contenu stocké en session était complètement écrasé par les critères entrants
			// -> il faut bien-sûr retrouver ce comportement afin que les appels depuis l'extérieurs ne se réduisent pas tous à leur première instance...
			session.removeAttribute(TIERS_CRITERIA_SESSION_NAME);
		}

		final TiersCriteria.TypeVisualisation typeVisualisation = SecurityHelper.isGranted(securityProvider, Role.VISU_ALL) ? TiersCriteria.TypeVisualisation.COMPLETE : TiersCriteria.TypeVisualisation.LIMITEE;
		final TiersCriteriaView criteria = helper.getCriteria(session, TIERS_CRITERIA_SESSION_NAME, numeroParam, nomRaisonParam, localitePaysParam, noOfsForParam, dateNaissance,
		                                                      numeroAssureSocialParam, typeTiers, seulementForPrincipalActif, typeVisualisation);
		session.setAttribute(TIERS_CRITERIA_SESSION_NAME, criteria);

		List<TiersIndexedDataView> result = null;
		try {
			 result = helper.search(criteria);
		}
		catch (TooManyResultsIndexerException e) {
			if (e.getNbResults() > 0) {
				model.addAttribute(ERROR_MESSAGE_ATTRIBUTE_NAME, getMessage("error.preciser.recherche.trouves", String.valueOf(e.getNbResults())));
			}
			else {
				model.addAttribute(ERROR_MESSAGE_ATTRIBUTE_NAME, getMessage("error.preciser.recherche"));
			}
		}
		catch (EmptySearchCriteriaException e) {
			// rien de spécial...
		}
		catch (IndexerException e) {
			LOGGER.error("Exception levée dans l'indexeur", e);
			model.addAttribute(ERROR_MESSAGE_ATTRIBUTE_NAME, getMessage("error.recherche"));
		}
		return show(model, session, criteria, result);
	}

	private String getMessage(String key, Object... args) {
		return messageSource.getMessage(key, args, WebContextUtils.getDefaultLocale());
	}

	private String show(Model model, HttpSession session, TiersCriteriaView criteria, @Nullable List<TiersIndexedDataView> results) {
		helper.fillModelValuesForCriteria(model, COMMAND_NAME, criteria);
		model.addAttribute(URL_RETOUR_SESSION_NAME, session.getAttribute(URL_RETOUR_SESSION_NAME));
		model.addAttribute(TIERS_LIST_ATTRIBUTE_NAME, results);
		model.addAttribute(PARAMETRES_APP, paramApplicationManager.getForm());
		return "tiers/recherche/list";
	}

	@RequestMapping(value = "/tiers/list.do", method = RequestMethod.POST)
	public String search(Model model, HttpSession session, @Valid @ModelAttribute(COMMAND_NAME) TiersCriteriaView criteria, BindingResult bindingResult) {
		if (bindingResult.hasErrors()) {
			return show(model, session, criteria, null);
		}
		session.setAttribute(TIERS_CRITERIA_SESSION_NAME, criteria);
		return "redirect:/tiers/list.do";
	}

	@RequestMapping(value = "/tiers/reset-search.do", method = RequestMethod.GET)
	public String effacerCriteres(HttpSession session) throws Exception {
		session.removeAttribute(TIERS_CRITERIA_SESSION_NAME);
		return "redirect:/tiers/list.do";
	}
}
