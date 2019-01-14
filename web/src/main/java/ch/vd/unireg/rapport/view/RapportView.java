package ch.vd.unireg.rapport.view;

import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.common.Annulable;
import ch.vd.unireg.common.BaseComparator;
import ch.vd.unireg.general.view.TiersGeneralView;
import ch.vd.unireg.message.MessageHelper;
import ch.vd.unireg.rapport.SensRapportEntreTiers;
import ch.vd.unireg.rapport.TypeRapportEntreTiersWeb;
import ch.vd.unireg.tiers.ActiviteEconomique;
import ch.vd.unireg.tiers.CollectiviteAdministrative;
import ch.vd.unireg.tiers.Etablissement;
import ch.vd.unireg.tiers.ForFiscal;
import ch.vd.unireg.tiers.Heritage;
import ch.vd.unireg.tiers.RapportEntreTiers;
import ch.vd.unireg.tiers.RepresentationConventionnelle;
import ch.vd.unireg.tiers.RepresentationLegale;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.tiers.TiersWebHelper;
import ch.vd.unireg.type.TypeAutoriteFiscale;

/**
 * Classe utilisée pour le fromBackingObject du RapportController
 *
 * @author xcifde
 */
public class RapportView implements Comparable<RapportView>, Annulable {

	protected static final Logger LOGGER = LoggerFactory.getLogger(RapportView.class);

	private static BaseComparator<RapportView> comparator = new BaseComparator<>(new String[]{"annule", "dateDebut"}, new Boolean[]{true, false});

	private RegDate dateDebut;

	private RegDate dateFin;

	private SensRapportEntreTiers sensRapportEntreTiers;

	private TiersGeneralView tiers;

	private TiersGeneralView tiersLie;

	private Long id;

	private boolean annule;

	private boolean etablissementAnnulable;

	private TypeRapportEntreTiersWeb typeRapportEntreTiers;

	private boolean activiteEconomiquePrincipale;

	/**
	 * Le numéro du tiers courant
	 */
	private Long numeroCourant;

	/**
	 * Le numéro du tiers lié
	 */
	private Long numero;

	/**
	 * Message à afficher en lieu et place du numéro de contribuable si celui-ci est absent
	 */
	private String messageNumeroAbsent;

	private List<String> nomCourrier;

	private String natureRapportEntreTiers;

	private boolean isAllowed;

	// --- uniquement pour RapportPrestationImposable ----


	// -- uniquement pour RepresentationConventionnelle --

	private Boolean extensionExecutionForcee;
	private boolean extensionExecutionForceeAllowed;

	// -- uniquement pour Héritage --

	private Boolean principalCommunaute;

	private String toolTipMessage;

	// -- uniquement pour la Representation légale

	private Long autoriteTutelaireId;

	private String nomAutoriteTutelaire;

	private String viewRetour;

	public RapportView() {
	}

	public RapportView(RapportEntreTiers rapport, SensRapportEntreTiers sens, TiersService tiersService, AdresseService adresseService, MessageHelper messageHelper) {
		this.id = rapport.getId();
		this.dateDebut = rapport.getDateDebut();
		this.dateFin = rapport.getDateFin();
		this.sensRapportEntreTiers = sens;
		this.annule = rapport.isAnnule();
		this.typeRapportEntreTiers = TypeRapportEntreTiersWeb.fromCore(rapport.getType());
		if (rapport instanceof ActiviteEconomique && sens == SensRapportEntreTiers.SUJET) {
			if (((ActiviteEconomique) rapport).isPrincipal()) {
				this.activiteEconomiquePrincipale = true;
			}
			Etablissement etablissement = (Etablissement) tiersService.getTiers(rapport.getObjetId());
			if (!this.activiteEconomiquePrincipale && !this.annule && etablissement.getNumeroEtablissement() == null) {
				this.etablissementAnnulable = true;
			}
		}

		this.numeroCourant = (sens == SensRapportEntreTiers.SUJET ? rapport.getSujetId() : rapport.getObjetId());
		this.numero = (sens == SensRapportEntreTiers.SUJET ? rapport.getObjetId() : rapport.getSujetId());
		this.nomCourrier = buildNomCourrier(getTiers(this.numero, tiersService), adresseService);
		this.natureRapportEntreTiers = rapport.getClass().getSimpleName();

		if (rapport instanceof RepresentationConventionnelle) {
			final RepresentationConventionnelle rc = (RepresentationConventionnelle) rapport;
			this.extensionExecutionForcee = rc.getExtensionExecutionForcee();
			final Tiers tiersSujet = getTiers(rapport.getSujetId(), tiersService);
			this.extensionExecutionForceeAllowed = isHorsSuisse(tiersSujet, rapport); // [UNIREG-2655]
		}

		if (rapport instanceof Heritage) {
			this.principalCommunaute = ((Heritage) rapport).getPrincipalCommunaute();
		}

		if (rapport instanceof RepresentationLegale) {
			final RepresentationLegale rl = (RepresentationLegale) rapport;
			this.autoriteTutelaireId = rl.getAutoriteTutelaireId();
			if (this.autoriteTutelaireId != null) {
				this.nomAutoriteTutelaire = getNomAutoriteTutelaire(this.autoriteTutelaireId, tiersService);
			}
		}

		this.messageNumeroAbsent = null; // TDDO (msi) rapports de filiation
		this.toolTipMessage = TiersWebHelper.getRapportEntreTiersTooltips(rapport, adresseService, tiersService, messageHelper);
	}

	/**
	 * Récupération du nom de l'autorité tutellaire à partir de son id, depuis le service infrastructure
	 *
	 * @param autoriteTutelaireId id de l'autorité tutellaire
	 * @param tiersService        service utilisé pour la recherche
	 * @return le nom de l'autorité tutellaire
	 */
	String getNomAutoriteTutelaire(Long autoriteTutelaireId, TiersService tiersService) {
		String nom = null;
		if (autoriteTutelaireId != null) {
			final Tiers autoriteTutelaire = tiersService.getTiers(autoriteTutelaireId);
			if (autoriteTutelaire instanceof CollectiviteAdministrative) {
				nom = tiersService.getNomCollectiviteAdministrative(((CollectiviteAdministrative) autoriteTutelaire).getNumeroCollectiviteAdministrative());
			}
		}
		return nom;
	}

	// ---------------------------------------------------

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Override
	public boolean isAnnule() {
		return annule;
	}

	public void setAnnule(boolean annule) {
		this.annule = annule;
	}

	public boolean isEtablissementAnnulable() {
		return etablissementAnnulable;
	}

	public void setEtablissementAnnulable(boolean etablissementAnnulable) {
		this.etablissementAnnulable = etablissementAnnulable;
	}

	public TypeRapportEntreTiersWeb getTypeRapportEntreTiers() {
		return typeRapportEntreTiers;
	}

	public void setTypeRapportEntreTiers(TypeRapportEntreTiersWeb typeRapportEntreTiers) {
		this.typeRapportEntreTiers = typeRapportEntreTiers;
	}

	public boolean isActiviteEconomiquePrincipale() {
		return activiteEconomiquePrincipale;
	}

	public void setActiviteEconomiquePrincipale(boolean activiteEconomiquePrincipale) {
		this.activiteEconomiquePrincipale = activiteEconomiquePrincipale;
	}

	public RegDate getDateDebut() {
		return dateDebut;
	}

	public void setDateDebut(RegDate dateDebut) {
		this.dateDebut = dateDebut;
	}

	public RegDate getDateFin() {
		return dateFin;
	}

	public void setDateFin(RegDate dateFin) {
		this.dateFin = dateFin;
	}

	/**
	 * @return Le numéro du tiers courant
	 */
	public Long getNumeroCourant() {
		return numeroCourant;
	}

	public void setNumeroCourant(Long numeroCourant) {
		this.numeroCourant = numeroCourant;
	}

	/**
	 * @return Le numéro du tiers lié par ce rapport.
	 */
	public Long getNumero() {
		return numero;
	}

	public void setNumero(Long numero) {
		this.numero = numero;
	}

	public String getMessageNumeroAbsent() {
		return messageNumeroAbsent;
	}

	public void setMessageNumeroAbsent(String messageNumeroAbsent) {
		this.messageNumeroAbsent = messageNumeroAbsent;
	}

	public void setNomCourrier(List<String> nomCourrier) {
		this.nomCourrier = nomCourrier;
	}

	public List<String> getNomCourrier() {
		return nomCourrier;
	}

	public TiersGeneralView getTiers() {
		return tiers;
	}

	public void setTiers(TiersGeneralView tiers) {
		this.tiers = tiers;
	}

	public TiersGeneralView getTiersLie() {
		return tiersLie;
	}

	public void setTiersLie(TiersGeneralView tiersLie) {
		this.tiersLie = tiersLie;
	}

	public Long getAutoriteTutelaireId() {
		return autoriteTutelaireId;
	}

	public void setAutoriteTutelaireId(Long autoriteTutelaireId) {
		this.autoriteTutelaireId = autoriteTutelaireId;
	}

	/**
	 * @return <i>OBJET</i> si le rapport est édité depuis le tiers objet ou  <i>SUJET</i> si le rapport est édité depuis le tiers sujet.
	 */
	public SensRapportEntreTiers getSensRapportEntreTiers() {
		return sensRapportEntreTiers;
	}

	public void setSensRapportEntreTiers(SensRapportEntreTiers sensRapportEntreTiers) {
		this.sensRapportEntreTiers = sensRapportEntreTiers;
	}

	public String getNatureRapportEntreTiers() {
		return natureRapportEntreTiers;
	}

	public void setNatureRapportEntreTiers(String natureRapportEntreTiers) {
		this.natureRapportEntreTiers = natureRapportEntreTiers;
	}

	@Override
	public int compareTo(RapportView o) {
		return comparator.compare(this, o);
	}

	public boolean isAllowed() {
		return isAllowed;
	}

	public void setAllowed(boolean isAllowed) {
		this.isAllowed = isAllowed;
	}

	public Boolean getExtensionExecutionForcee() {
		return extensionExecutionForcee;
	}

	public void setExtensionExecutionForcee(Boolean extensionExecutionForcee) {
		this.extensionExecutionForcee = extensionExecutionForcee;
	}

	public boolean isExtensionExecutionForceeAllowed() {
		return extensionExecutionForceeAllowed;
	}

	public void setExtensionExecutionForceeAllowed(boolean extensionExecutionForceeAllowed) {
		this.extensionExecutionForceeAllowed = extensionExecutionForceeAllowed;
	}

	public Boolean getPrincipalCommunaute() {
		return principalCommunaute;
	}

	public void setPrincipalCommunaute(Boolean principalCommunaute) {
		this.principalCommunaute = principalCommunaute;
	}

	public static BaseComparator<RapportView> getComparator() {
		return comparator;
	}

	public static void setComparator(BaseComparator<RapportView> comparator) {
		RapportView.comparator = comparator;
	}

	public String getToolTipMessage() {
		return toolTipMessage;
	}

	public void setToolTipMessage(String toolTipMessage) {
		this.toolTipMessage = toolTipMessage;
	}

	public String getNomAutoriteTutelaire() {
		return nomAutoriteTutelaire;
	}

	public void setNomAutoriteTutelaire(String nomAutoriteTutelaire) {
		this.nomAutoriteTutelaire = nomAutoriteTutelaire;
	}

	public String getViewRetour() {
		return viewRetour;
	}

	public void setViewRetour(String viewRetour) {
		this.viewRetour = viewRetour;
	}

	/**
	 * [UNIREG-2655] Détermine si un tiers possède un for fiscal principal hors-Suisse durant une certaine période
	 *
	 * @param tiers le iters
	 * @param range une période temporelle
	 * @return <b>vrai</b> si le tiers possède au moins un fors fiscal principal hors-Suisse pendant la période considérée.
	 */
	private static boolean isHorsSuisse(Tiers tiers, DateRange range) {

		boolean horsSuisse = false;

		for (ForFiscal ff : tiers.getForsFiscaux()) {
			if (!ff.isAnnule() && ff.isPrincipal() && DateRangeHelper.intersect(ff, range)) {
				horsSuisse |= ff.getTypeAutoriteFiscale() == TypeAutoriteFiscale.PAYS_HS;
			}
		}

		return horsSuisse;
	}

	private static Tiers getTiers(Long numeroTiers, TiersService tiersService) {
		return numeroTiers != null ? tiersService.getTiers(numeroTiers) : null;
	}

	public static List<String> buildNomCourrier(Tiers tiers, AdresseService adresseService) {
		if (tiers == null) {
			return null;
		}
		try {
			return adresseService.getNomCourrier(tiers, null, false);
		}
		catch (Exception e) {
			return Collections.singletonList(e.getMessage());
		}
	}
}
