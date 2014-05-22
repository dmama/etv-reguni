package ch.vd.uniregctb.taglibs;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyTagSupport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.web.util.HtmlUtils;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.avatars.AvatarService;
import ch.vd.unireg.avatars.TypeAvatar;
import ch.vd.uniregctb.adresse.AdresseEnvoiDetaillee;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.adresse.TypeAdresseFiscale;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.declaration.Periodicite;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityHelper;
import ch.vd.uniregctb.security.SecurityProviderInterface;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.EvenementsCivilsNonTraites;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.transaction.TransactionTemplate;
import ch.vd.uniregctb.type.PeriodeDecompte;
import ch.vd.uniregctb.type.PeriodiciteDecompte;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.utils.WebContextUtils;

/**
 * Affiche les informations générales d'un tiers (nom, prénom, type d'assujettissement, adresse, image, ...).
 */
public class JspTagBandeauTiers extends BodyTagSupport implements MessageSourceAware {

	private static final long serialVersionUID = -7103375494735633544L;

	/*
	 * Ces membres sont statiques pour permettre l'injection par Spring des beans accessibles par toutes les instances de ce tag
	 */
	private static MessageSource messageSource;
	private static TiersDAO tiersDAO;
	private static TiersService tiersService;
	private static AdresseService adresseService;
	private static PlatformTransactionManager transactionManager;
	private static SecurityProviderInterface securityProvider;
	private static AvatarService avatarService;

	public static final List<Action> actions;

	static {
		final List<Action> list = new ArrayList<>();
		list.add(new Reindexer());
		list.add(new RecalculerParentes());
		list.add(new Marier());
		list.add(new Deceder());
		list.add(new Separer());
		list.add(new Reactiver());
		list.add(new AnnulerCouple());
		list.add(new AnnulerSeparation());
		list.add(new AnnulerDeces());
		list.add(new AnnulerTiers());
		list.add(new Exporter());

		actions = Collections.unmodifiableList(list);
	}

	private Long numero;
	private String titre;
	private String cssClass;
	private boolean showValidation = true;
	private boolean showEvenementsCivils = true;
	private boolean showLinks = true;
	private boolean showAvatar = true;
	private boolean showComplements = false;
	private String forceAvatar;
	private int rowcount;
	private String urlRetour;

	@Override
	public int doStartTag() throws JspTagException {
		try {
			JspWriter out = pageContext.getOut();
			final String html = buidHtlm();
			out.print(html);
			return SKIP_BODY;
		}
		catch (Exception ex) {
			throw new JspTagException(ex);
		}
	}

	public void setNumero(Long num) {
		this.numero = num;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setTitre(String titre) {
		this.titre = titre;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setCssClass(String cssClass) {
		this.cssClass = cssClass;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setShowValidation(boolean showValidation) {
		this.showValidation = showValidation;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setShowEvenementsCivils(boolean showEvenementsCivils) {
		this.showEvenementsCivils = showEvenementsCivils;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setShowLinks(boolean showLinks) {
		this.showLinks = showLinks;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setShowAvatar(boolean showAvatar) {
		this.showAvatar = showAvatar;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setShowComplements(boolean showComplements) {
		this.showComplements = showComplements;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setUrlRetour(String urlRetour) {
		this.urlRetour = urlRetour;
	}

	public void setForceAvatar(String forceAvatar) {
		this.forceAvatar = forceAvatar;
	}

	@Override
	public void setMessageSource(MessageSource messageSource) {
		JspTagBandeauTiers.messageSource = messageSource;
	}

	public void setTiersDAO(TiersDAO tiersDAO) {
		JspTagBandeauTiers.tiersDAO = tiersDAO;
	}

	public void setTiersService(TiersService tiersService) {
		JspTagBandeauTiers.tiersService = tiersService;
	}

	public void setAdresseService(AdresseService adresseService) {
		JspTagBandeauTiers.adresseService = adresseService;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		JspTagBandeauTiers.transactionManager = transactionManager;
	}

	public void setSecurityProvider(SecurityProviderInterface securityProvider) {
		JspTagBandeauTiers.securityProvider = securityProvider;
	}

	public void setAvatarService(AvatarService avatarService) {
		JspTagBandeauTiers.avatarService = avatarService;
	}

	private String buidHtlm() {

		rowcount = 0;

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		return template.execute(new TransactionCallback<String>() {
			@Override
			public String doInTransaction(TransactionStatus status) {
				final Tiers tiers = tiersDAO.get(numero);
				if (tiers == null) {
					return "";
				}

				EnsembleTiersCouple ensemble = null;
				if (tiers instanceof PersonnePhysique) {
					ensemble = tiersService.getEnsembleTiersCouple((PersonnePhysique) tiers, null);
				}
				else if (tiers instanceof MenageCommun) {
					ensemble = tiersService.getEnsembleTiersCouple((MenageCommun) tiers, null);
				}

				if (titre == null) {
					titre = message("caracteristiques.tiers");
				}

				if (cssClass == null) {
					cssClass = "information";
				}

				StringBuilder s = new StringBuilder();
				s.append("<fieldset class=\"").append(cssClass).append("\">\n");
				s.append("<legend><span>").append(HtmlUtils.htmlEscape(titre)).append("</span></legend>\n");
				s.append(buildDebugInfo(tiers));

				if (showAvatar || showLinks) {
					s.append("<table cellspacing=\"0\" cellpadding=\"0\" border=\"0\"><tr><td>\n");
					s.append(buildDescriptifTiers(tiers));
					s.append("</td><td width=\"130 px\">\n");
					s.append(buildImageTiers(tiers));

					if (showLinks && ensemble != null) {
						// si on a un tiers appartenant à un ensemble tiers-couple, on affiche des raccourcis vers les autres membres
						final String others = buildImageOtherTiers(tiers, ensemble);
						if (others != null) {
							s.append("</td><td class=\"composition_menage noprint\" valign=\"top\">\n");
							s.append(others);
						}
					}
					s.append("</td></tr></table>\n");
				}
				else {
					s.append(buildDescriptifTiers(tiers));
				}

				s.append("</fieldset>");

				return s.toString();
			}
		});
	}

	private String buildDebugInfo(Tiers tiers) {
		StringBuilder s1 = new StringBuilder();
		s1.append("<input name=\"debugNatureTiers\" type=\"hidden\" value=\"").append(tiers.getNatureTiers()).append("\"/>\n");

		final ForFiscalPrincipal ffp = tiers.getForFiscalPrincipalAt(null);
		final TypeAutoriteFiscale type = (ffp == null ? null : ffp.getTypeAutoriteFiscale());
		s1.append("<input name=\"debugTypeForPrincipalActif\" type=\"hidden\" value=\"").append(type).append("\"/>\n");

		return s1.toString();
	}

	private String buildDescriptifTiers(Tiers tiers) {
		StringBuilder s = new StringBuilder();

		if (showValidation) {
			// [SIFISC-2561] on passe par un appel Ajax pour afficher les erreurs de validation car cela peut prendre beaucoup de temps sur certains tiers
			s.append("<div id=\"validationMessage\" width=\"100%\"></div>");
			s.append("<script>");
			s.append("$(function() {");
			s.append("    Tiers.loadValidationMessages(").append(tiers.getNumero()).append(", $('#validationMessage'));");
			s.append("});");
			s.append("</script>");
		}

		s.append("<table cellspacing=\"0\" cellpadding=\"5\" border=\"0\" class=\"display_table\">\n");

		if (tiers.isAnnule()) {
			s.append("<tr class=\"inactif\">\n");
			s.append("\t<td colspan=\"3\" width=\"100%\"><center>").append(message("label.tiers.annule")).append("</center></td>\n");
			s.append("</tr>\n");
		}
		else if (tiers.isDesactive(null)) {
			s.append("<tr class=\"inactif\">\n");
			s.append("\t<td colspan=\"3\" width=\"100%\"><center>").append(message("label.tiers.desactive.au")).append("&nbsp;").append(RegDateHelper.dateToDisplayString(tiers.getDateDesactivation()))
					.append("</center></td>\n");
			s.append("</tr>\n");
		}

		if (showEvenementsCivils && SecurityHelper.isGranted(securityProvider, Role.MODIF_VD_ORD)) {
			final EvenementsCivilsNonTraites evtsCivilNonTraites = tiersService.getIndividusAvecEvenementsCivilsNonTraites(tiers);
			if (evtsCivilNonTraites != null && !evtsCivilNonTraites.isEmpty()) {
				s.append("<tr class=\"evts-civils-non-traites\"><td colspan=\"3\" width=\"100%\"><center>\n");
				s.append(message("label.tiers.evts.non.traites")).append("&nbsp;: ");

				boolean sourcePresent = false;
				for (EvenementsCivilsNonTraites.Source src : EvenementsCivilsNonTraites.Source.values()) {
					if (evtsCivilNonTraites.hasForSource(src)) {
						if (sourcePresent) {
							s.append(", ");
						}
						s.append(HtmlUtils.htmlEscape(src.getLibelle())).append(" (");
						boolean individuPresent = false;
						for (Long no : evtsCivilNonTraites.getForSource(src)) {
							if (individuPresent) {
								s.append(", ");
							}
							s.append(no);
							individuPresent = true;
						}
						s.append(")");
						sourcePresent = true;
					}
				}
				s.append("</center></td></tr>\n");
			}
		}

		// Numéro de contribuable
		s.append("<tr class=\"").append(nextRowClass()).append("\">\n");
		s.append("\t<td width=\"25%\" nowrap>").append(message("label.numero.tiers")).append("&nbsp;:&nbsp;</td>\n");
		s.append("\t<td width=\"50%\">").append(FormatNumeroHelper.numeroCTBToDisplay(numero));
		if (showLinks) {
			final JspTagConsulterLog consulter = new JspTagConsulterLog();
			consulter.setEntityNature("Tiers");
			consulter.setEntityId(String.valueOf(numero));
			s.append(consulter.buildHtml());
		}
		s.append("</td>\n");
		if (!tiers.isAnnule() && showLinks) {
			s.append("\t<td width=\"25%\">\n");
			s.append(buildVers(tiers));
			s.append("\t</td>\n");
		}
		else {
			s.append("\t<td width=\"25%\">&nbsp;</td>\n");
		}
		s.append("</tr>\n");

		// Rôle
		s.append("<tr class=\"").append(nextRowClass()).append("\">\n");
		s.append("\t<td width=\"25%\">").append(message("label.role")).append("&nbsp;:</td>\n");
		s.append("\t<td width=\"50%\">");
		s.append(HtmlUtils.htmlEscape(tiers.getRoleLigne1()));
		final String roleLigne2 = tiersService.getRoleAssujettissement(tiers, RegDate.get());
		if (StringUtils.isNotBlank(roleLigne2)) {
			s.append("<br>").append(HtmlUtils.htmlEscape(roleLigne2));
		}
		s.append("</td>\n");

		// Actions
		if (showLinks) {

			final List<Action> actionsToDisplay = new ArrayList<>(actions.size());
			for (Action action : actions) {
				if (action.isGranted() && action.isValide(tiers)) {
					actionsToDisplay.add(action);
				}
			}

			if (actionsToDisplay.isEmpty()) {
				// pas d'action à afficher
				s.append("\t<td width=\"25%\">&nbsp;</td>\n");
			}
			else {
				s.append("\t<td width=\"25%\">\n");
				s.append("\t<div style=\"float:right;margin-right:10px;text-align:right\">\n");
				s.append("\t\t<span>").append(message("label.actions")).append(" : </span>\n");
				s.append("\t\t<select onchange=\"return App.executeAction($(this).val() + ").append(tiers.getNumero()).append(");\">\n");
				s.append("\t\t\t<option>---</option>\n");

				for (Action action : actionsToDisplay) {
					s.append("\t\t\t<option value=\"").append(action.getActionUrl()).append("\">").append(action.getLabel()).append("</option>\n");
				}

				s.append("\t\t</select>");
				s.append("\t</div>\n");
				s.append("\t</td>\n");
			}
		}
		else {
			s.append("\t<td width=\"25%\">&nbsp;</td>\n");
		}
		s.append("</tr>\n");

		// Adresse envoi
		try {
			AdresseEnvoiDetaillee adresse = adresseService.getAdresseEnvoi(tiers, null, TypeAdresseFiscale.COURRIER, false);

			// 1ère ligne
			s.append("<tr class=\"").append(nextRowClass()).append("\">\n");
			s.append("\t<td width=\"25%\">").append(message("label.adresse")).append("&nbsp;:</td>\n");
			final String ligne1 = adresse.getLigne1();
			if (StringUtils.isNotBlank(ligne1)) {
				s.append("\t<td width=\"75%\" colspan=\"2\">").append(HtmlUtils.htmlEscape(ligne1)).append("</td>\n");
			}
			s.append("</tr>\n");

			// lignes 2 à 6
			for (int i = 2; i <= 6; ++i) {
				final String ligne = adresse.getLigne(i);
				if (StringUtils.isNotBlank(ligne)) {
					s.append("<tr class=\"").append(nextRowClass()).append("\">\n");
					s.append("\t<td width=\"25%\">&nbsp;</td>\n");
					s.append("\t<td width=\"75%\" colspan=\"2\">").append(HtmlUtils.htmlEscape(ligne)).append("</td>\n");
					s.append("</tr>\n");
				}
			}
		}
		catch (Exception e) {
			s.append("<tr class=\"").append(nextRowClass()).append("\">\n");
			s.append("\t<td width=\"25%\">").append(message("label.adresse")).append("&nbsp;:</td>\n");
			s.append("\t<td width=\"75%\" colspan=\"2\" class=\"error\">").append(message("error.adresse.envoi.entete")).append("</td>\n");
			s.append("</tr>\n");
			s.append("<tr class=\"").append(nextRowClass()).append("\">\n");
			s.append("\t<td width=\"25%\">&nbsp;</td>\n");
			s.append("\t<td width=\"75%\"  colspan=\"2\" class=\"error\">=&gt;&nbsp;").append(HtmlUtils.htmlEscape(e.getMessage())).append("</td>\n");
			s.append("</tr>\n");
			s.append("<tr class=\"").append(nextRowClass()).append("\">\n");
			s.append("\t<td width=\"25%\">&nbsp;</td>\n");
			s.append("\t<td  width=\"75%\"  colspan=\"2\" class=\"error\">").append(message("error.adresse.envoi.remarque")).append("</td>\n");
			s.append("</tr>\n");
		}

		if (showComplements) {
			if (tiers instanceof PersonnePhysique) {
				// Date de naissance
				final PersonnePhysique pp = (PersonnePhysique) tiers;
				final RegDate dateNaissance = tiersService.getDateNaissance(pp);
				s.append("<tr class=\"").append(nextRowClass()).append("\">\n");
				s.append("\t<td width=\"25%\">").append(message("label.date.naissance")).append("&nbsp;:</td>\n");
				s.append("\t<td width=\"50%\">").append(RegDateHelper.dateToDisplayString(dateNaissance)).append("</td>\n");
				s.append("\t<td width=\"25%\">&nbsp;</td>\n");
				s.append("</tr>\n");

				// Numéro AVS
				final String numeroAssureSocial = tiersService.getNumeroAssureSocial(pp);
				s.append("<tr class=\"").append(nextRowClass()).append("\">\n");
				s.append("\t<td width=\"25%\">").append(message("label.nouveau.numero.avs")).append("&nbsp;:</td>\n");
				s.append("\t<td width=\"50%\">").append(FormatNumeroHelper.formatNumAVS(numeroAssureSocial)).append("</td>\n");
				s.append("\t<td width=\"25%\">&nbsp;</td>\n");
				s.append("</tr>\n");

				// Ancien numéro AVS
				final String ancienNumeroAssureSocial = tiersService.getAncienNumeroAssureSocial(pp);
				s.append("<tr class=\"").append(nextRowClass()).append("\">\n");
				s.append("\t<td width=\"25%\">").append(message("label.ancien.numero.avs")).append("&nbsp;:</td>\n");
				s.append("\t<td width=\"50%\">").append(FormatNumeroHelper.formatAncienNumAVS(ancienNumeroAssureSocial)).append("</td>\n");
				s.append("\t<td width=\"25%\">&nbsp;</td>\n");
				s.append("</tr>\n");
			}
			else if (tiers instanceof DebiteurPrestationImposable) {
				final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiers;

				// Catégorie Impôt-source
				s.append("<tr class=\"").append(nextRowClass()).append("\">\n");
				s.append("\t<td width=\"25%\">").append(message("label.debiteur.is")).append("&nbsp;:</td>\n");
				s.append("\t<td width=\"50%\">").append(message("option.categorie.impot.source." + dpi.getCategorieImpotSource().name())).append("</td>\n");
				s.append("\t<td width=\"25%\">&nbsp;</td>\n");
				s.append("</tr>\n");

				// Périodicité
				final Periodicite periodicite = dpi.getPeriodiciteAt(RegDate.get());
				if (periodicite != null) {
					final PeriodiciteDecompte periodiciteDecompte = periodicite.getPeriodiciteDecompte();
					final PeriodeDecompte periodeDecompte = periodicite.getPeriodeDecompte();
					s.append("<tr class=\"").append(nextRowClass()).append("\">\n");
					s.append("\t<td width=\"25%\">").append(message("label.periodicite")).append("&nbsp;:</td>\n");
					s.append("\t<td width=\"50%\">");
					s.append(message("option.periodicite.decompte." + periodiciteDecompte.name()));
					if (periodiciteDecompte == PeriodiciteDecompte.UNIQUE && periodeDecompte != null) {
						s.append("&nbsp;(").append(message("option.periode.decompte." + periodeDecompte.name())).append(')');
					}
					s.append("</td>\n");
					s.append("\t<td width=\"25%\">&nbsp;</td>\n");
					s.append("</tr>\n");
				}

				// Mode de communication
				s.append("<tr class=\"").append(nextRowClass()).append("\">\n");
				s.append("\t<td width=\"25%\">").append(message("label.mode.communication")).append("&nbsp;:</td>\n");
				s.append("\t<td width=\"50%\">").append(message("option.mode.communication." + dpi.getModeCommunication())).append("</td>\n");
				s.append("\t<td width=\"25%\">&nbsp;</td>\n");
				s.append("</tr>\n");

				// Personne de contact
				s.append("<tr class=\"").append(nextRowClass()).append("\">\n");
				s.append("\t<td width=\"25%\">").append(message("label.personne.contact")).append("&nbsp;:</td>\n");
				final String personneContact = dpi.getPersonneContact();
				s.append("\t<td width=\"50%\">").append(HtmlUtils.htmlEscape(StringUtils.trimToEmpty(personneContact))).append("</td>\n");
				s.append("\t<td width=\"25%\">&nbsp;</td>\n");
				s.append("</tr>\n");

				// Numéro de téléphone fixe
				s.append("<tr class=\"").append(nextRowClass()).append("\">\n");
				s.append("\t<td width=\"25%\">").append(message("label.numero.telephone.fixe")).append("&nbsp;:</td>\n");
				final String numeroTelephonePrive = dpi.getNumeroTelephonePrive();
				s.append("\t<td width=\"50%\">").append(HtmlUtils.htmlEscape(StringUtils.trimToEmpty(numeroTelephonePrive))).append("</td>\n");
				s.append("\t<td width=\"25%\">&nbsp;</td>\n");
				s.append("</tr>\n");
			}
		}

		s.append("</table>");
		return s.toString();
	}

	private String nextRowClass() {
		return ++rowcount % 2 == 0 ? "even" : "odd";
	}

	private static String message(String key) {
		return messageSource.getMessage(key, null, WebContextUtils.getDefaultLocale());
	}

	private String url(String relative) {
		return getContextPath() + relative;
	}

	private String getContextPath() {
		final HttpServletRequest request = (HttpServletRequest) this.pageContext.getRequest();
		return request.getContextPath();
	}

	private String buildVers(Tiers tiers) {
		final StringBuilder s = new StringBuilder();

		s.append("<div style=\"float: right;margin-right: 10px\">\n");
		s.append("<span>").append(message("label.ouvrir.vers")).append(" : </span>\n");

		if (StringUtils.isBlank(urlRetour)) {
			s.append(JspTagInteroperabilite.buildHtml(getContextPath(), tiers.getNatureTiers(), tiers.getNumero(), tiers.isDebiteurInactif()));
		}
		else {
			s.append("<a href=\"").append(urlRetour).append(tiers.getNumero()).append("\" class=\"detail\" title=\"").append(message("label.retour.application.appelante")).append("\">&nbsp;</a>\n");
		}

		s.append("</div>\n");

		return s.toString();
	}

	private String buildImageUrl(TypeAvatar type, boolean withLink) {
		final List<String> params = new ArrayList<>();
		params.add("type=" + type);
		if (withLink) {
			params.add("link=true");
		}
		final StringBuilder b = new StringBuilder();
		for (String param : params) {
			if (b.length() > 0) {
				b.append('&');
			}
			b.append(param);
		}

		// [SIFISC-12477] l'affichage des images des avatars ne doit pas être considéré comme la dernière page consultée !!
		return String.format("/tiers/avatar.do?%s&url_memorize=false", b.toString());
	}

	private String buildImageTiers(Tiers tiers) {
		final String image = buildImageUrl(forceAvatar == null ? avatarService.getTypeAvatar(tiers) : TypeAvatar.valueOf(forceAvatar), false);
		final StringBuilder s = new StringBuilder();
		s.append("<img class=\"iepngfix\" src=\"").append(url(image)).append("\">\n");
		return s.toString();
	}

	/**
	 * Construit le code Html qui permet d'afficher les avatars miniatures des autres composants d'un ménage-commun.
	 *
	 * @param tiers    le tiers couramment affiché
	 * @param ensemble l'ensemble tiers-couple à partir duquel les autres composants du ménage sont déduits.
	 * @return le code Html à insérer ou <b>null</b> s'il n'y a rien à faire.
	 */
	private String buildImageOtherTiers(Tiers tiers, EnsembleTiersCouple ensemble) {

		if (tiers == ensemble.getConjoint() || tiers == ensemble.getPrincipal()) {
			final MenageCommun menage = ensemble.getMenage();
			final TypeAvatar typeAvatar = avatarService.getTypeAvatar(menage);
			final String image = buildImageUrl(typeAvatar, true);

			final StringBuilder s = new StringBuilder();
			s.append("<a title=\"Aller vers le ménage du tiers\" href=\"").append(url("/tiers/visu.do?id=")).append(menage.getId()).append("\">");
			s.append("<img class=\"iepngfix avatar\" src=\"").append(url(image)).append("\">\n");
			s.append("</a>");

			return s.toString();
		}
		else if (tiers == ensemble.getMenage()) {
			StringBuilder s = null;

			final PersonnePhysique principal = ensemble.getPrincipal();
			if (principal != null) {
				final TypeAvatar typeAvatar = avatarService.getTypeAvatar(principal);
				final String image = buildImageUrl(typeAvatar, true);
				s = new StringBuilder();
				s.append("<a title=\"Aller vers le tiers principal du ménage\" href=\"").append(url("/tiers/visu.do?id=")).append(principal.getId()).append("\">");
				s.append("<img class=\"iepngfix avatar\" src=\"").append(url(image)).append("\">\n");
				s.append("</a>");
			}

			final PersonnePhysique conjoint = ensemble.getConjoint();
			if (conjoint != null) {
				final TypeAvatar typeAvatar = avatarService.getTypeAvatar(conjoint);
				final String image = buildImageUrl(typeAvatar, true);
				if (s == null) {
					s = new StringBuilder();
				}
				s.append("<a title=\"Aller vers le tiers secondaire du ménage\" href=\"").append(url("/tiers/visu.do?id=")).append(conjoint.getId()).append("\">");
				s.append("<img class=\"iepngfix avatar\" src=\"").append(url(image)).append("\">\n");
				s.append("</a>");
			}

			return s == null ? null : s.toString();
		}
		else {
			return null;
		}
	}

	public static interface Action {
		boolean isGranted();
		boolean isValide(Tiers tiers);
		String getLabel();
		String getActionUrl();
	}

	private static class Reindexer implements Action {

		@Override
		public boolean isGranted() {
			return SecurityHelper.isAnyGranted(securityProvider, Role.TESTER, Role.ADMIN);
		}

		@Override
		public boolean isValide(Tiers tiers) {
			return true;
		}

		@Override
		public String getLabel() {
			return "Réindexer";
		}

		@Override
		public String getActionUrl() {
			return "post:/admin/indexation.do?action=reindexTiers&id=";
		}
	}

	private static class Exporter implements Action {

		@Override
		public boolean isGranted() {
			return SecurityHelper.isAnyGranted(securityProvider, Role.TESTER, Role.ADMIN);
		}

		@Override
		public boolean isValide(Tiers tiers) {
			return true;
		}

		@Override
		public String getLabel() {
			return "Exporter";
		}

		@Override
		public String getActionUrl() {
			return "post:/admin/dbdump.do?action=dumptiers&tiers=";
		}
	}

	private static class Marier implements Action {

		@Override
		public boolean isGranted() {
			return SecurityHelper.isAnyGranted(securityProvider, Role.MODIF_VD_ORD, Role.MODIF_VD_SOURC, Role.MODIF_HC_HS, Role.MODIF_HAB_DEBPUR, Role.MODIF_NONHAB_DEBPUR);
		}

		@Override
		public boolean isValide(Tiers tiers) {
			return !tiers.isAnnule() && tiers instanceof PersonnePhysique && !tiersService.isInMenageCommun((PersonnePhysique) tiers, null);
		}

		@Override
		public String getLabel() {
			return "Marier";
		}

		@Override
		public String getActionUrl() {
			return "goto:/couple/create.do?pp1=";
		}
	}

	private static class Deceder implements Action {

		@Override
		public boolean isGranted() {
			return SecurityHelper.isAnyGranted(securityProvider, Role.MODIF_VD_ORD, Role.MODIF_VD_SOURC, Role.MODIF_HC_HS, Role.MODIF_HAB_DEBPUR, Role.MODIF_NONHAB_DEBPUR);
		}

		@Override
		public boolean isValide(Tiers tiers) {
			return !tiers.isAnnule() && tiers instanceof PersonnePhysique && tiersService.getDateDeces((PersonnePhysique) tiers) == null;
		}

		@Override
		public String getLabel() {
			return "Enregistrer décès";
		}

		@Override
		public String getActionUrl() {
			return "goto:/deces/recap.do?numero=";
		}
	}

	private static class Separer implements Action {

		@Override
		public boolean isGranted() {
			return SecurityHelper.isAnyGranted(securityProvider, Role.MODIF_VD_ORD, Role.MODIF_VD_SOURC, Role.MODIF_HC_HS, Role.MODIF_HAB_DEBPUR, Role.MODIF_NONHAB_DEBPUR);
		}

		@Override
		public boolean isValide(Tiers tiers) {
			return !tiers.isAnnule() && tiers instanceof MenageCommun && tiersService.isMenageActif((MenageCommun) tiers, null);
		}

		@Override
		public String getLabel() {
			return "Séparer";
		}

		@Override
		public String getActionUrl() {
			return "goto:/separation/recap.do?numeroCple=";
		}
	}

	private static class Reactiver implements Action {

		@Override
		public boolean isGranted() {
			return SecurityHelper.isGranted(securityProvider, Role.ANNUL_TIERS);
		}

		@Override
		public boolean isValide(Tiers tiers) {
			return tiers.isDesactive(null);
		}

		@Override
		public String getLabel() {
			return "Réactiver";
		}

		@Override
		public String getActionUrl() {
			return "goto:/activation/reactivation/recap.do?numero=";
		}
	}

	private static class AnnulerCouple implements Action {

		@Override
		public boolean isGranted() {
			return SecurityHelper.isAnyGranted(securityProvider, Role.MODIF_VD_ORD, Role.MODIF_VD_SOURC, Role.MODIF_HC_HS, Role.MODIF_HAB_DEBPUR, Role.MODIF_NONHAB_DEBPUR);
		}

		@Override
		public boolean isValide(Tiers tiers) {
			return !tiers.isAnnule() && tiers instanceof MenageCommun && tiersService.isMenageActif((MenageCommun) tiers, null);
		}

		@Override
		public String getLabel() {
			return "Annuler le couple";
		}

		@Override
		public String getActionUrl() {
			return "goto:/annulation/couple/recap.do?numeroCple=";
		}
	}

	private static class AnnulerSeparation implements Action {

		@Override
		public boolean isGranted() {
			return SecurityHelper.isAnyGranted(securityProvider, Role.MODIF_VD_ORD, Role.MODIF_VD_SOURC, Role.MODIF_HC_HS, Role.MODIF_HAB_DEBPUR, Role.MODIF_NONHAB_DEBPUR);
		}

		@Override
		public boolean isValide(Tiers tiers) {
			return !tiers.isAnnule() && tiers instanceof MenageCommun && tiersService.isDernierForFiscalPrincipalFermePourSeparation(tiers);
		}

		@Override
		public String getLabel() {
			return "Annuler la séparation";
		}

		@Override
		public String getActionUrl() {
			return "goto:/annulation/separation/recap.do?numero=";
		}
	}

	private static class AnnulerDeces implements Action {

		@Override
		public boolean isGranted() {
			return SecurityHelper.isAnyGranted(securityProvider, Role.MODIF_VD_ORD, Role.MODIF_VD_SOURC, Role.MODIF_HC_HS, Role.MODIF_HAB_DEBPUR, Role.MODIF_NONHAB_DEBPUR);
		}

		@Override
		public boolean isValide(Tiers tiers) {
			return !tiers.isAnnule() && tiers instanceof PersonnePhysique && tiersService.getDateDeces((PersonnePhysique) tiers) != null;
		}

		@Override
		public String getLabel() {
			return "Annuler le décès";
		}

		@Override
		public String getActionUrl() {
			return "goto:/annulation/deces/recap.do?numero=";
		}
	}

	private static class AnnulerTiers implements Action {

		@Override
		public boolean isGranted() {
			return SecurityHelper.isGranted(securityProvider, Role.ANNUL_TIERS);
		}

		@Override
		public boolean isValide(Tiers tiers) {
			return !tiers.isDesactive(null);
		}

		@Override
		public String getLabel() {
			return "Annuler le tiers";
		}

		@Override
		public String getActionUrl() {
			return "goto:/activation/annulation/recap.do?numero=";
		}
	}

	private static class RecalculerParentes implements Action {

		@Override
		public boolean isGranted() {
			return SecurityHelper.isAnyGranted(securityProvider, Role.TESTER, Role.ADMIN);
		}

		@Override
		public boolean isValide(Tiers tiers) {
			return tiers instanceof PersonnePhysique && ((PersonnePhysique) tiers).isHabitantVD();
		}

		@Override
		public String getLabel() {
			return "Recalculer les parentés";
		}

		@Override
		public String getActionUrl() {
			return "post:/admin/refreshParentes.do?id=";
		}
	}
}


