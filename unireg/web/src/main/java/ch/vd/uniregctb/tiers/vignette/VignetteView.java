package ch.vd.uniregctb.tiers.vignette;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.springframework.context.MessageSource;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.data.ApplicationFiscale;
import ch.vd.uniregctb.adresse.AdresseEnvoiDetaillee;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.adresse.TypeAdresseFiscale;
import ch.vd.uniregctb.general.view.TypeAvatar;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.taglibs.JspTagBandeauTiers;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.utils.WebContextUtils;

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
	                    AdresseService adresseService,
	                    ServiceInfrastructureService infraService, MessageSource messageSource) {

		this.numero = tiers.getNumero();
		this.nature = tiers.getNatureTiers().name();
		this.dateDebutActivite = tiers.getDateDebutActivite();
		this.dateAnnulation = tiers.getAnnulationDate();
		this.dateDesactivation = tiers.getDateDesactivation();

		final ForFiscalPrincipal ffp = tiers.getForFiscalPrincipalAt(null);
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

		this.typeAvatar = JspTagBandeauTiers.getTypeAvatar(tiers);

		// le ménage et consort
		EnsembleTiersCouple ensemble = null;
		if (fillEnsemble) {
			if (tiers instanceof PersonnePhysique) {
				ensemble = tiersService.getEnsembleTiersCouple((PersonnePhysique) tiers, null);
			}
			else if (tiers instanceof MenageCommun) {
				ensemble = tiersService.getEnsembleTiersCouple((MenageCommun) tiers, null);
			}
			this.ensemble = (ensemble == null ? null : new EnsembleTiersCoupleView(ensemble, tiersService, adresseService, infraService, messageSource));
		}

		// les urls vers les autres applications
		if (fillUrlVers) {
			final List<ApplicationFiscale> list;
			if (tiers instanceof Entreprise) {
				// [UNIREG-1949] débranchement uniquement vers SIPF pour les PMs
				list = Arrays.asList(ApplicationFiscale.SIPF);
			}
			else {
				list = Arrays.asList(ApplicationFiscale.TAO_PP, ApplicationFiscale.TAO_BA, ApplicationFiscale.TAO_IS, ApplicationFiscale.SIPF);
			}

			this.urlsVers = new ArrayList<UrlVersView>();
			for (ApplicationFiscale af : list) {
				final String label = messageSource.getMessage(af.getMessageKey(), null, WebContextUtils.getDefaultLocale());
				this.urlsVers.add(new UrlVersView(af.name(), label, af.name()));
			}
		}

		if (fillActions) {
			this.actions = new ArrayList<ActionView>();
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
