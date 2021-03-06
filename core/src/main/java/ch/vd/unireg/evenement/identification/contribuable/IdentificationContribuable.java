package ch.vd.unireg.evenement.identification.contribuable;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.Date;

import org.hibernate.annotations.Type;

import ch.vd.unireg.common.HibernateEntity;

/**
 * Contient une demande (et éventuellement la réponse) pour l'identification d'un contribuable. L'identification d'un contribuable est une
 * fonctionnalité demandée initialement par l'application Meldewesen.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@Entity
@Table(name = "EVENEMENT_IDENTIFICATION_CTB")
public class IdentificationContribuable extends HibernateEntity {

	public enum Etat {
		/**
		 * L'événement a été reçu et va être traité automatiquement. Cet état est transient et il ne devrait donc jamais être persisté.
		 */
		RECU,
		/**
		 * L'événement a été reçu, processé mais aucun résultat n'apu être retourné immédiatement et il devra être traité manuellement.
		 */
		A_TRAITER_MANUELLEMENT,
		/**
		 * L'événement à l'état traité manuellement est suspendu.
		 */
		A_TRAITER_MAN_SUSPENDU,
		/**
		 * L'événement a été reçu, processé et un résultat a pu être retourné immédiatement.
		 */
		TRAITE_AUTOMATIQUEMENT,
		/**
		 * L'événement a été traité manuellement.
		 */
		TRAITE_MANUELLEMENT,

		/**
		 * L'événement a été traité manuellement par un gestionnaire back office.
		 */
		TRAITE_MAN_EXPERT,
		/**
		 * Une erreur inattendue est apparue pendant le traitement automatique de l'événement. Il est mis en attente d'une correction de bug
		 * ou d'un traitement manuel.
		 */
		EXCEPTION,

		/**
		 * L'événement est à expertiser par un responsable.
		 */
		A_EXPERTISER,

		/**
		 * L'événement à l'état à expertiser par un responsable est suspendu.
		 */
		A_EXPERTISER_SUSPENDU,
		/**
		 * Un événement suspendu. <b>Attention !</b> A ne plus utiliser, cet état est gardé pour des raisons historiques uniquement.
		 */
		SUSPENDU,

		/**
		 * L'événement n'a pas permis d'identifier le contribuable
		 */
		NON_IDENTIFIE;

		/**
		 * @return <code>true</code> si l'événement n'est pas dans un état terminal
		 */
		public boolean isEncoreATraiter() {
			return this == RECU || this == A_TRAITER_MANUELLEMENT || this == A_TRAITER_MAN_SUSPENDU
					|| this == EXCEPTION || this == A_EXPERTISER || this == A_EXPERTISER_SUSPENDU;
		}
	}

	public enum	ErreurMessage {

		AUCUNE_CORRESPONDANCE("Aucun contribuable ne correspond au message", "01"),
        ACI_AUTRE_CANTON("Envoi manuel à ACI autre canton", "02"),
		SECTION_IMPOT_SOURCE("Envoi manuel à IS", "03"),
		OIPM("Envoi manuel à OIPM", "04"),
		FRONTALIER("Frontalier", "05");

		private final String libelle;
		private final String code;

		ErreurMessage(String libelle, String code){
			this.libelle = libelle;
			this.code = code;
		}

		public String getLibelle() {
			return libelle;
		}

		public String getCode() {
			return code;
		}
	}

	/**
	 * L'id technique
	 */
	private Long id;

	/**
	 * Informations spécifique à l'esb.
	 */
	private EsbHeader header;

	/**
	 * Informations techniques concernant la demande
	 */
	private Demande demande;

	/**
	 * Le nombre de contribuables trouvés par la recherche automatique.
	 */
	private Integer nbContribuablesTrouves;

	/**
	 * Eventuel commentaire généré par la phase automatique du traitement
	 */
	private String commentaireTraitement;

	/**
	 * Etat courant du traitement du message
	 */
	private Etat etat;

	/**
	 * La réponse à la recherche.
	 */
	private Reponse reponse;

	/**
	 * l'id de l'utilisateur qui est en train d'identifier le message
	 */
	private String utilisateurTraitant;

	private String traitementUser;

	private Date dateTraitement;

	/**
	 * En cas d'identification automatique réussi qui a fait intervenir un numéro AVS13 fourni par l'UPI et différent de celui présent dans la demande initiale, ce numéro
	 */
	private String NAVS13Upi;

	@Id
	@GeneratedValue(generator = "defaultGenerator")
	@SequenceGenerator(name = "defaultGenerator", sequenceName = "hibernate_sequence", allocationSize = 1)
	public Long getId() {
		return id;
	}

	public void setId(Long theId) {
		this.id = theId;
	}

	@Transient
	@Override
	public Object getKey() {
		return id;
	}

	@Embedded
	public EsbHeader getHeader() {
		return header;
	}

	public void setHeader(EsbHeader header) {
		this.header = header;
	}

	@Embedded
	public Demande getDemande() {
		return demande;
	}

	public void setDemande(Demande demande) {
		this.demande = demande;
	}

	@Column(name = "NB_CTB_TROUVES")
	public Integer getNbContribuablesTrouves() {
		return nbContribuablesTrouves;
	}

	public void setNbContribuablesTrouves(Integer nbContribuablesTrouves) {
		this.nbContribuablesTrouves = nbContribuablesTrouves;
	}

	@Column(name = "COMMENTAIRE_TRAITEMENT", length = 255)
	public String getCommentaireTraitement() {
		return commentaireTraitement;
	}

	public void setCommentaireTraitement(String commentaireTraitement) {
		this.commentaireTraitement = commentaireTraitement;
	}

	@Column(name = "ETAT", length = 23, nullable = false)
	@Type(type = "ch.vd.unireg.hibernate.identification.contribuable.TypeEtatIdentCtbUserType")
	public Etat getEtat() {
		return etat;
	}

	public void setEtat(Etat etat) {
		this.etat = etat;
	}

	@Embedded
	public Reponse getReponse() {
		return reponse;
	}

	public void setReponse(Reponse reponse) {
		this.reponse = reponse;
	}
	@Column(name = "WORK_USER", length = 65,nullable = true)
	public String getUtilisateurTraitant() {
		return utilisateurTraitant;
	}

	public void setUtilisateurTraitant(String utilisateurTraitant) {
		this.utilisateurTraitant = utilisateurTraitant;
	}

	 @Column(name = "TRAITEMENT_USER", length = 65,nullable = true)
	public String getTraitementUser() {
		return traitementUser;
	}

	public void setTraitementUser(String traitementUser) {
		this.traitementUser = traitementUser;
	}

	@Column(name = "DATE_TRAITEMENT")
	public Date getDateTraitement() {
		return dateTraitement;
	}

	public void setDateTraitement(Date dateTraitement) {
		this.dateTraitement = dateTraitement;
	}

	@Column(name = "NAVS13_UPI", length = 13)
	public String getNAVS13Upi() {
		return NAVS13Upi;
	}

	public void setNAVS13Upi(String NAVS13Upi) {
		this.NAVS13Upi = NAVS13Upi;
	}
}
