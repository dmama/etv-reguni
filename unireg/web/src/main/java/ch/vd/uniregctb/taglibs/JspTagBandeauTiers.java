package ch.vd.uniregctb.taglibs;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyTagSupport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.web.util.HtmlUtils;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.uniregctb.activation.ActivationDesactivationHelper;
import ch.vd.uniregctb.adresse.AdresseEnvoiDetaillee;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.adresse.TypeAdresseFiscale;
import ch.vd.uniregctb.avatar.AvatarService;
import ch.vd.uniregctb.avatar.TypeAvatar;
import ch.vd.uniregctb.common.CollectionsUtils;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.common.MovingWindow;
import ch.vd.uniregctb.declaration.Periodicite;
import ch.vd.uniregctb.entreprise.complexe.FusionEntreprisesHelper;
import ch.vd.uniregctb.entreprise.complexe.ScissionEntrepriseHelper;
import ch.vd.uniregctb.entreprise.complexe.TransfertPatrimoineHelper;
import ch.vd.uniregctb.fourreNeutre.FourreNeutreService;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityHelper;
import ch.vd.uniregctb.security.SecurityProviderInterface;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Etablissement;
import ch.vd.uniregctb.tiers.EtatEntreprise;
import ch.vd.uniregctb.tiers.EvenementsCivilsNonTraites;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.FormeLegaleHisto;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.transaction.TransactionTemplate;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.Niveau;
import ch.vd.uniregctb.type.PeriodeDecompte;
import ch.vd.uniregctb.type.PeriodiciteDecompte;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeEtatEntreprise;
import ch.vd.uniregctb.utils.WebContextUtils;

/**
 * Affiche les informations générales d'un tiers (nom, prénom, type d'assujettissement, adresse, image, ...).
 */
public class JspTagBandeauTiers extends BodyTagSupport implements MessageSourceAware {

	private static final long serialVersionUID = 7545284534993448401L;

	private final Logger LOGGER = LoggerFactory.getLogger(JspTagBandeauTiers.class);

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
	private static FourreNeutreService fourreNeutreService;


	public static final List<Action> actions;

	static {
		final List<Action> list = new ArrayList<>();
		list.add(new Reindexer());
		list.add(new RecalculerParentes());
		list.add(new RecalculerFlagHabitant());
		list.add(new ImprimerFourreNeutre());
		list.add(new Separator());

		list.add(new Marier());
		list.add(new Deceder());
		list.add(new Separer());
		list.add(new Reactiver());
		list.add(new AnnulerCouple());
		list.add(new AnnulerSeparation());
		list.add(new AnnulerDeces());
		list.add(new Separator());

		list.add(new TraiterFaillite());
		list.add(new RevoquerFaillite());
		list.add(new DemenagerSiege());
		list.add(new TerminerActivite());
		list.add(new RequisitionRadiationRC());
		list.add(new ReprendreActivitePartielle());
		list.add(new ReinscriptionRegistreCommerce());
		list.add(new AbsorberEntreprises());
		list.add(new ScinderEntreprise());
		list.add(new TransfererPatrimoine());
		list.add(new AnnulerFaillite());
		list.add(new AnnulerDemenagementSiege());
		list.add(new AnnulerFinActivite());
		list.add(new AnnulerFusionEntreprises());
		list.add(new AnnulerScissionEntreprise());
		list.add(new AnnulerTransfertPatrimoine());
		list.add(new Separator());

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
	private TypeAvatar forceAvatar;
	private int rowcount;
	private String urlRetour;
	private TypeAdresseFiscale typeAdresse = TypeAdresseFiscale.COURRIER;

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

	@SuppressWarnings({"UnusedDeclaration"})
	public void setForceAvatar(TypeAvatar forceAvatar) {
		this.forceAvatar = forceAvatar;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setTypeAdresse(TypeAdresseFiscale typeAdresse) {
		this.typeAdresse = typeAdresse;
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

	public  void setFourreNeutreService(FourreNeutreService fourreNeutreService) {
		JspTagBandeauTiers.fourreNeutreService = fourreNeutreService;
	}

	private String buidHtlm() {

		rowcount = 0;

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		return template.execute(new TransactionCallback<String>() {
			@Override
			public String doInTransaction(TransactionStatus status) {

				if (numero == null) {
					throw new IllegalArgumentException("Le numéro du tiers à afficher est nul.");
				}

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
			EvenementsCivilsNonTraites evtsCivilNonTraites = null;
			if (! (tiers instanceof Entreprise || tiers instanceof Etablissement) ) {
				evtsCivilNonTraites = tiersService.getIndividusAvecEvenementsCivilsNonTraites(tiers);
			}
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
		s.append("\t<td width=\"15%\" nowrap>").append(message("label.numero.tiers")).append("&nbsp;:&nbsp;</td>\n");
		s.append("\t<td width=\"50%\">").append(FormatNumeroHelper.numeroCTBToDisplay(numero));
		if (showLinks) {
			final JspTagConsulterLog consulter = new JspTagConsulterLog();
			consulter.setEntityNature("Tiers");
			consulter.setEntityId(String.valueOf(numero));
			s.append(consulter.buildHtml());
		}
		s.append("</td>\n");
		if (!tiers.isAnnule() && showLinks) {
			s.append("\t<td width=\"35%\">\n");
			s.append(buildVers(tiers));
			s.append("\t</td>\n");
		}
		else {
			s.append("\t<td width=\"35%\">&nbsp;</td>\n");
		}
		s.append("</tr>\n");

		// Rôle
		s.append("<tr class=\"").append(nextRowClass()).append("\">\n");
		s.append("\t<td width=\"15%\">").append(message("label.role")).append("&nbsp;:</td>\n");
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

			// Nettoyage des séparateurs non-entourés
			final MovingWindow<Action> wndActions = new MovingWindow<>(actionsToDisplay);
			while (wndActions.hasNext()) {
				final MovingWindow.Snapshot<Action> snap = wndActions.next();
				final Action current = snap.getCurrent();
				if (current.isSeparator()) {
					if (snap.getPrevious() == null || snap.getPrevious().isSeparator() || snap.getNext() == null) {
						wndActions.remove();
					}
				}
			}

			if (actionsToDisplay.isEmpty()) {
				// pas d'action à afficher
				s.append("\t<td width=\"35%\">&nbsp;</td>\n");
			}
			else {
				s.append("\t<td width=\"35%\">\n");
				s.append("\t<div style=\"float:right;margin-right:10px;text-align:right\">\n");
				s.append("\t\t<span>").append(message("label.actions")).append(" : </span>\n");
				s.append("\t\t<select onchange=\"return App.executeAction($(this).val() + ").append(tiers.getNumero()).append(");\">\n");
				s.append("\t\t\t<option disabled selected>&mdash;&mdash;</option>\n");

				for (Action action : actionsToDisplay) {
					if (action.isSeparator()) {
						s.append("\t\t\t<option disabled>&mdash;&mdash;</option>\n");
					}
					else {
						s.append("\t\t\t<option value=\"").append(action.getActionUrl()).append("\">").append(action.getLabel()).append("</option>\n");
					}
				}

				s.append("\t\t</select>");
				s.append("\t</div>\n");
				s.append("\t</td>\n");
			}
		}
		else {
			s.append("\t<td width=\"35%\">&nbsp;</td>\n");
		}
		s.append("</tr>\n");

		// Adresse envoi
		try {
			final AdresseEnvoiDetaillee adresse = adresseService.getAdresseEnvoi(tiers, null, typeAdresse, false);

			// 1ère ligne
			s.append("<tr class=\"").append(nextRowClass()).append("\">\n");
			s.append("\t<td width=\"15%\">").append(message("label.adresse")).append("&nbsp;:</td>\n");
			final String ligne1 = adresse.getLigne1();
			if (StringUtils.isNotBlank(ligne1)) {
				s.append("\t<td width=\"85%\" colspan=\"2\">").append(HtmlUtils.htmlEscape(ligne1)).append("</td>\n");
			}
			s.append("</tr>\n");

			// lignes 2 à 6
			boolean typeAdresseEcrit = false;
			for (int i = 2; i <= 6; ++i) {
				final String ligne = adresse.getLigne(i);
				if (StringUtils.isNotBlank(ligne)) {
					s.append("<tr class=\"").append(nextRowClass()).append("\">\n");
					s.append("\t<td width=\"15%\">");
					if (typeAdresse != TypeAdresseFiscale.COURRIER && !typeAdresseEcrit) {
						s.append("(").append(message("option.usage." + typeAdresse.name())).append(")");
						typeAdresseEcrit = true;
					}
					else {
						s.append("&nbsp;");
					}
					s.append("</td>\n");
					s.append("\t<td width=\"85%\" colspan=\"2\">").append(HtmlUtils.htmlEscape(ligne)).append("</td>\n");
					s.append("</tr>\n");
				}
			}
		}
		catch (Exception e) {
			LOGGER.error(String.format("Une exception est survenue pendant le rendu du bandeau (%s): %s.", tiers.toString(), e.getMessage()), e);
			s.append("<tr class=\"").append(nextRowClass()).append("\">\n");
			s.append("\t<td width=\"15%\">").append(message("label.adresse")).append("&nbsp;:</td>\n");
			s.append("\t<td width=\"85%\" colspan=\"2\" class=\"error\">").append(message("error.adresse.envoi.entete")).append("</td>\n");
			s.append("</tr>\n");
			s.append("<tr class=\"").append(nextRowClass()).append("\">\n");
			s.append("\t<td width=\"15%\">&nbsp;</td>\n");
			s.append("\t<td width=\"85%\"  colspan=\"2\" class=\"error\">=&gt;&nbsp;").append(HtmlUtils.htmlEscape(e.getMessage())).append("</td>\n");
			s.append("</tr>\n");
			s.append("<tr class=\"").append(nextRowClass()).append("\">\n");
			s.append("\t<td width=\"15%\">&nbsp;</td>\n");
			s.append("\t<td  width=\"85%\"  colspan=\"2\" class=\"error\">").append(message("error.adresse.envoi.remarque")).append("</td>\n");
			s.append("</tr>\n");
		}

		if (showComplements) {
			if (tiers instanceof PersonnePhysique) {
				// Date de naissance
				final PersonnePhysique pp = (PersonnePhysique) tiers;
				final RegDate dateNaissance = tiersService.getDateNaissance(pp);
				s.append("<tr class=\"").append(nextRowClass()).append("\">\n");
				s.append("\t<td width=\"15%\">").append(message("label.date.naissance")).append("&nbsp;:</td>\n");
				s.append("\t<td width=\"50%\">").append(RegDateHelper.dateToDisplayString(dateNaissance)).append("</td>\n");
				s.append("\t<td width=\"35%\">&nbsp;</td>\n");
				s.append("</tr>\n");

				// Numéro AVS
				final String numeroAssureSocial = tiersService.getNumeroAssureSocial(pp);
				s.append("<tr class=\"").append(nextRowClass()).append("\">\n");
				s.append("\t<td width=\"15%\">").append(message("label.nouveau.numero.avs")).append("&nbsp;:</td>\n");
				s.append("\t<td width=\"50%\">").append(FormatNumeroHelper.formatNumAVS(numeroAssureSocial)).append("</td>\n");
				s.append("\t<td width=\"35%\">&nbsp;</td>\n");
				s.append("</tr>\n");

				// Ancien numéro AVS
				final String ancienNumeroAssureSocial = tiersService.getAncienNumeroAssureSocial(pp);
				s.append("<tr class=\"").append(nextRowClass()).append("\">\n");
				s.append("\t<td width=\"15%\">").append(message("label.ancien.numero.avs")).append("&nbsp;:</td>\n");
				s.append("\t<td width=\"50%\">").append(FormatNumeroHelper.formatAncienNumAVS(ancienNumeroAssureSocial)).append("</td>\n");
				s.append("\t<td width=\"35%\">&nbsp;</td>\n");
				s.append("</tr>\n");
			}
			else if (tiers instanceof DebiteurPrestationImposable) {
				final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiers;

				// Catégorie Impôt-source
				s.append("<tr class=\"").append(nextRowClass()).append("\">\n");
				s.append("\t<td width=\"15%\">").append(message("label.debiteur.is")).append("&nbsp;:</td>\n");
				s.append("\t<td width=\"50%\">").append(message("option.categorie.impot.source." + dpi.getCategorieImpotSource().name())).append("</td>\n");
				s.append("\t<td width=\"35%\">&nbsp;</td>\n");
				s.append("</tr>\n");

				// Périodicité
				final Periodicite periodicite = dpi.getPeriodiciteAt(RegDate.get());
				if (periodicite != null) {
					final PeriodiciteDecompte periodiciteDecompte = periodicite.getPeriodiciteDecompte();
					final PeriodeDecompte periodeDecompte = periodicite.getPeriodeDecompte();
					s.append("<tr class=\"").append(nextRowClass()).append("\">\n");
					s.append("\t<td width=\"15%\">").append(message("label.periodicite")).append("&nbsp;:</td>\n");
					s.append("\t<td width=\"50%\">");
					s.append(message("option.periodicite.decompte." + periodiciteDecompte.name()));
					if (periodiciteDecompte == PeriodiciteDecompte.UNIQUE && periodeDecompte != null) {
						s.append("&nbsp;(").append(message("option.periode.decompte." + periodeDecompte.name())).append(')');
					}
					s.append("</td>\n");
					s.append("\t<td width=\"35%\">&nbsp;</td>\n");
					s.append("</tr>\n");
				}

				// Mode de communication
				s.append("<tr class=\"").append(nextRowClass()).append("\">\n");
				s.append("\t<td width=\"15%\">").append(message("label.mode.communication")).append("&nbsp;:</td>\n");
				s.append("\t<td width=\"50%\">").append(message("option.mode.communication." + dpi.getModeCommunication())).append("</td>\n");
				s.append("\t<td width=\"35%\">&nbsp;</td>\n");
				s.append("</tr>\n");

				// Personne de contact
				s.append("<tr class=\"").append(nextRowClass()).append("\">\n");
				s.append("\t<td width=\"15%\">").append(message("label.personne.contact")).append("&nbsp;:</td>\n");
				final String personneContact = dpi.getPersonneContact();
				s.append("\t<td width=\"50%\">").append(HtmlUtils.htmlEscape(StringUtils.trimToEmpty(personneContact))).append("</td>\n");
				s.append("\t<td width=\"35%\">&nbsp;</td>\n");
				s.append("</tr>\n");

				// Numéro de téléphone fixe
				s.append("<tr class=\"").append(nextRowClass()).append("\">\n");
				s.append("\t<td width=\"15%\">").append(message("label.numero.telephone.fixe")).append("&nbsp;:</td>\n");
				final String numeroTelephonePrive = dpi.getNumeroTelephonePrive();
				s.append("\t<td width=\"50%\">").append(HtmlUtils.htmlEscape(StringUtils.trimToEmpty(numeroTelephonePrive))).append("</td>\n");
				s.append("\t<td width=\"35%\">&nbsp;</td>\n");
				s.append("</tr>\n");
			}
			else if (tiers instanceof Entreprise) {
				final Entreprise entreprise = (Entreprise) tiers;

				// numéro IDE
				final String numeroIDE = tiersService.getNumeroIDE(entreprise);
				s.append("<tr class=\"").append(nextRowClass()).append("\">\n");
				s.append("\t<td width=\"15%\">").append(message("label.numero.ide")).append("&nbsp;:</td>\n");
				s.append("\t<td width=\"50%\">").append(FormatNumeroHelper.formatNumIDE(numeroIDE)).append("</td>\n");
				s.append("\t<td width=\"35%\">&nbsp;</td>\n");
				s.append("</tr>\n");

				// forme juridique
				final List<FormeLegaleHisto> formesJuridiques = tiersService.getFormesLegales(entreprise, false);
				final FormeLegale formeJuridique = formesJuridiques != null && !formesJuridiques.isEmpty() ? CollectionsUtils.getLastElement(formesJuridiques).getFormeLegale() : null;
				s.append("<tr class=\"").append(nextRowClass()).append("\">\n");
				s.append("\t<td width=\"15%\">").append(message("label.forme.juridique")).append("&nbsp;:</td>\n");
				s.append("\t<td width=\"50%\">").append(formeJuridique != null ? formeJuridique.toString() : StringUtils.EMPTY).append("</td>\n");
				s.append("\t<td width=\"35%\">&nbsp;</td>\n");
				s.append("</tr>\n");
			}
			else if (tiers instanceof Etablissement) {
				final Etablissement etablissement = (Etablissement) tiers;

				// numéro IDE
				final String numeroIDE = tiersService.getNumeroIDE(etablissement);
				s.append("<tr class=\"").append(nextRowClass()).append("\">\n");
				s.append("\t<td width=\"15%\">").append(message("label.numero.ide")).append("&nbsp;:</td>\n");
				s.append("\t<td width=\"50%\">").append(FormatNumeroHelper.formatNumIDE(numeroIDE)).append("</td>\n");
				s.append("\t<td width=\"35%\">&nbsp;</td>\n");
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
		final String image = buildImageUrl(forceAvatar == null ? avatarService.getTypeAvatar(tiers) : forceAvatar, false);
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

	public interface Action {
		boolean isGranted();
		boolean isValide(Tiers tiers);
		String getLabel();
		String getActionUrl();

		/**
		 * @return <code>false</code> pour tous les éléments sauf les séparateurs
		 */
		default boolean isSeparator() {
			return false;
		}
	}

	private static class Separator implements Action {

		@Override
		public boolean isGranted() {
			return true;
		}

		@Override
		public boolean isValide(Tiers tiers) {
			return true;
		}

		@Override
		public String getLabel() {
			return "---";
		}

		@Override
		public String getActionUrl() {
			return null;
		}

		@Override
		public boolean isSeparator() {
			return true;
		}
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
			return "post:/admin/indexation/reindexTiers.do?id=";
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
			return "post:/admin/dbdump/dumptiers.do?tiers=";
		}
	}
	private static class ImprimerFourreNeutre implements Action {
		@Override
		public boolean isGranted() {
			return SecurityHelper.isAnyGranted(securityProvider, Role.GEST_FOURRE_NEUTRE);
		}

		@Override
		public boolean isValide(Tiers tiers) {
			return fourreNeutreService.isAutorisePourFourreNeutre(tiers.getId());
		}

		@Override
		public String getLabel() {
			return "Imprimer une fourre neutre";
		}

		@Override
		public String getActionUrl() {
			return "goto:/fourre-neutre/imprimer.do?numero=";
		}
	}

	private static class Marier implements Action {

		@Override
		public boolean isGranted() {
			return SecurityHelper.isAnyGranted(securityProvider, Role.MODIF_VD_ORD, Role.MODIF_VD_SOURC, Role.MODIF_HC_HS, Role.MODIF_HAB_DEBPUR, Role.MODIF_NONHAB_DEBPUR);
		}

		@Override
		public boolean isValide(Tiers tiers) {
			return !tiers.isAnnule()
					&& tiers instanceof PersonnePhysique
					&& !tiersService.isInMenageCommun((PersonnePhysique) tiers, null)
					&& SecurityHelper.getDroitAcces(securityProvider, tiers) == Niveau.ECRITURE;
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
			return !tiers.isAnnule()
					&& tiers instanceof PersonnePhysique
					&& tiersService.getDateDeces((PersonnePhysique) tiers) == null
					&& SecurityHelper.getDroitAcces(securityProvider, tiers) == Niveau.ECRITURE;
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
			return !tiers.isAnnule()
					&& tiers instanceof MenageCommun
					&& tiersService.isMenageActif((MenageCommun) tiers, null)
					&& SecurityHelper.getDroitAcces(securityProvider, tiers) == Niveau.ECRITURE;
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
			final boolean droitOk = ActivationDesactivationHelper.isActivationDesactivationAllowed(tiers.getNatureTiers(), securityProvider)
					&& SecurityHelper.getDroitAcces(securityProvider, tiers) == Niveau.ECRITURE;
			return droitOk && tiers.isDesactive(null);
		}

		@Override
		public String getLabel() {
			return "Réactiver";
		}

		@Override
		public String getActionUrl() {
			return "goto:/activation/reactivate.do?numero=";
		}
	}

	private static class AnnulerCouple implements Action {

		@Override
		public boolean isGranted() {
			return SecurityHelper.isAnyGranted(securityProvider, Role.MODIF_VD_ORD, Role.MODIF_VD_SOURC, Role.MODIF_HC_HS, Role.MODIF_HAB_DEBPUR, Role.MODIF_NONHAB_DEBPUR);
		}

		@Override
		public boolean isValide(Tiers tiers) {
			return !tiers.isAnnule()
					&& tiers instanceof MenageCommun
					&& tiersService.isMenageActif((MenageCommun) tiers, null)
					&& SecurityHelper.getDroitAcces(securityProvider, tiers) == Niveau.ECRITURE;
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
			return !tiers.isAnnule()
					&& tiers instanceof MenageCommun
					&& tiersService.isDernierForFiscalPrincipalFermePourSeparation(tiers)
					&& SecurityHelper.getDroitAcces(securityProvider, tiers) == Niveau.ECRITURE;
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
			return !tiers.isAnnule()
					&& tiers instanceof PersonnePhysique
					&& tiersService.getDateDeces((PersonnePhysique) tiers) != null
					&& SecurityHelper.getDroitAcces(securityProvider, tiers) == Niveau.ECRITURE;
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
			final boolean droitOk = ActivationDesactivationHelper.isActivationDesactivationAllowed(tiers.getNatureTiers(), securityProvider)
					&& SecurityHelper.getDroitAcces(securityProvider, tiers) == Niveau.ECRITURE;
			return droitOk && !tiers.isDesactive(null);
		}

		@Override
		public String getLabel() {
			return "Annuler le tiers";
		}

		@Override
		public String getActionUrl() {
			return "goto:/activation/deactivate.do?numero=";
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

	private static class RecalculerFlagHabitant implements Action {

		@Override
		public boolean isGranted() {
			return SecurityHelper.isAnyGranted(securityProvider, Role.TESTER, Role.ADMIN);
		}

		@Override
		public boolean isValide(Tiers tiers) {
			return tiers instanceof PersonnePhysique && ((PersonnePhysique) tiers).isConnuAuCivil();
		}

		@Override
		public String getLabel() {
			return "Recalculer le flag habitant";
		}

		@Override
		public String getActionUrl() {
			return "post:/admin/refreshFlagHabitant.do?id=";
		}
	}

	/**
	 * @param entreprise entreprise à tester
	 * @param etat type d'état à vérifier
	 * @return <code>true</code> si l'état actuel de l'entreprise est du type donné
	 */
	private static boolean isEtatCourant(Entreprise entreprise, TypeEtatEntreprise etat) {
		final EtatEntreprise etatActuel = entreprise.getEtatActuel();
		return etatActuel != null && etatActuel.getType() == etat;
	}

	/**
	 * @param entreprise entreprise à tester
	 * @param etats types d'état à vérifier
	 * @return <code>true</code> si l'entreprise a eu au moins un état non-annulé d'un des types donnés
	 */
	private static boolean hadEtatOnce(Entreprise entreprise, Set<TypeEtatEntreprise> etats) {
		final Set<EtatEntreprise> tousEtats = entreprise.getEtats();
		if (tousEtats != null && !tousEtats.isEmpty()) {
			for (EtatEntreprise etatEntreprise : tousEtats) {
				if (!etatEntreprise.isAnnule() && etats.contains(etatEntreprise.getType())) {
					return true;
				}
			}
		}
		return false;
	}

	private static class TraiterFaillite implements Action {
		@Override
		public boolean isGranted() {
			return SecurityHelper.isAnyGranted(securityProvider, Role.FAILLITE_ENTREPRISE);
		}

		@Override
		public boolean isValide(Tiers tiers) {
			return !tiers.isAnnule()
					&& tiers instanceof Entreprise
					&& !isEtatCourant((Entreprise) tiers, TypeEtatEntreprise.EN_FAILLITE)
					&& !hadEtatOnce((Entreprise) tiers, EnumSet.of(TypeEtatEntreprise.ABSORBEE, TypeEtatEntreprise.DISSOUTE, TypeEtatEntreprise.RADIEE_RC))
					&& tiers.getForFiscalPrincipalAt(null) != null
					&& SecurityHelper.getDroitAcces(securityProvider, tiers) == Niveau.ECRITURE;
		}

		@Override
		public String getLabel() {
			return "Traiter la faillite de l'entreprise";
		}

		@Override
		public String getActionUrl() {
			return "goto:/processuscomplexe/faillite/start.do?id=";
		}
	}

	private static class AnnulerFaillite implements Action {
		@Override
		public boolean isGranted() {
			return SecurityHelper.isAnyGranted(securityProvider, Role.FAILLITE_ENTREPRISE);
		}

		@Override
		public boolean isValide(Tiers tiers) {
			return !tiers.isAnnule()
					&& tiers instanceof Entreprise
					&& isEtatCourant((Entreprise) tiers, TypeEtatEntreprise.EN_FAILLITE)
					&& !hadEtatOnce((Entreprise) tiers, EnumSet.of(TypeEtatEntreprise.ABSORBEE, TypeEtatEntreprise.DISSOUTE))
					&& tiers.getForFiscalPrincipalAt(null) == null
					&& SecurityHelper.getDroitAcces(securityProvider, tiers) == Niveau.ECRITURE;
		}

		@Override
		public String getLabel() {
			return "Annuler la faillite de l'entreprise";
		}

		@Override
		public String getActionUrl() {
			return "goto:/processuscomplexe/annulation/faillite/start.do?id=";
		}
	}

	private static class RevoquerFaillite implements Action {
		@Override
		public boolean isGranted() {
			return SecurityHelper.isAnyGranted(securityProvider, Role.FAILLITE_ENTREPRISE);
		}

		@Override
		public boolean isValide(Tiers tiers) {
			return !tiers.isAnnule()
					&& tiers instanceof Entreprise
					&& isEtatCourant((Entreprise) tiers, TypeEtatEntreprise.EN_FAILLITE)
					&& !hadEtatOnce((Entreprise) tiers, EnumSet.of(TypeEtatEntreprise.ABSORBEE, TypeEtatEntreprise.DISSOUTE))
					&& tiers.getForFiscalPrincipalAt(null) == null
					&& SecurityHelper.getDroitAcces(securityProvider, tiers) == Niveau.ECRITURE;
		}

		@Override
		public String getLabel() {
			return "Révoquer la faillite de l'entreprise";
		}

		@Override
		public String getActionUrl() {
			return "goto:/processuscomplexe/revocation/faillite/start.do?id=";
		}
	}

	private static class DemenagerSiege implements Action {
		@Override
		public boolean isGranted() {
			return SecurityHelper.isAnyGranted(securityProvider, Role.DEMENAGEMENT_SIEGE_ENTREPRISE);
		}

		@Override
		public boolean isValide(Tiers tiers) {
			return !tiers.isAnnule()
					&& tiers instanceof Entreprise
					&& tiers.getForFiscalPrincipalAt(null) != null
					&& SecurityHelper.getDroitAcces(securityProvider, tiers) == Niveau.ECRITURE;
		}

		@Override
		public String getLabel() {
			return "Déménager le siège de l'entreprise";
		}

		@Override
		public String getActionUrl() {
			return "goto:/processuscomplexe/demenagement/start.do?id=";
		}
	}

	private static class AnnulerDemenagementSiege implements Action {
		@Override
		public boolean isGranted() {
			return SecurityHelper.isAnyGranted(securityProvider, Role.DEMENAGEMENT_SIEGE_ENTREPRISE);
		}

		@Override
		public boolean isValide(Tiers tiers) {
			return !tiers.isAnnule()
					&& tiers instanceof Entreprise
					&& hasForPrincipalActifEtForPrecedent(tiers)
					&& SecurityHelper.getDroitAcces(securityProvider, tiers) == Niveau.ECRITURE;
		}

		private static boolean hasForPrincipalActifEtForPrecedent(Tiers tiers) {
			final List<? extends ForFiscalPrincipal> forsPrincipaux = tiers.getForsFiscauxPrincipauxActifsSorted();
			final ForFiscalPrincipal actif = DateRangeHelper.rangeAt(forsPrincipaux, null);
			final Set<MotifFor> motifsDemenagement = EnumSet.of(MotifFor.DEMENAGEMENT_VD, MotifFor.ARRIVEE_HC, MotifFor.ARRIVEE_HS, MotifFor.DEPART_HC, MotifFor.DEPART_HS);
			return actif != null
					&& DateRangeHelper.rangeAt(forsPrincipaux, actif.getDateDebut().getOneDayBefore()) != null
					&& motifsDemenagement.contains(actif.getMotifOuverture());
		}

		@Override
		public String getLabel() {
			return "Annuler le dernier déménagement de siège de l'entreprise";
		}

		@Override
		public String getActionUrl() {
			return "goto:/processuscomplexe/annulation/demenagement/start.do?id=";
		}
	}

	private static class TerminerActivite implements Action {
		@Override
		public boolean isGranted() {
			return SecurityHelper.isAnyGranted(securityProvider, Role.FIN_ACTIVITE_ENTREPRISE);
		}

		@Override
		public boolean isValide(Tiers tiers) {
			return !tiers.isAnnule()
					&& tiers instanceof Entreprise
					&& !hadEtatOnce((Entreprise) tiers, EnumSet.of(TypeEtatEntreprise.ABSORBEE, TypeEtatEntreprise.DISSOUTE))
					&& tiers.getForFiscalPrincipalAt(null) != null
					&& SecurityHelper.getDroitAcces(securityProvider, tiers) == Niveau.ECRITURE;
		}

		@Override
		public String getLabel() {
			return "Mettre fin à l'activité de l'entreprise";
		}

		@Override
		public String getActionUrl() {
			return "goto:/processuscomplexe/finactivite/start.do?id=";
		}
	}

	private static class ReprendreActivitePartielle implements Action {
		@Override
		public boolean isGranted() {
			return SecurityHelper.isAnyGranted(securityProvider, Role.FIN_ACTIVITE_ENTREPRISE);
		}

		@Override
		public boolean isValide(Tiers tiers) {
			return !tiers.isAnnule()
					&& tiers instanceof Entreprise
					&& !hadEtatOnce((Entreprise) tiers, EnumSet.of(TypeEtatEntreprise.ABSORBEE))
					&& isDernierForPrincipalFermePourFinActivite(tiers)
					&& SecurityHelper.getDroitAcces(securityProvider, tiers) == Niveau.ECRITURE;
		}

		private static boolean isDernierForPrincipalFermePourFinActivite(Tiers tiers) {
			final ForFiscalPrincipal dernierFor = tiers.getDernierForFiscalPrincipal();
			return dernierFor != null && dernierFor.getDateFin() != null && dernierFor.getMotifFermeture() == MotifFor.FIN_EXPLOITATION;
		}

		@Override
		public String getLabel() {
			return "Reprendre une activité partielle pour l'entreprise";
		}

		@Override
		public String getActionUrl() {
			return "goto:/processuscomplexe/repriseactivite/start.do?id=";
		}
	}

	private static class AnnulerFinActivite implements Action {
		@Override
		public boolean isGranted() {
			return SecurityHelper.isAnyGranted(securityProvider, Role.FIN_ACTIVITE_ENTREPRISE);
		}

		@Override
		public boolean isValide(Tiers tiers) {
			return !tiers.isAnnule()
					&& tiers instanceof Entreprise
					&& !hadEtatOnce((Entreprise) tiers, EnumSet.of(TypeEtatEntreprise.ABSORBEE))
					&& isDernierForPrincipalFermePourFinActivite(tiers)
					&& SecurityHelper.getDroitAcces(securityProvider, tiers) == Niveau.ECRITURE;
		}

		private static boolean isDernierForPrincipalFermePourFinActivite(Tiers tiers) {
			final ForFiscalPrincipal dernierFor = tiers.getDernierForFiscalPrincipal();
			return dernierFor != null && dernierFor.getDateFin() != null && dernierFor.getMotifFermeture() == MotifFor.FIN_EXPLOITATION;
		}

		@Override
		public String getLabel() {
			return "Annuler la fin d'activité de l'entreprise";
		}

		@Override
		public String getActionUrl() {
			return "goto:/processuscomplexe/annulation/finactivite/start.do?id=";
		}
	}

	private static class AbsorberEntreprises implements Action {
		@Override
		public boolean isGranted() {
			return SecurityHelper.isAnyGranted(securityProvider, Role.FUSION_ENTREPRISES);
		}

		@Override
		public boolean isValide(Tiers tiers) {
			return !tiers.isAnnule()
					&& tiers instanceof Entreprise
					&& !hadEtatOnce((Entreprise) tiers, EnumSet.of(TypeEtatEntreprise.ABSORBEE, TypeEtatEntreprise.DISSOUTE, TypeEtatEntreprise.RADIEE_RC))
					&& SecurityHelper.getDroitAcces(securityProvider, tiers) == Niveau.ECRITURE;
		}

		@Override
		public String getLabel() {
			return "Absorber une ou plusieurs autres entreprises";
		}

		@Override
		public String getActionUrl() {
			return "goto:/processuscomplexe/fusion/choix-dates.do?absorbante=";
		}
	}

	private static class AnnulerFusionEntreprises implements Action {
		@Override
		public boolean isGranted() {
			return SecurityHelper.isAnyGranted(securityProvider, Role.FUSION_ENTREPRISES);
		}

		@Override
		public boolean isValide(Tiers tiers) {
			return !tiers.isAnnule()
					&& tiers instanceof Entreprise
					&& !FusionEntreprisesHelper.getAbsorptions((Entreprise) tiers, tiersService).isEmpty()
					&& SecurityHelper.getDroitAcces(securityProvider, tiers) == Niveau.ECRITURE;
		}

		@Override
		public String getLabel() {
			return "Annuler une absorption d'entreprise(s)";
		}

		@Override
		public String getActionUrl() {
			return "goto:/processuscomplexe/annulation/fusion/choix-dates.do?absorbante=";
		}
	}

	private static class ScinderEntreprise implements Action {
		@Override
		public boolean isGranted() {
			return SecurityHelper.isAnyGranted(securityProvider, Role.SCISSION_ENTREPRISE);
		}

		@Override
		public boolean isValide(Tiers tiers) {
			return !tiers.isAnnule()
					&& tiers instanceof Entreprise
					&& !hadEtatOnce((Entreprise) tiers, EnumSet.of(TypeEtatEntreprise.ABSORBEE, TypeEtatEntreprise.DISSOUTE, TypeEtatEntreprise.RADIEE_RC))
					&& SecurityHelper.getDroitAcces(securityProvider, tiers) == Niveau.ECRITURE;
		}

		@Override
		public String getLabel() {
			return "Scinder l'entreprise";
		}

		@Override
		public String getActionUrl() {
			return "goto:/processuscomplexe/scission/choix-date.do?scindee=";
		}
	}

	private static class AnnulerScissionEntreprise implements Action {
		@Override
		public boolean isGranted() {
			return SecurityHelper.isAnyGranted(securityProvider, Role.SCISSION_ENTREPRISE);
		}

		@Override
		public boolean isValide(Tiers tiers) {
			return !tiers.isAnnule()
					&& tiers instanceof Entreprise
					&& !ScissionEntrepriseHelper.getScissions((Entreprise) tiers, tiersService).isEmpty()
					&& SecurityHelper.getDroitAcces(securityProvider, tiers) == Niveau.ECRITURE;
		}

		@Override
		public String getLabel() {
			return "Annuler une scission d'entreprise";
		}

		@Override
		public String getActionUrl() {
			return "goto:/processuscomplexe/annulation/scission/choix-date.do?scindee=";
		}
	}

	private static class TransfererPatrimoine implements Action {
		@Override
		public boolean isGranted() {
			return SecurityHelper.isAnyGranted(securityProvider, Role.TRANSFERT_PATRIMOINE_ENTREPRISE);
		}

		@Override
		public boolean isValide(Tiers tiers) {
			return !tiers.isAnnule()
					&& tiers instanceof Entreprise
					&& !hadEtatOnce((Entreprise) tiers, EnumSet.of(TypeEtatEntreprise.ABSORBEE, TypeEtatEntreprise.DISSOUTE, TypeEtatEntreprise.RADIEE_RC, TypeEtatEntreprise.EN_FAILLITE))
					&& SecurityHelper.getDroitAcces(securityProvider, tiers) == Niveau.ECRITURE;
		}

		@Override
		public String getLabel() {
			return "Transférer du patrimoine";
		}

		@Override
		public String getActionUrl() {
			return "goto:/processuscomplexe/transfertpatrimoine/choix-date.do?emettrice=";
		}
	}

	private static class AnnulerTransfertPatrimoine implements Action {
		@Override
		public boolean isGranted() {
			return SecurityHelper.isAnyGranted(securityProvider, Role.TRANSFERT_PATRIMOINE_ENTREPRISE);
		}

		@Override
		public boolean isValide(Tiers tiers) {
			return !tiers.isAnnule()
					&& tiers instanceof Entreprise
					&& !TransfertPatrimoineHelper.getTransferts((Entreprise) tiers, tiersService).isEmpty()
					&& SecurityHelper.getDroitAcces(securityProvider, tiers) == Niveau.ECRITURE;
		}

		@Override
		public String getLabel() {
			return "Annuler un transfert de patrimoine";
		}

		@Override
		public String getActionUrl() {
			return "goto:/processuscomplexe/annulation/transfertpatrimoine/choix-date.do?emettrice=";
		}
	}

	private static class ReinscriptionRegistreCommerce implements Action {
		@Override
		public boolean isGranted() {
			return SecurityHelper.isAnyGranted(securityProvider, Role.REINSCRIPTION_RC_ENTREPRISE);
		}

		@Override
		public boolean isValide(Tiers tiers) {
			return !tiers.isAnnule()
					&& tiers instanceof Entreprise
					&& ((Entreprise) tiers).getEtatActuel() != null
					&& ((Entreprise) tiers).getEtatActuel().getType() == TypeEtatEntreprise.RADIEE_RC
					&& SecurityHelper.getDroitAcces(securityProvider, tiers) == Niveau.ECRITURE;
		}

		@Override
		public String getLabel() {
			return "Traiter une ré-inscription au RC";
		}

		@Override
		public String getActionUrl() {
			return "goto:/processuscomplexe/reinscriptionrc/start.do?id=";
		}
	}

	private static class RequisitionRadiationRC implements Action {
		@Override
		public boolean isGranted() {
			return SecurityHelper.isAnyGranted(securityProvider, Role.REQUISITION_RADIATION_RC);
		}

		@Override
		public boolean isValide(Tiers tiers) {
			return !tiers.isAnnule()
					&& tiers instanceof Entreprise
					&& tiers.getForFiscalPrincipalAt(null) != null
					&& !hadEtatOnce((Entreprise) tiers, EnumSet.of(TypeEtatEntreprise.RADIEE_RC, TypeEtatEntreprise.DISSOUTE))
					&& SecurityHelper.getDroitAcces(securityProvider, tiers) == Niveau.ECRITURE;
		}

		@Override
		public String getLabel() {
			return "Traiter la réquisition de radiation du RC";
		}

		@Override
		public String getActionUrl() {
			return "goto:/processuscomplexe/requisitionradiationrc/start.do?id=";
		}
	}
}
