package ch.vd.unireg.editique.impl;

import java.util.List;

import noNamespace.InfoArchivageDocument;
import noNamespace.InfoArchivageDocument.InfoArchivage;
import noNamespace.InfoEnteteDocumentDocument1.InfoEnteteDocument;
import noNamespace.InfoEnteteDocumentDocument1.InfoEnteteDocument.Destinataire;
import noNamespace.InfoEnteteDocumentDocument1.InfoEnteteDocument.Expediteur;
import noNamespace.TypAdresse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.adresse.AdresseEnvoi;
import ch.vd.unireg.adresse.AdresseEnvoiDetaillee;
import ch.vd.unireg.adresse.AdresseException;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.adresse.TypeAdresseFiscale;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.common.RueEtNumero;
import ch.vd.unireg.declaration.Declaration;
import ch.vd.unireg.declaration.EtatDeclarationSommee;
import ch.vd.unireg.editique.ConstantesEditique;
import ch.vd.unireg.editique.EditiqueException;
import ch.vd.unireg.editique.EditiquePrefixeHelper;
import ch.vd.unireg.editique.LegacyEditiqueHelper;
import ch.vd.unireg.editique.TypeDocumentEditique;
import ch.vd.unireg.editique.ZoneAffranchissementEditique;
import ch.vd.unireg.interfaces.common.Adresse;
import ch.vd.unireg.interfaces.infra.InfrastructureException;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.data.TypeAffranchissement;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.tiers.CollectiviteAdministrative;
import ch.vd.unireg.tiers.ForGestion;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.type.TypeEtatDocumentFiscal;

import static ch.vd.unireg.editique.EditiqueAbstractHelperImpl.CAT_NOM_SERVICE_EXPEDITEUR;
import static ch.vd.unireg.editique.EditiqueAbstractHelperImpl.CAT_TRAITE_PAR;
import static noNamespace.InfoDocumentDocument1.InfoDocument;
import static noNamespace.InfoDocumentDocument1.InfoDocument.Affranchissement;

/**
 * Implémentation du Helper éditique pour le bon vieux temps où les XSD éditiques étaient gérées par XmlBeans,
 * sans namespace (i.e. tous les documents d'avant l'avènement des PM)...
 */
public class LegacyEditiqueHelperImpl implements LegacyEditiqueHelper {

	public static final Logger LOGGER = LoggerFactory.getLogger(LegacyEditiqueHelperImpl.class);

	private static final String IMPOT_A_LA_SOURCE_MIN = "Impôt à la source";

	private ServiceInfrastructureService infraService;
	private AdresseService adresseService;
	private TiersService tiersService;

	public void setInfraService(ServiceInfrastructureService infraService) {
		this.infraService = infraService;
	}

	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	/**
	 * Alimente la partie Destinataire du document
	 */
	@Override
	public Destinataire remplitDestinataire(Tiers tiers, InfoEnteteDocument infoEnteteDocument) throws AdresseException {
		final Destinataire destinataire = remplitAdresseEnvoiDestinataire(tiers, infoEnteteDocument);
		destinataire.setNumContribuable(FormatNumeroHelper.numeroCTBToDisplay(tiers.getNumero()));
		return destinataire;
	}

	private Destinataire remplitAdresseEnvoiDestinataire(Tiers tiers, InfoEnteteDocument infoEnteteDocument) throws AdresseException {
		final AdresseEnvoi adresseEnvoi = adresseService.getAdresseEnvoi(tiers, null, TypeAdresseFiscale.COURRIER, false);
		final Destinataire destinataire = infoEnteteDocument.addNewDestinataire();
		final TypAdresse.Adresse adresseDestinataire = destinataire.addNewAdresse();
		remplitAdresse(adresseEnvoi, adresseDestinataire);
		return destinataire;
	}

	@Override
	public Destinataire remplitDestinataire(CollectiviteAdministrative collAdm, InfoEnteteDocument infoEnteteDocument) throws AdresseException {
		return remplitAdresseEnvoiDestinataire(collAdm, infoEnteteDocument);
	}

	@Override
	public Destinataire remplitDestinataireArchives(InfoEnteteDocument infoEnteteDocument) {
		final Destinataire destinataire = infoEnteteDocument.addNewDestinataire();
		final TypAdresse.Adresse adresseDestinataire = destinataire.addNewAdresse();
		adresseDestinataire.setAdresseCourrierLigne1("Archives");
		adresseDestinataire.setNilAdresseCourrierLigne2();
		adresseDestinataire.setNilAdresseCourrierLigne3();
		adresseDestinataire.setNilAdresseCourrierLigne4();
		adresseDestinataire.setNilAdresseCourrierLigne5();
		adresseDestinataire.setNilAdresseCourrierLigne6();
		return destinataire;
	}

	/**
	 * Alimente la partie PorteAdresse du document
	 */
	@Override
	public TypAdresse remplitPorteAdresse(Tiers tiers, InfoEnteteDocument infoEnteteDocument) throws AdresseException {
		AdresseEnvoi adresseEnvoi = adresseService.getAdresseEnvoi(tiers, null, TypeAdresseFiscale.COURRIER, false);
		TypAdresse porteAdresse = infoEnteteDocument.addNewPorteAdresse();
		TypAdresse.Adresse adressePorteAdresse = porteAdresse.addNewAdresse();
		remplitAdresse(adresseEnvoi, adressePorteAdresse);
		return porteAdresse;
	}

	@Override
	public TypAdresse.Adresse remplitAdresse(AdresseEnvoi source, TypAdresse.Adresse cible) {

		// On teste la non-nullité des champs avant l'assignation car si on assigne un
		// null dans un element qui a été déclaré avec l'attribut nillable="true" dans la XSD
		// alors une AssertionError est levée par xmlbeans lorsqu'on accede à cet objet.
		// L'exception est générée via un assert, donc seulement vrai si '-ea' est spécifié au démarrage de la JVM
		//
		// il faut utiliser les méthodes setNilXXX() pour renseigner un champs à nill
		//
		// cf. https://issues.apache.org/jira/browse/XMLBEANS-317

		if (source.getLigne1() != null) {
			cible.setAdresseCourrierLigne1(source.getLigne1());
		}
		if (source.getLigne2() != null) {
			cible.setAdresseCourrierLigne2(source.getLigne2());
		} else {
			cible.setNilAdresseCourrierLigne2();
		}
		if (source.getLigne3() != null) {
			cible.setAdresseCourrierLigne3(source.getLigne3());
		} else {
			cible.setNilAdresseCourrierLigne3();
		}
		if (source.getLigne4() != null) {
			cible.setAdresseCourrierLigne4(source.getLigne4());
		} else {
			cible.setNilAdresseCourrierLigne4();
		}
		if (source.getLigne5() != null) {
			cible.setAdresseCourrierLigne5(source.getLigne5());
		} else {
			cible.setNilAdresseCourrierLigne5();
		}
		if (source.getLigne6() != null) {
			cible.setAdresseCourrierLigne6(source.getLigne6());
		} else {
			cible.setNilAdresseCourrierLigne6();
		}
		return cible;
	}

	/**
	 * Alimente la partie expéditeur du document
	 */
	@Override
	public Expediteur remplitExpediteurACI(InfoEnteteDocument infoEnteteDocument) throws InfrastructureException {
		ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrative aci = infraService.getACI();
		return remplitExpediteur(aci, infoEnteteDocument);
	}

	private Expediteur remplitExpediteur(ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrative ca, InfoEnteteDocument infoEnteteDocument) throws InfrastructureException {
		final Adresse adresse = ca.getAdresse();
		if (adresse == null) {
			throw new InfrastructureException("Le collectivité administrative n°" + ca.getNoColAdm() + " ne possède pas d'adresse courrier.");
		}
		final Commune commune = infraService.getCommuneByAdresse(adresse, null);

		final Expediteur expediteur = infoEnteteDocument.addNewExpediteur();
		final TypAdresse.Adresse adresseExpediteur = expediteur.addNewAdresse();
		AdresseEnvoi adresseEnvoi = new AdresseEnvoi();
		adresseEnvoi.addLine(ca.getNomComplet1());
		adresseEnvoi.addLine(ca.getNomComplet2());
		adresseEnvoi.addLine(ca.getNomComplet3());
		adresseEnvoi.addLine(RueEtNumero.format(adresse.getRue(), adresse.getNumero()));
		adresseEnvoi.addLine(adresse.getNumeroPostal() + ' ' + adresse.getLocalite());
		remplitAdresse(adresseEnvoi, adresseExpediteur);
		ExpediteurNillableValuesFiller expNilValues = new ExpediteurNillableValuesFiller();
		expNilValues.setAdrMes(ca.getAdresseEmail());
		expNilValues.setNumFax(ca.getNoFax());
		expNilValues.setNumCCP(ca.getNoCCP());
		expNilValues.setNumTelephone(ca.getNoTelephone());
		expNilValues.fill(expediteur);
		expediteur.setTraitePar("");
		// Apparement la commune de l'aci n'est pas renseignée dans le host ...
		if (commune == null) {
			expediteur.setLocaliteExpedition("Lausanne");
		}
		else {
			expediteur.setLocaliteExpedition(StringUtils.capitalize(commune.getNomOfficiel()));
		}
		return expediteur;
	}

	/**
	 * Alimente la partie expéditeur CAT du document
	 */
	@Override
	public Expediteur remplitExpediteurCAT(InfoEnteteDocument infoEnteteDocument) throws InfrastructureException {
		final ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrative cat = infraService.getCAT();
		final Expediteur expediteur = remplitExpediteur(cat, infoEnteteDocument);
		expediteur.setSrvExp(CAT_NOM_SERVICE_EXPEDITEUR);
		expediteur.setTraitePar(CAT_TRAITE_PAR);
		return expediteur;
	}

	@Override
	public Expediteur remplitExpediteur(CollectiviteAdministrative collAdm, InfoEnteteDocument infoEnteteDocument) throws InfrastructureException {
		final ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrative ca = infraService.getCollectivite(collAdm.getNumeroCollectiviteAdministrative());
		return remplitExpediteur(ca, infoEnteteDocument);
	}

	@Override
	public String getCommune(Declaration di) throws EditiqueException {
		final List<ForGestion> gestionHisto = tiersService.getForsGestionHisto(di.getTiers());
		final List<DateRange> gestionSurPf = DateRangeHelper.intersections(di, gestionHisto);
		if (gestionSurPf == null || gestionSurPf.size() == 0) {
			String message = String.format(
					"Le contribuable %s n'a pas de for gestion actif sur la période de la déclaration du %s au %s",
					di.getTiers().getNumero(),
					di.getDateDebut(),
					di.getDateFin()
			);
			throw new EditiqueException(message);
		}

		final ForGestion forGestion = DateRangeHelper.rangeAt(gestionHisto, gestionSurPf.get(gestionSurPf.size() - 1).getDateFin());
		Integer numeroOfsAutoriteFiscale = forGestion.getNoOfsCommune();
		Commune commune;
		try {
			commune = infraService.getCommuneByNumeroOfs(numeroOfsAutoriteFiscale, forGestion.getDateFin());
		}
		catch (InfrastructureException e) {
			commune = null;
			LOGGER.error("Exception lors de la recherche de la commune par numéro " + numeroOfsAutoriteFiscale, e);
		}
		if (commune == null) {
			String message = "La commune correspondant au numéro " + numeroOfsAutoriteFiscale + " n'a pas pu être déterminée";
			throw new EditiqueException(message);
		}
		return commune.getNomOfficiel();
	}

	/**
	 * Alimente la partie expéditeur d'une sommation de LR
	 */
	@Override
	public Expediteur remplitExpediteurPourSommationLR(Declaration declaration, InfoEnteteDocument infoEnteteDocument, String traitePar) throws InfrastructureException {
		//
		// Expediteur
		//
		ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrative aciImpotSource = infraService.getACIImpotSource();
		Adresse adresseAciImpotSource = aciImpotSource.getAdresse();
		if (adresseAciImpotSource == null) {
			throw new InfrastructureException("Le collectivité administrative n°" + aciImpotSource.getNoColAdm() + " ne possède pas d'adresse courrier.");
		}

		Expediteur expediteur = infoEnteteDocument.addNewExpediteur();
		TypAdresse.Adresse adresseExpediteur = expediteur.addNewAdresse();
		AdresseEnvoi adresseExpACI = new AdresseEnvoi();
		adresseExpACI.addLine(aciImpotSource.getNomComplet1());
		adresseExpACI.addLine(aciImpotSource.getNomComplet2());
		adresseExpACI.addLine(aciImpotSource.getNomComplet3());
		adresseExpACI.addLine(RueEtNumero.format(adresseAciImpotSource.getRue(), adresseAciImpotSource.getNumero()));
		adresseExpACI.addLine(adresseAciImpotSource.getNumeroPostal() + ' ' + adresseAciImpotSource.getLocalite());
		remplitAdresse(adresseExpACI, adresseExpediteur);
		ExpediteurNillableValuesFiller expNilValues = new ExpediteurNillableValuesFiller();
		expNilValues.setAdrMes(aciImpotSource.getAdresseEmail());
		expNilValues.setNumFax(aciImpotSource.getNoFax());
		expNilValues.setNumCCP(aciImpotSource.getNoCCP());
		//UNIREG-3309
		//Il faut que pour les sommations de LR ( et UNIQUEMENT les sommations de LR )
		// Modifier le n° de téléphone pour mettre celui du CAT. (le n° de fax doit rester inchangé).
		expNilValues.setNumTelephone(infraService.getCAT().getNoTelephone());
		expNilValues.fill(expediteur);
		if (traitePar != null) {
			expediteur.setTraitePar(traitePar);
		}
		expediteur.setLocaliteExpedition("Lausanne");
		final RegDate dateExpedition;
		final EtatDeclarationSommee sommee = (EtatDeclarationSommee) declaration.getDernierEtatDeclarationOfType(TypeEtatDocumentFiscal.SOMME);
		dateExpedition = sommee.getDateEnvoiCourrier();
		expediteur.setDateExpedition(Integer.toString(dateExpedition.index()));
		expediteur.setNotreReference(FormatNumeroHelper.numeroCTBToDisplay(declaration.getTiers().getNumero()));

		return expediteur;
	}

	
	/**
	 * Alimente la partie expéditeur d'une LR
	 */
	@Override
	public Expediteur remplitExpediteurPourEnvoiLR(Declaration declaration, InfoEnteteDocument infoEnteteDocument, String traitePar) throws InfrastructureException {
		//
		// Expediteur
		//
		ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrative aci = infraService.getACI();
		Adresse aciAdresse = aci.getAdresse();
		if (aciAdresse == null) {
			throw new InfrastructureException("Le collectivité administrative n°" + aci.getNoColAdm() + " ne possède pas d'adresse courrier.");
		}

		Expediteur expediteur = infoEnteteDocument.addNewExpediteur();
		TypAdresse.Adresse adresseExpediteur = expediteur.addNewAdresse();
		AdresseEnvoi adresseEnvoiExp = new AdresseEnvoi();
		adresseEnvoiExp.addLine(aci.getNomComplet1());
		adresseEnvoiExp.addLine(IMPOT_A_LA_SOURCE_MIN);
		adresseEnvoiExp.addLine(aci.getNomComplet3());
		adresseEnvoiExp.addLine(RueEtNumero.format(aciAdresse.getRue(), aciAdresse.getNumero()));
		adresseEnvoiExp.addLine(aciAdresse.getNumeroPostal() + ' ' + aciAdresse.getLocalite());
		remplitAdresse(adresseEnvoiExp, adresseExpediteur);
		ExpediteurNillableValuesFiller expNilValues = new ExpediteurNillableValuesFiller();
		expNilValues.setAdrMes(aci.getAdresseEmail());
		expNilValues.setNumFax(aci.getNoFax());
		expNilValues.setNumCCP(aci.getNoCCP());
		expNilValues.setNumTelephone(infraService.getCAT().getNoTelephone());
		expNilValues.fill(expediteur);
		if (traitePar != null) {
			expediteur.setTraitePar(traitePar);
		}

		expediteur.setLocaliteExpedition("Lausanne");
		final RegDate dateExpedition;
		dateExpedition = RegDate.get();
		expediteur.setDateExpedition(Integer.toString(dateExpedition.index()));
		expediteur.setNotreReference(FormatNumeroHelper.numeroCTBToDisplay(declaration.getTiers().getNumero()));

		return expediteur;
	}

	@Override
	public InfoArchivage buildInfoArchivage(TypeDocumentEditique typeDocument, long noTiers, String cleArchivage, RegDate dateTraitement) {
		if (typeDocument.getCodeDocumentArchivage() == null) {
			throw new IllegalArgumentException("Archivage non-supporté pour le document de type " + typeDocument);
		}
		final InfoArchivage infoArchivage = InfoArchivageDocument.Factory.newInstance().addNewInfoArchivage();
		fillInfoArchivage(infoArchivage, typeDocument, noTiers, cleArchivage, dateTraitement);
		return infoArchivage;
	}

	@Override
	public InfoArchivage fillInfoArchivage(InfoArchivage infoArchivage, TypeDocumentEditique typeDocument, long noTiers, String cleArchivage, RegDate dateTraitement) {
		if (typeDocument.getCodeDocumentArchivage() == null) {
			throw new IllegalArgumentException("Archivage non-supporté pour le document de type " + typeDocument);
		}
		infoArchivage.setPrefixe(EditiquePrefixeHelper.buildPrefixeInfoArchivage(typeDocument));
		infoArchivage.setNomApplication(ConstantesEditique.APPLICATION_ARCHIVAGE);
		infoArchivage.setTypDossier(ConstantesEditique.TYPE_DOSSIER_ARCHIVAGE);
		infoArchivage.setNomDossier(FormatNumeroHelper.numeroCTBToDisplay(noTiers));
		infoArchivage.setTypDocument(typeDocument.getCodeDocumentArchivage());
		infoArchivage.setIdDocument(cleArchivage);
		infoArchivage.setDatTravail(String.valueOf(dateTraitement.index()));
		return infoArchivage;
	}

	@Override
	public ZoneAffranchissementEditique remplitAffranchissement(InfoDocument infoDocument, AdresseEnvoiDetaillee adresseEnvoiDetaillee) throws EditiqueException {

		final ZoneAffranchissementEditique zone;

		//SIFISC-6270
		if (adresseEnvoiDetaillee.isIncomplete()) {
			zone = ZoneAffranchissementEditique.INCONNU;
		}
		else {

			final TypeAffranchissement typeAffranchissementAdresse = adresseEnvoiDetaillee.getTypeAffranchissement();

			if (typeAffranchissementAdresse != null) {
				switch (typeAffranchissementAdresse) {
				case SUISSE:
					zone = ZoneAffranchissementEditique.SUISSE;
					break;
				case EUROPE:
					zone = ZoneAffranchissementEditique.EUROPE;
					break;
				case MONDE:
					zone = ZoneAffranchissementEditique.RESTE_MONDE;
					break;
				default:
					zone = null;
				}
			}
			else {
				zone = null;
			}
		}

		final Affranchissement affranchissement = infoDocument.addNewAffranchissement();
		affranchissement.setZone(zone != null ? zone.getCode() : StringUtils.EMPTY);
		return zone;
	}

	@Override
	public ZoneAffranchissementEditique remplitAffranchissement(InfoDocument infoDocument, Tiers tiers) throws EditiqueException {
		AdresseEnvoiDetaillee adresse;
		try {
			adresse = adresseService.getAdresseEnvoi(tiers, null, TypeAdresseFiscale.COURRIER, false);
		}
		catch (AdresseException e) {
			throw new EditiqueException("Impossible de récuperer l'adresse d'envoi pour le tiers = " + tiers.getNumero() + " erreur: " + e.getMessage());
		}
		return remplitAffranchissement(infoDocument, adresse);
	}

}
