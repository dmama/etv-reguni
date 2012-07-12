package ch.vd.uniregctb.mouvement.view;

import java.util.Date;

import ch.vd.uniregctb.common.Annulable;
import ch.vd.uniregctb.common.BaseComparator;
import ch.vd.uniregctb.mouvement.EtatMouvementDossier;
import ch.vd.uniregctb.type.Localisation;
import ch.vd.uniregctb.type.TypeMouvement;

/**
 * Vue sur les mouvements de dossier
 */
public class MouvementDetailView implements Comparable<MouvementDetailView>, Annulable {

	private static final BaseComparator<MouvementDetailView> comparator = new BaseComparator<MouvementDetailView>(
			new String[]{"annule", "dateMouvement", "dateExecution"},
			new Boolean[]{true, false, false});

	private ContribuableView contribuable;

	/**
	 * ID du mouvement
	 */
	private Long id;

	/**
	 * Réception ou Envoi
	 */
	private TypeMouvement typeMouvement;

	/**
	 * Etat du mouvement
	 */
	private EtatMouvementDossier etatMouvement;

	/**
	 * Date de création du mouvement
	 */
	private Date dateMouvement;

	/**
	 * Visa de l'exécutant du mouvement (= créateur)
	 */
	private String executant;

	/**
	 * Date de création du mouvement
	 */
	private Date dateExecution;

	/**
	 * ID technique de la tâche associée
	 */
	private Long idTache;

	/**
	 * Flag interne d'annulation
	 */
	private boolean annule;

	/**
	 * Détermine si oui ou non ce mouvement sera annulable dans l'IHM
	 */
	private boolean annulable;

	//
	// Partie relative à l'affichage simple dans une liste
	//

	/**
	 * Collectivité administrative émettrice (envoi) ou réceptrice (reception) du mouvement
	 */
	private String collectiviteAdministrative;

	/**
	 * Pour un envoi, collectivité/utilisateur de destination
	 * <br/>
	 * Pour une réception, localisation de la réception (ou utilisateur si réception "personnelle")
	 */
	private String destinationUtilisateur;

	//
	// Affichage des détails d'un mouvement
	//

	/**
	 * Nom et prénom de l'utilisateur à montrer dans le détail du mouvement (destinataire de l'envoi ou récepteur personnel)
	 */
	private String nomPrenomUtilisateur;

	/**
	 * Numéro de téléphone et OID de l'utilisateur à montrer dans le détail du mouvement (destinataire de l'envoi ou récepteur personnel)
	 */
	private String numeroTelephoneUtilisateur;

	//
	// Mouvements d'envoi
	//

	/**
	 * Numéro de la collectivité administrative destinataire d'un mouvement d'envoi
	 */
	private Integer noCollAdmDestinataireEnvoi;

	/**
	 * Nom de la collectivité administrative destinataire d'un envoi
	 */
	private String collAdmDestinataireEnvoi;

	/**
	 * Numéro d'invididu de l'utilisateur destinataire d'un mouvement d'envoi
	 */
	private Long numeroUtilisateurEnvoi;

	/**
	 * visa de l'utilisateur destination de l'envoi
	 */
	private String utilisateurEnvoi;

	/**
	 * Type de mouvement d'envoi (édition) : utilisateurEnvoi ou collectivite
	 */
	private String destinationEnvoi;


	//
	// Mouvements de réception
	//

	/**
	 * Numéro d'individu de l'utilisateur recepteur d'un mouvement de réception personnel
	 */
	private Long numeroUtilisateurReception;

	/**
	 * visa de l'utilisateur qui réceptionne l'envoi
	 */
	private String utilisateurReception;

	/**
	 * Type de mouvement de réception
	 */
	private Localisation localisation;

	//
	// Méthodes
	//

	@Override
	public int compareTo(MouvementDetailView o) {
		return comparator.compare(this, o);
	}

	public ContribuableView getContribuable() {
		return contribuable;
	}

	public void setContribuable(ContribuableView contribuable) {
		this.contribuable = contribuable;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getCollAdmDestinataireEnvoi() {
		return collAdmDestinataireEnvoi;
	}

	public void setCollAdmDestinataireEnvoi(String collAdmDestinataireEnvoi) {
		this.collAdmDestinataireEnvoi = collAdmDestinataireEnvoi;
	}

	public Localisation getLocalisation() {
		return localisation;
	}

	public void setLocalisation(Localisation localisation) {
		this.localisation = localisation;
	}

	public Date getDateMouvement() {
		return dateMouvement;
	}

	public void setDateMouvement(Date dateMouvement) {
		this.dateMouvement = dateMouvement;
	}

	@Override
	public boolean isAnnule() {
		return annule;
	}

	public void setAnnule(boolean annule) {
		this.annule = annule;
	}

	public TypeMouvement getTypeMouvement() {
		return typeMouvement;
	}

	public void setTypeMouvement(TypeMouvement typeMouvement) {
		this.typeMouvement = typeMouvement;
	}

	public Integer getNoCollAdmDestinataireEnvoi() {
		return noCollAdmDestinataireEnvoi;
	}

	public void setNoCollAdmDestinataireEnvoi(Integer noCollAdmDestinataireEnvoi) {
		this.noCollAdmDestinataireEnvoi = noCollAdmDestinataireEnvoi;
	}

	public Long getIdTache() {
		return idTache;
	}

	public void setIdTache(Long idTache) {
		this.idTache = idTache;
	}

	public boolean isAnnulable() {
		return annulable;
	}

	public void setAnnulable(boolean annulable) {
		this.annulable = annulable;
	}

	public Long getNumeroUtilisateurEnvoi() {
		return numeroUtilisateurEnvoi;
	}

	public void setNumeroUtilisateurEnvoi(Long numeroUtilisateurEnvoi) {
		this.numeroUtilisateurEnvoi = numeroUtilisateurEnvoi;
	}

	public Long getNumeroUtilisateurReception() {
		return numeroUtilisateurReception;
	}

	public void setNumeroUtilisateurReception(Long numeroUtilisateurReception) {
		this.numeroUtilisateurReception = numeroUtilisateurReception;
	}

	public EtatMouvementDossier getEtatMouvement() {
		return etatMouvement;
	}

	public void setEtatMouvement(EtatMouvementDossier etatMouvement) {
		this.etatMouvement = etatMouvement;
	}

	public String getCollectiviteAdministrative() {
		return collectiviteAdministrative;
	}

	public void setCollectiviteAdministrative(String collectiviteAdministrative) {
		this.collectiviteAdministrative = collectiviteAdministrative;
	}

	public String getDestinationUtilisateur() {
		return destinationUtilisateur;
	}

	public void setDestinationUtilisateur(String destinationUtilisateur) {
		this.destinationUtilisateur = destinationUtilisateur;
	}

	public String getNomPrenomUtilisateur() {
		return nomPrenomUtilisateur;
	}

	public void setNomPrenomUtilisateur(String nomPrenomUtilisateur) {
		this.nomPrenomUtilisateur = nomPrenomUtilisateur;
	}

	public String getNumeroTelephoneUtilisateur() {
		return numeroTelephoneUtilisateur;
	}

	public void setNumeroTelephoneUtilisateur(String numeroTelephoneUtilisateur) {
		this.numeroTelephoneUtilisateur = numeroTelephoneUtilisateur;
	}

	public String getDestinationEnvoi() {
		return destinationEnvoi;
	}

	public void setDestinationEnvoi(String destinationEnvoi) {
		this.destinationEnvoi = destinationEnvoi;
	}

	public Date getDateExecution() {
		return dateExecution;
	}

	public void setDateExecution(Date dateExecution) {
		this.dateExecution = dateExecution;
	}

	public String getExecutant() {
		return executant;
	}

	public void setExecutant(String executant) {
		this.executant = executant;
	}

	public String getUtilisateurEnvoi() {
		return utilisateurEnvoi;
	}

	public void setUtilisateurEnvoi(String utilisateurEnvoi) {
		this.utilisateurEnvoi = utilisateurEnvoi;
	}

	public String getUtilisateurReception() {
		return utilisateurReception;
	}

	public void setUtilisateurReception(String utilisateurReception) {
		this.utilisateurReception = utilisateurReception;
	}
}
