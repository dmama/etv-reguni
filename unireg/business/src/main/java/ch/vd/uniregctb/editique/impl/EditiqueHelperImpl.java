package ch.vd.uniregctb.editique.impl;

import java.rmi.RemoteException;

import noNamespace.InfoArchivageDocument;
import noNamespace.InfoArchivageDocument.InfoArchivage;
import noNamespace.InfoEnteteDocumentDocument1.InfoEnteteDocument;
import noNamespace.InfoEnteteDocumentDocument1.InfoEnteteDocument.Destinataire;
import noNamespace.InfoEnteteDocumentDocument1.InfoEnteteDocument.Expediteur;
import noNamespace.TypAdresse;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.uniregctb.adresse.AdresseEnvoi;
import ch.vd.uniregctb.adresse.AdresseEnvoiDetaillee;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.adresse.AdressesResolutionException;
import ch.vd.uniregctb.adresse.TypeAdresseFiscale;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.EtatDeclarationSommee;
import ch.vd.uniregctb.editique.EditiqueAbstractHelper;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.EditiqueHelper;
import ch.vd.uniregctb.editique.TypeDocumentEditique;
import ch.vd.uniregctb.interfaces.model.TypeAffranchissement;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.ForGestion;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.TypeEtatDeclaration;

import static noNamespace.InfoDocumentDocument1.InfoDocument;
import static noNamespace.InfoDocumentDocument1.InfoDocument.Affranchissement;

public class EditiqueHelperImpl extends EditiqueAbstractHelper implements EditiqueHelper {

	public static final Logger LOGGER = Logger.getLogger(EditiqueHelperImpl.class);

	private static final String IMPOT_A_LA_SOURCE_MIN = "Impôt à la source";

	private static final String APPLICATION_ARCHIVAGE = "FOLDERS";
	private static final String TYPE_DOSSIER_ARCHIVAGE = "003";


	private ServiceInfrastructureService infraService;
	private AdresseService adresseService;
	private TiersService tiersService;


	/**
	 * Alimente la partie Destinataire du document
	 *
	 * @param tiers
	 * @param infoEnteteDocument
	 * @return
	 * @throws AdressesResolutionException
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
		final TypAdresse.Adresse adresseDestinataireImpression = remplitAdresse(adresseEnvoi, adresseDestinataire);
		destinataire.setAdresse(adresseDestinataireImpression);
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
		adresseDestinataire.setAdresseCourrierLigne2(null);
		adresseDestinataire.setAdresseCourrierLigne3(null);
		adresseDestinataire.setAdresseCourrierLigne4(null);
		adresseDestinataire.setAdresseCourrierLigne5(null);
		adresseDestinataire.setAdresseCourrierLigne6(null);
		return destinataire;
	}

	/**
	 * Alimente la partie PorteAdresse du document
	 *
	 * @param tiers
	 * @param infoEnteteDocument
	 * @return
	 * @throws AdressesResolutionException
	 */
	@Override
	public TypAdresse remplitPorteAdresse(Tiers tiers, InfoEnteteDocument infoEnteteDocument) throws AdresseException {
		//
		// Porte adresse
		//
		AdresseEnvoi adresseEnvoi = adresseService.getAdresseEnvoi(tiers, null, TypeAdresseFiscale.COURRIER, false);
		TypAdresse porteAdresse = infoEnteteDocument.addNewPorteAdresse();
		TypAdresse.Adresse adressePorteAdresse = porteAdresse.addNewAdresse();
		TypAdresse.Adresse adressePorteAdresseImpression = remplitAdresse(adresseEnvoi, adressePorteAdresse);
		porteAdresse.setAdresse(adressePorteAdresseImpression);
		// porteAdresse.setNumRecommande(numRecommande);
		return porteAdresse;
	}

	/**
	 * Alimente la partie Adresse
	 *
	 * @param adresseEnvoi
	 * @param adresseDestinataire
	 * @return
	 */
	private TypAdresse.Adresse remplitAdresse(AdresseEnvoi adresseEnvoi, TypAdresse.Adresse adresseDestinataire) {
		adresseDestinataire.setAdresseCourrierLigne1(adresseEnvoi.getLigne1());
		adresseDestinataire.setAdresseCourrierLigne2(adresseEnvoi.getLigne2());
		adresseDestinataire.setAdresseCourrierLigne3(adresseEnvoi.getLigne3());
		adresseDestinataire.setAdresseCourrierLigne4(adresseEnvoi.getLigne4());
		adresseDestinataire.setAdresseCourrierLigne5(adresseEnvoi.getLigne5());
		adresseDestinataire.setAdresseCourrierLigne6(adresseEnvoi.getLigne6());
		return adresseDestinataire;
	}

	/**
	 * Alimente la partie expéditeur du document
	 *
	 * @param infoEnteteDocument
	 * @return
	 * @throws ServiceInfrastructureException
	 * @throws RemoteException
	 * @throws ServiceInfrastructureException
	 * @throws ServiceInfrastructureException
	 */
	@Override
	public Expediteur remplitExpediteurACI(InfoEnteteDocument infoEnteteDocument) throws ServiceInfrastructureException {
		ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrative aci = infraService.getACI();
		return remplitExpediteur(aci, infoEnteteDocument);
	}

	private Expediteur remplitExpediteur(ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrative ca, InfoEnteteDocument infoEnteteDocument) throws ServiceInfrastructureException {
		final ch.vd.unireg.interfaces.civil.data.Adresse adresse = ca.getAdresse();
		final Commune commune = infraService.getCommuneByAdresse(adresse, null);

		final Expediteur expediteur = infoEnteteDocument.addNewExpediteur();
		final TypAdresse.Adresse adresseExpediteur = expediteur.addNewAdresse();
		adresseExpediteur.setAdresseCourrierLigne1(ca.getNomComplet1());
		adresseExpediteur.setAdresseCourrierLigne2(ca.getNomComplet2());
		adresseExpediteur.setAdresseCourrierLigne3(ca.getNomComplet3());
		adresseExpediteur.setAdresseCourrierLigne4(adresse.getRue());
		adresseExpediteur.setAdresseCourrierLigne5(adresse.getNumeroPostal() + ' ' + adresse.getLocalite());
		adresseExpediteur.setAdresseCourrierLigne6(null);
		expediteur.setAdresse(adresseExpediteur);
		expediteur.setAdrMes(ca.getAdresseEmail());
		expediteur.setNumTelephone(ca.getNoTelephone());
		expediteur.setNumFax(ca.getNoFax());
		expediteur.setNumCCP(ca.getNoCCP());
		expediteur.setTraitePar("");
		// Apparement la commune de l'aci n'est pas renseignée dans le host ...
		if (commune == null) {
			expediteur.setLocaliteExpedition("Lausanne");
		}
		else {
			expediteur.setLocaliteExpedition(StringUtils.capitalize(commune.getNomMinuscule()));
		}
		return expediteur;
	}

	/**
	 * Alimente la partie expéditeur CAT du document
	 *
	 * @param infoEnteteDocument
	 * @return
	 * @throws ServiceInfrastructureException
	 * @throws RemoteException
	 * @throws ServiceInfrastructureException
	 * @throws ServiceInfrastructureException
	 */
	@Override
	public Expediteur remplitExpediteurCAT(InfoEnteteDocument infoEnteteDocument) throws ServiceInfrastructureException {
		final ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrative cat = infraService.getCAT();
		return remplitExpediteur(cat, infoEnteteDocument);
	}

	@Override
	public Expediteur remplitExpediteur(CollectiviteAdministrative collAdm, InfoEnteteDocument infoEnteteDocument) throws ServiceInfrastructureException {
		final ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrative ca = infraService.getCollectivite(collAdm.getNumeroCollectiviteAdministrative());
		return remplitExpediteur(ca, infoEnteteDocument);
	}

	@Override
	public String getCommune(Declaration di) throws EditiqueException {
		ForGestion forGestion = tiersService.getForGestionActif(di.getTiers(), di.getDateDebut());
		if (forGestion == null) {
			forGestion = tiersService.getForGestionActif(di.getTiers(), di.getDateFin());
			if (forGestion == null) {
				String message = String.format(
						"Le contribuable %s n'a pas de for gestion actif au %s ou au %s",
						di.getTiers().getNumero(),
						di.getDateDebut(),
						di.getDateFin()
				);
				throw new EditiqueException(message);
			}
		}
		Integer numeroOfsAutoriteFiscale = forGestion.getNoOfsCommune();
		Commune commune;
		try {
			commune = infraService.getCommuneByNumeroOfsEtendu(numeroOfsAutoriteFiscale, forGestion.getDateFin());
		}
		catch (ServiceInfrastructureException e) {
			commune = null;
			LOGGER.error("Exception lors de la recherche de la commune par numéro " + numeroOfsAutoriteFiscale, e);
		}
		if (commune == null) {
			String message = "La commune correspondant au numéro " + numeroOfsAutoriteFiscale + " n'a pas pu être déterminée";
			throw new EditiqueException(message);
		}
		return commune.getNomMinuscule();
	}

	/**
	 * Alimente la partie expéditeur d'une sommation de LR
	 */
	@Override
	public Expediteur remplitExpediteurPourSommationLR(Declaration declaration, InfoEnteteDocument infoEnteteDocument, String traitePar) throws ServiceInfrastructureException {
		//
		// Expediteur
		//
		ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrative aciImpotSource = infraService.getACIImpotSource();
		ch.vd.unireg.interfaces.civil.data.Adresse adresseAciImpotSource = aciImpotSource.getAdresse();

		Expediteur expediteur = infoEnteteDocument.addNewExpediteur();
		TypAdresse.Adresse adresseExpediteur = expediteur.addNewAdresse();
		adresseExpediteur.setAdresseCourrierLigne1(aciImpotSource.getNomComplet1());
		adresseExpediteur.setAdresseCourrierLigne2(aciImpotSource.getNomComplet2());
		adresseExpediteur.setAdresseCourrierLigne3(aciImpotSource.getNomComplet3());
		adresseExpediteur.setAdresseCourrierLigne4(adresseAciImpotSource.getRue());
		adresseExpediteur.setAdresseCourrierLigne5(adresseAciImpotSource.getNumeroPostal() + ' ' + adresseAciImpotSource.getLocalite());
		adresseExpediteur.setAdresseCourrierLigne6(null);
		expediteur.setAdresse(adresseExpediteur);
		expediteur.setAdrMes(aciImpotSource.getAdresseEmail());
		expediteur.setNumFax(aciImpotSource.getNoFax());
		expediteur.setNumCCP(aciImpotSource.getNoCCP());
		if (traitePar != null) {
			expediteur.setTraitePar(traitePar);
		}
		expediteur.setLocaliteExpedition("Lausanne");
		final RegDate dateExpedition;
		final EtatDeclarationSommee sommee = (EtatDeclarationSommee) declaration.getDernierEtatOfType(TypeEtatDeclaration.SOMMEE);
		dateExpedition = sommee.getDateEnvoiCourrier();
		//UNIREG-3309
		//Il faut que pour les sommations de LR ( et UNIQUEMENT les sommations de LR )
		// Modifier le n° de téléphone pour mettre celui du CAT. (le n° de fax doit rester inchangé).
		expediteur.setNumTelephone(infraService.getCAT().getNoTelephone());
		expediteur.setDateExpedition(Integer.toString(dateExpedition.index()));
		expediteur.setNotreReference(FormatNumeroHelper.numeroCTBToDisplay(declaration.getTiers().getNumero()));

		return expediteur;
	}


	/**
	 * Alimente la partie expéditeur d'une LR
	 */
	@Override
	public Expediteur remplitExpediteurPourEnvoiLR(Declaration declaration, InfoEnteteDocument infoEnteteDocument, String traitePar) throws ServiceInfrastructureException {
		//
		// Expediteur
		//
		ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrative aci = infraService.getACI();
		ch.vd.unireg.interfaces.civil.data.Adresse aciAdresse = aci.getAdresse();

		Expediteur expediteur = infoEnteteDocument.addNewExpediteur();
		TypAdresse.Adresse adresseExpediteur = expediteur.addNewAdresse();
		adresseExpediteur.setAdresseCourrierLigne1(aci.getNomComplet1());
		adresseExpediteur.setAdresseCourrierLigne2(IMPOT_A_LA_SOURCE_MIN);
		adresseExpediteur.setAdresseCourrierLigne3(aci.getNomComplet3());
		adresseExpediteur.setAdresseCourrierLigne4(aciAdresse.getRue());
		adresseExpediteur.setAdresseCourrierLigne5(aciAdresse.getNumeroPostal() + ' ' + aciAdresse.getLocalite());
		adresseExpediteur.setAdresseCourrierLigne6(null);
		expediteur.setAdresse(adresseExpediteur);
		expediteur.setAdrMes(aci.getAdresseEmail());
		expediteur.setNumFax(aci.getNoFax());
		expediteur.setNumCCP(aci.getNoCCP());
		if (traitePar != null) {
			expediteur.setTraitePar(traitePar);
		}

		expediteur.setLocaliteExpedition("Lausanne");
		final RegDate dateExpedition;
		dateExpedition = RegDate.get();
		expediteur.setNumTelephone(infraService.getCAT().getNoTelephone());
		expediteur.setDateExpedition(Integer.toString(dateExpedition.index()));
		expediteur.setNotreReference(FormatNumeroHelper.numeroCTBToDisplay(declaration.getTiers().getNumero()));

		return expediteur;
	}

	/**
	 * Construit une structure éditique pour une demande d'archivage de document lors de sa génération
	 *
	 * @param typeDocument   le type de document qui nous intéresse
	 * @param noTiers        le numéro du tiers concerné par le document
	 * @param cleArchivage   la clé d'archivage du document
	 * @param dateTraitement la date de génération du document
	 * @return la structure de demande d'archivage remplie
	 */
	@Override
	public InfoArchivage buildInfoArchivage(TypeDocumentEditique typeDocument, long noTiers, String cleArchivage, RegDate dateTraitement) {
		if (typeDocument.getCodeDocumentArchivage() == null) {
			throw new IllegalArgumentException("Archivage non-supporté pour le document de type " + typeDocument);
		}

		final InfoArchivage infoArchivage = InfoArchivageDocument.Factory.newInstance().addNewInfoArchivage();
		infoArchivage.setPrefixe(buildPrefixeInfoArchivage(typeDocument));
		infoArchivage.setNomApplication(APPLICATION_ARCHIVAGE);
		infoArchivage.setTypDossier(getTypeDossierArchivage());
		infoArchivage.setNomDossier(FormatNumeroHelper.numeroCTBToDisplay(noTiers));
		infoArchivage.setTypDocument(typeDocument.getCodeDocumentArchivage());
		infoArchivage.setIdDocument(cleArchivage);
		infoArchivage.setDatTravail(String.valueOf(dateTraitement.index()));
		return infoArchivage;
	}

	/**
	 * @return le code du type de dossier à donner au service d'archivage
	 */
	@Override
	public String getTypeDossierArchivage() {
		return TYPE_DOSSIER_ARCHIVAGE;
	}

	@Override
	public void remplitAffranchissement(InfoDocument infoDocument, AdresseEnvoiDetaillee adresseEnvoiDetaillee) throws EditiqueException {

		final Affranchissement affranchissement = infoDocument.addNewAffranchissement();

		//SIFISC-6270
		if (adresseEnvoiDetaillee.isIncomplete()) {
			affranchissement.setZone(ZONE_AFFRANCHISSEMENT_NA);
		}
		else {

			final TypeAffranchissement typeAffranchissementAdresse = adresseEnvoiDetaillee.getTypeAffranchissement();

			if (typeAffranchissementAdresse != null) {
				switch (typeAffranchissementAdresse) {
				case SUISSE:
					affranchissement.setZone(ZONE_AFFRANCHISSEMENT_SUISSE);
					break;
				case EUROPE:
					affranchissement.setZone(ZONE_AFFRANCHISSEMENT_EUROPE);
					break;
				case MONDE:
					affranchissement.setZone(ZONE_AFFRANCHISSEMENT_RESTE_MONDE);
					break;

				default:
					affranchissement.setZone(null);
				}
			}
			else {
				affranchissement.setZone(null);
			}
		}

	}


	@Override
	public void remplitAffranchissement(InfoDocument infoDocument, Tiers tiers) throws EditiqueException {
		AdresseEnvoiDetaillee adresse;
		try {
			adresse = adresseService.getAdresseEnvoi(tiers, null, TypeAdresseFiscale.COURRIER, false);
		}
		catch (AdresseException e) {
			throw new EditiqueException("Impossible de récuperer l'adresse d'envoi pour le tiers = " + tiers.getNumero() + " erreur: " + e.getMessage());
		}
		remplitAffranchissement(infoDocument, adresse);
	}

	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}

	public void setInfraService(ServiceInfrastructureService infraService) {
		this.infraService = infraService;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}
}
