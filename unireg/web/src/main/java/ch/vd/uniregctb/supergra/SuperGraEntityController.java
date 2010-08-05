package ch.vd.uniregctb.supergra;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.beans.PropertyEditor;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.propertyeditors.CustomBooleanEditor;
import org.springframework.beans.propertyeditors.CustomCollectionEditor;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.beans.propertyeditors.CustomNumberEditor;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.validation.BindException;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.utils.EnumEditor;
import ch.vd.uniregctb.utils.RegDateEditor;

/**
 * Contrôleur permettant l'édition en mode SuperGra d'une entité Hibernate.
 */
public class SuperGraEntityController extends SuperGraAbstractController {

	private SuperGraManager manager;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setManager(SuperGraManager manager) {
		this.manager = manager;
	}

	@SuppressWarnings({"unchecked"})
	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {

		final EntityView view = (EntityView) super.formBackingObject(request);
		final String typeAsString = request.getParameter("class");
		final String idAsString = request.getParameter("id");

		if (StringUtils.isNotBlank(typeAsString) && StringUtils.isNotBlank(idAsString)) {

			// On récupère les deltas déjà mémorisés
			final SuperGraSession session = getSession(request);
			final List<Delta> deltas = session.getDeltas();

			// On recharge l'entité de la base de données
			final EntityType type = EntityType.valueOf(typeAsString);
			final Long id = Long.valueOf(idAsString);
			final EntityKey key = new EntityKey(type, id);
			manager.fillView(key, view, deltas);

			// On mémorise une version de référence pour pouvoir détecter de nouveaux deltas plus tard
			final EntityView referenceView = (EntityView) view.clone();
			request.getSession().setAttribute("referenceEntity", referenceView);
		}

		return view;
	}

	@SuppressWarnings({"unchecked"})
	@Override
	protected void initBinder(HttpServletRequest request, ServletRequestDataBinder binder) throws Exception {
		super.initBinder(request, binder);

		// On enregistre les éditeurs standards
		Locale locale = request.getLocale();
		SimpleDateFormat sdf = new SimpleDateFormat(DateHelper.DATE_FORMAT_DISPLAY, locale);
		sdf.setLenient(false);
		binder.registerCustomEditor(Date.class, new CustomDateEditor(sdf, true));
		NumberFormat numberFormat = NumberFormat.getInstance(locale);
		numberFormat.setGroupingUsed(true);
		binder.registerCustomEditor(BigDecimal.class, new CustomNumberEditor(BigDecimal.class, numberFormat, true));
		binder.registerCustomEditor(Integer.class, new CustomNumberEditor(Integer.class, numberFormat, true));
		binder.registerCustomEditor(Long.class, new CustomNumberEditor(Long.class, numberFormat, true));
		binder.registerCustomEditor(List.class, new CustomCollectionEditor(List.class));
		binder.registerCustomEditor(boolean.class, new CustomBooleanEditor(true));
		binder.registerCustomEditor(Boolean.class, new CustomBooleanEditor(true));
		binder.registerCustomEditor(RegDate.class, new RegDateEditor(true));
		binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));

		final EntityView view = (EntityView) binder.getTarget();

		// On enregistre des éditeurs spécialisés sur chacun des attributs de l'entité en donnant le chemin d'accès précis. Ceci permet de faire
		// la traduction Object <-> String correct sur chacun des attributs. Sans ce code, toutes les valeurs entrées dans le formulaire reviendraient
		// sous forme de strings, car Spring ne peut pas deviner quel est le type réel des valeurs 'Object' exposées dans les AttributeView.
		final List<AttributeView> attributes = view.getAttributes();
		if (attributes != null) {
			for (int i = 0; i < attributes.size(); i++) {
				final AttributeView a = view.getAttributes().get(i);
				final Class<?> type = a.getType();

				PropertyEditor editor = binder.findCustomEditor(type, null);
				if (editor == null && type.isEnum()) {
					editor = new EnumEditor((Class<? extends Enum>) type, true);
				}

				if (editor != null) {
					binder.registerCustomEditor(AttributeView.class, "attributes[" + i + "].value", editor);
				}
			}
		}
	}

	@SuppressWarnings({"unchecked"})
	@Override
	protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object command, BindException errors) throws Exception {

		final EntityView view = (EntityView) command;
		Assert.notNull(view);

		if (handleDeltaDelete(request)) {
			// rien d'autre à faire
		}
		else {
			final EntityView referenceView = (EntityView) request.getSession().getAttribute("referenceEntity");
			Assert.notNull(referenceView);

			// On détermine les changements effectués
			final SuperGraSession session = getSession(request);
			final List<Delta> newdeltas = referenceView.delta(view);
			session.addDeltas(newdeltas);

			if (newdeltas.isEmpty()) {
				flashWarning(request, "Aucune différence trouvée. Avez-vous bien changé une valeur avant de sauver ?");
			}
			else {
				final String message;
				if (newdeltas.size() == 1) {
					message = "1 attribut a été changé.";
				}
				else {
					message = String.valueOf(newdeltas.size()) + " attributs ont été changés.";
				}
				flash(request, message);
			}

			final EntityView updatedReferenceView = (EntityView) view.clone();
			request.getSession().setAttribute("referenceEntity", updatedReferenceView);
		}

		return new ModelAndView(new RedirectView(getSuccessView() + "?id=" + view.getKey().getId() + "&class=" + view.getKey().getType()));
	}
}
