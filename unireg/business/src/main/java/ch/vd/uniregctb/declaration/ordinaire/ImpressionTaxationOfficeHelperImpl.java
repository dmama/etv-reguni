package ch.vd.uniregctb.declaration.ordinaire;

import java.rmi.RemoteException;
import java.text.SimpleDateFormat;

import noNamespace.ChemiseTODocument;
import noNamespace.ChemiseTODocument.ChemiseTO;
import noNamespace.ChemiseTODocument.ChemiseTO.Contrib1;
import noNamespace.ChemiseTODocument.ChemiseTO.Contrib2;
import noNamespace.ChemiseTODocument.ChemiseTO.Dossier;
import noNamespace.CleRgpDocument.CleRgp;
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
import org.apache.log4j.Logger;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.common.NomPrenom;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.editique.EditiqueAbstractHelper;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.EditiqueHelper;
import ch.vd.uniregctb.editique.TypeDocumentEditique;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.situationfamille.SituationFamilleService;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.ForGestion;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.EtatCivil;
import ch.vd.uniregctb.type.TypeEtatDeclaration;

public class ImpressionTaxationOfficeHelperImpl extends EditiqueAbstractHelper implements ImpressionTaxationOfficeHelper {

	public static final Logger LOGGER = Logger.getLogger(ImpressionTaxationOfficeHelperImpl.class);

	private static final String MOTIF_TO = "Taxation d'office pour défaut de déclaration";
	private static final String TYPE_DOC_CHEMISE_TO = "CT";
	private static final String CODE_DOC_CHEMISE_TO = "CHEM_TO";
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

	@Override
	public TypeDocumentEditique getTypeDocumentEditique() {
		return TypeDocumentEditique.CHEMISE_TO;
	}

	@Override
	public String construitIdDocument(DeclarationImpotOrdinaire declaration) {
		return String.format(
				"%s %s %s %s" ,
				StringUtils.leftPad(declaration.getTiers().getNumero().toString(), 9, '0'),
				declaration.getPeriode().getAnnee().toString(),
				declaration.getNumero().toString(),
				new SimpleDateFormat("yyyyMMddHHmmssSSS").format(declaration.getLogCreationDate())
		);
	}

	@Override
	public FichierImpressionDocument remplitTaxationOffice(DeclarationImpotOrdinaire declaration) throws EditiqueException {

		final Contribuable contribuable = (Contribuable) declaration.getTiers();
		final FichierImpressionDocument mainDocument = FichierImpressionDocument.Factory.newInstance();
		final TypFichierImpression typFichierImpression = mainDocument.addNewFichierImpression();
		final Document[] documents = new Document[1];
		final InfoDocument infoDocument = remplitInfoDocument(declaration, contribuable);
		InfoEnteteDocument infoEnteteDocument;
		try {
			infoEnteteDocument = remplitEnteteDocument(declaration, contribuable);
		}
		catch (Exception e) {
			throw new EditiqueException(e);
		}
		final ChemiseTO chemiseTO = remplitSpecifiqueTaxationOffice(declaration);
		final Document document = typFichierImpression.addNewDocument();

		document.setInfoEnteteDocument(infoEnteteDocument);
		document.setInfoDocument(infoDocument);
		document.setChemiseTO(chemiseTO);
		documents[0] = document;

		typFichierImpression.setDocumentArray(documents);

		return mainDocument;
	}

	/**
	 * Remplit la partie spécifique de la fiche d'ouverture de dossier
	 *
	 * @param contribuable
	 * @throws ServiceInfrastructureException
	 */
	private ChemiseTO remplitSpecifiqueTaxationOffice(DeclarationImpotOrdinaire declaration) throws EditiqueException {
		final Contribuable contribuable  =  (Contribuable) declaration.getTiers();

		final ChemiseTO chemiseTO = ChemiseTODocument.Factory.newInstance().addNewChemiseTO();

		chemiseTO.setNumContrib(FormatNumeroHelper.numeroCTBToDisplay(declaration.getTiers().getNumero()));
		chemiseTO.setDateEdition(String.valueOf(RegDate.get().index()));
		final Dossier dossier = chemiseTO.addNewDossier();
		dossier.setMotif(MOTIF_TO);

		final EtatDeclaration etatSomme = declaration.getEtatDeclarationActif(TypeEtatDeclaration.SOMMEE);
		final RegDate dateSommation = etatSomme.getDateObtention();
		dossier.setDateEvenement(String.valueOf(dateSommation.index()));

		final Integer oid = getOfficeImpotAt(contribuable, declaration.getDateFin());
		dossier.setNumOffice(oid.toString());

		try {
			final ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrative collectivite = serviceInfrastructureService.getCollectivite(oid);
			final String nomComplet;
			if (!StringUtils.isBlank(collectivite.getNomComplet2()) && !StringUtils.isBlank(collectivite.getNomComplet3())) {
				nomComplet = String.format("%s %s", collectivite.getNomComplet2().trim(), collectivite.getNomComplet3().trim());
			}
			else if (!StringUtils.isBlank(collectivite.getNomComplet2())) {
				nomComplet = collectivite.getNomComplet2().trim();
			}
			else {
				nomComplet = collectivite.getNomComplet3() != null ? collectivite.getNomComplet3().trim() : null;
			}
			dossier.setOfficeImpot(nomComplet);
		}
		catch (ServiceInfrastructureException e) {
			throw new EditiqueException(e);
		}

		final ForGestion forGestion = tiersService.getDernierForGestionConnu(contribuable, declaration.getDateFin());
		if (forGestion == null) {
			final String msg = String.format("Impossible de déterminer le for de gestion du contribuable %s au %s",
											FormatNumeroHelper.numeroCTBToDisplay(contribuable.getNumero()),
											RegDateHelper.dateToDisplayString(declaration.getDateFin()));
			throw new EditiqueException(msg);
		}

		final int noOfsCommune = forGestion.getNoOfsCommune();
		try {
			final Commune commune = serviceInfrastructureService.getCommuneByNumeroOfsEtendu(noOfsCommune, forGestion.getDateFin());
			if (commune == null) {
				final String message = String.format("La commune correspondant au numéro %d n'a pas pu être déterminée", noOfsCommune);
				throw new EditiqueException(message);
			}
			final String communeLabel = commune.getNomMinuscule();
			dossier.setCommune(communeLabel);
		}
		catch (ServiceInfrastructureException e) {
			LOGGER.error("Exception lors de la recherche de la commune par numéro " + noOfsCommune, e);
		}

		dossier.setAnneeFiscale(declaration.getPeriode().getAnnee().toString());

		final Contrib1 contrib1 = chemiseTO.addNewContrib1();
		if (contribuable instanceof MenageCommun) {
			final MenageCommun menage = (MenageCommun) contribuable;
			final EnsembleTiersCouple ensembleTiersCouple = tiersService.getEnsembleTiersCouple(menage, null);
			final PersonnePhysique principal = ensembleTiersCouple.getPrincipal();
			if (principal != null) {
				fillCtb1(contrib1, principal);
			}

			final PersonnePhysique conjoint = ensembleTiersCouple.getConjoint();
			if (conjoint != null) {
				final Contrib2 contrib2 = chemiseTO.addNewContrib2();
				final NomPrenom nomPrenom = tiersService.getDecompositionNomPrenom(conjoint);
				contrib2.setNom2(nomPrenom.getNom());
				contrib2.setPrenom2(nomPrenom.getPrenom());
				final RegDate dateNaissance = tiersService.getDateNaissance(conjoint);
				final String displayDateNaissance = dateNaissance != null ? RegDateHelper.dateToDisplayString(dateNaissance) : null;
				contrib2.setDateNaissance2(displayDateNaissance);
				final String numeroFormate = FormatNumeroHelper.numeroCTBToDisplay(conjoint.getNumero());
				contrib2.setNumCTB2(numeroFormate);
				final String navs13 = tiersService.getNumeroAssureSocial(conjoint);
				contrib2.setNumAVS132(navs13 != null ? FormatNumeroHelper.formatNumAVS(navs13) : null);
			}
		}
		else if (contribuable instanceof PersonnePhysique) {
			final PersonnePhysique pp = (PersonnePhysique) contribuable;
			fillCtb1(contrib1, pp);
		}

		return chemiseTO;
	}

	private void fillCtb1(Contrib1 contrib1, PersonnePhysique pp) {
		final NomPrenom nomPrenom = tiersService.getDecompositionNomPrenom(pp);
		contrib1.setNom1(nomPrenom.getNom());
		contrib1.setPrenom1(nomPrenom.getPrenom());
		final RegDate dateNaissance = tiersService.getDateNaissance(pp);
		final String displayDateNaissance = dateNaissance != null ? RegDateHelper.dateToDisplayString(dateNaissance) : null;
		contrib1.setDateNaissance1(displayDateNaissance);
		final EtatCivil etatCivil = situationFamilleService.getEtatCivil(pp, null, true);
		contrib1.setCivil(etatCivil != null ? etatCivil.format() : null);
		final String numeroFormate = FormatNumeroHelper.numeroCTBToDisplay(pp.getNumero());
		contrib1.setNumCTB1(numeroFormate);
		final String navs13 = tiersService.getNumeroAssureSocial(pp);
		contrib1.setNumAVS131(navs13 != null ? FormatNumeroHelper.formatNumAVS(navs13) : null);
	}

	/**
	 * Alimente la partie infoDocument du Document
	 *
	 * @return
	 */
	private InfoDocument remplitInfoDocument(DeclarationImpotOrdinaire declaration, Contribuable contribuable) throws EditiqueException{

		final InfoDocument infoDocument = InfoDocumentDocument1.Factory.newInstance().addNewInfoDocument();
		final String prefixe = buildPrefixeInfoDocument(getTypeDocumentEditique());
		infoDocument.setPrefixe(prefixe);
		infoDocument.setTypDoc(TYPE_DOC_CHEMISE_TO);
		infoDocument.setCodDoc(CODE_DOC_CHEMISE_TO);
		final CleRgp cleRgp = infoDocument.addNewCleRgp();
		cleRgp.setAnneeFiscale(Integer.toString(declaration.getPeriode().getAnnee()));
		infoDocument.setVersion(VERSION);
		infoDocument.setLogo(LOGO_CANTON);
		infoDocument.setPopulations(POPULATION_PP);
		final InfoDocument.Affranchissement affranchissement= editiqueHelper.getAffranchissement(infoDocument,contribuable);
		infoDocument.setAffranchissement(affranchissement);

		final Integer officeImpotId = getOfficeImpotAt(contribuable, declaration.getDateFin());
		if (officeImpotId != null) {
			final String idEnvoi = officeImpotId.toString();
			infoDocument.setIdEnvoi(idEnvoi);
		}

		return infoDocument;
	}

	private Integer getOfficeImpotAt(Contribuable ctb, RegDate date) {
		return tiersService.getOfficeImpotIdAt(ctb, date);
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
	private InfoEnteteDocument remplitEnteteDocument(DeclarationImpotOrdinaire declaration, Contribuable contribuable) throws EditiqueException, AdresseException, ServiceInfrastructureException {

		final InfoEnteteDocument infoEnteteDocument = InfoEnteteDocumentDocument1.Factory.newInstance().addNewInfoEnteteDocument();
		infoEnteteDocument.setPrefixe(buildPrefixeEnteteDocument(getTypeDocumentEditique()));

		final Integer oid = getOfficeImpotAt(contribuable, declaration.getDateFin());
		if (oid != null) {
			final CollectiviteAdministrative colAdm = tiersService.getCollectiviteAdministrative(oid);
			final TypAdresse porteAdresse = editiqueHelper.remplitPorteAdresse(colAdm, infoEnteteDocument);
			infoEnteteDocument.setPorteAdresse(porteAdresse);
		}
		else {
			final String message = "Le contribuable " + contribuable.getNumero() + " n'a pas de for de gestion. Il s'agit peut-être d'un contribuable annulé.";
			throw new EditiqueException(message);
		}

		final Expediteur expediteur = editiqueHelper.remplitExpediteurACI(infoEnteteDocument);
		infoEnteteDocument.setExpediteur(expediteur);

		final Destinataire destinataire = editiqueHelper.remplitDestinataire(contribuable, infoEnteteDocument);
		infoEnteteDocument.setDestinataire(destinataire);

		return infoEnteteDocument;

	}


}
