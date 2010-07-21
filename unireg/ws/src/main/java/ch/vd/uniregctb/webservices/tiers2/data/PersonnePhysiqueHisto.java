package ch.vd.uniregctb.webservices.tiers2.data;

import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import ch.vd.registre.civil.model.EnumAttributeIndividu;
import ch.vd.uniregctb.webservices.tiers2.data.PersonnePhysique.Categorie;
import ch.vd.uniregctb.webservices.tiers2.exception.BusinessException;
import ch.vd.uniregctb.webservices.tiers2.impl.Context;
import ch.vd.uniregctb.webservices.tiers2.impl.DataHelper;
import ch.vd.uniregctb.webservices.tiers2.impl.EnumHelper;
import org.apache.log4j.Logger;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PersonnePhysiqueHisto", propOrder = {
		"nom", "prenom", "dateNaissance", "sexe", "dateDeces", "ancienNumeroAssureSocial", "nouveauNumeroAssureSocial", "dateArrivee", "categorie"
})
public class PersonnePhysiqueHisto extends ContribuableHisto {

	private static final Logger LOGGER = Logger.getLogger(PersonnePhysiqueHisto.class);

	/** Nom de famille de la personne. */
	@XmlElement(required = true)
	public String nom;

	/** Prénom de la personne. */
	@XmlElement(required = false)
	public String prenom;

	/**
	 * La date de naissance de la personne physique. Cette date peut être partielle, c'est-à-dire que les informations de jour et/ou de mois
	 * peuvent valoir 0.
	 */
	@XmlElement(required = false)
	public Date dateNaissance;

	/**
	 * Le sexe de la personne (qui peut ne pas être connu dans certains cas).
	 */
	@XmlElement(required = false)
	public Sexe sexe;

	/**
	 * La date de décès de la personne
	 */
	@XmlElement(required = false)
	public Date dateDeces;

	/** L'ancien  numéro de sécurité sociale (ancien format sur 11 positions). */
	@XmlElement(required = false)
	public String ancienNumeroAssureSocial;

	/** Le nouveau numéro de sécurité sociale (nouveau format sur 13 positions). */
	@XmlElement(required = false)
	public String nouveauNumeroAssureSocial;

	/** Date d'arrivée dans le canton. Nulle si cette information n'est pas connue. */
	@XmlElement(required = false)
	public Date dateArrivee;

	/** Catégorie de personne physique */
	@XmlElement(required = true)
	public Categorie categorie;

	public PersonnePhysiqueHisto() {
	}

	public PersonnePhysiqueHisto(ch.vd.uniregctb.tiers.PersonnePhysique personne, Set<TiersPart> setParts, Context context) throws BusinessException {
		super(personne, setParts, context);
		initBase(personne, context);
	}

	public PersonnePhysiqueHisto(ch.vd.uniregctb.tiers.PersonnePhysique personne, int periode, Set<TiersPart> setParts, Context context) throws BusinessException {
		super(personne, periode, setParts, context);
		initBase(personne, context);
	}

	public PersonnePhysiqueHisto(PersonnePhysiqueHisto personne, Set<TiersPart> setParts) {
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

	private void initBase(ch.vd.uniregctb.tiers.PersonnePhysique personne, Context context) throws BusinessException {
		if (!personne.isHabitantVD()) {
			this.nom = personne.getNom();
			this.prenom = personne.getPrenom();
			this.dateNaissance = DataHelper.coreToWeb(personne.getDateNaissance());
			this.sexe = EnumHelper.coreToWeb(personne.getSexe());
			this.dateDeces = DataHelper.coreToWeb(personne.getDateDeces());
			for (ch.vd.uniregctb.tiers.IdentificationPersonne ident : personne.getIdentificationsPersonnes()) {
				if (ident.getCategorieIdentifiant().equals(ch.vd.uniregctb.type.CategorieIdentifiant.CH_AHV_AVS)) {
					this.ancienNumeroAssureSocial = ident.getIdentifiant();
				}
			}
			this.nouveauNumeroAssureSocial = personne.getNumeroAssureSocial();
			this.dateArrivee = DataHelper.coreToWeb(personne.getDateDebutActivite());
			this.categorie = EnumHelper.coreToWeb(personne.getCategorieEtranger());
		}
		else {
			final int annee = 2400;
			final ch.vd.uniregctb.interfaces.model.Individu individu = context.serviceCivilService.getIndividu(personne.getNumeroIndividu(), annee, EnumAttributeIndividu.PERMIS);

			if (individu == null) {
				final String message = String.format("Impossible de trouver l'individu n°%d pour l'habitant n°%d", personne
						.getNumeroIndividu(), personne.getNumero());
				LOGGER.error(message);
				throw new BusinessException(message);
			}

			final ch.vd.uniregctb.interfaces.model.HistoriqueIndividu data = individu.getDernierHistoriqueIndividu();
			this.nom = data.getNom();
			this.prenom = data.getPrenom();
			this.dateNaissance = DataHelper.coreToWeb(individu.getDateNaissance());
			this.sexe = (individu.isSexeMasculin() ? Sexe.MASCULIN : Sexe.FEMININ);
			if (personne.getDateDeces() != null) {
				this.dateDeces =  DataHelper.coreToWeb(personne.getDateDeces());
			} else {
				this.dateDeces = DataHelper.coreToWeb(individu.getDateDeces());
			}

			this.nouveauNumeroAssureSocial = individu.getNouveauNoAVS();
			this.ancienNumeroAssureSocial = data.getNoAVS();
			this.dateArrivee = DataHelper.coreToWeb(data.getDateDebutValidite());

			final ch.vd.uniregctb.interfaces.model.Permis permis = individu.getPermisActif(null);
			if (permis == null) {
				this.categorie = ch.vd.uniregctb.webservices.tiers2.data.PersonnePhysique.Categorie.SUISSE;
			}
			else {
				this.categorie = EnumHelper.coreToWeb(permis.getTypePermis());
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public TiersHisto clone(Set<TiersPart> parts) {
		return new PersonnePhysiqueHisto(this, parts);
	}
}
