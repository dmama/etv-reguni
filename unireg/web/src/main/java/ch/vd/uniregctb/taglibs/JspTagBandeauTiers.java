package ch.vd.uniregctb.taglibs;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyTagSupport;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang.StringUtils;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.util.HtmlUtils;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.adresse.AdresseEnvoiDetaillee;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.adresse.TypeAdresseFiscale;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.declaration.Periodicite;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.uniregctb.general.view.TypeAvatar;
import ch.vd.uniregctb.interfaces.model.ApplicationFiscale;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityProvider;
import ch.vd.uniregctb.tiers.AutreCommunaute;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.PeriodeDecompte;
import ch.vd.uniregctb.type.PeriodiciteDecompte;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.utils.WebContextUtils;

/**
 * Affiche les informations générales d'un tiers (nom, prénom, type d'assujettissement, adresse, image, ...).
 */
public class JspTagBandeauTiers extends BodyTagSupport implements MessageSourceAware {

	private static final long serialVersionUID = -8839926167038851691L;

	/*
	 * Ces membres sont statiques pour permettre l'injection par Spring des beans accessibles par toutes les instances de ce tag
	 */
	private static MessageSource messageSource;
	private static TiersDAO tiersDAO;
	private static TiersService tiersService;
	private static ServiceInfrastructureService serviceInfrastructure;
	private static AdresseService adresseService;
	private static PlatformTransactionManager transactionManager;

	private static final List<Action> actions;

	static {
		actions = new ArrayList<Action>();
		actions.add(new Reindexer());
		actions.add(new Marier());
		actions.add(new Deceder());
		actions.add(new Separer());
		actions.add(new Reactiver());
		actions.add(new AnnulerCouple());
		actions.add(new AnnulerSeparation());
		actions.add(new AnnulerDeces());
		actions.add(new AnnulerTiers());
		actions.add(new Exporter());
	}

	private Long numero;
	private String titre;
	private String cssClass;
	private boolean showValidation = true;
	private boolean showEvenementsCivils = true;
	private boolean showLinks = true;
	private boolean showAvatar = true;
	private boolean showComplements = false;
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

	@SuppressWarnings({"UnusedDeclaration"})
	public void setServiceInfrastructure(ServiceInfrastructureService serviceInfrastructure) {
		JspTagBandeauTiers.serviceInfrastructure = serviceInfrastructure;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		JspTagBandeauTiers.transactionManager = transactionManager;
	}

	private String buidHtlm() {

		rowcount = 0;

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		return template.execute(new TransactionCallback<String>() {
			@Override
			public String doInTransaction(TransactionStatus status) {
				status.setRollbackOnly();

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
			s.append("    $('#validationMessage').load('").append(url("/validation/message.do?tiers=")).append(tiers.getNumero()).append("&' + new Date().getTime());");
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

		if (showEvenementsCivils && SecurityProvider.isGranted(Role.MODIF_VD_ORD)) {
			final List<EvenementCivilRegPP> evtsNonTraites = tiersService.getEvenementsCivilsNonTraites(tiers);
			if (evtsNonTraites != null && !evtsNonTraites.isEmpty()) {

				final Set<Long> nos = new TreeSet<Long>();
				for (EvenementCivilRegPP evt : evtsNonTraites) {
					final Long indPrincipal = evt.getNumeroIndividuPrincipal();
					if (indPrincipal != null) {
						nos.add(indPrincipal);
					}
					final Long indConjoint = evt.getNumeroIndividuConjoint();
					if (indConjoint != null) {
						nos.add(indConjoint);
					}
				}

				s.append("<tr class=\"evts-civils-non-traites\"><td colspan=\"3\" width=\"100%\"><center>\n");
				s.append(message("label.tiers.evts.non.traites"));
				for (Long no : nos) {
					s.append(' ').append(no);
				}
				s.append("</center></td></tr>\n");
			}
		}

		// Numéro de contribuable
		s.append("<tr class=\"").append(nextRowClass()).append("\">\n");
		s.append("\t<td width=\"25%\" nowrap>").append(message("label.numero.tiers")).append("&nbsp;:</td>\n");
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

			final List<Action> actionsToDisplay = new ArrayList<Action>(actions.size());
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
		HttpServletRequest request = (HttpServletRequest) this.pageContext.getRequest();
		return request.getContextPath() + relative;
	}

	private String buildVers(Tiers tiers) {
		final StringBuilder s = new StringBuilder();

		s.append("<div style=\"float: right;margin-right: 10px\">\n");
		s.append("<span>").append(message("label.ouvrir.vers")).append(" : </span>\n");

		if (StringUtils.isBlank(urlRetour)) {
			s.append("<select name=\"AppSelect\" onchange=\"App.gotoExternalApp(this);\">\n");
			s.append("\t<option value=\"\">---</option>\n");
			final boolean isEntreprise = tiers instanceof Entreprise;
			if (!isEntreprise) { // [UNIREG-1949] débranchement uniquement vers SIPF pour les PMs
				final String urlTaoPP = serviceInfrastructure.getUrlVers(ApplicationFiscale.TAO_PP, tiers.getNumero());
				final String urlTaoBA = serviceInfrastructure.getUrlVers(ApplicationFiscale.TAO_BA, tiers.getNumero());
				final String urlTaoIS = serviceInfrastructure.getUrlVers(ApplicationFiscale.TAO_IS, tiers.getNumero());
				s.append("\t<option value=\"").append(urlTaoPP).append("\">").append(message("label.TAOPP")).append("</option>\n");
				s.append("\t<option value=\"").append(urlTaoBA).append("\">").append(message("label.TAOBA")).append("</option>\n");
				s.append("\t<option value=\"").append(urlTaoIS).append("\">").append(message("label.TAOIS")).append("</option>\n");
			}
			final String urlSIPF = serviceInfrastructure.getUrlVers(ApplicationFiscale.SIPF, tiers.getNumero());
			s.append("\t<option value=\"").append(urlSIPF).append("\">").append(message("label.SIPF")).append("</option>\n");
			if (!isEntreprise) { // [UNIREG-1949] débranchement uniquement vers SIPF pour les PMs
				s.append("\t<option value=\"launchcat.do?numero=").append(tiers.getNumero()).append("\">").append(message("label.CAT")).append("</option>\n");
			}
			s.append("</select>\n");
		}
		else {
			s.append("<a href=\"").append(urlRetour).append(tiers.getNumero()).append("\" class=\"detail\" title=\"").append(message("label.retour.application.appelante")).append("\">&nbsp;</a>\n");
		}

		s.append("</div>\n");

		return s.toString();
	}

	private String buildImageTiers(Tiers tiers) {

		final TypeAvatar type = getTypeAvatar(tiers);
		final String image = getImageUrl(type, false);

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
			final TypeAvatar type = getTypeAvatar(menage);

			final String image = getImageUrl(type, true);

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
				final TypeAvatar type = getTypeAvatar(principal);
				final String image = getImageUrl(type, true);
				s = new StringBuilder();
				s.append("<a title=\"Aller vers le tiers principal du ménage\" href=\"").append(url("/tiers/visu.do?id=")).append(principal.getId()).append("\">");
				s.append("<img class=\"iepngfix avatar\" src=\"").append(url(image)).append("\">\n");
				s.append("</a>");
			}

			final PersonnePhysique conjoint = ensemble.getConjoint();
			if (conjoint != null) {
				final TypeAvatar type = getTypeAvatar(conjoint);
				final String image = getImageUrl(type, true);
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

	private static String getImageUrl(TypeAvatar type, boolean forLink) {
		final String image;
		switch (type) {
		case HOMME:
			image = "homme.png";
			break;
		case FEMME:
			image = "femme.png";
			break;
		case SEXE_INCONNU:
			image = "inconnu.png";
			break;
		case MC_MIXTE:
			image = "menagecommun.png";
			break;
		case MC_HOMME_SEUL:
			image = "homme_seul.png";
			break;
		case MC_FEMME_SEULE:
			image = "femme_seule.png";
			break;
		case MC_HOMME_HOMME:
			image = "homme_homme.png";
			break;
		case MC_FEMME_FEMME:
			image = "femme_femme.png";
			break;
		case MC_SEXE_INCONNU:
			image = "mc_inconnu.png";
			break;
		case ENTREPRISE:
			image = "entreprise.png";
			break;
		case ETABLISSEMENT:
			image = "etablissement.png";
			break;
		case AUTRE_COMM:
			image = "autrecommunaute.png";
			break;
		case COLLECT_ADMIN:
			image = "collectiviteadministrative.png";
			break;
		case DEBITEUR:
			image = "debiteur.png";
			break;
		default:
			throw new IllegalArgumentException("Type de tiers inconnu = [" + type + ']');
		}

		final String basePath = (forLink ? "/images/tiers/links/" : "/images/tiers/");
		return basePath + image;
	}

	private TypeAvatar getTypeAvatar(Tiers tiers) {

		final TypeAvatar type;

		if (tiers instanceof PersonnePhysique) {
			final Sexe sexe = tiersService.getSexe((PersonnePhysique) tiers);
			if (sexe == null) {
				type = TypeAvatar.SEXE_INCONNU;
			}
			else if (sexe == Sexe.MASCULIN) {
				type = TypeAvatar.HOMME;
			}
			else {
				type = TypeAvatar.FEMME;
			}
		}
		else if (tiers instanceof Entreprise) {
			type = TypeAvatar.ENTREPRISE;
		}
		else if (tiers instanceof AutreCommunaute) {
			type = TypeAvatar.AUTRE_COMM;
		}
		else if (tiers instanceof MenageCommun) {
			final EnsembleTiersCouple ensemble = tiersService.getEnsembleTiersCouple((MenageCommun) tiers, null);
			final PersonnePhysique principal = ensemble.getPrincipal();
			final PersonnePhysique conjoint = ensemble.getConjoint();

			Sexe sexePrincipal = tiersService.getSexe(principal);
			Sexe sexeConjoint = tiersService.getSexe(conjoint);
			if (sexePrincipal == null && sexeConjoint != null) {
				// Le conjoint passe principal si son sexe est connu mais que celui du principal ne l'est pas
				sexePrincipal = sexeConjoint;
				sexeConjoint = null;
			}

			if (sexePrincipal == null) {
				type = TypeAvatar.MC_SEXE_INCONNU;
			}
			else if (sexeConjoint == null) {
				if (sexePrincipal == Sexe.MASCULIN) {
					type = TypeAvatar.MC_HOMME_SEUL;
				}
				else {
					type = TypeAvatar.MC_FEMME_SEULE;
				}
			}
			else {
				if (sexePrincipal == sexeConjoint) {
					if (sexePrincipal == Sexe.MASCULIN) {
						type = TypeAvatar.MC_HOMME_HOMME;
					}
					else {
						type = TypeAvatar.MC_FEMME_FEMME;
					}
				}
				else {
					type = TypeAvatar.MC_MIXTE;
				}
			}
		}
		else if (tiers instanceof CollectiviteAdministrative) {
			type = TypeAvatar.COLLECT_ADMIN;
		}
		else if (tiers instanceof DebiteurPrestationImposable) {
			type = TypeAvatar.DEBITEUR;
		}
		else {
			type = null;
		}

		return type;
	}

	private static interface Action {
		boolean isGranted();
		boolean isValide(Tiers tiers);
		String getLabel();
		String getActionUrl();
	}

	private static class Reindexer implements Action {

		@Override
		public boolean isGranted() {
			return SecurityProvider.isAnyGranted(Role.TESTER, Role.ADMIN);
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
			return SecurityProvider.isAnyGranted(Role.TESTER, Role.ADMIN);
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
			return SecurityProvider.isAnyGranted(Role.MODIF_VD_ORD, Role.MODIF_VD_SOURC, Role.MODIF_HC_HS, Role.MODIF_HAB_DEBPUR, Role.MODIF_NONHAB_DEBPUR);
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
			return SecurityProvider.isAnyGranted(Role.MODIF_VD_ORD, Role.MODIF_VD_SOURC, Role.MODIF_HC_HS, Role.MODIF_HAB_DEBPUR, Role.MODIF_NONHAB_DEBPUR);
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
			return SecurityProvider.isAnyGranted(Role.MODIF_VD_ORD, Role.MODIF_VD_SOURC, Role.MODIF_HC_HS, Role.MODIF_HAB_DEBPUR, Role.MODIF_NONHAB_DEBPUR);
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
			return SecurityProvider.isGranted(Role.ANNUL_TIERS);
		}

		@Override
		public boolean isValide(Tiers tiers) {
			return tiers.isAnnule();
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
			return SecurityProvider.isAnyGranted(Role.MODIF_VD_ORD, Role.MODIF_VD_SOURC, Role.MODIF_HC_HS, Role.MODIF_HAB_DEBPUR, Role.MODIF_NONHAB_DEBPUR);
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
			return SecurityProvider.isAnyGranted(Role.MODIF_VD_ORD, Role.MODIF_VD_SOURC, Role.MODIF_HC_HS, Role.MODIF_HAB_DEBPUR, Role.MODIF_NONHAB_DEBPUR);
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
			return SecurityProvider.isAnyGranted(Role.MODIF_VD_ORD, Role.MODIF_VD_SOURC, Role.MODIF_HC_HS, Role.MODIF_HAB_DEBPUR, Role.MODIF_NONHAB_DEBPUR);
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
			return SecurityProvider.isGranted(Role.ANNUL_TIERS);
		}

		@Override
		public boolean isValide(Tiers tiers) {
			return !tiers.isAnnule();
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
}


