package ch.vd.uniregctb.taglibs;

import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyTagSupport;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.web.util.HtmlUtils;

import ch.vd.uniregctb.adresse.AdresseEnvoiDetaillee;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.AdresseGenerique;
import ch.vd.uniregctb.adresse.AdresseMandataire;
import ch.vd.uniregctb.adresse.AdresseMandataireAdapter;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.transaction.TransactionTemplate;

public class JspTagAdresseMandataire extends BodyTagSupport {

	private static final Logger LOGGER = LoggerFactory.getLogger(JspTagAdresseMandataire.class);

	/*
	 * Ces membres sont statiques pour permettre l'injection par Spring des beans accessibles par toutes les instances de ce tag
	 */
	private static AdresseService adresseService;
	private static ServiceInfrastructureService infraService;
	private static HibernateTemplate hibernateTemplate;
	private static PlatformTransactionManager transactionManager;

	public void setAdresseService(AdresseService adresseService) {
		JspTagAdresseMandataire.adresseService = adresseService;
	}

	public void setInfraService(ServiceInfrastructureService infraService) {
		JspTagAdresseMandataire.infraService = infraService;
	}

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		JspTagAdresseMandataire.hibernateTemplate = hibernateTemplate;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		JspTagAdresseMandataire.transactionManager = transactionManager;
	}

	/*
	 * Les membres du tag, maintenant...
	 */

	public enum DisplayMode {
		/**
		 * Un seul bloc, multiligne (séparées par des <br/>)
		 */
		MULTILINE,

		/**
		 * avec une table, chaque ligne sur une "row" de la table
		 */
		TABLE
	}

	/**
	 * L'identifiant de l'adresse mandataire
	 */
	private Long idAdresse;

	/**
	 * Le type de rendu
	 */
	private DisplayMode displayMode = DisplayMode.MULTILINE;

	public void setIdAdresse(Long idAdresse) {
		this.idAdresse = idAdresse;
	}

	public void setDisplayMode(DisplayMode displayMode) {
		this.displayMode = displayMode;
	}

	@Override
	public int doStartTag() throws JspTagException {
		try {
			final JspWriter out = pageContext.getOut();
			out.print(buildHtml());
			return SKIP_BODY;
		}
		catch (Exception e) {
			throw new JspTagException(e);
		}
	}

	private String buildHtml() {

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		return template.execute(new TransactionCallback<String>() {
			@Override
			public String doInTransaction(TransactionStatus status) {
				final AdresseMandataire am = hibernateTemplate.get(AdresseMandataire.class, idAdresse);
				if (am == null) {
					return StringUtils.EMPTY;
				}

				final String[] lignes;
				try {
					final AdresseGenerique generique = new AdresseMandataireAdapter(am, infraService);
					final AdresseEnvoiDetaillee envoi = adresseService.buildAdresseEnvoi(generique.getSource().getTiers(), generique, am.getDateFin());
					lignes = envoi.getLignes().asTexte();
				}
				catch (AdresseException e) {
					LOGGER.error("Erreur à la constitution de l'adresse mandataire " + idAdresse, e);
					return StringUtils.EMPTY;
				}

				final List<String> actualLines = new ArrayList<>(lignes.length);
				for (String line : lignes) {
					final String stripped = StringUtils.trimToNull(line);
					if (stripped != null) {
						actualLines.add(stripped);
					}
				}
				if (actualLines.isEmpty()) {
					return StringUtils.EMPTY;
				}

				final StringBuilder b = new StringBuilder();
				int rowindex = 0;
				if (displayMode == DisplayMode.TABLE) {
					b.append("<table border='0'>");
				}
				for (String line : actualLines) {
					if (displayMode == DisplayMode.TABLE) {
						b.append("<tr class='").append(rowindex % 2 == 1 ? "even" : "odd").append("'><td>");
					}
					else if (b.length() > 0) {
						b.append("<br/>");
					}
					b.append(HtmlUtils.htmlEscape(line));
					if (displayMode == DisplayMode.TABLE) {
						b.append("</td></tr>");
					}
					++ rowindex;
				}
				if (displayMode == DisplayMode.TABLE) {
					b.append("</table>");
				}

				return b.toString();
			}
		});
	}
}
