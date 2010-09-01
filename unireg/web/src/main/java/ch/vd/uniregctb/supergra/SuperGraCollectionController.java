package ch.vd.uniregctb.supergra;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityProvider;

/**
 * Contrôleur permettant l'édition en mode SuperGra d'une collection appartenant à une entité Hibernate.
 */
public class SuperGraCollectionController extends SuperGraAbstractController {

	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {

		final CollectionView view = (CollectionView) super.formBackingObject(request);
		final String typeAsString = request.getParameter("class");
		final String idAsString = request.getParameter("id");
		final String collName = request.getParameter("name");

		if (!SecurityProvider.isGranted(Role.SUPERGRA)) {
			throw new AccessDeniedException(ACCESS_DENIED);
		}

		if (StringUtils.isNotBlank(typeAsString) && StringUtils.isNotBlank(idAsString)) {

			// On récupère la session
			final SuperGraSession session = getSession(request);

			// On recharge toutes les entités de la base de données
			final EntityType type = EntityType.valueOf(typeAsString);
			final Long id = Long.valueOf(idAsString);
			final EntityKey key = new EntityKey(type, id);
			manager.fillView(key, collName, view, session);
		}

		return view;
	}

	@SuppressWarnings({"unchecked"})
	@Override
	protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object command, BindException errors) throws Exception {
		final CollectionView view = (CollectionView) command;

		final String add = request.getParameter("add");
		
		if (!SecurityProvider.isGranted(Role.SUPERGRA)) {
			throw new AccessDeniedException(ACCESS_DENIED);
		}

		if (handleCommonAction(request)) {
			// rien d'autre à faire
		}
		else if (StringUtils.isNotBlank(add)) {
			final String newClassAsString = request.getParameter("newClass");
			if (newClassAsString.startsWith("--")) {
				flashError(request,"Veuillez sélectionner un type d'élément avant de cliquer le bouton.");
			}
			else {
				// On crée une nouvelle sous-entité
				final Class newClass = Class.forName(newClassAsString);
				final Long id = manager.nextId(newClass);
				final AddSubEntity newEntity = new AddSubEntity(view.getKey(), view.getName(), newClass, id);

				final SuperGraSession session = getSession(request);
				session.addDelta(newEntity);

				flash(request, "Nouvel élément créé avec l'id n°" + id);
				return new ModelAndView(new RedirectView("entity.do?id=" + id + "&class=" + EntityType.fromHibernateClass(newClass)));
			}
		}

		return new ModelAndView(new RedirectView(getSuccessView() + "?id=" + view.getKey().getId() + "&class=" + view.getKey().getType() + "&name=" + view.getName()));
	}
}