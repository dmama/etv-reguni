package ch.vd.uniregctb.tache;

import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import ch.vd.uniregctb.adresse.AdresseException;
import noNamespace.FicheOuvertureDossierDocument;
import noNamespace.FichierImpressionDocument;
import noNamespace.InfoDocumentDocument1;
import noNamespace.InfoEnteteDocumentDocument1;
import noNamespace.TypAdresse;
import noNamespace.TypFichierImpression;
import noNamespace.CleRgpDocument.CleRgp;
import noNamespace.FicheOuvertureDossierDocument.FicheOuvertureDossier;
import noNamespace.FicheOuvertureDossierDocument.FicheOuvertureDossier.Contrib1;
import noNamespace.FicheOuvertureDossierDocument.FicheOuvertureDossier.Contrib2;
import noNamespace.FicheOuvertureDossierDocument.FicheOuvertureDossier.Dossier;
import noNamespace.InfoDocumentDocument1.InfoDocument;
import noNamespace.InfoEnteteDocumentDocument1.InfoEnteteDocument;
import noNamespace.InfoEnteteDocumentDocument1.InfoEnteteDocument.Destinataire;
import noNamespace.InfoEnteteDocumentDocument1.InfoEnteteDocument.Expediteur;
import noNamespace.TypFichierImpression.Document;

import org.apache.commons.lang.StringUtils;

import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.adresse.AdressesResolutionException;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.EditiqueHelper;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.situationfamille.SituationFamilleService;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ForFiscalRevenuFortune;
import ch.vd.uniregctb.tiers.ForGestion;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.EtatCivil;
import ch.vd.uniregctb.type.MotifFor;

public class ImpressionNouveauxDossiersHelperImpl implements ImpressionNouveauxDossiersHelper {

	private static final String PREFIXE_FIC_OUV_DOS = "RGPF0801";
	private static final String TYPE_DOC_NOUVEAU_DOSSIER = "FO";
	private static final String CODE_DOC_NOUVEAU_DOSSIER = "FIC_OUV_DOS";
	private static final String VERSION = "1.0";
	private static final String POPULATIONS_PP = "PP";
	private static final String LOGO_CANT = "CANT";
	private static final String DOCUM = "DOCUM";
	private static final String HAUT1 = "HAUT1";

	private TiersService tiersService;
	private SituationFamilleService situationFamilleService;
	private ServiceInfrastructureService serviceInfrastructureService;
	private EditiqueHelper editiqueHelper;

	public TiersService getTiersService() {
		return tiersService;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public SituationFamilleService getSituationFamilleService() {
		return situationFamilleService;
	}

	public void setSituationFamilleService(SituationFamilleService situationFamilleService) {
		this.situationFamilleService = situationFamilleService;
	}

	public ServiceInfrastructureService getServiceInfrastructureService() {
		return serviceInfrastructureService;
	}

	public void setServiceInfrastructureService(ServiceInfrastructureService serviceInfrastructureService) {
		this.serviceInfrastructureService = serviceInfrastructureService;
	}



	public EditiqueHelper getEditiqueHelper() {
		return editiqueHelper;
	}

	public void setEditiqueHelper(EditiqueHelper editiqueHelper) {
		this.editiqueHelper = editiqueHelper;
	}

	/**
	 * Remplit la partie spécifique de la fiche d'ouverture de dossier
	 *
	 * @param contribuable
	 * @throws InfrastructureException
	 */
	private FicheOuvertureDossier remplitSpecifiqueNouveauDossier(Contribuable contribuable) throws EditiqueException {

		FicheOuvertureDossier ficheOuvertureDossier = FicheOuvertureDossierDocument.Factory.newInstance().addNewFicheOuvertureDossier();
		Dossier dossier =ficheOuvertureDossier.addNewDossier();

		ForGestion forGestion = tiersService.getDernierForGestionConnu(contribuable, null);
		if (forGestion == null) {
			String message = "Le contribuable " + contribuable.getNumero() + " n'a pas de for de gestion. Il s'agit peut-être d'un contribuable annulé.";
			throw new EditiqueException(message);
		}

		ForFiscalRevenuFortune forFiscalGestion = forGestion.getSousjacent();
		if (forFiscalGestion == null) {
			String message = "Le contribuable " + contribuable.getNumero() + " n'a pas de for de gestion. Il s'agit peut-être d'un contribuable annulé.";
			throw new EditiqueException(message);
		}

		final MotifFor motifFor = forFiscalGestion.getMotifOuverture();
		if (motifFor != null) {
			dossier.setMotif(motifFor.getDescription(true));
		}

		RegDate dateDebutFor = forFiscalGestion.getDateDebut();
		String displayDateDebutFor = String.valueOf(dateDebutFor.index());
		dossier.setDateEvenement(displayDateDebutFor);
		Integer numeroOfsAutoriteFiscale = forFiscalGestion.getNumeroOfsAutoriteFiscale();
		dossier.setNumOffice(numeroOfsAutoriteFiscale.toString());
		Commune commune;
		try {
			commune = serviceInfrastructureService.getCommuneByNumeroOfsEtendu(numeroOfsAutoriteFiscale, forFiscalGestion.getDateFin());
		}
		catch (InfrastructureException e) {
			commune = null;
		}
		if (commune == null) {
			String message = "La commune correspondant au numéro " + numeroOfsAutoriteFiscale + " n'a pas pu être déterminée";
			throw new EditiqueException(message);
		}

		String communeLabel = commune.getNomMinuscule();
		dossier.setCommune(communeLabel);

		Contrib1 contrib1 = ficheOuvertureDossier.addNewContrib1();
		if (contribuable instanceof MenageCommun) {
			MenageCommun menage = (MenageCommun) contribuable;
			EnsembleTiersCouple ensembleTiersCouple = tiersService.getEnsembleTiersCouple(menage, null);
			PersonnePhysique principal = ensembleTiersCouple.getPrincipal();
			String nom = tiersService.getNom(principal);
			contrib1.setNom1(nom);
			String prenom = tiersService.getPrenom(principal);
			contrib1.setPrenom1(prenom);
			RegDate dateNaissance = tiersService.getDateNaissance(principal);
			String displayDateNaissance = RegDateHelper.dateToDisplayString(dateNaissance);

			contrib1.setDateNaissance1(displayDateNaissance);
			EtatCivil etatCivil = situationFamilleService.getEtatCivil(principal, null);
			if (etatCivil != null) {
				contrib1.setCivil(etatCivil.format());
			}
			String numeroFormate = FormatNumeroHelper.numeroCTBToDisplay(principal.getNumero());
			contrib1.setNumCTB1(numeroFormate);
			String navs13 = tiersService.getNumeroAssureSocial(principal);
			navs13 = FormatNumeroHelper.formatNumAVS(navs13);
			contrib1.setNumAVS131(navs13);

			PersonnePhysique conjoint = ensembleTiersCouple.getConjoint();
			if (conjoint != null) {
				Contrib2 contrib2 = ficheOuvertureDossier.addNewContrib2();
				nom = tiersService.getNom(conjoint);
				contrib2.setNom2(nom);
				prenom = tiersService.getPrenom(conjoint);
				contrib2.setPrenom2(prenom);
				dateNaissance = tiersService.getDateNaissance(conjoint);
				displayDateNaissance = RegDateHelper.dateToDisplayString(dateNaissance);
				contrib2.setDateNaissance2(displayDateNaissance);
				numeroFormate = FormatNumeroHelper.numeroCTBToDisplay(conjoint.getNumero());
				contrib2.setNumCTB2(numeroFormate);
				navs13 = FormatNumeroHelper.formatNumAVS(tiersService.getNumeroAssureSocial(conjoint));
				contrib2.setNumAVS132(navs13);
			}
		}
		else if (contribuable instanceof PersonnePhysique) {
			PersonnePhysique pp = (PersonnePhysique) contribuable;
			String nom = tiersService.getNom(pp);
			contrib1.setNom1(nom);
			String prenom = tiersService.getPrenom(pp);
			contrib1.setPrenom1(prenom);
			RegDate dateNaissance = tiersService.getDateNaissance(pp);
			String displayDateNaissance = RegDateHelper.dateToDisplayString(dateNaissance);
			contrib1.setDateNaissance1(displayDateNaissance);
			EtatCivil etatCivil = situationFamilleService.getEtatCivil(pp, null);
			if (etatCivil != null) {
				contrib1.setCivil(etatCivil.format());
			}
			String numeroFormate = FormatNumeroHelper.numeroCTBToDisplay(pp.getNumero());
			contrib1.setNumCTB1(numeroFormate);
			String navs13 = tiersService.getNumeroAssureSocial(pp);
			navs13 = FormatNumeroHelper.formatNumAVS(navs13);
			contrib1.setNumAVS131(navs13);
		}

		RegDate dateEdition = RegDate.get();
		String dateEditionDisplay = String.valueOf(dateEdition.index());
		ficheOuvertureDossier.setDateEdition(dateEditionDisplay);

		return ficheOuvertureDossier;

	}


	/**
	 * Calcul le prefixe
	 *
	 * @param contribuable
	 * @return
	 */

	public String calculPrefixe() {
		String prefixe = PREFIXE_FIC_OUV_DOS;
		return prefixe;
	}


	/**
	 * Alimente la partie infoDocument du Document
	 *
	 * @return
	 */
	private InfoDocument remplitInfoDocument(Contribuable contribuable) {
		InfoDocument infoDocument = InfoDocumentDocument1.Factory.newInstance().addNewInfoDocument();
		String prefixe = calculPrefixe();
		prefixe += DOCUM;
		infoDocument.setPrefixe(prefixe);
		infoDocument.setTypDoc(TYPE_DOC_NOUVEAU_DOSSIER);
		infoDocument.setCodDoc(CODE_DOC_NOUVEAU_DOSSIER);
		CleRgp cleRgp = infoDocument.addNewCleRgp();
		cleRgp.addNewGenreImpot();
		infoDocument.setVersion(VERSION);
		infoDocument.setLogo(LOGO_CANT);
		infoDocument.setPopulations(POPULATIONS_PP);
		return infoDocument;
	}


	/**
	 * Alimente l'entête du document
	 *
	 * @return
	 * @throws AdressesResolutionException
	 * @throws InfrastructureException
	 * @throws RemoteException
	 * @throws AdressesResolutionException
	 * @throws InfrastructureException
	 */
	private InfoEnteteDocument remplitEnteteDocument(Contribuable contribuable) throws AdresseException, InfrastructureException {
		InfoEnteteDocument infoEnteteDocument = InfoEnteteDocumentDocument1.Factory.newInstance().addNewInfoEnteteDocument();

		String prefixe = calculPrefixe();
		prefixe += HAUT1;
		infoEnteteDocument.setPrefixe(prefixe);

		TypAdresse porteAdresse = editiqueHelper.remplitPorteAdresse(contribuable, infoEnteteDocument);
		infoEnteteDocument.setPorteAdresse(porteAdresse);

		Expediteur expediteur = editiqueHelper.remplitExpediteurACI(infoEnteteDocument);
		infoEnteteDocument.setExpediteur(expediteur);

		Destinataire destinataire = editiqueHelper.remplitDestinataire(contribuable, infoEnteteDocument);
		infoEnteteDocument.setDestinataire(destinataire);

		return infoEnteteDocument;

	}

	/**
	 * Alimente un objet Document pour l'impression des nouveaux dossiers
	 *
	 * @param declaration
	 * @param annexes
	 * @return
	 * @throws InfrastructureException
	 */
	public TypFichierImpression remplitNouveauDossier(List<Contribuable> contribuables) throws EditiqueException {
		TypFichierImpression typFichierImpression = FichierImpressionDocument.Factory.newInstance().addNewFichierImpression();
		Document[] documents = new Document[contribuables.size()];
		int i = 0;
		for (Contribuable contribuable : contribuables) {
			InfoDocument infoDocument = remplitInfoDocument(contribuable);
			InfoEnteteDocument infoEnteteDocument;
			try {
				infoEnteteDocument = remplitEnteteDocument(contribuable);
			}
			catch (Exception e) {
				throw new EditiqueException(e);
			}
			FicheOuvertureDossier ficheOuvertureDossier = remplitSpecifiqueNouveauDossier(contribuable);
			Document document = typFichierImpression.addNewDocument();

			document.setInfoEnteteDocument(infoEnteteDocument);
			document.setInfoDocument(infoDocument);
			document.setFicheOuvertureDossier(ficheOuvertureDossier);
			documents[i] = document;
			i++;
		}
		typFichierImpression.setDocumentArray(documents);

		return typFichierImpression;
	}


	/**
	 * Construit le champ idDocument
	 *
	 * @param declaration
	 * @return
	 */
	public String construitIdDocument(Contribuable contribuable) {
		return String.format(
				"%s %s %s",
				StringUtils.leftPad(contribuable.getNumero().toString(), 9, '0'),
				new SimpleDateFormat("yyyyMMddHHmmssSSS").format(contribuable.getLogCreationDate()),
				new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date())
		);
	}

}
