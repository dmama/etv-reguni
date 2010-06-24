package ch.vd.uniregctb.mouvement;

import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.List;

import noNamespace.BordereauEnvoiDocument;
import noNamespace.FichierImpressionDocument;
import noNamespace.InfoDocumentDocument1;
import noNamespace.InfoEnteteDocumentDocument1;
import noNamespace.TypAdresse;
import noNamespace.TypFichierImpression;
import noNamespace.TypPeriode;
import noNamespace.BordereauEnvoiDocument.BordereauEnvoi;
import noNamespace.BordereauEnvoiDocument.BordereauEnvoi.Dossier;
import noNamespace.CleRgpDocument.CleRgp;
import noNamespace.InfoDocumentDocument1.InfoDocument;
import noNamespace.InfoEnteteDocumentDocument1.InfoEnteteDocument;
import noNamespace.InfoEnteteDocumentDocument1.InfoEnteteDocument.Destinataire;
import noNamespace.InfoEnteteDocumentDocument1.InfoEnteteDocument.Expediteur;
import noNamespace.TypFichierImpression.Document;
import noNamespace.TypPeriode.Entete;
import noNamespace.TypPeriode.Entete.ImpCcn;
import noNamespace.TypPeriode.Entete.Tit;

import org.apache.commons.lang.StringUtils;

import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.AdressesResolutionException;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.EditiqueHelper;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.interfaces.model.OfficeImpot;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.metier.assujettissement.Assujettissement;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementException;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersService;

/**
 * Classe permettant de former les classes d'impression pour l'éditique
 *
 * @author xcifde
 *
 */
public class ImpressionBordereauEnvoiHelperImpl implements ImpressionBordereauEnvoiHelper{

	private static final String TYPE_DOC_BORDEREAU_ENVOI = "BE";
	private static final String CODE_DOC_BORDEREAU_ENVOI = "BRD_ENV";
	private static final String DOCUM = "DOCUM";
	private static final String HAUT1 = "HAUT1";
	private static final String PERIO = "PERIO";
	private static final String TITIM = "TITIM";
	private static final String IMPCC = "IMPCC";
	private static final String VERSION = "1.0";
	private static final String POPULATIONS_PP = "PP";
	private static final String LOGO_CANT = "CANT";

	private TiersService tiersService;
	private EditiqueHelper editiqueHelper;
	private ServiceInfrastructureService serviceInfrastructureService;

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setEditiqueHelper(EditiqueHelper editiqueHelper) {
		this.editiqueHelper = editiqueHelper;
	}

	public void setServiceInfrastructureService(ServiceInfrastructureService serviceInfrastructureService) {
		this.serviceInfrastructureService = serviceInfrastructureService;
	}

	public String calculPrefixe() {
		return "RGPB0801";
	}

	public String construitIdDocument(MouvementDossier mouvementDossier) {
		return String.format(
				"%s %s %s",
				mouvementDossier.getId(),
				StringUtils.leftPad(mouvementDossier.getContribuable().getNumero().toString(), 9, '0'),
				new SimpleDateFormat("yyyyMMddHHmmssSSS").format(
						mouvementDossier.getLogCreationDate()
				)
		);
	}

	public TypFichierImpression remplitBordereauEnvoi(MouvementDossier mouvementDossier, Long anneeFiscale) throws EditiqueException {
		try {
			TypFichierImpression typeFichierImpression = FichierImpressionDocument.Factory.newInstance().addNewFichierImpression();
			InfoDocument infoDocument = remplitInfoDocument(mouvementDossier);
			InfoEnteteDocument infoEnteteDocument;
			infoEnteteDocument = remplitEnteteDocument(mouvementDossier.getContribuable());
			Document document = typeFichierImpression.addNewDocument();
			document.setBordereauEnvoi(remplitSpecifiqueBordereauEnvoi(mouvementDossier, anneeFiscale));
			document.setInfoEnteteDocument(infoEnteteDocument);
			document.setInfoDocument(infoDocument);
			typeFichierImpression.setDocumentArray(new Document[]{ document });
			return typeFichierImpression;
		}
		catch (Exception e) {
			throw new EditiqueException(e);
		}
	}


	private BordereauEnvoi remplitSpecifiqueBordereauEnvoi(MouvementDossier mouvementDossier, Long anneeFiscale) throws InfrastructureException {

		Contribuable contribuable = mouvementDossier.getContribuable();
		BordereauEnvoi bordereauEnvoi = BordereauEnvoiDocument.Factory.newInstance().addNewBordereauEnvoi();
		Dossier dossier =  bordereauEnvoi.addNewDossier();
		PersonnePhysique pp = null;
		if (contribuable instanceof PersonnePhysique) {
			pp = (PersonnePhysique) contribuable;
		}
		else if (contribuable instanceof MenageCommun) {
			MenageCommun menage = (MenageCommun) contribuable;
			EnsembleTiersCouple ensembleTiersCouple = tiersService.getEnsembleTiersCouple(menage, null);
			pp = ensembleTiersCouple.getPrincipal();
		}
		String nom = tiersService.getNom(pp);
		String prenom = tiersService.getPrenom(pp);
		String nomPrenom = nom;
		if (prenom != null) {
			nomPrenom = nomPrenom + " " + prenom;
		}
		dossier.setPrenom1(prenom);
		dossier.setNom1(nom);
		// ?? dossier.setNum("1");
		dossier.setNumCTB(FormatNumeroHelper.numeroCTBToDisplay(contribuable.getNumero()));
		RegDate dateFinExerciceDepart = RegDate.get(anneeFiscale.intValue() - 2, 12, 31);
		Integer numeroOfficeImpotDepart = tiersService.getOfficeImpotIdAt(contribuable, dateFinExerciceDepart);
		OfficeImpot officeImpotDepart = serviceInfrastructureService.getOfficeImpot(numeroOfficeImpotDepart);
		ForFiscalPrincipal forFiscalPrincipal = contribuable.getForFiscalPrincipalAt(dateFinExerciceDepart);

		try {
			final List<Assujettissement> assujettissements = Assujettissement.determine(contribuable, anneeFiscale.intValue());
		}
		catch (AssujettissementException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


//		dossier.setCommuneDe(officeImpotDepart.getNomCourt());
		RegDate dateFinExerciceArrivee = RegDate.get(anneeFiscale.intValue() -1, 12, 31);
		Integer numeroOfficeImpotArrivee = tiersService.getOfficeImpotIdAt(contribuable, dateFinExerciceArrivee);
		OfficeImpot officeImpotArrivee = serviceInfrastructureService.getOfficeImpot(numeroOfficeImpotArrivee);
//		dossier.setCommuneA(officeImpotArrivee.getNomCourt());
//		dossier.setDateEnv(forFiscalPrincipal.getDateDebut().toString());
		TypPeriode typPeriode = bordereauEnvoi.addNewPeriode();
		typPeriode.setPrefixe(calculPrefixe() + PERIO);
		typPeriode.setAnneeFiscale(anneeFiscale.toString());
		typPeriode.setDateDecompte("");
		Entete entete = typPeriode.addNewEntete();
		ImpCcn impCcn = entete.addNewImpCcn();
		impCcn.setPrefixe(calculPrefixe() + IMPCC);
		impCcn.setLibImpCcn("");
		Tit tit = entete.addNewTit();
		tit.setPrefixe(calculPrefixe() + TITIM);
		tit.setLibTit("");
		typPeriode.setHorsCanton("");
		typPeriode.setHorsSuisse("");
		return bordereauEnvoi;

	}


	/**
	 * Alimente la partie infoDocument du Document
	 *
	 * @return
	 */
	private InfoDocument remplitInfoDocument(MouvementDossier mouvementDossier) throws EditiqueException{
		InfoDocument infoDocument = InfoDocumentDocument1.Factory.newInstance().addNewInfoDocument();
		String prefixe = calculPrefixe();
		prefixe += DOCUM;
		infoDocument.setPrefixe(prefixe);
		infoDocument.setTypDoc(TYPE_DOC_BORDEREAU_ENVOI);
		infoDocument.setCodDoc(CODE_DOC_BORDEREAU_ENVOI);
		CleRgp cleRgp = infoDocument.addNewCleRgp();
		cleRgp.addNewGenreImpot();
		infoDocument.setVersion(VERSION);
		infoDocument.setLogo(LOGO_CANT);
		infoDocument.setPopulations(POPULATIONS_PP);

		Integer officeImpotId = tiersService.getAndSetOfficeImpot(mouvementDossier.getContribuable());
		if (officeImpotId != null) {
			String idEnvoi = officeImpotId.toString();
			infoDocument.setIdEnvoi(idEnvoi);
		}

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
	private InfoEnteteDocument remplitEnteteDocument(Contribuable contribuable) throws EditiqueException, AdresseException, InfrastructureException {
		InfoEnteteDocument infoEnteteDocument = InfoEnteteDocumentDocument1.Factory.newInstance().addNewInfoEnteteDocument();

		String prefixe = calculPrefixe();
		prefixe += HAUT1;
		infoEnteteDocument.setPrefixe(prefixe);

		Integer officeImpotId = tiersService.getAndSetOfficeImpot(contribuable);
		if (officeImpotId != null) {
			ch.vd.uniregctb.tiers.CollectiviteAdministrative  collectiviteAsCore = tiersService.getOrCreateCollectiviteAdministrative(officeImpotId.intValue());
			if (collectiviteAsCore != null) {
				TypAdresse porteAdresse = editiqueHelper.remplitPorteAdresse(collectiviteAsCore, infoEnteteDocument);
				infoEnteteDocument.setPorteAdresse(porteAdresse);
			}
		}
		else {
			String message = "Le contribuable " + contribuable.getNumero() + " n'a pas de for de gestion. Il s'agit peut-être d'un contribuable annulé.";
			throw new EditiqueException(message);
		}

		Expediteur expediteur = editiqueHelper.remplitExpediteurACI(infoEnteteDocument);
		infoEnteteDocument.setExpediteur(expediteur);

		Destinataire destinataire = editiqueHelper.remplitDestinataire(contribuable, infoEnteteDocument);
		infoEnteteDocument.setDestinataire(destinataire);

		return infoEnteteDocument;

	}

}
