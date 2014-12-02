package ch.vd.uniregctb.indexer.messageidentification;

import org.apache.lucene.document.Document;

import ch.vd.uniregctb.indexer.IndexableData;

public class MessageIdentificationIndexableData extends IndexableData {

	//
	// clés des champs de recherche
	//

	public static final String TYPE_MESSAGE = "S_TYPE_MESSAGE";
	public static final String PERIODE_FISCALE = "S_PF";
	public static final String EMETTEUR = "S_EMETTEUR";
	public static final String PRIORITE = "S_PRIO";
	public static final String DATE_MESSAGE = "S_DATE_MESSAGE";
	public static final String ETAT = "S_ETAT";
	public static final String NOM = "S_NOM";
	public static final String PRENOMS = "S_PRENOMS";
	public static final String NAVS13 = "S_NAVS13";
	public static final String NAVS11 = "S_NAVS11";
	public static final String DATE_NAISSANCE = "S_DATE_NAISSANCE";
	public static final String VISA_TRAITEMENT = "S_VISA_TRAITEMENT";
	public static final String DATE_TRAITEMENT = "S_DATE_TRAITEMENT";

	//
	// clés des données uniquement utilisées pour le tri
	//

	public static final String TRI_ID = "T_ID";
	public static final String TRI_NOM = "T_NOM";
	public static final String TRI_PRENOMS = "T_PRENOMS";
	public static final String TRI_DATE_NAISSANCE = "T_DATE_NAISSANCE";
	public static final String TRI_MONTANT = "T_MONTANT";
	public static final String TRI_CTB_TROUVE = "T_CTB_TROUVE";

	//
	// clés des données uniquement stockées
	//

	public static final String CTB_TROUVE = "D_CTB_TROUVE";
	public static final String AVS_UPI = "D_NAVS13_UPI";
	public static final String MONTANT = "D_MONTANT";
	public static final String ANNULE = "D_ANNULE";
	public static final String UTILISATEUR_TRAITANT = "D_UTILISATEUR_TRAITANT";
	public static final String TRANSMETTEUR = "D_TRANSMETTEUR";
	public static final String IDENTIFIE = "D_IDENTIFIE";
	public static final String ERREUR = "D_ERREUR";

	//
	// Données des champs indexés pour la recherche
	//

	private String typeMessage;
	private int periodeFiscale;
	private String emetteur;
	private String priorite;
	private Integer dateMessage;
	private String etat;
	private String nom;
	private String prenoms;
	private String navs13;
	private String navs11;
	private Integer dateNaissance;
	private String visaTraitement;
	private Long dateTraitement;

	//
	// Données uniquement stockées
	//

	private Long noCtbTrouve;
	private String navs13Upi;
	private Long montant;
	private String annule;
	private String utilisateurTraitant;
	private String transmetteur;
	private String identifie;
	private String messageErreur;

	public MessageIdentificationIndexableData(Long id, String type, String subType) {
		super(id, type, subType);
	}

	@Override
	public Document asDoc() {

		final Document d = super.asDoc();

		// Note : pour des raisons de performance de l'index Lucene, il est important que l'ordre des champs soit constant

		addNotAnalyzedValue(d, TYPE_MESSAGE, typeMessage);
		addNumber(d, PERIODE_FISCALE, periodeFiscale);
		addNotAnalyzedValue(d, EMETTEUR, emetteur);
		addNotAnalyzedValue(d, PRIORITE, priorite);
		addNumber(d, DATE_MESSAGE, dateMessage);
		addNotAnalyzedValue(d, ETAT, etat);
		addAnalyzedValue(d, NOM, nom);
		addAnalyzedValue(d, PRENOMS, prenoms);
		addNotAnalyzedValue(d, NAVS13, navs13);
		addNotAnalyzedValue(d, NAVS11, navs11);
		addNumber(d, DATE_NAISSANCE, dateNaissance);
		addNotAnalyzedValue(d, VISA_TRAITEMENT, visaTraitement);
		addNumber(d, DATE_TRAITEMENT, dateTraitement);
		addNumber(d, MONTANT, montant);
		addNumber(d, CTB_TROUVE, noCtbTrouve);

		addNumber(d,TRI_ID,id);
		addNotAnalyzedValue(d, TRI_NOM, nom);
		addNotAnalyzedValue(d, TRI_PRENOMS, prenoms);
		addNumber(d, TRI_DATE_NAISSANCE, dateNaissance == null ? 0 : dateNaissance);
		addNumber(d, TRI_MONTANT, montant == null ? 0L : montant);
		addNumber(d, TRI_CTB_TROUVE, noCtbTrouve == null ? 0L : noCtbTrouve);

		addStoredValue(d, AVS_UPI, navs13Upi);
		addStoredValue(d, ANNULE, annule);
		addStoredValue(d, UTILISATEUR_TRAITANT, utilisateurTraitant);
		addStoredValue(d, TRANSMETTEUR, transmetteur);
		addStoredValue(d, IDENTIFIE, identifie);
		addStoredValue(d, ERREUR, messageErreur);

		return d;
	}

	public String getTypeMessage() {
		return typeMessage;
	}

	public void setTypeMessage(String typeMessage) {
		this.typeMessage = typeMessage;
	}

	public int getPeriodeFiscale() {
		return periodeFiscale;
	}

	public void setPeriodeFiscale(int periodeFiscale) {
		this.periodeFiscale = periodeFiscale;
	}

	public String getEmetteur() {
		return emetteur;
	}

	public void setEmetteur(String emetteur) {
		this.emetteur = emetteur;
	}

	public String getPriorite() {
		return priorite;
	}

	public void setPriorite(String priorite) {
		this.priorite = priorite;
	}

	public Integer getDateMessage() {
		return dateMessage;
	}

	public void setDateMessage(Integer dateMessage) {
		this.dateMessage = dateMessage;
	}

	public String getEtat() {
		return etat;
	}

	public void setEtat(String etat) {
		this.etat = etat;
	}

	public String getNom() {
		return nom;
	}

	public void setNom(String nom) {
		this.nom = nom;
	}

	public String getPrenoms() {
		return prenoms;
	}

	public void setPrenoms(String prenoms) {
		this.prenoms = prenoms;
	}

	public String getNavs13() {
		return navs13;
	}

	public void setNavs13(String navs13) {
		this.navs13 = navs13;
	}

	public String getNavs11() {
		return navs11;
	}

	public void setNavs11(String navs11) {
		this.navs11 = navs11;
	}

	public Integer getDateNaissance() {
		return dateNaissance;
	}

	public void setDateNaissance(Integer dateNaissance) {
		this.dateNaissance = dateNaissance;
	}

	public String getVisaTraitement() {
		return visaTraitement;
	}

	public void setVisaTraitement(String visaTraitement) {
		this.visaTraitement = visaTraitement;
	}

	public Long getDateTraitement() {
		return dateTraitement;
	}

	public void setDateTraitement(Long dateTraitement) {
		this.dateTraitement = dateTraitement;
	}

	public Long getNoCtbTrouve() {
		return noCtbTrouve;
	}

	public void setNoCtbTrouve(Long noCtbTrouve) {
		this.noCtbTrouve = noCtbTrouve;
	}

	public String getNavs13Upi() {
		return navs13Upi;
	}

	public void setNavs13Upi(String navs13Upi) {
		this.navs13Upi = navs13Upi;
	}

	public Long getMontant() {
		return montant;
	}

	public void setMontant(Long montant) {
		this.montant = montant;
	}

	public String getAnnule() {
		return annule;
	}

	public void setAnnule(String annule) {
		this.annule = annule;
	}

	public String getIdentifie() {
		return identifie;
	}

	public void setIdentifie(String identifie) {
		this.identifie = identifie;
	}

	public String getUtilisateurTraitant() {
		return utilisateurTraitant;
	}

	public void setUtilisateurTraitant(String utilisateurTraitant) {
		this.utilisateurTraitant = utilisateurTraitant;
	}

	public String getTransmetteur() {
		return transmetteur;
	}

	public void setTransmetteur(String transmetteur) {
		this.transmetteur = transmetteur;
	}

	public String getMessageErreur() {
		return messageErreur;
	}

	public void setMessageErreur(String messageErreur) {
		this.messageErreur = messageErreur;
	}
}
