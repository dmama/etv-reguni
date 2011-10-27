package ch.vd.uniregctb.taglibs;

import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyTagSupport;

import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;

import ch.vd.uniregctb.utils.WebContextUtils;

/**
 * Ce tag permet d'afficher un lien permettant d'afficher ou de réduire les lignes d'un tableau.
 */
public class JspTagToggleAffichage extends BodyTagSupport implements MessageSourceAware {

	private static final long serialVersionUID = -5339054043898793018L;


	private String tableId;

	private String numeroColonne;

	private int nombreLignes;


	private static MessageSource messageSource;

	public String getTableId() {
		return tableId;
	}

	public void setTableId(String tableId) {
		this.tableId = tableId;
	}

	public String getNumeroColonne() {
		return numeroColonne;
	}

	public void setNumeroColonne(String numeroColonne) {
		this.numeroColonne = numeroColonne;
	}


	public int getNombreLignes() {
		return nombreLignes;
	}

	public void setNombreLignes(int nombreLignes) {
		this.nombreLignes = nombreLignes;
	}

	private static String getMessage(String key) {
		return messageSource.getMessage(key, null, WebContextUtils.getDefaultLocale());
	}

	@Override
	public int doStartTag() throws JspTagException {
		try {

			final String displayAll = getMessage("label.bouton.afficher.tout");
			final String tooltipAll = "Afficher toutes les lignes";
			final String idAll = "linkAll";
			final String onClickAll = "toggleAffichageRows('" + tableId + "',true," + numeroColonne + ");";

			final String displayReduce = getMessage("label.bouton.reduire");
			final String tooltipReduce = "Réduire l'affichage aux 3 premières lignes";
			final String idReduce = "linkReduce";
			final String onClickReduce = "toggleAffichageRows('" + tableId + "',false," + numeroColonne + ");";


			final JspWriter out = pageContext.getOut();
			//Si le nombre de lignes du tableau est supérieur à 3,on affiche le lien
			if (nombreLignes > 3) {
				out.print(buildHtml(displayAll, onClickAll, tooltipAll, idAll));
				out.print(buildHtml(displayReduce, onClickReduce, tooltipReduce, idReduce));
			}

			// Skips the body.
			return SKIP_BODY;

		}
		catch (Exception ex) {
			throw new JspTagException(ex);
		}
	}

	public String buildHtml(String display, String onClick, String tooltip, String id) {
		final StringBuilder builder = new StringBuilder();
		builder.append("<a href=\"");
		builder.append("#");
		builder.append("\" class=\"");
		builder.append(" noprint");

		builder.append("\" onClick=\"");
		builder.append(onClick);

		builder.append("\" title=\"");
		builder.append(getMessage(tooltip));

		builder.append("\" id=\"");
		builder.append(id);

		builder.append("\">");
		builder.append(display);
		builder.append("</a>");
		return builder.toString();
	}

	/**
	 * @see org.springframework.context.MessageSourceAware#setMessageSource(org.springframework.context.MessageSource)
	 */
	@Override
	public void setMessageSource(MessageSource messageSource) {
		JspTagToggleAffichage.messageSource = messageSource;
	}
}