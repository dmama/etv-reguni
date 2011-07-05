package ch.vd.uniregctb.webservices.tiers2.data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import ch.vd.uniregctb.interfaces.model.AttributeIndividu;
import ch.vd.uniregctb.webservices.tiers2.exception.BusinessException;
import ch.vd.uniregctb.webservices.tiers2.impl.Context;
import ch.vd.uniregctb.webservices.tiers2.impl.DataHelper;
import ch.vd.uniregctb.webservices.tiers2.impl.EnumHelper;

/**
 * <b>Dans la version 3 du web-service :</b> <i>naturalPersonType</i> (xml) / <i>NaturalPerson</i> (client java)
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PersonnePhysique", propOrder = {
		"nom", "prenom", "dateNaissance", "sexe", "dateDeces", "ancienNumeroAssureSocial", "nouveauNumeroAssureSocial", "dateArrivee", "categorie"
})
public class PersonnePhysique extends Contribuable {

	private static final Logger LOGGER = Logger.getLogger(PersonnePhysique.class);

	/**
	 * <b>Dans la version 3 du web-service :</b> <i>naturalPersonCategoryType</i> (xml) / <i>NaturalPersonCategory</i> (client java)
	 *
	 * @see http://www.ejpd.admin.ch/ejpd/fr/home/themen/migration/ref_aufenthalt.html
	 */
	@XmlType(name = "Categorie")
	@XmlEnum(String.class)
	public static enum Categorie {

		/**
		 * <b>Dans la version 3 du web-service :</b> <i>SWISS</i>.
		 */
		SUISSE,
		/**
		 * <b>Dans la version 3 du web-service :</b> <i>CIVIL_SERVANT</i>.
		 */
		FONCTIONNAIRE,
		/**
		 * <b>Dans la version 3 du web-service :</b> <i>C_02_B_PERMIT</i>.
		 */
		_02_PERMIS_SEJOUR_B,
		/**
		 * <b>Dans la version 3 du web-service :</b> <i>C_03_C_PERMIT</i>.
		 */
		_03_ETABLI_C,
		/**
		 * <b>Dans la version 3 du web-service :</b> <i>C_04_CI_PERMIT</i>.
		 */
		_04_CONJOINT_DIPLOMATE_CI,
		/**
		 * <b>Dans la version 3 du web-service :</b> <i>C_05_F_PERMIT</i>.
		 */
		_05_ETRANGER_ADMIS_PROVISOIREMENT_F,
		/**
		 * <b>Dans la version 3 du web-service :</b> <i>C_06_G_PERMIT</i>.
		 */
		_06_FRONTALIER_G,
		/**
		 * <b>Dans la version 3 du web-service :</b> <i>C_07_L_PERMIT</i>.
		 */
		_07_PERMIS_SEJOUR_COURTE_DUREE_L,
		/**
		 * <b>Dans la version 3 du web-service :</b> <i>C_08_N_PERMIT</i>.
		 */
		_08_REQUERANT_ASILE_N,
		/**
		 * <b>Dans la version 3 du web-service :</b> <i>C_09_S_PERMIT</i>.
		 */
		_09_A_PROTEGER_S,
		/**
		 * <b>Dans la version 3 du web-service :</b> <i>C_10_OBLIGED_TO_ANNOUNCE</i>.
		 */
		_10_TENUE_DE_S_ANNONCER,
		/**
		 * <b>Dans la version 3 du web-service :</b> <i>C_11_DIPLOMAT</i>.
		 */
		_11_DIPLOMATE,
		/**
		 * <b>Dans la version 3 du web-service :</b> <i>C_12_INTERNATIONAL_CIVIL_SERVANT</i>.
		 */
		_12_FONCTIONNAIRE_INTERNATIONAL,
		/**
		 * <b>Dans la version 3 du web-service :</b> <i>C_13_NOT_ASSIGNED</i>.
		 */
		_13_NON_ATTRIBUEE;

		public static Categorie fromValue(String v) {
			return valueOf(v);
		}
	}

	/**
	 * Nom de famille de la personne.
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> champ <i>officialName</i> de la structure <i>personIdentificationType</i>.
	 */
	@XmlElement(required = true)
	public String nom;

	/**
	 * Prénom de la personne.
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> champ <i>firstName</i> de la structure <i>personIdentificationType</i>.
	 */
	@XmlElement(required = false)
	public String prenom;

	/**
	 * La date de naissance de la personne physique. Cette date peut être partielle, c'est-à-dire que les informations de jour et/ou de mois peuvent valoir 0.
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>dateOfBirth</i>.
	 */
	@XmlElement(required = false)
	public Date dateNaissance;

	/**
	 * Le sexe de la personne (qui peut ne pas être connu dans certains cas).
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> champ <i>sex</i> de la structure <i>personIdentificationType</i>.
	 */
	@XmlElement(required = false)
	public Sexe sexe;

	/**
	 * La date de décès de la personne
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>dateOfDeath</i>.
	 */
	@XmlElement(required = false)
	public Date dateDeces;

	/**
	 * L'ancien numéro de sécurité sociale (ancien format sur 11 positions).
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> entrée avec la clé <i>CH_AHV_AVS</i> de la collection <i>otherPersonId</i> de la structure <i>personIdentificationType</i>.
	 */
	@XmlElement(required = false)
	public String ancienNumeroAssureSocial;

	/**
	 * Le nouveau numéro de sécurité sociale (nouveau format sur 13 positions).
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> champ <i>vn</i> de la structure <i>personIdentificationType</i>.
	 */
	@XmlElement(required = false)
	public String nouveauNumeroAssureSocial;

	/**
	 * Date d'arrivée dans le canton. Nulle si cette information n'est pas connue.
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>activityStartDate</i>.
	 */
	@XmlElement(required = false)
	public Date dateArrivee;

	/**
	 * Catégorie de personne physique
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>category</i>.
	 */
	@XmlElement(required = true)
	public Categorie categorie;

	public PersonnePhysique() {
	}

	public PersonnePhysique(ch.vd.uniregctb.tiers.PersonnePhysique personne, Set<TiersPart> setParts, ch.vd.registre.base.date.RegDate date,
	                        Context context) throws BusinessException {
		super(personne, setParts, date, context);

		if (!personne.isHabitantVD()) {
			this.nom = StringUtils.trimToNull(personne.getNom());
			this.prenom = StringUtils.trimToNull(personne.getPrenom());
			this.dateNaissance = DataHelper.coreToWeb(personne.getDateNaissance());
			this.sexe = EnumHelper.coreToWeb(personne.getSexe());
			this.dateDeces = DataHelper.coreToWeb(personne.getDateDeces());
			for (ch.vd.uniregctb.tiers.IdentificationPersonne ident : personne.getIdentificationsPersonnes()) {
				if (ident.getCategorieIdentifiant() == ch.vd.uniregctb.type.CategorieIdentifiant.CH_AHV_AVS) {
					this.ancienNumeroAssureSocial = ident.getIdentifiant();
				}
			}
			this.nouveauNumeroAssureSocial = personne.getNumeroAssureSocial();
			this.dateArrivee = DataHelper.coreToWeb(personne.getDateDebutActivite());
			this.categorie = EnumHelper.coreToWeb(personne.getCategorieEtranger());
		}
		else {
			final ch.vd.uniregctb.interfaces.model.Individu individu = context.serviceCivilService.getIndividu(personne.getNumeroIndividu(), null, AttributeIndividu.PERMIS);
			if (individu == null) {
				final String message = String.format("Impossible de trouver l'individu n°%d pour l'habitant n°%d", personne.getNumeroIndividu(), personne.getNumero());
				LOGGER.error(message);
				throw new BusinessException(message);
			}

			ch.vd.uniregctb.interfaces.model.HistoriqueIndividu data = individu.getHistoriqueIndividuAt(date);
			if (data == null) {
				// on se rabat sur le premier
				data = individu.getHistoriqueIndividu().iterator().next();
			}
			this.nom = StringUtils.trimToNull(data.getNom());
			this.prenom = StringUtils.trimToNull(data.getPrenom());
			this.dateNaissance = DataHelper.coreToWeb(individu.getDateNaissance());
			this.sexe = (individu.isSexeMasculin() ? Sexe.MASCULIN : Sexe.FEMININ);
			if (personne.getDateDeces() != null) {
				this.dateDeces = DataHelper.coreToWeb(personne.getDateDeces());
			}
			else {
				this.dateDeces = DataHelper.coreToWeb(individu.getDateDeces());
			}

			this.nouveauNumeroAssureSocial = individu.getNouveauNoAVS();
			this.ancienNumeroAssureSocial = data.getNoAVS();
			this.dateArrivee = DataHelper.coreToWeb(data.getDateDebutValidite());

			final ch.vd.uniregctb.interfaces.model.Permis permis = individu.getPermisActif(date);
			if (permis == null) {
				this.categorie = Categorie.SUISSE;
			}
			else {
				this.categorie = EnumHelper.coreToWeb(permis.getTypePermis());
			}
		}
	}

	public PersonnePhysique(PersonnePhysique personne, Set<TiersPart> setParts) {
		super(personne, setParts);
		this.nom = personne.nom;
		this.prenom = personne.prenom;
		this.dateNaissance = personne.dateNaissance;
		this.sexe = personne.sexe;
		this.dateDeces = personne.dateDeces;
		this.nouveauNumeroAssureSocial = personne.nouveauNumeroAssureSocial;
		this.ancienNumeroAssureSocial = personne.ancienNumeroAssureSocial;
		this.dateArrivee = personne.dateArrivee;
		this.categorie = personne.categorie;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Tiers clone(Set<TiersPart> parts) {
		return new PersonnePhysique(this, parts);
	}
}
