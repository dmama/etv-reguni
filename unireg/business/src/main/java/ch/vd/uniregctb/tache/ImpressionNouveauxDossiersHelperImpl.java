package ch.vd.uniregctb.tache;

import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.List;

import noNamespace.FicheOuvertureDossierDocument;
import noNamespace.FicheOuvertureDossierDocument.FicheOuvertureDossier;
import noNamespace.FicheOuvertureDossierDocument.FicheOuvertureDossier.Contrib1;
import noNamespace.FicheOuvertureDossierDocument.FicheOuvertureDossier.Contrib2;
import noNamespace.FicheOuvertureDossierDocument.FicheOuvertureDossier.Dossier;
import noNamespace.FichierImpressionDocument;
import noNamespace.InfoDocumentDocument1;
import noNamespace.InfoDocumentDocument1.InfoDocument;
import noNamespace.InfoEnteteDocumentDocument1;
import noNamespace.InfoEnteteDocumentDocument1.InfoEnteteDocument;
import noNamespace.InfoEnteteDocumentDocument1.InfoEnteteDocument.Destinataire;
import noNamespace.InfoEnteteDocumentDocument1.InfoEnteteDocument.Expediteur;
import noNamespace.TypAdresse;
import noNamespace.TypFichierImpression;
import noNamespace.TypFichierImpression.Document;
import org.apache.commons.lang.StringUtils;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.AdressesResolutionException;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.common.NomPrenom;
import ch.vd.uniregctb.editique.EditiqueAbstractHelper;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.EditiqueHelper;
import ch.vd.uniregctb.editique.TypeDocumentEditique;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureException;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.situationfamille.SituationFamilleService;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.ForFiscalRevenuFortune;
import ch.vd.uniregctb.tiers.ForGestion;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.EtatCivil;
import ch.vd.uniregctb.type.MotifFor;

public class ImpressionNouveauxDossiersHelperImpl extends EditiqueAbstractHelper implements ImpressionNouveauxDossiersHelper {

	private static final String TYPE_DOC_NOUVEAU_DOSSIER = "FO";
	private static final String CODE_DOC_NOUVEAU_DOSSIER = "FIC_OUV_DOS";
	private static final String VERSION = "1.0";

	private TiersService tiersService;
	private SituationFamilleService situationFamilleService;
	private ServiceInfrastructureService serviceInfrastructureService;
	private EditiqueHelper editiqueHelper;

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setSituationFamilleService(SituationFamilleService situationFamilleService) {
		this.situationFamilleService = situationFamilleService;
	}

	public void setServiceInfrastructureService(ServiceInfrastructureService serviceInfrastructureService) {
		this.serviceInfrastructureService = serviceInfrastructureService;
	}

	public void setEditiqueHelper(EditiqueHelper editiqueHelper) {
		this.editiqueHelper = editiqueHelper;
	}

	/**
	 * Remplit la partie spécifique de la fiche d'ouverture de dossier
	 *
	 * @param contribuable
	 * @throws ServiceInfrastructureException
	 */
	private FicheOuvertureDossier remplitSpecifiqueNouveauDossier(Contribuable contribuable) throws EditiqueException {

		final FicheOuvertureDossier ficheOuvertureDossier = FicheOuvertureDossierDocument.Factory.newInstance().addNewFicheOuvertureDossier();
		final Dossier dossier =ficheOuvertureDossier.addNewDossier();

		final ForGestion forGestion = tiersService.getDernierForGestionConnu(contribuable, null);
		if (forGestion == null) {
			final String message = String.format("Le contribuable %d n'a pas de for de gestion. Il s'agit peut-être d'un contribuable annulé.", contribuable.getNumero());
			throw new EditiqueException(message);
		}

		final ForFiscalRevenuFortune forFiscalGestion = forGestion.getSousjacent();
		if (forFiscalGestion == null) {
			final String message = String.format("Le contribuable %d n'a pas de for de gestion. Il s'agit peut-être d'un contribuable annulé.", contribuable.getNumero());
			throw new EditiqueException(message);
		}

		final MotifFor motifFor = forFiscalGestion.getMotifOuverture();
		if (motifFor != null) {
			dossier.setMotif(motifFor.getDescription(true));
		}

		final RegDate dateDebutFor = forFiscalGestion.getDateDebut();
		final String displayDateDebutFor = String.valueOf(dateDebutFor.index());
		dossier.setDateEvenement(displayDateDebutFor);
		final Integer numeroOfsAutoriteFiscale = forFiscalGestion.getNumeroOfsAutoriteFiscale();
		dossier.setNumOffice(numeroOfsAutoriteFiscale.toString());
		Commune commune;
		try {
			commune = serviceInfrastructureService.getCommuneByNumeroOfsEtendu(numeroOfsAutoriteFiscale, forFiscalGestion.getDateFin());
		}
		catch (ServiceInfrastructureException e) {
			commune = null;
		}
		if (commune == null) {
			final String message = String.format("La commune correspondant au numéro %d n'a pas pu être déterminée", numeroOfsAutoriteFiscale);
			throw new EditiqueException(message);
		}

		final String communeLabel = commune.getNomMinuscule();
		dossier.setCommune(communeLabel);

		final Contrib1 contrib1 = ficheOuvertureDossier.addNewContrib1();
		if (contribuable instanceof MenageCommun) {
			final MenageCommun menage = (MenageCommun) contribuable;
			final EnsembleTiersCouple ensembleTiersCouple = tiersService.getEnsembleTiersCouple(menage, null);
			final PersonnePhysique principal = ensembleTiersCouple.getPrincipal();
			final NomPrenom nomPrenomPrincipal = tiersService.getDecompositionNomPrenom(principal);
			contrib1.setNom1(nomPrenomPrincipal.getNom());
			contrib1.setPrenom1(nomPrenomPrincipal.getPrenom());
			final RegDate dateNaissance = tiersService.getDateNaissance(principal);
			contrib1.setDateNaissance1(dateNaissance != null ? RegDateHelper.dateToDisplayString(dateNaissance) : null);

			final EtatCivil etatCivil = situationFamilleService.getEtatCivil(principal, null, true);
			contrib1.setCivil(etatCivil != null ? etatCivil.format() : null);
			contrib1.setNumCTB1(FormatNumeroHelper.numeroCTBToDisplay(principal.getNumero()));
			final String noAvs = tiersService.getNumeroAssureSocial(principal);
			contrib1.setNumAVS131(noAvs != null ? FormatNumeroHelper.formatNumAVS(noAvs) : null);

			final PersonnePhysique conjoint = ensembleTiersCouple.getConjoint();
			if (conjoint != null) {
				final Contrib2 contrib2 = ficheOuvertureDossier.addNewContrib2();
				final NomPrenom nomPrenomConjoint = tiersService.getDecompositionNomPrenom(conjoint);
				contrib2.setNom2(nomPrenomConjoint.getNom());
				contrib2.setPrenom2(nomPrenomConjoint.getPrenom());
				final RegDate dateNaissance2 = tiersService.getDateNaissance(conjoint);
				contrib2.setDateNaissance2(dateNaissance2 != null ? RegDateHelper.dateToDisplayString(dateNaissance2) : null);
				contrib2.setNumCTB2(FormatNumeroHelper.numeroCTBToDisplay(conjoint.getNumero()));
				final String noAvs2 = tiersService.getNumeroAssureSocial(conjoint);
				contrib2.setNumAVS132(noAvs2 != null ? FormatNumeroHelper.formatNumAVS(noAvs2) : null);
			}
		}
		else if (contribuable instanceof PersonnePhysique) {
			final PersonnePhysique pp = (PersonnePhysique) contribuable;
			final NomPrenom nomPrenom = tiersService.getDecompositionNomPrenom(pp);
			contrib1.setNom1(nomPrenom.getNom());
			contrib1.setPrenom1(nomPrenom.getPrenom());
			final RegDate dateNaissance = tiersService.getDateNaissance(pp);
			contrib1.setDateNaissance1(dateNaissance != null ? RegDateHelper.dateToDisplayString(dateNaissance) : null);
			final EtatCivil etatCivil = situationFamilleService.getEtatCivil(pp, null, true);
			contrib1.setCivil(etatCivil != null ? etatCivil.format() : null);
			contrib1.setNumCTB1(FormatNumeroHelper.numeroCTBToDisplay(pp.getNumero()));
			final String noAvs = tiersService.getNumeroAssureSocial(pp);
			contrib1.setNumAVS131(noAvs != null ? FormatNumeroHelper.formatNumAVS(noAvs) : null);
		}

		final RegDate dateEdition = RegDate.get();
		final String dateEditionDisplay = String.valueOf(dateEdition.index());
		ficheOuvertureDossier.setDateEdition(dateEditionDisplay);

		return ficheOuvertureDossier;
	}

	/**
	 * Calcul le prefixe
	 *
	 * @param contribuable
	 * @return
	 */
	@Override
	public TypeDocumentEditique getTypeDocumentEditique() {
		return TypeDocumentEditique.FICHE_OUVERTURE_DOSSIER;
	}

	/**
	 * Alimente la partie infoDocument du Document
	 *
	 * @return
	 */
	private InfoDocument remplitInfoDocument() {
		final InfoDocument infoDocument = InfoDocumentDocument1.Factory.newInstance().addNewInfoDocument();
		final String prefixe = buildPrefixeInfoDocument(getTypeDocumentEditique());
		infoDocument.setPrefixe(prefixe);
		infoDocument.setTypDoc(TYPE_DOC_NOUVEAU_DOSSIER);
		infoDocument.setCodDoc(CODE_DOC_NOUVEAU_DOSSIER);
		infoDocument.setVersion(VERSION);
		infoDocument.setLogo(LOGO_CANTON);
		infoDocument.setPopulations(POPULATION_PP);
		return infoDocument;
	}

	/**
	 * Alimente l'entête du document
	 *
	 * @return
	 * @throws AdressesResolutionException
	 * @throws ServiceInfrastructureException
	 * @throws RemoteException
	 * @throws AdressesResolutionException
	 * @throws ServiceInfrastructureException
	 */
	private InfoEnteteDocument remplitEnteteDocument(Contribuable contribuable) throws AdresseException, ServiceInfrastructureException {
		final InfoEnteteDocument infoEnteteDocument = InfoEnteteDocumentDocument1.Factory.newInstance().addNewInfoEnteteDocument();
		infoEnteteDocument.setPrefixe(buildPrefixeEnteteDocument(getTypeDocumentEditique()));

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
	 * @return
	 * @throws ServiceInfrastructureException
	 */
	@Override
	public FichierImpressionDocument remplitNouveauDossier(List<Contribuable> contribuables) throws EditiqueException {
		final FichierImpressionDocument mainDocument = FichierImpressionDocument.Factory.newInstance();
		TypFichierImpression typFichierImpression = mainDocument.addNewFichierImpression();
		Document[] documents = new Document[contribuables.size()];
		int i = 0;
		for (Contribuable contribuable : contribuables) {
			InfoDocument infoDocument = remplitInfoDocument();
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

		return mainDocument;
	}

	/**
	 * Construit le champ idDocument
	 *
	 * @param declaration
	 * @return
	 */
	@Override
	public String construitIdDocument(Contribuable contribuable) {
		return String.format(
				"%s %s %s",
				StringUtils.leftPad(contribuable.getNumero().toString(), 9, '0'),
				new SimpleDateFormat("yyyyMMddHHmmssSSS").format(contribuable.getLogCreationDate()),
				new SimpleDateFormat("yyyyMMddHHmmssSSS").format(DateHelper.getCurrentDate())
		);
	}
}
