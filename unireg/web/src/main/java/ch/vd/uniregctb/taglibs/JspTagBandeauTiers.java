package ch.vd.uniregctb.taglibs;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyTagSupport;
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
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.adresse.AdresseEnvoiDetaillee;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.adresse.TypeAdresseFiscale;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.declaration.Periodicite;
import ch.vd.uniregctb.evenement.EvenementCivilData;
import ch.vd.uniregctb.general.view.TypeAvatar;
import ch.vd.uniregctb.interfaces.model.EtatCivil;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
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
import ch.vd.uniregctb.validation.ValidationService;
import ch.vd.uniregctb.wsclient.fidor.FidorService;

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
	private static ServiceCivilService serviceCivilService;
	private static AdresseService adresseService;
	private static FidorService fidorService;
	private static PlatformTransactionManager transactionManager;
	private static ValidationService validationService;

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

	public void setMessageSource(MessageSource messageSource) {
		JspTagBandeauTiers.messageSource = messageSource;
	}

	public void setTiersDAO(TiersDAO tiersDAO) {
		JspTagBandeauTiers.tiersDAO = tiersDAO;
	}

	public void setTiersService(TiersService tiersService) {
		JspTagBandeauTiers.tiersService = tiersService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setServiceCivilService(ServiceCivilService serviceCivilService) {
		JspTagBandeauTiers.serviceCivilService = serviceCivilService;
	}

	public void setAdresseService(AdresseService adresseService) {
		JspTagBandeauTiers.adresseService = adresseService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setFidorService(FidorService fidorService) {
		JspTagBandeauTiers.fidorService = fidorService;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		JspTagBandeauTiers.transactionManager = transactionManager;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setValidationService(ValidationService validationService) {
		JspTagBandeauTiers.validationService = validationService;
	}

	private String buidHtlm() {

		rowcount = 0;

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		return (String) template.execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				status.setRollbackOnly();

				final Tiers tiers = tiersDAO.get(numero);
				if (tiers == null) {
					return "";
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

				if (showAvatar) {
					s.append("<table cellspacing=\"0\" cellpadding=\"0\" border=\"0\"><tr><td>\n");
					s.append(buildDescriptifTiers(tiers));
					s.append("</td><td width=\"130 px\">\n");
					s.append(buildImageTiers(tiers));
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
			final List<EvenementCivilData> evtsNonTraites = tiersService.getEvenementsCivilsNonTraites(tiers);
			if (evtsNonTraites != null && !evtsNonTraites.isEmpty()) {

				final Set<Long> nos = new TreeSet<Long>();
				for (EvenementCivilData evt : evtsNonTraites) {
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
					s.append(" ").append(no);
				}
				s.append("</center></td></tr>\n");
			}
		}

		if (showValidation) {
			final ValidationResults validationResults = validationService.validate(tiers);
			setErreursEtatsCivils(tiers, validationResults);
			setErreursAdresses(tiers, validationResults);

			if (validationResults.hasErrors() || validationResults.hasWarnings()) {
				s.append("<tr><td colspan=\"3\" width=\"100%\">\n");
				s.append(buildValidationResults(validationResults));
				s.append("</td></tr>\n");
			}
		}

		// Numéro de contribuable
		s.append("<tr class=\"").append(nextRowClass()).append("\">\n");
		s.append("\t<td width=\"25%\">").append(message("label.numero.tiers")).append("&nbsp;:</td>\n");
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

		// Réindexation
		if (showLinks && (SecurityProvider.isGranted(Role.TESTER) || SecurityProvider.isGranted(Role.ADMIN))) {
			s.append("\t<td width=\"25%\">\n");
			s.append("\t\t<form method=\"post\" style=\"text-align: right; padding-right: 1em\"");
			s.append("action=\"").append(url("/admin/indexation.do")).append("?action=reindexTiers&id=").append(tiers.getNumero()).append("\">\n");
			s.append("\t\t\t<input type=\"submit\" value=\"").append(message("label.bouton.forcer.reindexation")).append("\"/>");
			s.append("\t\t</form>\n");
			s.append("\t</td>\n");
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
		catch (AdresseException e) {
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
						s.append("&nbsp;(").append(message("option.periode.decompte." + periodeDecompte.name())).append(")");
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

		if (urlRetour == null) {
			s.append("<select name=\"AppSelect\" onchange=\"javascript:AppSelect_OnChange(this);\">\n");
			s.append("\t<option value=\"\">---</option>\n");
			final boolean isEntreprise = tiers instanceof Entreprise;
			if (!isEntreprise) { // [UNIREG-1949] débranchement uniquement vers SIPF pour les PMs
				s.append("\t<option value=\"").append(fidorService.getUrlTaoPP(tiers.getNumero())).append("\">").append(message("label.TAOPP")).append("</option>\n");
				s.append("\t<option value=\"").append(fidorService.getUrlTaoBA(tiers.getNumero())).append("\">").append(message("label.TAOBA")).append("</option>\n");
				s.append("\t<option value=\"").append(fidorService.getUrlTaoIS(tiers.getNumero())).append("\">").append(message("label.TAOIS")).append("</option>\n");
			}
			s.append("\t<option value=\"").append(fidorService.getUrlSipf(tiers.getNumero())).append("\">").append(message("label.SIPF")).append("</option>\n");
			if (!isEntreprise) { // [UNIREG-1949] débranchement uniquement vers SIPF pour les PMs
				s.append("\t<option value=\"launchcat.do?numero=").append(tiers.getNumero()).append("\">").append(message("label.CAT")).append("</option>\n");
			}
			s.append("</select>\n");
		}
		else {
			s.append("<a href=\"").append(urlRetour).append(tiers.getNumero()).append("\" class=\"detail\" title=\"").append(message("label.retour.application.appelante")).append("\">&nbsp;</a>\n");
		}

		s.append("<script type=\"text/javascript\">\n");
		s.append("\tfunction AppSelect_OnChange(select) {\n");
		s.append("\t\tvar value = select.options[select.selectedIndex].value;\n");
		s.append("\t\tif ( value && value !== '') {\n");
		s.append("\t\t\twindow.location.href = value;\n");
		s.append("\t\t}\n");
		s.append("\t}\n");
		s.append("</script>\n");

		s.append("</div>\n");

		return s.toString();
	}

	private String buildValidationResults(ValidationResults validationResults) {
		StringBuilder s = new StringBuilder();

		s.append("<table class=\"validation_error\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\">\n");
		s.append("<tr><td class=\"heading\">").append(message("label.validation.problemes.detectes")).append(" <span id=\"val_script\">(<a href=\"#\" onclick=\"javascript:showDetails()\">")
				.append(message("label.validation.voir.details")).append("</a>)</span></td></tr>\n");
		s.append("<tr id=\"val_errors\"><td class=\"details\"><ul>\n");
		for (String error : validationResults.getErrors()) {
			s.append("<li class=\"err\">").append(message("label.validation.erreur")).append(": ").append(HtmlUtils.htmlEscape(error)).append("</li>\n");
		}
		for (String warning : validationResults.getWarnings()) {
			s.append("<li class=\"warn\">").append(message("label.validation.warning")).append(": ").append(HtmlUtils.htmlEscape(warning)).append("</li>\n");
		}
		s.append("</ul></td></tr>\n");
		s.append("</table>\n");

		s.append("<script type=\"text/javascript\">\n");
		s.append("    // cache les erreurs par défaut\n");
		s.append("    $('#val_errors').hide();\n");
		s.append("\n");
		s.append("    // affiche les erreurs\n");
		s.append("    function showDetails() {\n");
		s.append("        $('#val_errors').show();\n");
		s.append("        $('#val_script').hide();\n");
		s.append("    }\n");
		s.append("</script>");

		return s.toString();
	}

	private String buildImageTiers(Tiers tiers) {

		final TypeAvatar type = getTypeAvatar(tiers);
		final String image = getImageUrl(type);

		final StringBuilder s = new StringBuilder();
		s.append("<img class=\"iepngfix\" src=\"").append(url(image)).append("\">\n");

		return s.toString();
	}

	private static String getImageUrl(TypeAvatar type) {
		final String image;
		switch (type) {
		case HOMME:
			image = "/images/tiers/homme.png";
			break;
		case FEMME:
			image = "/images/tiers/femme.png";
			break;
		case SEXE_INCONNU:
			image = "/images/tiers/inconnu.png";
			break;
		case MC_MIXTE:
			image = "/images/tiers/menagecommun.png";
			break;
		case MC_HOMME_SEUL:
			image = "/images/tiers/homme_seul.png";
			break;
		case MC_FEMME_SEULE:
			image = "/images/tiers/femme_seule.png";
			break;
		case MC_HOMME_HOMME:
			image = "/images/tiers/homme_homme.png";
			break;
		case MC_FEMME_FEMME:
			image = "/images/tiers/femme_femme.png";
			break;
		case MC_SEXE_INCONNU:
			image = "/images/tiers/mc_inconnu.png";
			break;
		case ENTREPRISE:
			image = "/images/tiers/entreprise.png";
			break;
		case ETABLISSEMENT:
			image = "/images/tiers/etablissement.png";
			break;
		case AUTRE_COMM:
			image = "/images/tiers/autrecommunaute.png";
			break;
		case COLLECT_ADMIN:
			image = "/images/tiers/collectiviteadministrative.png";
			break;
		case DEBITEUR:
			image = "/images/tiers/debiteur.png";
			break;
		default:
			throw new IllegalArgumentException("Type de tiers inconnu = [" + type + "]");
		}
		return image;
	}

	/**
	 * Met à jour les erreurs autour des états civils
	 *
	 * @param tiers             un tiers
	 * @param validationResults le résultat de validation à augmenter
	 */
	private void setErreursEtatsCivils(Tiers tiers, ValidationResults validationResults) {
		if (tiers instanceof PersonnePhysique) {
			final PersonnePhysique pp = (PersonnePhysique) tiers;
			if (pp.isConnuAuCivil()) {
				final int year = RegDate.get().year();
				final Individu ind = serviceCivilService.getIndividu(pp.getNumeroIndividu(), year);
				for (EtatCivil etatCivil : ind.getEtatsCivils()) {
					if (etatCivil.getDateDebutValidite() == null) {
						final String message = String.format("Le contribuable possède un état civil (%s) sans date de début. Dans la mesure du possible, cette date a été estimée.",
								etatCivil.getTypeEtatCivil().asCore());
						validationResults.addWarning(message);
					}
				}
			}
		}
	}

	/**
	 * Calcul les adresses historiques de manière stricte, et reporte toutes les erreurs trouvées.
	 *
	 * @param tiers             le tiers dont on veut vérifier les adresses
	 * @param validationResults le résultat de la validation à compléter avec les éventuelles erreurs trouvées.
	 */
	private void setErreursAdresses(Tiers tiers, ValidationResults validationResults) {
		try {
			adresseService.getAdressesFiscalHisto(tiers, true /* strict */);
		}
		catch (AdresseException e) {
			validationResults.addWarning("Des incohérences ont été détectées dans les adresses du tiers : " + e.getMessage() +
					". Dans la mesure du possible, ces incohérences ont été corrigées à la volée (mais pas sauvées en base).");
		}
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

}


