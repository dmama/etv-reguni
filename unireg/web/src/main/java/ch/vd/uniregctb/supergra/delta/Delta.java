package ch.vd.uniregctb.supergra.delta;

import org.springframework.web.util.HtmlUtils;

import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.supergra.EntityKey;
import ch.vd.uniregctb.supergra.SuperGraContext;

public abstract class Delta {

	/**
	 * @return la clé de l'entité sur laquelle s'applique le changement.
	 */
	public abstract EntityKey getKey();

	/**
	 * Applique le changement sur l'entité spécifiée.
	 *
	 * @param entity  l'entité Hibernate sur laquelle le changement doit être appliqué.
	 * @param context le context d'exécution courant
	 */
	public abstract void apply(HibernateEntity entity, SuperGraContext context);

	/**
	 * @return la description du changement sous forme de String qui peut contenir des éléments de mise-en-page Html.
	 */
	public abstract String getHtml();

	@Override
	public String toString() {
		final String html = getHtml();
		return html.replaceAll("<.*?>", ""); // on supprime toutes les balises
	}

	protected String attribute2html(String name) {
		return "<span class=\"attributeName\">" + HtmlUtils.htmlEscape(name) + "</span>";
	}

	protected String value2html(Object value) {
		return "<span class=\"attributeValue\">" + HtmlUtils.htmlEscape(value.toString()) + "</span>";
	}
}
