package ch.vd.unireg.mouvement;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import noNamespace.BordereauEnvoiDocument;
import noNamespace.FichierImpressionDocument;
import noNamespace.InfoDocumentDocument1;
import noNamespace.InfoEnteteDocumentDocument1;
import noNamespace.TypFichierImpression;
import noNamespace.TypPeriode;
import org.apache.commons.lang3.StringUtils;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.common.NomPrenom;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.adresse.AdresseException;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.editique.ConstantesEditique;
import ch.vd.unireg.editique.EditiqueAbstractLegacyHelper;
import ch.vd.unireg.editique.EditiqueException;
import ch.vd.unireg.editique.EditiquePrefixeHelper;
import ch.vd.unireg.editique.TypeDocumentEditique;
import ch.vd.unireg.editique.ZoneAffranchissementEditique;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.tiers.CollectiviteAdministrative;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.EnsembleTiersCouple;
import ch.vd.unireg.tiers.ForGestion;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;

/**
 * Impression éditique d'un bordereau de mouvements de dossier
 */
public class ImpressionBordereauMouvementDossierHelperImpl extends EditiqueAbstractLegacyHelper implements ImpressionBordereauMouvementDossierHelper {

	private static final String TYPE_DOC_BORDEREAU_ENVOI = "BE";
	private static final String CODE_DOC_BORDEREAU_ENVOI = "BRD_ENV";
	private static final String VERSION = "1.0";
	private static final String TITRE = "Bordereau d'envoi des dossiers";

	private ServiceInfrastructureService infraService;

	public void setInfraService(ServiceInfrastructureService infraService) {
		this.infraService = infraService;
	}

	@Override
	public TypeDocumentEditique getTypeDocumentEditique() {
		return TypeDocumentEditique.BORDEREAU_MVT_DOSSIER;
	}

	@Override
	public FichierImpressionDocument remplitBordereau(ImpressionBordereauMouvementDossierHelperParams params) throws EditiqueException {

		try {
			final FichierImpressionDocument mainDocument = FichierImpressionDocument.Factory.newInstance();
			final TypFichierImpression typeFichierImpression = mainDocument.addNewFichierImpression();
			final InfoDocumentDocument1.InfoDocument infoDocument = remplitInfoDocument();
			final InfoEnteteDocumentDocument1.InfoEnteteDocument infoEnteteDocument = remplitEnteteDocument(params);

			final TypFichierImpression.Document document = typeFichierImpression.addNewDocument();
			document.setBordereauEnvoi(remplitSpecifiqueBordereauEnvoi(params.getBordereau()));
			document.setInfoEnteteDocument(infoEnteteDocument);
			document.setInfoDocument(infoDocument);
			typeFichierImpression.setDocumentArray(new TypFichierImpression.Document[]{ document });

			return mainDocument;
		}
		catch (RuntimeException e) {
			throw e;
		}
		catch (Exception e) {
			throw new EditiqueException(e);
		}
	}

	private BordereauEnvoiDocument.BordereauEnvoi remplitSpecifiqueBordereauEnvoi(BordereauMouvementDossier bordereau) throws ServiceInfrastructureException {

		final BordereauEnvoiDocument.BordereauEnvoi bordereauEnvoi = BordereauEnvoiDocument.Factory.newInstance().addNewBordereauEnvoi();

		// d'abord les dossiers
		final Set<MouvementDossier> contenu = bordereau.getContenu();
		final List<BordereauEnvoiDocument.BordereauEnvoi.Dossier> dossiers = new ArrayList<>(contenu.size());
		for (MouvementDossier mvt : contenu) {
			final BordereauEnvoiDocument.BordereauEnvoi.Dossier dossier =  bordereauEnvoi.addNewDossier();

			final Contribuable ctb = mvt.getContribuable();
			dossier.setNumCTB(FormatNumeroHelper.numeroCTBToDisplay(ctb.getNumero()));
			if (ctb instanceof PersonnePhysique) {
				final PersonnePhysique pp = (PersonnePhysique) ctb;
				final NomPrenom nomPrenom = tiersService.getDecompositionNomPrenom(pp, false);
				dossier.setNom1(nomPrenom.getNom());
				dossier.setPrenom1(nomPrenom.getPrenom());
				dossier.setNom2(null);
				dossier.setPrenom2(null);
			}
			else if (ctb instanceof MenageCommun) {
				final EnsembleTiersCouple ensemble = tiersService.getEnsembleTiersCouple((MenageCommun) ctb, null);
				final PersonnePhysique principal = ensemble.getPrincipal();
				if (principal != null) {
					final NomPrenom nomPrenom = tiersService.getDecompositionNomPrenom(principal, false);
					dossier.setNom1(nomPrenom.getNom());
					dossier.setPrenom1(nomPrenom.getPrenom());
				}

				final PersonnePhysique conjoint = ensemble.getConjoint(principal);
				if (conjoint != null) {
					final NomPrenom nomPrenom = tiersService.getDecompositionNomPrenom(conjoint, false);
					dossier.setNom2(nomPrenom.getNom());
					dossier.setPrenom2(nomPrenom.getPrenom());
				}
				else {
					dossier.setNom2(null);
					dossier.setPrenom2(null);
				}
			}
			else {
				throw new RuntimeException("Type de tiers non supporté dans un bordereau de mouvements de dossiers : " + ctb.getClass().getName());
			}

			final ForGestion forGestion = tiersService.getDernierForGestionConnu(ctb, null);
			dossier.setNumOFS(Integer.toString(forGestion.getNoOfsCommune()));

			final Commune commune = infraService.getCommuneByNumeroOfs(forGestion.getNoOfsCommune(), forGestion.getDateFin());
			if (commune != null) {
				dossier.setOFS(commune.getNomOfficiel());
			}

			dossiers.add(dossier);
		}
		bordereauEnvoi.setDossierArray(dossiers.toArray(new BordereauEnvoiDocument.BordereauEnvoi.Dossier[dossiers.size()]));

		// signature
		final CollectiviteAdministrative caSignataire = bordereau.getExpediteur();
		final ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrative ca = infraService.getCollectivite(caSignataire.getNumeroCollectiviteAdministrative());
		final BordereauEnvoiDocument.BordereauEnvoi.Signature signature = bordereauEnvoi.addNewSignature();
		signature.setOfficeImpot(ca.getNomCourt());
		bordereauEnvoi.setSignature(signature);

		final TypeDocumentEditique typeDocumentEditique = getTypeDocumentEditique();

		// période fiscale
		final TypPeriode pf = bordereauEnvoi.addNewPeriode();
		pf.setPrefixe(EditiquePrefixeHelper.buildPrefixePeriode(typeDocumentEditique));
		pf.setOrigDuplicat(ORIGINAL);
		pf.setHorsSuisse("");
		pf.setHorsCanton("");
		pf.setDateDecompte(null);
		pf.setAnneeFiscale(Integer.toString(RegDate.get().year()));

		final TypPeriode.Entete entete = pf.addNewEntete();
		final TypPeriode.Entete.Tit titre = entete.addNewTit();
		titre.setLibTit(TITRE);
		titre.setPrefixe(EditiquePrefixeHelper.buildPrefixeTitreEntete(typeDocumentEditique));
		entete.setTitArray(new TypPeriode.Entete.Tit[] {titre});
		pf.setEntete(entete);
		bordereauEnvoi.setPeriode(pf);

		return bordereauEnvoi;
	}

	@Override
	public String construitIdDocument(BordereauMouvementDossier bordereau) {
		return String.format("%s %s", bordereau.getId(), new SimpleDateFormat("yyyyMMddHHmmssSSS").format(bordereau.getLogCreationDate()));
	}

	/**
	 * Alimente la partie infoDocument du Document
	 */
	private InfoDocumentDocument1.InfoDocument remplitInfoDocument() {
		final InfoDocumentDocument1.InfoDocument infoDocument = InfoDocumentDocument1.Factory.newInstance().addNewInfoDocument();
		final String prefixe = EditiquePrefixeHelper.buildPrefixeInfoDocument(getTypeDocumentEditique());
		infoDocument.setPrefixe(prefixe);
		infoDocument.setTypDoc(TYPE_DOC_BORDEREAU_ENVOI);
		infoDocument.setCodDoc(CODE_DOC_BORDEREAU_ENVOI);
		infoDocument.setVersion(VERSION);
		infoDocument.setLogo(LOGO_CANTON);
		infoDocument.setPopulations(ConstantesEditique.POPULATION_PP);
		// les bordereaux d'envoi de mouvements de dossier en masse ne sont jamais imprimés en batch, toujours en local :
		// la valeur que l'on met dans l'affranchissement n'a aucune importance.
		// Ils sont toujours envoyés aux OID, qui sont effectivement en Suisse
		final InfoDocumentDocument1.InfoDocument.Affranchissement affranchissement = infoDocument.addNewAffranchissement();
		affranchissement.setZone(ZoneAffranchissementEditique.SUISSE.getCode());
		infoDocument.setAffranchissement(affranchissement);
		return infoDocument;
	}

	/**
	 * Remplit la partie entête du document
	 */
	private InfoEnteteDocumentDocument1.InfoEnteteDocument remplitEnteteDocument(ImpressionBordereauMouvementDossierHelperParams params) throws ServiceInfrastructureException, AdresseException {
		final InfoEnteteDocumentDocument1.InfoEnteteDocument infoEnteteDocument = InfoEnteteDocumentDocument1.Factory.newInstance().addNewInfoEnteteDocument();
		infoEnteteDocument.setPrefixe(EditiquePrefixeHelper.buildPrefixeEnteteDocument(getTypeDocumentEditique()));

		final BordereauMouvementDossier bordereau = params.getBordereau();
		final CollectiviteAdministrative expediteurBordereau = bordereau.getExpediteur();
		final InfoEnteteDocumentDocument1.InfoEnteteDocument.Expediteur expediteur = legacyEditiqueHelper.remplitExpediteur(expediteurBordereau, infoEnteteDocument);
		expediteur.setTraitePar(params.getNomOperateur());
		expediteur.setDateExpedition(RegDateHelper.toIndexString(RegDate.get()));
		if (!StringUtils.isBlank(params.getNumeroTelephoneOperateur())) {
			expediteur.setNumTelephone(params.getNumeroTelephoneOperateur());
		}
		if (!StringUtils.isBlank(params.getEmailOperateur())) {
			expediteur.setAdrMes(params.getEmailOperateur());
		}
		infoEnteteDocument.setExpediteur(expediteur);

		final CollectiviteAdministrative destinataireBordereau = bordereau.getDestinataire();
		final InfoEnteteDocumentDocument1.InfoEnteteDocument.Destinataire destinataire;
		if (destinataireBordereau != null) {
			destinataire = legacyEditiqueHelper.remplitDestinataire(destinataireBordereau, infoEnteteDocument);
		}
		else {
			// bordereau vers les archives
			destinataire = legacyEditiqueHelper.remplitDestinataireArchives(infoEnteteDocument);
		}
		infoEnteteDocument.setDestinataire(destinataire);

		return infoEnteteDocument;
	}
}
