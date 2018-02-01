package ch.vd.unireg.tiers.vignette;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.springframework.context.MessageSource;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.data.ApplicationFiscale;
import ch.vd.unireg.adresse.AdresseEnvoiDetaillee;
import ch.vd.unireg.adresse.AdresseException;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.adresse.TypeAdresseFiscale;
import ch.vd.unireg.avatar.AvatarService;
import ch.vd.unireg.avatar.TypeAvatar;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.taglibs.JspTagBandeauTiers;
import ch.vd.unireg.taglibs.JspTagInteroperabilite;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.EnsembleTiersCouple;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.utils.WebContextUtils;

/**
 * Contient les informations brutes nécessaires à la construction d'une vignette.
 */
public class VignetteView {

	private long numero;
	private String nature;
	private TypeAutoriteFiscale typeAutoriteFiscaleForPrincipal;
	private RegDate dateDebutActivite;
	private Date dateAnnulation;
	private RegDate dateDesactivation;
	private String roleLigne1;
	private String roleLigne2;
	private AdresseEnvoiView adresseEnvoi;
	private String adresseEnvoiException;
	private TypeAvatar typeAvatar;
	private EnsembleTiersCoupleView ensemble;
	private List<UrlVersView> urlsVers;
	private List<ActionView> actions;
	private String accessDenied;

	public VignetteView(Tiers tiers, boolean fillEnsemble, boolean fillAdresses, boolean fillRoles, boolean fillUrlVers, boolean fillActions, TiersService tiersService,
	                    AdresseService adresseService, AvatarService avatarService,
	                    ServiceInfrastructureService infraService, MessageSource messageSource) {

		this.numero = tiers.getNumero();
		this.nature = tiers.getNatureTiers().name();
		this.dateDebutActivite = tiers.getDateDebutActivite();
		this.dateAnnulation = tiers.getAnnulationDate();
		this.dateDesactivation = tiers.getDateDesactivation();

		final ForFiscalPrincipal ffp = tiers instanceof Contribuable ? ((Contribuable) tiers).getForFiscalPrincipalAt(null) : null;
		this.typeAutoriteFiscaleForPrincipal = (ffp == null ? null : ffp.getTypeAutoriteFiscale());

		// les rôles
		if (fillRoles) {
			this.roleLigne1 = tiers.getRoleLigne1();
			this.roleLigne2 = tiersService.getRoleAssujettissement(tiers, null);
		}

		// les adresses
		if (fillAdresses) {
			try {
				final AdresseEnvoiDetaillee adresseEnvoi = adresseService.getAdresseEnvoi(tiers, null, TypeAdresseFiscale.COURRIER, false);
				this.adresseEnvoi = (adresseEnvoi == null ? null : new AdresseEnvoiView(adresseEnvoi));
			}
			catch (AdresseException e) {
				this.adresseEnvoiException = e.getMessage();
			}
		}

		this.typeAvatar = avatarService.getTypeAvatar(tiers);

		// le ménage et consort
		EnsembleTiersCouple ensemble = null;
		if (fillEnsemble) {
			if (tiers instanceof PersonnePhysique) {
				ensemble = tiersService.getEnsembleTiersCouple((PersonnePhysique) tiers, null);
			}
			else if (tiers instanceof MenageCommun) {
				ensemble = tiersService.getEnsembleTiersCouple((MenageCommun) tiers, null);
			}
			this.ensemble = (ensemble == null ? null : new EnsembleTiersCoupleView(ensemble, tiersService, adresseService, avatarService, infraService, messageSource));
		}

		// les urls vers les autres applications
		if (fillUrlVers) {
			final Set<ApplicationFiscale> apps = JspTagInteroperabilite.getApplicationsFiscalesAutorisees(tiers.getNatureTiers(), tiers.isDebiteurInactif());
			this.urlsVers = new ArrayList<>();
			for (ApplicationFiscale af : apps) {
				final String label = messageSource.getMessage(af.getMessageKey(), null, WebContextUtils.getDefaultLocale());
				this.urlsVers.add(new UrlVersView(af.name(), label, af.name()));
			}
		}

		if (fillActions) {
			this.actions = new ArrayList<>();
			for (JspTagBandeauTiers.Action action : JspTagBandeauTiers.actions) {
				if (action.isGranted() && action.isValide(tiers)) {
					this.actions.add(new ActionView(action.getLabel(), action.getActionUrl() + tiers.getNumero()));
				}
			}
		}
	}

	public VignetteView(String accessDeniedMessage) {
		this.accessDenied = accessDeniedMessage;
	}

	public long getNumero() {
		return numero;
	}

	public String getNature() {
		return nature;
	}

	public RegDate getDateDebutActivite() {
		return dateDebutActivite;
	}

	public TypeAutoriteFiscale getTypeAutoriteFiscaleForPrincipal() {
		return typeAutoriteFiscaleForPrincipal;
	}

	public Date getDateAnnulation() {
		return dateAnnulation;
	}

	public RegDate getDateDesactivation() {
		return dateDesactivation;
	}

	public String getRoleLigne1() {
		return roleLigne1;
	}

	public String getRoleLigne2() {
		return roleLigne2;
	}

	public AdresseEnvoiView getAdresseEnvoi() {
		return adresseEnvoi;
	}

	public String getAdresseEnvoiException() {
		return adresseEnvoiException;
	}

	public TypeAvatar getTypeAvatar() {
		return typeAvatar;
	}

	public EnsembleTiersCoupleView getEnsemble() {
		return ensemble;
	}

	public List<UrlVersView> getUrlsVers() {
		return urlsVers;
	}

	public List<ActionView> getActions() {
		return actions;
	}

	/**
	 * @return un message d'erreur explicite si l'accès sur le tiers est refusé pour des raisons de droits d'accès insuffisants.
	 */
	public String getAccessDenied() {
		return accessDenied;
	}
}
