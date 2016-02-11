package ch.vd.uniregctb.rapport.view;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.Annulable;
import ch.vd.uniregctb.common.BaseComparator;
import ch.vd.uniregctb.common.NomCourrierViewPart;
import ch.vd.uniregctb.general.view.TiersGeneralView;
import ch.vd.uniregctb.rapport.SensRapportEntreTiers;
import ch.vd.uniregctb.rapport.TypeRapportEntreTiersWeb;
import ch.vd.uniregctb.tiers.ActiviteEconomique;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.RepresentationConventionnelle;
import ch.vd.uniregctb.tiers.RepresentationLegale;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.tiers.TiersWebHelper;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

/**
 * Classe utilisée pour le fromBackingObject du RapportEditController
 *
 * @author xcifde
 */
public class RapportView implements Comparable<RapportView>, Annulable {

	protected static final Logger LOGGER = LoggerFactory.getLogger(RapportView.class);

	private static BaseComparator<RapportView> comparator = new BaseComparator<>(new String[]{"annule", "dateDebut"}, new Boolean[]{true, true});

	private RegDate dateDebut;

	private RegDate dateFin;

	private SensRapportEntreTiers sensRapportEntreTiers;

	private TiersGeneralView tiers;

	private TiersGeneralView tiersLie;

	private Long id;

	private boolean annule;

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

	private final NomCourrierViewPart nomCourrier;

	private String natureRapportEntreTiers;

	private boolean isAllowed;

	// --- uniquement pour RapportPrestationImposable ----


	// -- uniquement pour RepresentationConventionnelle --

	private Boolean extensionExecutionForcee;
	private boolean extensionExecutionForceeAllowed;

	private String toolTipMessage;

	// -- uniquement pour la Representation légale

	private Long autoriteTutelaireId;

	private String nomAutoriteTutelaire;

	private String viewRetour;

	public RapportView() {
		this.nomCourrier = new NomCourrierViewPart();
	}

	public RapportView(RapportEntreTiers rapport, SensRapportEntreTiers sens, TiersService tiersService, AdresseService adresseService) {
		this.id = rapport.getId();
		this.dateDebut = rapport.getDateDebut();
		this.dateFin = rapport.getDateFin();
		this.sensRapportEntreTiers = sens;
		this.annule = rapport.isAnnule();
		this.typeRapportEntreTiers = TypeRapportEntreTiersWeb.fromCore(rapport.getType());
		if (rapport.getType() == TypeRapportEntreTiers.ACTIVITE_ECONOMIQUE && sens == SensRapportEntreTiers.SUJET && ((ActiviteEconomique) rapport).isPrincipal()) {
			this.activiteEconomiquePrincipale = true;
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

		if (rapport instanceof RepresentationLegale) {
			final RepresentationLegale rl = (RepresentationLegale) rapport;
			this.autoriteTutelaireId = rl.getAutoriteTutelaireId();
			if (this.autoriteTutelaireId != null) {
				this.nomAutoriteTutelaire = tiersService.getNomCollectiviteAdministrative(this.autoriteTutelaireId.intValue());
			}
		}

		this.messageNumeroAbsent = null; // TDDO (msi) rapports de filiation
		this.toolTipMessage = TiersWebHelper.getRapportEntreTiersTooltips(rapport, adresseService, tiersService);
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

	public RegDate getRegDateDebut() {
		return dateDebut;
	}

	public RegDate getRegDateFin() {
		return dateFin;
	}

	public void setDateFin(RegDate dateFin) {
		this.dateFin = dateFin;
	}

	public Date getDateDebut() {
		return RegDate.asJavaDate(dateDebut);
	}

	public void setDateDebut(Date dateDebut) {
		this.dateDebut = RegDateHelper.get(dateDebut);
	}

	public Date getDateFin() {
		return RegDate.asJavaDate(dateFin);
	}

	public void setDateFin(Date dateFin) {
		this.dateFin = RegDateHelper.get(dateFin);
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
		this.nomCourrier.setNomCourrier(nomCourrier);
	}

	public String getNomCourrier1() {
		return this.nomCourrier.getNomCourrier1();
	}

	public void setNomCourrier1(String nomCourrier1) {
		this.nomCourrier.setNomCourrier1(nomCourrier1);
	}

	public String getNomCourrier2() {
		return this.nomCourrier.getNomCourrier2();
	}

	public void setNomCourrier2(String nomCourrier2) {
		this.nomCourrier.setNomCourrier2(nomCourrier2);
	}

	public void setDateDebut(RegDate dateDebut) {
		this.dateDebut = dateDebut;
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

	public static NomCourrierViewPart buildNomCourrier(Tiers tiers, AdresseService adresseService) {

		if (tiers == null) {
			return null;
		}

		List<String> nomSujet;
		try {
			nomSujet = adresseService.getNomCourrier(tiers, null, false);
		}
		catch (Exception e) {
			nomSujet = new ArrayList<>();
			nomSujet.add(e.getMessage());
		}

		return new NomCourrierViewPart(nomSujet);
	}

}
