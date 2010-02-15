package ch.vd.uniregctb.mouvement;

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

import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.EditiqueHelper;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.ForGestion;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersService;

/**
 * Impression éditique d'un bordereau de mouvements de dossier
 */
public class ImpressionBordereauMouvementDossierHelperImpl implements ImpressionBordereauMouvementDossierHelper {

	private static final String PREFIXE = "RGPB0801";

	private static final String TYPE_DOC_BORDEREAU_ENVOI = "BE";
	private static final String CODE_DOC_BORDEREAU_ENVOI = "BRD_ENV";
	private static final String DOCUM = "DOCUM";
	private static final String HAUT1 = "HAUT1";
	private static final String VERSION = "1.0";
	private static final String POPULATIONS_PP = "PP";
	private static final String LOGO_CANT = "CANT";
	private static final String TITRE = "Bordereau d'envoi des dossiers";

	private EditiqueHelper editiqueHelper;

	private TiersService tiersService;
	private ServiceInfrastructureService infraService;

	public void setEditiqueHelper(EditiqueHelper editiqueHelper) {
		this.editiqueHelper = editiqueHelper;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setInfraService(ServiceInfrastructureService infraService) {
		this.infraService = infraService;
	}

	public String calculePrefixe() {
		return PREFIXE;
	}

	public TypFichierImpression remplitBordereau(ImpressionBordereauMouvementDossierHelperParams params) throws EditiqueException {

		try {
			final TypFichierImpression typeFichierImpression = FichierImpressionDocument.Factory.newInstance().addNewFichierImpression();
			final InfoDocumentDocument1.InfoDocument infoDocument = remplitInfoDocument();
			final InfoEnteteDocumentDocument1.InfoEnteteDocument infoEnteteDocument = remplitEnteteDocument(params);

			final TypFichierImpression.Document document = typeFichierImpression.addNewDocument();
			document.setBordereauEnvoi(remplitSpecifiqueBordereauEnvoi(params.getBordereau()));
			document.setInfoEnteteDocument(infoEnteteDocument);
			document.setInfoDocument(infoDocument);
			typeFichierImpression.setDocumentArray(new TypFichierImpression.Document[]{ document });

			return typeFichierImpression;
		}
		catch (RuntimeException e) {
			throw e;
		}
		catch (Exception e) {
			throw new EditiqueException(e);
		}
	}

	private BordereauEnvoiDocument.BordereauEnvoi remplitSpecifiqueBordereauEnvoi(BordereauMouvementDossier bordereau) throws InfrastructureException {

		final BordereauEnvoiDocument.BordereauEnvoi bordereauEnvoi = BordereauEnvoiDocument.Factory.newInstance().addNewBordereauEnvoi();

		// d'abord les dossiers
		final Set<MouvementDossier> contenu = bordereau.getContenu();
		final List<BordereauEnvoiDocument.BordereauEnvoi.Dossier> dossiers = new ArrayList<BordereauEnvoiDocument.BordereauEnvoi.Dossier>(contenu.size());
		for (MouvementDossier mvt : contenu) {
			final BordereauEnvoiDocument.BordereauEnvoi.Dossier dossier =  bordereauEnvoi.addNewDossier();

			final Contribuable ctb = mvt.getContribuable();
			dossier.setNumCTB(FormatNumeroHelper.numeroCTBToDisplay(ctb.getNumero()));
			if (ctb instanceof PersonnePhysique) {
				final PersonnePhysique pp = (PersonnePhysique) ctb;
				dossier.setNom1(tiersService.getNom(pp));
				dossier.setPrenom1(tiersService.getPrenom(pp));
			}
			else if (ctb instanceof MenageCommun) {
				final EnsembleTiersCouple ensemble = tiersService.getEnsembleTiersCouple((MenageCommun) ctb, null);
				final PersonnePhysique principal = ensemble.getPrincipal();
				if (principal != null) {
					dossier.setNom1(tiersService.getNom(principal));
					dossier.setPrenom1(tiersService.getPrenom(principal));
				}

				final PersonnePhysique conjoint = ensemble.getConjoint(principal);
				if (conjoint != null) {
					dossier.setNom2(tiersService.getNom(conjoint));
					dossier.setPrenom2(tiersService.getPrenom(conjoint));
				}
			}
			else {
				throw new RuntimeException("Type de tiers non supporté dans un bordereau de mouvements de dossier : " + ctb.getClass().getName());
			}

			final ForGestion forGestion = tiersService.getDernierForGestionConnu(ctb, null);
			dossier.setNumOFS(Integer.toString(forGestion.getNoOfsCommune()));

			final Commune commune = infraService.getCommuneByNumeroOfsEtendu(forGestion.getNoOfsCommune());
			if (commune != null) {
				dossier.setOFS(commune.getNomMinuscule());
			}

			dossiers.add(dossier);
		}
		bordereauEnvoi.setDossierArray(dossiers.toArray(new BordereauEnvoiDocument.BordereauEnvoi.Dossier[dossiers.size()]));

		// signature
		final CollectiviteAdministrative caSignataire = bordereau.getExpediteur();
		final ch.vd.uniregctb.interfaces.model.CollectiviteAdministrative ca = infraService.getCollectivite(caSignataire.getNumeroCollectiviteAdministrative());
		final BordereauEnvoiDocument.BordereauEnvoi.Signature signature = bordereauEnvoi.addNewSignature();
		signature.setOfficeImpot(ca.getNomCourt());
		bordereauEnvoi.setSignature(signature);

		// période fiscale
		final TypPeriode pf = bordereauEnvoi.addNewPeriode();
		pf.setAnneeFiscale(Integer.toString(RegDate.get().year()));

		final TypPeriode.Entete entete = pf.addNewEntete();
		final TypPeriode.Entete.Tit titre = entete.addNewTit();
		titre.setLibTit(TITRE);
		entete.setTitArray(new TypPeriode.Entete.Tit[] {titre});
		pf.setEntete(entete);
		bordereauEnvoi.setPeriode(pf);

		return bordereauEnvoi;
	}

	public String construitIdDocument(BordereauMouvementDossier bordereau) {
		return String.format("%s %s", bordereau.getId(), new SimpleDateFormat("yyyyMMddHHmmssSSS").format(bordereau.getLogCreationDate()));
	}

	/**
	 * Alimente la partie infoDocument du Document
	 */
	private InfoDocumentDocument1.InfoDocument remplitInfoDocument() {
		final InfoDocumentDocument1.InfoDocument infoDocument = InfoDocumentDocument1.Factory.newInstance().addNewInfoDocument();
		final String prefixe = String.format("%s%s", calculePrefixe(), DOCUM);
		infoDocument.setPrefixe(prefixe);
		infoDocument.setTypDoc(TYPE_DOC_BORDEREAU_ENVOI);
		infoDocument.setCodDoc(CODE_DOC_BORDEREAU_ENVOI);
		infoDocument.setVersion(VERSION);
		infoDocument.setLogo(LOGO_CANT);
		infoDocument.setPopulations(POPULATIONS_PP);
		return infoDocument;
	}

	/**
	 * Remplit la partie entête du document
	 */
	private InfoEnteteDocumentDocument1.InfoEnteteDocument remplitEnteteDocument(ImpressionBordereauMouvementDossierHelperParams params) throws InfrastructureException, AdresseException {
		InfoEnteteDocumentDocument1.InfoEnteteDocument infoEnteteDocument = InfoEnteteDocumentDocument1.Factory.newInstance().addNewInfoEnteteDocument();

		final String prefixe = String.format("%s%s", calculePrefixe(), HAUT1);
		infoEnteteDocument.setPrefixe(prefixe);

		final BordereauMouvementDossier bordereau = params.getBordereau();
		final CollectiviteAdministrative expediteurBordereau = bordereau.getExpediteur();
		final InfoEnteteDocumentDocument1.InfoEnteteDocument.Expediteur expediteur = editiqueHelper.remplitExpediteur(expediteurBordereau, infoEnteteDocument);
		expediteur.setTraitePar(params.getNomOperateur());
		expediteur.setDateExpedition(RegDateHelper.toIndexString(RegDate.get()));
		infoEnteteDocument.setExpediteur(expediteur);

		final CollectiviteAdministrative destinataireBordereau = bordereau.getDestinataire();
		final InfoEnteteDocumentDocument1.InfoEnteteDocument.Destinataire destinataire;
		if (destinataireBordereau != null) {
			destinataire = editiqueHelper.remplitDestinataire(destinataireBordereau, infoEnteteDocument);
		}
		else {
			// bordereau vers les archives
			destinataire = editiqueHelper.remplitDestinataireArchives(infoEnteteDocument);
		}
		infoEnteteDocument.setDestinataire(destinataire);

		return infoEnteteDocument;
	}
}
