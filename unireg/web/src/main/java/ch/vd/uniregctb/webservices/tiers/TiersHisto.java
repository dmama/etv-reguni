package ch.vd.uniregctb.webservices.tiers;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlType;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.DateRangeHelper.Range;
import ch.vd.uniregctb.adresse.TypeAdresseFiscale;
import ch.vd.uniregctb.type.TypeAdresseTiers;
import ch.vd.uniregctb.webservices.tiers.impl.Context;
import ch.vd.uniregctb.webservices.tiers.impl.DataHelper;

/**
 * Contient les données historique d'un tiers. Ces données peuvent être complètes ou limitées à une période fiscale.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "TiersHisto", propOrder = {
		"numero", "complementNom", "dateDebutActivite", "dateFinActivite", "dateAnnulation", "personneContact", "numeroTelPrive",
		"numeroTelProf", "numeroTelPortable", "numeroTelecopie", "adresseCourrierElectronique", "blocageRemboursementAutomatique",
		"isDebiteurInactif", "adressesCourrier", "adressesPoursuite", "adressesRepresentation", "adressesDomicile", "adresseEnvoi",
		"rapportsEntreTiers", "forsFiscauxPrincipaux", "autresForsFiscaux", "forsGestions", "declarations", "comptesBancaires"
})
public abstract class TiersHisto {

	/** Numéro de contribuable. */
	@XmlElement(required = true)
	public Long numero;

	/** Complément du nom utilisé lors de l'adressage */
	@XmlElement(required = false)
	public String complementNom;

	/** Date à laquelle l'activité du tiers a débuté. Si le tiers n'a jamais été assujetti, cette date est nulle */
	@XmlElement(required = false)
	public Date dateDebutActivite;

	/** Date à laquelle l'activité du tiers a pris fin. Si le tiers est toujours actif, la date n'est pas renseignée */
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
	public boolean blocageRemboursementAutomatique;

	/** true si débiteur non fiscal - I107 */
	@XmlElement(required = true)
	public boolean isDebiteurInactif;

	/** Historique des adresses courrier du tiers (de la plus ancienne à la plus récente). */
	@XmlElement(required = false)
	public List<Adresse> adressesCourrier = null;

	/** Historique des adresses poursuite du tiers (de la plus ancienne à la plus récente). */
	@XmlElement(required = false)
	public List<Adresse> adressesPoursuite = null;

	/** Historique des adresses représentation du tiers (de la plus ancienne à la plus récente). */
	@XmlElement(required = false)
	public List<Adresse> adressesRepresentation = null;

	/** Historique des adresses domicile du tiers (de la plus ancienne à la plus récente). */
	@XmlElement(required = false)
	public List<Adresse> adressesDomicile = null;

	/**
	 * L'adresse d'envoi de courrier complète (six lignes) du tiers.
	 * <p>
	 * <b>Attention !</b> Il s'agit de l'adresse d'envoi la plus récente connue, indépendemment de la période historique demandée.
	 */
	@XmlElement(required = false)
	public AdresseEnvoi adresseEnvoi = null;

	/** Historique des rapports entre tiers pour ce tiers (du plus ancien au plus récent). */
	@XmlElement(required = false)
	public List<RapportEntreTiers> rapportsEntreTiers = null;

	/** Historique des fors fiscaux principaux existants sur la personne physique ou sur le débiteur (du plus ancien au plus récent). */
	@XmlElement(required = false)
	public List<ForFiscal> forsFiscauxPrincipaux = null;

	/**
	 * Historique des autres fors fiscaux (secondaires, ...) existants sur la personne physique (du plus ancien au plus récent). Toujours
	 * vide dans le cas d'un débiteur.
	 */
	@XmlElement(required = false)
	public List<ForFiscal> autresForsFiscaux = null;

	/** Historique des fors de gestion (du plus ancien au plus récent). Cette liste peut-être vide si le tiers n'a jamais été assujetti. */
	@XmlElement(required = false)
	public List<ForGestion> forsGestions = null;

	/** Les coordonnées financières du tiers. */
	@XmlElement(required = false)
	public List<CompteBancaire> comptesBancaires;

	/** Historique des déclarations pour le tiers. Ou null, si aucune déclaration n'est ouverte. */
	@XmlElements(value = {
			@XmlElement(name = "di", type = DeclarationImpotOrdinaire.class),
			@XmlElement(name = "lr", type = DeclarationImpotSource.class)
	})
	public List<Declaration> declarations = null;

	public TiersHisto() {
	}

	public TiersHisto(ch.vd.uniregctb.tiers.Tiers tiers, Set<TiersPart> parts, Context context) {
		initBase(tiers, context);
		initParts(context, tiers, parts, null);
	}

	public TiersHisto(ch.vd.uniregctb.tiers.Tiers tiers, int periode, Set<TiersPart> parts, Context context) {
		final Range range = new Range(RegDate.get(periode, 1, 1), RegDate.get(periode, 12, 31));
		initBase(tiers, context);
		initParts(context, tiers, parts, range);
	}

	public TiersHisto(TiersHisto tiers, Set<TiersPart> parts) {
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
		this.comptesBancaires = tiers.comptesBancaires;

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
	public void copyPartsFrom(TiersHisto tiers, Set<TiersPart> parts) {
		copyParts(tiers, parts);
	}

	/**
	 * @return une copie du tiers courant dont seules les parts spécifiées sont renseignées.
	 */
	public abstract TiersHisto clone(Set<TiersPart> parts);

	private void initBase(ch.vd.uniregctb.tiers.Tiers tiers, Context context) {
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

		final String numero = tiers.getNumeroCompteBancaire();
		if (numero != null && !"".equals(numero)) {
			this.comptesBancaires = new ArrayList<CompteBancaire>();
			comptesBancaires.add(new CompteBancaire(tiers, context));
		}
	}

	private void initParts(Context context, ch.vd.uniregctb.tiers.Tiers tiers, Set<TiersPart> parts, final Range range) {
		if (parts != null && parts.contains(TiersPart.ADRESSES)) {
			initAdresses(tiers, context, range);
		}

		if (parts != null && parts.contains(TiersPart.ADRESSES_ENVOI)) {
			initAdresseEnvoi(tiers, context);
		}

		if (parts != null && parts.contains(TiersPart.RAPPORTS_ENTRE_TIERS)) {
			initRapports(tiers, context, range);
		}

		if (parts != null && parts.contains(TiersPart.FORS_FISCAUX)) {
			initForsFiscaux(tiers, range, context);
		}

		if (parts != null && parts.contains(TiersPart.FORS_GESTION)) {
			initForsGestion(tiers, range, context);
		}

		if (parts != null && parts.contains(TiersPart.DECLARATIONS)) {
			initDeclarations(tiers, range, context);
		}
	}

	private void initAdresseEnvoi(ch.vd.uniregctb.tiers.Tiers tiers, Context context) {
		ch.vd.uniregctb.adresse.AdresseEnvoiDetaillee adresse;
		try {
			adresse = context.adresseService.getAdresseEnvoi(tiers, null, TypeAdresseFiscale.COURRIER, false);
		}
		catch (ch.vd.uniregctb.adresse.AdresseException e) {
			// impossible d'exposer quoi que ce soit
			adresse = null;
		}

		if (adresse != null) {
			this.adresseEnvoi = DataHelper.coreToWeb(adresse);
		}
	}

	private void initDeclarations(ch.vd.uniregctb.tiers.Tiers tiers, final Range range, Context context) {
		this.declarations = new ArrayList<Declaration>();
		for (ch.vd.uniregctb.declaration.Declaration declaration : tiers.getDeclarationsSorted()) {
			if (range != null && !DateRangeHelper.intersect(declaration, range)) {
				continue;
			}
			if (declaration instanceof ch.vd.uniregctb.declaration.DeclarationImpotSource) {
				this.declarations.add(new DeclarationImpotSource((ch.vd.uniregctb.declaration.DeclarationImpotSource) declaration));
			}
			else if (declaration instanceof ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire) {
				this.declarations.add(new DeclarationImpotOrdinaire((ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire) declaration, context));
			}
		}
		if (this.declarations.isEmpty()) {
			this.declarations = null;
		}
	}

	private void initForsGestion(ch.vd.uniregctb.tiers.Tiers tiers, final Range range,Context context) {
		this.forsGestions = new ArrayList<ForGestion>();
		for (ch.vd.uniregctb.tiers.ForGestion forGestion : context.tiersService.getForsGestionHisto(tiers)) {
			if (range != null && !DateRangeHelper.intersect(forGestion, range)) {
				continue;
			}
			this.forsGestions.add(new ForGestion(forGestion, context));
		}
		if (this.forsGestions.isEmpty()) {
			this.forsGestions = null;
		}
	}

	private void initForsFiscaux(ch.vd.uniregctb.tiers.Tiers tiers, final Range range,Context context) {
		this.forsFiscauxPrincipaux = new ArrayList<ForFiscal>();
		this.autresForsFiscaux = new ArrayList<ForFiscal>();
		for (ch.vd.uniregctb.tiers.ForFiscal forFiscal : tiers.getForsFiscauxSorted()) {
			if (range != null && !DateRangeHelper.intersect(forFiscal, range)) {
				continue;
			}
			if (forFiscal instanceof ch.vd.uniregctb.tiers.ForFiscalPrincipal
					|| forFiscal instanceof ch.vd.uniregctb.tiers.ForDebiteurPrestationImposable) {
				this.forsFiscauxPrincipaux.add(new ForFiscal(forFiscal,context));
			}
			else {
				this.autresForsFiscaux.add(new ForFiscal(forFiscal,context));
			}
		}
		if (this.forsFiscauxPrincipaux.isEmpty()) {
			this.forsFiscauxPrincipaux = null;
		}
		if (this.autresForsFiscaux.isEmpty()) {
			this.autresForsFiscaux = null;
		}
	}

	private void initRapports(ch.vd.uniregctb.tiers.Tiers tiers, Context context, final Range range) {
		this.rapportsEntreTiers = new ArrayList<RapportEntreTiers>();
		// Ajoute les rapports dont le tiers est le sujet
		for (ch.vd.uniregctb.tiers.RapportEntreTiers rapport : tiers.getRapportsSujet()) {
			if (rapport instanceof ch.vd.uniregctb.tiers.ContactImpotSource || (range != null && !DateRangeHelper.intersect(rapport, range))) {
				continue;
			}

			this.rapportsEntreTiers.add(new RapportEntreTiers(rapport, rapport.getObjetId()));
		}

		// Ajoute les rapports dont le tiers est l'objet
		for (ch.vd.uniregctb.tiers.RapportEntreTiers rapport : tiers.getRapportsObjet()) {
			if (rapport instanceof ch.vd.uniregctb.tiers.ContactImpotSource || (range != null && !DateRangeHelper.intersect(rapport, range))) {
				continue;
			}
			this.rapportsEntreTiers.add(new RapportEntreTiers(rapport, rapport.getSujetId()));
		}
		if (this.rapportsEntreTiers.isEmpty()) {
			this.rapportsEntreTiers = null;
		}
	}

	private void initAdresses(ch.vd.uniregctb.tiers.Tiers tiers, Context context, final Range range) {
		ch.vd.uniregctb.adresse.AdressesFiscalesHisto adresses;
		try {
			adresses = context.adresseService.getAdressesFiscalHisto(tiers, false);
		}
		catch (ch.vd.uniregctb.adresse.AdresseException e) {
			// impossible d'exposer quoi que ce soit
			adresses = null;
		}

		if (adresses != null) {
			this.adressesCourrier = DataHelper.coreToWeb(adresses.courrier, range, context.infraService);
			this.adressesPoursuite = DataHelper.coreToWeb(adresses.poursuite, range, context.infraService);
			this.adressesRepresentation = DataHelper.coreToWeb(adresses.representation, range, context.infraService);
			this.adressesDomicile = DataHelper.coreToWeb(adresses.domicile, range, context.infraService);
		}
	}

	private final void copyParts(TiersHisto tiers, Set<TiersPart> parts) {
		if (parts != null && parts.contains(TiersPart.ADRESSES)) {
			this.adressesCourrier = tiers.adressesCourrier;
			this.adressesPoursuite = tiers.adressesPoursuite;
			this.adressesRepresentation = tiers.adressesRepresentation;
			this.adressesDomicile = tiers.adressesDomicile;
		}

		if (parts != null && parts.contains(TiersPart.ADRESSES_ENVOI)) {
			this.adresseEnvoi = tiers.adresseEnvoi;
		}

		if (parts != null && parts.contains(TiersPart.RAPPORTS_ENTRE_TIERS)) {
			this.rapportsEntreTiers = tiers.rapportsEntreTiers;
		}

		if (parts != null && parts.contains(TiersPart.FORS_FISCAUX)) {
			this.forsFiscauxPrincipaux = tiers.forsFiscauxPrincipaux;
			this.autresForsFiscaux = tiers.autresForsFiscaux;
		}

		if (parts != null && parts.contains(TiersPart.FORS_GESTION)) {
			this.forsGestions = tiers.forsGestions;
		}

		if (parts != null && parts.contains(TiersPart.DECLARATIONS)) {
			this.declarations = tiers.declarations;
		}
	}
}
