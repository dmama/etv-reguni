package ch.vd.uniregctb.webservices.tiers2.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.type.TypeAdresseTiers;
import org.apache.log4j.Logger;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.webservices.tiers2.exception.BusinessException;
import ch.vd.uniregctb.webservices.tiers2.impl.Context;
import ch.vd.uniregctb.webservices.tiers2.impl.DataHelper;

/**
 * Contient les données d'un tiers (soit un contribuable, soit un débiteur). Ces données sont valides à une date donnée.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Tiers", propOrder = {
		"numero", "complementNom", "dateDebutActivite", "dateFinActivite", "dateAnnulation", "personneContact", "numeroTelPrive", "numeroTelProf", "numeroTelPortable", "numeroTelecopie",
		"adresseCourrierElectronique", "blocageRemboursementAutomatique", "isDebiteurInactif", "adresseCourrier", "adressePoursuite", "adresseRepresentation", "adresseDomicile", "adresseEnvoi",
		"adressePoursuiteFormattee", "adresseRepresentationFormattee", "adresseDomicileFormattee", "rapportsEntreTiers", "forFiscalPrincipal", "autresForsFiscaux", "forGestion", "declaration",
		"comptesBancaires"
})
public abstract class Tiers {

	private static final Logger LOGGER = Logger.getLogger(Tiers.class);

	@XmlType(name = "TypeTiers")
	@XmlEnum(String.class)
	public static enum Type {

		PERSONNE_PHYSIQUE,
		MENAGE_COMMUN,
		DEBITEUR,
		PERSONNE_MORALE;

		public static Type fromValue(String v) {
			return valueOf(v);
		}
	}

	/** Numéro de contribuable. */
	@XmlElement(required = true)
	public Long numero;

	/** Complément du nom utilisé lors de l'adressage */
	@XmlElement(required = false)
	public String complementNom;

	/** Date à laquelle l'activité du tiers a débuté. Si le tiers n'a jamais été assujetti, cette date est nulle */
	@XmlElement(required = false)
	public Date dateDebutActivite;

	/**
	 * Date à laquelle l'activité du tiers a pris fin. Si le tiers n'a jamais été assujetti ou s'il est toujours actif, cette date est nulle
	 */
	@XmlElement(required = false)
	public Date dateFinActivite;

	/**
	 * Date à laquelle le tiers a été annulé, ou <b>null</b> si le tiers n'est pas annulé.
	 */
	@XmlElement(required = false)
	public Date dateAnnulation;

	/** Coordonnées de la personne de contact chez le débiteur, ou null si le débiteur est associé avec une personne physique */
	@XmlElement(required = false)
	public String personneContact;

	@XmlElement(required = false)
	public String numeroTelPrive;

	@XmlElement(required = false)
	public String numeroTelProf;

	@XmlElement(required = false)
	public String numeroTelPortable;

	@XmlElement(required = false)
	public String numeroTelecopie;

	@XmlElement(required = false)
	public String adresseCourrierElectronique;

	@XmlElement(required = false)
	public boolean blocageRemboursementAutomatique = true; // [UNIREG-1266] Blocage des remboursements automatiques sur tous les nouveaux tiers

	/** true si débiteur non fiscal - I107 */
	@XmlElement(required = true)
	public boolean isDebiteurInactif;

	/** Adresse courrier du tiers */
	@XmlElement(required = false)
	public Adresse adresseCourrier;

	/** Adresse poursuite du tiers. */
	@XmlElement(required = false)
	public Adresse adressePoursuite;

	/** Adresse représentation du tiers. */
	@XmlElement(required = false)
	public Adresse adresseRepresentation;

	/** Adresse domicile du tiers. */
	@XmlElement(required = false)
	public Adresse adresseDomicile;

	/** Adresse <b>courrier</b> formattée pour l'envoi (six lignes) */
	@XmlElement(required = false)
	public AdresseEnvoi adresseEnvoi;

	/** Adresse de poursuite formattée pour l'envoi (six lignes) */
	@XmlElement(required = false)
	public AdresseEnvoi adressePoursuiteFormattee;

	/** Adresse de représentation formattée pour l'envoi (six lignes) */
	@XmlElement(required = false)
	public AdresseEnvoi adresseRepresentationFormattee;

	/** Adresse de domicile formattée pour l'envoi (six lignes) */
	@XmlElement(required = false)
	public AdresseEnvoi adresseDomicileFormattee;

	/** Rapports entre tiers ouverts pour le tiers. */
	@XmlElement(required = false)
	public List<RapportEntreTiers> rapportsEntreTiers = null;

	/** For fiscal principal ouvert sur la personne physique ou sur le débiteur. Peut être nul si le tiers n'est pas assujetti. */
	@XmlElement(required = false)
	public ForFiscal forFiscalPrincipal;

	/**
	 * Autres fors fiscaux (secondaires, ...) ouverts sur la personne physique (du plus ancien au plus récent). Toujours vide dans le cas
	 * d'un débiteur.
	 */
	@XmlElement(required = false)
	public List<ForFiscal> autresForsFiscaux = null;

	/** For de gestion du tiers. Peut être nul si le tiers n'a jamais été assujetti. */
	@XmlElement(required = false)
	public ForGestion forGestion;

	/** Declaration couramment ouverte pour le tiers. Ou null, si aucune déclaration n'est ouverte. */
	@XmlElement(required = false)
	public Declaration declaration;

	/** Les coordonnées financières du tiers. */
	@XmlElement(required = false)
	public List<CompteBancaire> comptesBancaires;

	public Tiers() {
	}

	public Tiers(ch.vd.uniregctb.tiers.Tiers tiers, Set<TiersPart> parts, ch.vd.registre.base.date.RegDate date, Context context)
			throws BusinessException {
		this.numero = tiers.getNumero();
		this.complementNom = tiers.getComplementNom();
		this.dateDebutActivite = DataHelper.coreToWeb(tiers.getDateDebutActivite());
		this.dateFinActivite = DataHelper.coreToWeb(tiers.getDateFinActivite());
		this.dateAnnulation = DataHelper.coreToWeb(tiers.getAnnulationDate());
		this.personneContact = tiers.getPersonneContact();
		this.numeroTelPrive = tiers.getNumeroTelephonePrive();
		this.numeroTelProf = tiers.getNumeroTelephoneProfessionnel();
		this.numeroTelPortable = tiers.getNumeroTelephonePortable();
		this.numeroTelecopie = tiers.getNumeroTelecopie();
		this.adresseCourrierElectronique = tiers.getAdresseCourrierElectronique();
		this.blocageRemboursementAutomatique = DataHelper.coreToWeb(tiers.getBlocageRemboursementAutomatique());
		this.isDebiteurInactif = tiers.isDebiteurInactif();

		if (parts != null && parts.contains(TiersPart.COMPTES_BANCAIRES)) {
			final String numero = tiers.getNumeroCompteBancaire();
			if (numero != null && !"".equals(numero)) {
				this.comptesBancaires = new ArrayList<CompteBancaire>();
				comptesBancaires.add(new CompteBancaire(tiers, context));
			}
		}

		if (parts != null && parts.contains(TiersPart.ADRESSES)) {

			final ch.vd.uniregctb.adresse.AdressesFiscales adresses;
			try {
				adresses = context.adresseService.getAdressesFiscales(tiers, date, false);
			}
			catch (ch.vd.uniregctb.adresse.AdresseException e) {
				LOGGER.error(e, e);
				throw new BusinessException(e);
			}

			if (adresses != null) {
				this.adresseCourrier = DataHelper.coreToWeb(adresses.courrier, context.infraService);
				this.adresseRepresentation = DataHelper.coreToWeb(adresses.representation, context.infraService);
				this.adressePoursuite = DataHelper.coreToWeb(adresses.poursuite, context.infraService);
				this.adresseDomicile = DataHelper.coreToWeb(adresses.domicile, context.infraService);
			}
		}

		if (parts != null && parts.contains(TiersPart.ADRESSES_ENVOI)) {
			try {
				this.adresseEnvoi = DataHelper.createAdresseFormattee(tiers, date, context, TypeAdresseTiers.COURRIER);
				this.adresseDomicileFormattee = DataHelper.createAdresseFormattee(tiers, date, context, TypeAdresseTiers.DOMICILE);
				this.adresseRepresentationFormattee = DataHelper.createAdresseFormattee(tiers, date, context, TypeAdresseTiers.REPRESENTATION);
				this.adressePoursuiteFormattee = DataHelper.createAdresseFormattee(tiers, date, context, TypeAdresseTiers.POURSUITE);
			}
			catch (AdresseException e) {
				LOGGER.error(e, e);
				throw new BusinessException(e);
			}
		}

		if (parts != null && parts.contains(TiersPart.RAPPORTS_ENTRE_TIERS)) {
			this.rapportsEntreTiers = new ArrayList<RapportEntreTiers>();
			// Ajoute les rapports dont le tiers est le sujet
			for (ch.vd.uniregctb.tiers.RapportEntreTiers rapport : tiers.getRapportsSujet()) {
				if (!(rapport instanceof ch.vd.uniregctb.tiers.ContactImpotSource) && rapport.isValidAt(date)) {
					this.rapportsEntreTiers.add(new RapportEntreTiers(rapport, rapport.getObjet().getNumero()));
				}
			}

			// Ajoute les rapports dont le tiers est l'objet
			for (ch.vd.uniregctb.tiers.RapportEntreTiers rapport : tiers.getRapportsObjet()) {
				if (!(rapport instanceof ch.vd.uniregctb.tiers.ContactImpotSource) && rapport.isValidAt(date)) {
					this.rapportsEntreTiers.add(new RapportEntreTiers(rapport, rapport.getSujet().getNumero()));
				}
			}
		}

		if (parts != null && (parts.contains(TiersPart.FORS_FISCAUX) || parts.contains(TiersPart.FORS_FISCAUX_VIRTUELS))) {
			this.autresForsFiscaux = new ArrayList<ForFiscal>();
			for (ch.vd.uniregctb.tiers.ForFiscal forFiscal : tiers.getForsFiscauxSorted()) {
				if (forFiscal.isValidAt(date)) {
					if (forFiscal instanceof ch.vd.uniregctb.tiers.ForFiscalPrincipal
							|| forFiscal instanceof ch.vd.uniregctb.tiers.ForDebiteurPrestationImposable) {
						Assert.isNull(this.forFiscalPrincipal, "Détecté 2 fors fiscaux principaux valides à la même date");
						this.forFiscalPrincipal = new ForFiscal(forFiscal, false, context);
					}
					else {
						this.autresForsFiscaux.add(new ForFiscal(forFiscal, false, context));
					}
				}
			}

			if (parts.contains(TiersPart.FORS_FISCAUX_VIRTUELS)) {
				final List<ch.vd.uniregctb.tiers.ForFiscalPrincipal> forsVirtuels = DataHelper.getForsFiscauxVirtuels(tiers);
				for (ch.vd.uniregctb.tiers.ForFiscalPrincipal forFiscal : forsVirtuels) {
					if (forFiscal.isValidAt(date)) {
						Assert.isNull(this.forFiscalPrincipal, "Détecté 2 fors fiscaux principaux valides à la même date");
						this.forFiscalPrincipal = new ForFiscal(forFiscal, true, context);
					}
				}
			}
		}

		if (parts != null && parts.contains(TiersPart.FORS_GESTION)) {
			ch.vd.uniregctb.tiers.ForGestion forGestion = context.tiersService.getDernierForGestionConnu(tiers, date);
			if (forGestion != null) {
				this.forGestion = new ForGestion(forGestion.getNoOfsCommune(), context);
			}
		}

		if (parts != null && parts.contains(TiersPart.DECLARATIONS)) {

			// Ajoute la déclaration active
			final ch.vd.uniregctb.declaration.Declaration declaration = tiers.getDeclarationActive(date);

			if (declaration instanceof ch.vd.uniregctb.declaration.DeclarationImpotSource) {
				this.declaration = new DeclarationImpotSource((ch.vd.uniregctb.declaration.DeclarationImpotSource) declaration);
			}
			else if (declaration instanceof ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire) {
				this.declaration = new DeclarationImpotOrdinaire((ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire) declaration, context);
			}
		}
	}

	public Tiers(Tiers tiers, Set<TiersPart> parts) {
		this.numero = tiers.numero;
		this.complementNom = tiers.complementNom;
		this.dateDebutActivite = tiers.dateDebutActivite;
		this.dateFinActivite = tiers.dateFinActivite;
		this.dateAnnulation = tiers.dateAnnulation;
		this.personneContact = tiers.personneContact;
		this.numeroTelPrive = tiers.numeroTelPrive;
		this.numeroTelProf = tiers.numeroTelProf;
		this.numeroTelPortable = tiers.numeroTelPortable;
		this.numeroTelecopie = tiers.numeroTelecopie;
		this.adresseCourrierElectronique = tiers.adresseCourrierElectronique;
		this.blocageRemboursementAutomatique = tiers.blocageRemboursementAutomatique;
		this.isDebiteurInactif = tiers.isDebiteurInactif;

		copyParts(tiers, parts);
	}

	/**
	 * Complète le tiers courant avec les parts spécifiées du tiers spécifié.
	 *
	 * @param tiers
	 *            le tiers possèdant les parts à copier
	 * @param parts
	 *            les parts à copier
	 */
	public void copyPartsFrom(Tiers tiers, Set<TiersPart> parts) {
		copyParts(tiers, parts);
	}

	/**
	 * @return une copie du tiers courant dont seules les parts spécifiées sont renseignées.
	 */
	public abstract Tiers clone(Set<TiersPart> parts);

	private void copyParts(Tiers tiers, Set<TiersPart> parts) {

		if (parts != null && parts.contains(TiersPart.COMPTES_BANCAIRES)) {
			this.comptesBancaires = tiers.comptesBancaires;
		}

		if (parts != null && parts.contains(TiersPart.ADRESSES)) {
			this.adresseCourrier = tiers.adresseCourrier;
			this.adresseRepresentation = tiers.adresseRepresentation;
			this.adressePoursuite = tiers.adressePoursuite;
			this.adresseDomicile = tiers.adresseDomicile;
		}

		if (parts != null && parts.contains(TiersPart.ADRESSES_ENVOI)) {
			this.adresseEnvoi = tiers.adresseEnvoi;
			this.adresseDomicileFormattee = tiers.adresseDomicileFormattee;
			this.adresseRepresentationFormattee = tiers.adresseRepresentationFormattee;
			this.adressePoursuiteFormattee = tiers.adressePoursuiteFormattee;
		}

		if (parts != null && parts.contains(TiersPart.RAPPORTS_ENTRE_TIERS)) {
			this.rapportsEntreTiers = tiers.rapportsEntreTiers;
		}

		if (parts != null && (parts.contains(TiersPart.FORS_FISCAUX) || parts.contains(TiersPart.FORS_FISCAUX_VIRTUELS))) {
			if (parts.contains(TiersPart.FORS_FISCAUX_VIRTUELS)) {
				this.forFiscalPrincipal = tiers.forFiscalPrincipal;
			}
			else {
				// supprime les éventuels fors virtuels s'ils ne sont pas demandés
				if (tiers.forFiscalPrincipal != null && !tiers.forFiscalPrincipal.virtuel) {
					this.forFiscalPrincipal = tiers.forFiscalPrincipal;
				}
				else {
					this.forFiscalPrincipal = null;
				}
			}
			this.autresForsFiscaux = tiers.autresForsFiscaux;
		}

		if (parts != null && parts.contains(TiersPart.FORS_GESTION)) {
			this.forGestion = tiers.forGestion;
		}

		if (parts != null && parts.contains(TiersPart.DECLARATIONS)) {
			this.declaration = tiers.declaration;
		}
	}
}
