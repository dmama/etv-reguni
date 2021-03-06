package ch.vd.unireg.supergra;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.beans.PropertyEditor;
import java.math.BigDecimal;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.propertyeditors.CustomBooleanEditor;
import org.springframework.beans.propertyeditors.CustomCollectionEditor;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.beans.propertyeditors.CustomNumberEditor;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.beans.propertyeditors.URLEditor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Validator;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.Flash;
import ch.vd.unireg.common.HibernateEntity;
import ch.vd.unireg.common.HttpHelper;
import ch.vd.unireg.common.UniregLocale;
import ch.vd.unireg.registrefoncier.IdentifiantAffaireRF;
import ch.vd.unireg.registrefoncier.IdentifiantDroitRF;
import ch.vd.unireg.security.AccessDeniedException;
import ch.vd.unireg.security.Role;
import ch.vd.unireg.security.SecurityHelper;
import ch.vd.unireg.security.SecurityProviderInterface;
import ch.vd.unireg.supergra.delta.AddSubEntity;
import ch.vd.unireg.supergra.delta.AttributeUpdate;
import ch.vd.unireg.supergra.delta.Delta;
import ch.vd.unireg.supergra.delta.DisableEntity;
import ch.vd.unireg.supergra.delta.EnableEntity;
import ch.vd.unireg.supergra.delta.RecalcRegroup;
import ch.vd.unireg.supergra.view.AttributeView;
import ch.vd.unireg.supergra.view.CollectionView;
import ch.vd.unireg.supergra.view.EntityView;
import ch.vd.unireg.supergra.view.Mc2PpView;
import ch.vd.unireg.supergra.view.Pp2McView;
import ch.vd.unireg.type.DayMonth;
import ch.vd.unireg.utils.DayMonthEditor;
import ch.vd.unireg.utils.EnumEditor;
import ch.vd.unireg.utils.IdentifiantAffaireRFEditor;
import ch.vd.unireg.utils.IdentifiantDroitRFEditor;
import ch.vd.unireg.utils.IndividuNumberEditor;
import ch.vd.unireg.utils.RegDateEditor;
import ch.vd.unireg.utils.TiersNumberEditor;
import ch.vd.unireg.xml.ExceptionHelper;

@Controller
public class SuperGraController {

	private static final String ACCESS_DENIED = "Cet écran nécessite le droit d'accès spécial Super-Gra !";

	private SuperGraManager manager;
	private SecurityProviderInterface securityProvider;
	private Validator pp2McValidator;
	private Validator mc2PpValidator;

	public void setManager(SuperGraManager manager) {
		this.manager = manager;
	}

	public void setSecurityProvider(SecurityProviderInterface securityProvider) {
		this.securityProvider = securityProvider;
	}

	public void setPp2McValidator(Validator pp2McValidator) {
		this.pp2McValidator = pp2McValidator;
	}

	public void setMc2PpValidator(Validator mc2PpValidator) {
		this.mc2PpValidator = mc2PpValidator;
	}

	@InitBinder(value = "entity")
	protected void initBinder(WebDataBinder binder, HttpServletRequest request) throws ClassNotFoundException {

		// On enregistre les éditeurs standards
		final Locale locale = request.getLocale();
		final SimpleDateFormat sdf = new SimpleDateFormat(DateHelper.DATE_FORMAT_DISPLAY, locale);
		sdf.setLenient(false);
		binder.registerCustomEditor(Date.class, new CustomDateEditor(sdf, true));
		final NumberFormat numberFormat = NumberFormat.getInstance(locale);
		((DecimalFormat) numberFormat).setDecimalFormatSymbols(UniregLocale.SYMBOLS);
		numberFormat.setGroupingUsed(false);
		binder.registerCustomEditor(BigDecimal.class, new CustomNumberEditor(BigDecimal.class, numberFormat, true));
		binder.registerCustomEditor(int.class, new CustomNumberEditor(Integer.class, numberFormat, true));
		binder.registerCustomEditor(Integer.class, new CustomNumberEditor(Integer.class, numberFormat, true));
		binder.registerCustomEditor(long.class, new CustomNumberEditor(Long.class, numberFormat, true));
		binder.registerCustomEditor(Long.class, new CustomNumberEditor(Long.class, numberFormat, true));
		binder.registerCustomEditor(List.class, new CustomCollectionEditor(List.class));
		binder.registerCustomEditor(boolean.class, new CustomBooleanEditor(true));
		binder.registerCustomEditor(Boolean.class, new CustomBooleanEditor(true));
		binder.registerCustomEditor(RegDate.class, new RegDateEditor(true, false, false));
		binder.registerCustomEditor(DayMonth.class, new DayMonthEditor(true, false));
		binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
		binder.registerCustomEditor(URL.class, new URLEditor());
		binder.registerCustomEditor(IdentifiantAffaireRF.class, new IdentifiantAffaireRFEditor(true, false));
		binder.registerCustomEditor(IdentifiantDroitRF.class, new IdentifiantDroitRFEditor(true, false));

		final PropertyEditor partialDateEditor = new RegDateEditor(true, true, false);

		final EntityView view = (EntityView) binder.getTarget();
		if (view.getAttributes() == null) {
			// Lorsqu'un formulaire est soumis, Spring va instancier une nouvelle instance de la vue liée à la requête (voir paramètre de l'annotation @InitBinder)
			// et utiliser cette instance pour binder les paramètres de la requête avec les attributs de la vue. Dans SuperGra, la vue est dynamique : elle dépend
			// complétement de l'instance éditée (une personne physique, un ménage-commun, un for principal, une adresse suisse, ...). Instancier une EntityView
			// ne suffit donc pas, car elle est vide. On va donc chercher la définition de la structure de l'entité sous-jacente, c'est-à-dire les noms et les
			// types des attributs et on met-à-jour la vue créée par Spring.
			final EntityType entityType = EntityType.valueOf(request.getParameter("class"));
			final Long entityId = Long.valueOf(request.getParameter("id"));
			final EntityKey key = new EntityKey(entityType, entityId);
			manager.fillView(key, view, getSession(request)); // les valeurs des attributs vont être mis-à-jour par le binding
		}

		// On enregistre des éditeurs spécialisés sur chacun des attributs de l'entité en donnant le chemin d'accès précis. Ceci permet de faire
		// la traduction Object <-> String correct sur chacun des attributs. Sans ce code, toutes les valeurs entrées dans le formulaire reviendraient
		// sous forme de strings, car Spring ne peut pas deviner quel est le type réel des valeurs 'Object' exposées dans les AttributeView.
		final List<AttributeView> attributes = view.getAttributes();
		if (attributes != null) {
			for (int i = 0; i < attributes.size(); i++) {
				final AttributeView a = view.getAttributes().get(i);
				final Class<?> type = a.getType();

				PropertyEditor editor = binder.findCustomEditor(type, null);
				if (editor == null) {
					if (type.isEnum()) {
						//noinspection unchecked
						editor = new EnumEditor((Class<? extends Enum>) type, true);
					}
					else if (EntityKey.class.isAssignableFrom(type)) {
						editor = new EntityKeyEditor(a.getEntityType(), true);
					}
				}

				// [UNIREG-3188] de toutes les dates de l'application, seule la date de naissance d'une personne physique
				// autorise la donnée d'une date partielle
				if (RegDate.class.equals(type) && "dateNaissance".equals(a.getName())) {
					editor = partialDateEditor;
				}

				if (editor != null) {
					binder.registerCustomEditor(AttributeView.class, "attributes[" + i + "].value", editor);
				}
			}
		}
	}

	@RequestMapping(value = "/supergra/entity/show.do", method = RequestMethod.GET)
	public String showEntity(@RequestParam(value = "class") EntityType type, @RequestParam(value = "id") long id, Model model, HttpServletRequest request) throws
			Exception {

		if (!SecurityHelper.isGranted(securityProvider, Role.SUPERGRA)) {
			throw new AccessDeniedException(ACCESS_DENIED);
		}

		// On récupère la session
		final SuperGraSession session = getSession(request);

		// On recharge toutes les entités de la base de données
		final EntityKey key = new EntityKey(type, id);
		final EntityView view = new EntityView();
		manager.fillView(key, view, session);

		model.addAttribute("entity", view);

		// On mémorise une version de référence pour pouvoir détecter de nouveaux deltas plus tard
		final EntityView referenceView = view.duplicate();
		request.getSession().setAttribute("referenceEntity", referenceView);

		return "supergra/entity";
	}

	@RequestMapping(value = "/supergra/entity/update.do", method = RequestMethod.POST)
	public String updateEntity(@Valid @ModelAttribute("entity") final EntityView view, BindingResult result, HttpServletRequest request) {

		if (!SecurityHelper.isGranted(securityProvider, Role.SUPERGRA)) {
			throw new AccessDeniedException(ACCESS_DENIED);
		}

		if (result.hasErrors()) {
			Flash.error("Une ou plusieurs erreurs de saisie ont été détectées. Aucun changement n'a été enregistré.");
			return "supergra/entity";
		}

		final EntityView referenceView = (EntityView) request.getSession().getAttribute("referenceEntity");
		if (referenceView == null) {
			throw new IllegalArgumentException();
		}

		// On détermine les changements effectués
		final SuperGraSession session = getSession(request);
		final List<AttributeUpdate> newdeltas = referenceView.delta(view);
		session.addDeltas(newdeltas);

		if (newdeltas.isEmpty()) {
			Flash.warning("Aucune différence trouvée. Avez-vous bien changé une valeur ?");
		}
		else {
			final String message;
			if (newdeltas.size() == 1) {
				message = "L'attribut '" + newdeltas.get(0).getName() + "' a été changé.";
			}
			else {
				final StringBuilder list = new StringBuilder();
				for (int i = 0, newdeltasSize = newdeltas.size(); i < newdeltasSize; i++) {
					final AttributeUpdate a = newdeltas.get(i);
					if (i > 0 && i < newdeltasSize - 2) {
						list.append(", ");
					}
					if (i > 0 && i == newdeltasSize - 1) {
						list.append(" et ");
					}
					list.append('\'').append(a.getName()).append('\'');
				}
				message = "Les attributs " + list + " ont été changés.";
			}
			Flash.message(message);
		}

		return "redirect:show.do?id=" + view.getKey().getId() + "&class=" + view.getKey().getType();
	}

	@RequestMapping(value = "/supergra/coll/list.do", method = RequestMethod.GET)
	public String listColl(@RequestParam(value = "class") EntityType type, @RequestParam(value = "id") long id,
	                       @RequestParam(value = "name") String collName, Model model, HttpServletRequest request) throws Exception {

		if (!SecurityHelper.isGranted(securityProvider, Role.SUPERGRA)) {
			throw new AccessDeniedException(ACCESS_DENIED);
		}

		// On récupère la session
		final SuperGraSession session = getSession(request);

		// On recharge toutes les entités de la base de données
		final EntityKey key = new EntityKey(type, id);
		final CollectionView view = new CollectionView();
		manager.fillView(key, collName, view, session);

		model.addAttribute("coll", view);

		return "supergra/coll";
	}

	@RequestMapping(value = "/supergra/coll/add.do", method = RequestMethod.POST)
	public String addEntity(@RequestParam(value = "class") EntityType type, @RequestParam(value = "id") long id,
	                        @RequestParam(value = "name") String collName, @RequestParam(value = "newClass") String newClassAsString,
	                        HttpServletRequest request) throws
			Exception {

		if (!SecurityHelper.isGranted(securityProvider, Role.SUPERGRA)) {
			throw new AccessDeniedException(ACCESS_DENIED);
		}

		// On récupère la session
		final SuperGraSession session = getSession(request);

		// On recharge toutes les entités de la base de données
		final EntityKey key = new EntityKey(type, id);
		final CollectionView view = new CollectionView();
		manager.fillView(key, collName, view, session);

		if (newClassAsString.startsWith("--")) {
			Flash.error("Veuillez sélectionner un type d'élément avant de cliquer le bouton.");
			return "redirect:list.do?id=" + view.getKey().getId() + "&class=" + view.getKey().getType() + "&name=" + collName;
		}

		if (view.isReadonly()) {
			Flash.error("Cette collection est en lecture-seule.");
			return "redirect:list.do?id=" + view.getKey().getId() + "&class=" + view.getKey().getType() + "&name=" + collName;
		}

		// On crée une nouvelle sous-entité
		//noinspection unchecked
		final Class<? extends HibernateEntity> newClass = (Class<? extends HibernateEntity>) Class.forName(newClassAsString);
		final Long newId = manager.nextId(newClass);
		final EntityKey newKey = new EntityKey(newClass, newId);
		final AddSubEntity newEntity = new AddSubEntity(view.getKey(), view.getName(), newClass, newId);

		session.addDelta(newEntity);

		Flash.message("Nouvel élément '" + newClass.getSimpleName() + "' créé avec l'id n°" + newId);
		return "redirect:/supergra/entity/show.do?id=" + newKey.getId() + "&class=" + newKey.getType();
	}

	@RequestMapping(value = "/supergra/entity/disable.do", method = RequestMethod.POST)
	public String disableEntity(@RequestParam(value = "class") EntityType type, @RequestParam(value = "id") long id, HttpServletRequest request) {

		if (!SecurityHelper.isGranted(securityProvider, Role.SUPERGRA)) {
			throw new AccessDeniedException(ACCESS_DENIED);
		}

		// On récupère la session
		final SuperGraSession session = getSession(request);

		// On recharge toutes les entités de la base de données
		final EntityKey key = new EntityKey(type, id);
		final EntityView view = new EntityView();
		manager.fillView(key, view, session);

		if (view.isAnnule()) {
			Flash.warning("L'entité est déjà annulée. Aucun changement effectué.");
		}
		else {
			session.addDelta(new DisableEntity(view.getKey()));
			Flash.message("L'entité a été annulée.");
		}

		return "redirect:show.do?id=" + view.getKey().getId() + "&class=" + view.getKey().getType();
	}

	@RequestMapping(value = "/supergra/entity/enable.do", method = RequestMethod.POST)
	public String enableEntity(@RequestParam(value = "class") EntityType type, @RequestParam(value = "id") long id, HttpServletRequest request) {

		if (!SecurityHelper.isGranted(securityProvider, Role.SUPERGRA)) {
			throw new AccessDeniedException(ACCESS_DENIED);
		}

		// On récupère la session
		final SuperGraSession session = getSession(request);

		// On recharge toutes les entités de la base de données
		final EntityKey key = new EntityKey(type, id);
		final EntityView view = new EntityView();
		manager.fillView(key, view, session);

		if (!view.isAnnule()) {
			Flash.warning("L'entité n'est pas annulée. Aucun changement effectué.");
		}
		else {
			session.addDelta(new EnableEntity(view.getKey()));
			Flash.message("L'entité a été désannulée.");
		}

		return "redirect:show.do?id=" + view.getKey().getId() + "&class=" + view.getKey().getType();
	}

	@RequestMapping(value = "/supergra/entity/recalcRegroup.do", method = RequestMethod.POST)
	public String recalcRegroup(@RequestParam(value = "class") EntityType type, @RequestParam(value = "id") long id, HttpServletRequest request) {

		if (!SecurityHelper.isGranted(securityProvider, Role.SUPERGRA)) {
			throw new AccessDeniedException(ACCESS_DENIED);
		}

		// On récupère la session
		final SuperGraSession session = getSession(request);

		// On recharge toutes les entités de la base de données
		final EntityKey key = new EntityKey(type, id);
		final EntityView view = new EntityView();
		manager.fillView(key, view, session);


		if (!view.isCommunauteRF()) {
			Flash.warning("L'entité n'est pas une communauté RF. Aucun changement effectué.");
		}
		else {
			// on ajoute le changement de recalcule des regroupements sur la communauté
			session.addDelta(new RecalcRegroup(view.getKey()));
			Flash.message("Le calcul des regroupements a été effectué.");
		}

		return "redirect:show.do?id=" + view.getKey().getId() + "&class=" + view.getKey().getType();
	}

	@RequestMapping(value = "/supergra/entity/pp2mc.do", method = RequestMethod.GET)
	public String pp2mc(@RequestParam(value = "class") EntityType type, @RequestParam(value = "id") long id, Model model, HttpServletRequest request) throws
			Exception {

		if (!SecurityHelper.isGranted(securityProvider, Role.SUPERGRA)) {
			throw new AccessDeniedException(ACCESS_DENIED);
		}

		// On récupère l'entité spécifiée
		final SuperGraSession session = getSession(request);
		final EntityKey key = new EntityKey(type, id);
		final EntityView entity = new EntityView();
		manager.fillView(key, entity, session);

		if (!entity.isPersonnePhysique()) {
			Flash.error("L'entité spécifiée n'est pas une personne physique.");
			return "redirect:/supergra/entity/show.do?id=" + key.getId() + "&class=" + key.getType();
		}

		// On crée la vue qui va bien
		final Pp2McView view = new Pp2McView();
		view.setId(id);
		model.addAttribute("pp2mc", view);

		return "supergra/pp2mc";
	}

	@InitBinder(value = "pp2mc")
	protected void initBinderPp2Mc(WebDataBinder binder) throws ClassNotFoundException {
		binder.setValidator(pp2McValidator);
		binder.registerCustomEditor(RegDate.class, new RegDateEditor(true, false, false));
		binder.registerCustomEditor(Long.class, new TiersNumberEditor(true));
	}

	@RequestMapping(value = "/supergra/entity/pp2mc.do", method = RequestMethod.POST)
	public String pp2mc(@Valid @ModelAttribute("pp2mc") final Pp2McView view, BindingResult result) throws Exception {

		if (!SecurityHelper.isGranted(securityProvider, Role.SUPERGRA)) {
			throw new AccessDeniedException(ACCESS_DENIED);
		}

		if (result.hasErrors()) {
			return "supergra/pp2mc";
		}

		manager.transformPp2Mc(view.getId(), view.getDateDebut(), view.getDateFin(), view.getIdPrincipal(), view.getIdSecondaire());

		Flash.message("Le tiers n°" + view.getId() + " a été transformé en ménage commun.");
		return "redirect:/supergra/entity/show.do?id=" + view.getId() + "&class=Tiers";
	}

	@RequestMapping(value = "/supergra/entity/mc2pp.do", method = RequestMethod.GET)
	public String mc2pp(@RequestParam(value = "class") EntityType type, @RequestParam(value = "id") long id, Model model, HttpServletRequest request) throws
			Exception {

		if (!SecurityHelper.isGranted(securityProvider, Role.SUPERGRA)) {
			throw new AccessDeniedException(ACCESS_DENIED);
		}

		// On récupère l'entité spécifiée
		final SuperGraSession session = getSession(request);
		final EntityKey key = new EntityKey(type, id);
		final EntityView entity = new EntityView();
		manager.fillView(key, entity, session);

		if (!entity.isMenageCommun()) {
			Flash.error("L'entité spécifiée n'est pas un ménage commun.");
			return "redirect:/supergra/entity/show.do?id=" + key.getId() + "&class=" + key.getType();
		}

		// On crée la vue qui va bien
		final Mc2PpView view = new Mc2PpView();
		view.setId(id);
		model.addAttribute("mc2pp", view);

		return "supergra/mc2pp";
	}

	@InitBinder(value = "mc2pp")
	protected void initBindermc2pp(WebDataBinder binder) throws ClassNotFoundException {
		binder.setValidator(mc2PpValidator);
		binder.registerCustomEditor(RegDate.class, new RegDateEditor(true, false, false));
		binder.registerCustomEditor(Long.class, "indNo", new IndividuNumberEditor(true));
	}

	@RequestMapping(value = "/supergra/entity/mc2pp.do", method = RequestMethod.POST)
	public String mc2pp(@Valid @ModelAttribute("mc2pp") final Mc2PpView view, BindingResult result) throws Exception {

		if (!SecurityHelper.isGranted(securityProvider, Role.SUPERGRA)) {
			throw new AccessDeniedException(ACCESS_DENIED);
		}

		if (result.hasErrors()) {
			return "supergra/mc2pp";
		}

		manager.transformMc2Pp(view.getId(), view.getIndNo());

		Flash.message("Le tiers n°" + view.getId() + " a été transformé en personne physique.");
		return "redirect:/supergra/entity/show.do?id=" + view.getId() + "&class=Tiers";
	}

	@RequestMapping(value = "/supergra/actions/delete.do", method = RequestMethod.POST)
	public String actionDelete(@RequestParam(value = "index") int index, HttpServletRequest request) {

		if (!SecurityHelper.isGranted(securityProvider, Role.SUPERGRA)) {
			throw new AccessDeniedException(ACCESS_DENIED);
		}

		// On récupère la session
		final SuperGraSession session = getSession(request);
		final Delta action = session.removeDelta(index);
		Flash.message("L'action \"" + action + "\" a été supprimée.");

		return HttpHelper.getRedirectPagePrecedente(request);
	}

	@RequestMapping(value = "/supergra/actions/rollback.do", method = RequestMethod.POST)
	public String actionsRollbackAll(HttpServletRequest request) {

		if (!SecurityHelper.isGranted(securityProvider, Role.SUPERGRA)) {
			throw new AccessDeniedException(ACCESS_DENIED);
		}

		// On récupère la session
		final SuperGraSession session = getSession(request);
		session.clearDeltas();
		Flash.message("Toutes les actions ont été supprimées.");

		return HttpHelper.getRedirectPagePrecedente(request);
	}

	@RequestMapping(value = "/supergra/actions/commit.do", method = RequestMethod.POST)
	public String actionsCommitAll(@RequestParam(value = "class") EntityType type, @RequestParam(value = "id") long id, HttpServletRequest request) {

		if (!SecurityHelper.isGranted(securityProvider, Role.SUPERGRA)) {
			throw new AccessDeniedException(ACCESS_DENIED);
		}

		// On récupère la session
		final SuperGraSession session = getSession(request);
		final int size = session.deltaSize();
		if (size <= 0) {
			Flash.warning("Il n'y a aucune modification en attente !");
		}
		else {
			final EntityKey currentKey = new EntityKey(type, id);
			final List<Delta> delta = session.getDeltas();
			boolean needRedirect = isEntityANewOne(currentKey, delta);

			// on applique et commit les deltas dans la DB
			manager.commitDeltas(delta);

			// on efface les deltas appliqués dans la session
			session.clearDeltas();

			if (size == 1) {
				Flash.message("La modification a été sauvegardée dans la base de données.");
			}
			else {
				Flash.message("Les " + size + " modifications ont été sauvegardées dans la base de données.");
			}

			final EntityKey lastKey = session.getLastKnownTopEntity();
			if (needRedirect && lastKey != null) {
				// si l'objet affiché couramment est un nouvel objet, il faut rediriger l'utilisateur sur le dernier tiers connu parce qu'Hibernate va
				// réassigner un id à l'objet courant et on ne sait pas lequel
				return "redirect:/supergra/entity/show.do?id=" + lastKey.getId() + "&class=" + lastKey.getType();
			}
		}

		return HttpHelper.getRedirectPagePrecedente(request);
	}

	/**
	 * Détermine si l'entité spécifiée est une nouvelle entité (= entité créée à travers la session SuperGra courante)
	 *
	 * @param key   la clé d'une entité
	 * @param delta la liste des deltas à scanner
	 * @return <b>vrai</b> si l'entité spécifiée est une nouvelle entité; <b>faux</b> autement.
	 */
	private boolean isEntityANewOne(EntityKey key, List<Delta> delta) {
		boolean newEntity = false;
		for (Delta d : delta) {
			if (d instanceof AddSubEntity) {
				final AddSubEntity add = (AddSubEntity) d;
				if (add.getSubKey().equals(key)) {
					newEntity = true;
					break;
				}
			}
		}
		return newEntity;
	}

	@RequestMapping(value = "/supergra/option/details.do", method = RequestMethod.POST)
	public String toggleShowDetails(@RequestParam(value = "show") boolean showDetails, HttpServletRequest request) {

		if (!SecurityHelper.isGranted(securityProvider, Role.SUPERGRA)) {
			throw new AccessDeniedException(ACCESS_DENIED);
		}

		// On récupère la session
		final SuperGraSession session = getSession(request);
		session.getOptions().setShowDetails(showDetails);

		Flash.message("Les détails sont maintenant " + (showDetails ? "visibles" : "masqués") + '.');

		return HttpHelper.getRedirectPagePrecedente(request);
	}

	@ExceptionHandler(Exception.class)
	public ModelAndView handleCustomException(Exception ex) {
		ModelAndView model = new ModelAndView("supergra/exception");
		model.addObject("message", ExceptionHelper.getEnhancedMessage(ex));
		model.addObject("exception", ex);
		return model;
	}

	protected SuperGraSession getSession(HttpServletRequest request) {
		SuperGraSession session = (SuperGraSession) request.getSession().getAttribute("superGraSession");
		if (session == null) {
			session = new SuperGraSession();
			request.getSession().setAttribute("superGraSession", session);
		}
		return session;
	}
}
