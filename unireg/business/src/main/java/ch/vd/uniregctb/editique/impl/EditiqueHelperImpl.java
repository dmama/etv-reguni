package ch.vd.uniregctb.editique.impl;

import java.rmi.RemoteException;

import ch.vd.uniregctb.type.TypeAdresseTiers;
import noNamespace.TypAdresse;
import noNamespace.InfoEnteteDocumentDocument1.InfoEnteteDocument;
import noNamespace.InfoEnteteDocumentDocument1.InfoEnteteDocument.Destinataire;
import noNamespace.InfoEnteteDocumentDocument1.InfoEnteteDocument.Expediteur;

import org.apache.commons.lang.StringUtils;

import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseEnvoi;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.adresse.AdressesResolutionException;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.EditiqueHelper;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.ForGestion;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;
import org.apache.log4j.Logger;

public class EditiqueHelperImpl implements EditiqueHelper {

	public static final Logger LOGGER = Logger.getLogger(EditiqueHelperImpl.class);

	private static final String IMPOT_A_LA_SOURCE_MIN = "Impôt à la source";

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
	public Destinataire remplitDestinataire(Tiers tiers, InfoEnteteDocument infoEnteteDocument) throws AdresseException {
		final Destinataire destinataire = remplitAdresseEnvoiDestinataire(tiers, infoEnteteDocument);
		destinataire.setNumContribuable(FormatNumeroHelper.numeroCTBToDisplay(tiers.getNumero()));
		return destinataire;
	}

	private Destinataire remplitAdresseEnvoiDestinataire(Tiers tiers, InfoEnteteDocument infoEnteteDocument) throws AdresseException {
		final AdresseEnvoi adresseEnvoi = adresseService.getAdresseEnvoi(tiers, null, TypeAdresseTiers.COURRIER, false);
		final Destinataire destinataire = infoEnteteDocument.addNewDestinataire();
		final TypAdresse.Adresse adresseDestinataire = destinataire.addNewAdresse();
		final TypAdresse.Adresse adresseDestinataireImpression = remplitAdresse(adresseEnvoi, adresseDestinataire);
		destinataire.setAdresse(adresseDestinataireImpression);
		return destinataire;
	}

	public Destinataire remplitDestinataire(CollectiviteAdministrative collAdm, InfoEnteteDocument infoEnteteDocument) throws AdresseException {
		return remplitAdresseEnvoiDestinataire(collAdm, infoEnteteDocument);
	}

	public Destinataire remplitDestinataireArchives(InfoEnteteDocument infoEnteteDocument) {
		final Destinataire destinataire = infoEnteteDocument.addNewDestinataire();
		final TypAdresse.Adresse adresseDestinataire = destinataire.addNewAdresse();
		adresseDestinataire.setAdresseCourrierLigne1("Archives");
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
	public TypAdresse remplitPorteAdresse(Tiers tiers, InfoEnteteDocument infoEnteteDocument) throws AdresseException {
		//
		// Porte adresse
		//
		AdresseEnvoi adresseEnvoi = adresseService.getAdresseEnvoi(tiers, null, TypeAdresseTiers.COURRIER, false);
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
	 * @throws InfrastructureException
	 * @throws RemoteException
	 * @throws InfrastructureException
	 * @throws InfrastructureException
	 */
	public Expediteur remplitExpediteurACI(InfoEnteteDocument infoEnteteDocument) throws InfrastructureException {
		ch.vd.uniregctb.interfaces.model.CollectiviteAdministrative aci = infraService.getACI();
		return remplitExpediteur(aci, infoEnteteDocument);
	}

	private Expediteur remplitExpediteur(ch.vd.uniregctb.interfaces.model.CollectiviteAdministrative ca, InfoEnteteDocument infoEnteteDocument) throws InfrastructureException {
		final ch.vd.uniregctb.interfaces.model.Adresse adresse = ca.getAdresse();
		final Commune commune = infraService.getCommuneByAdresse(adresse);

		final Expediteur expediteur = infoEnteteDocument.addNewExpediteur();
		final TypAdresse.Adresse adresseExpediteur = expediteur.addNewAdresse();
		adresseExpediteur.setAdresseCourrierLigne1(ca.getNomComplet1());
		adresseExpediteur.setAdresseCourrierLigne2(ca.getNomComplet2());
		adresseExpediteur.setAdresseCourrierLigne3(ca.getNomComplet3());
		adresseExpediteur.setAdresseCourrierLigne4(adresse.getRue());
		adresseExpediteur.setAdresseCourrierLigne5(adresse.getNumeroPostal() + " " + adresse.getLocalite());
		expediteur.setAdresse(adresseExpediteur);
		expediteur.setAdrMes(ca.getAdresseEmail());
		expediteur.setNumTelephone(ca.getNoTelephone());
		expediteur.setNumFax(ca.getNoFax());
		expediteur.setNumCCP(ca.getNoCCP());
		expediteur.setTraitePar("");
		// Apparement la commune de l'aci n'est pas renseignée dans le host ...
		if (commune == null) {
			expediteur.setLocaliteExpedition("Lausanne");
		} else {
			expediteur.setLocaliteExpedition(StringUtils.capitalize(commune.getNomMinuscule()));
		}
		return expediteur;
	}

	/**
	 * Alimente la partie expéditeur CAT du document
	 *
	 * @param infoEnteteDocument
	 * @return
	 * @throws InfrastructureException
	 * @throws RemoteException
	 * @throws InfrastructureException
	 * @throws InfrastructureException
	 */
	public Expediteur remplitExpediteurCAT(InfoEnteteDocument infoEnteteDocument) throws InfrastructureException {
		final ch.vd.uniregctb.interfaces.model.CollectiviteAdministrative cat = infraService.getCAT();
		return remplitExpediteur(cat, infoEnteteDocument);
	}

	public Expediteur remplitExpediteur(CollectiviteAdministrative collAdm, InfoEnteteDocument infoEnteteDocument) throws InfrastructureException {
		final ch.vd.uniregctb.interfaces.model.CollectiviteAdministrative ca = infraService.getCollectivite(collAdm.getNumeroCollectiviteAdministrative());
		return remplitExpediteur(ca, infoEnteteDocument);
	}

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
			commune = infraService.getCommuneByNumeroOfsEtendu(numeroOfsAutoriteFiscale);
		}
		catch (InfrastructureException e) {
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
	 * Alimente la partie expéditeur du document
	 */
	public Expediteur remplitExpediteurACIForIS(Declaration declaration, InfoEnteteDocument infoEnteteDocument) throws InfrastructureException {
		//
		// Expediteur
		//
		ch.vd.uniregctb.interfaces.model.CollectiviteAdministrative aci = infraService.getACI();
		ch.vd.uniregctb.interfaces.model.Adresse adresseAci = aci.getAdresse();

		Expediteur expediteur = infoEnteteDocument.addNewExpediteur();
		TypAdresse.Adresse adresseExpediteur = expediteur.addNewAdresse();
		adresseExpediteur.setAdresseCourrierLigne1(aci.getNomComplet1());
		adresseExpediteur.setAdresseCourrierLigne2(IMPOT_A_LA_SOURCE_MIN);
		adresseExpediteur.setAdresseCourrierLigne3(aci.getNomComplet3());
		adresseExpediteur.setAdresseCourrierLigne4(adresseAci.getRue());
		adresseExpediteur.setAdresseCourrierLigne5(adresseAci.getNumeroPostal() + " " + adresseAci.getLocalite());
		expediteur.setAdresse(adresseExpediteur);
		expediteur.setAdrMes(aci.getAdresseEmail());
		expediteur.setNumTelephone(aci.getNoTelephone());
		expediteur.setNumFax(aci.getNoFax());
		expediteur.setNumCCP(aci.getNoCCP());
		// expediteur.setTraitePar();
		// expediteur.setSrvExp(srvExp);
		// expediteur.setIdeUti(ideUti);
		expediteur.setLocaliteExpedition("Lausanne");
		expediteur.setDateExpedition(Integer.valueOf(RegDate.get().index()).toString());
		expediteur.setNotreReference(FormatNumeroHelper.numeroCTBToDisplay(declaration.getTiers().getNumero()));
		expediteur.setVotreReference("");
		// expediteur.setNumIBAN(numIBAN);
		return expediteur;
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
