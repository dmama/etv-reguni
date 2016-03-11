package ch.vd.uniregctb.editique;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.editique.unireg.CTypeAdresse;
import ch.vd.editique.unireg.CTypeAffranchissement;
import ch.vd.editique.unireg.CTypeDestinataire;
import ch.vd.editique.unireg.CTypeExpediteur;
import ch.vd.editique.unireg.CTypeInfoArchivage;
import ch.vd.editique.unireg.CTypeInfoDocument;
import ch.vd.editique.unireg.CTypeInfoEnteteDocument;
import ch.vd.editique.unireg.CTypePorteAdresse;
import ch.vd.editique.unireg.FichierImpression;
import ch.vd.editique.unireg.STypeZoneAffranchissement;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrative;
import ch.vd.uniregctb.adresse.AdresseEnvoi;
import ch.vd.uniregctb.adresse.AdresseEnvoiDetaillee;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.AdresseGenerique;
import ch.vd.uniregctb.adresse.AdresseMandataire;
import ch.vd.uniregctb.adresse.AdresseMandataireAdapter;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.adresse.TypeAdresseFiscale;
import ch.vd.uniregctb.common.CollectionsUtils;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.common.NumeroIDEHelper;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Mandat;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.TypeMandat;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

public abstract class EditiqueAbstractHelperImpl implements EditiqueAbstractHelper {

	public static final Logger LOGGER = LoggerFactory.getLogger(EditiqueAbstractHelperImpl.class);

	public static final String IMPOT_BENEFICE_CAPITAL = "IMPÔT SUR LE BÉNÉFICE ET LE CAPITAL";
	public static final String CODE_PORTE_ADRESSE_MANDATAIRE = "M";
	public static final String VERSION_XSD = "16.3";

	protected AdresseService adresseService;
	protected TiersService tiersService;
	protected ServiceInfrastructureService infraService;

	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setInfraService(ServiceInfrastructureService infraService) {
		this.infraService = infraService;
	}

	/**
	 * @param adresseEnvoi l'adresse à laquelle on veut envoyer un document
	 * @param idEnvoiSiEtranger <code>true</code> si une adresse à l'étranger doit être en fait traitée par un idEnvoi
	 * @param valeurIdEnvoi valeur à utiliser
	 * @return un type d'affranchissement et, éventuellement, un idEnvoi à utiliser pour
	 */
	@NotNull
	protected static Pair<STypeZoneAffranchissement, String> getInformationsAffranchissement(AdresseEnvoiDetaillee adresseEnvoi, boolean idEnvoiSiEtranger, int valeurIdEnvoi) {

		final STypeZoneAffranchissement zoneAffranchissement;
		final String idEnvoi;
		if (adresseEnvoi.isIncomplete()) {
			idEnvoi = String.valueOf(valeurIdEnvoi);
			zoneAffranchissement = STypeZoneAffranchissement.NA;
		}
		else if (adresseEnvoi.getTypeAffranchissement() == null) {
			idEnvoi = String.valueOf(valeurIdEnvoi);
			zoneAffranchissement = STypeZoneAffranchissement.NA;
		}
		else {
			switch (adresseEnvoi.getTypeAffranchissement()) {
			case SUISSE:
				idEnvoi = null;
				zoneAffranchissement = STypeZoneAffranchissement.CH;
				break;
			case EUROPE:
				idEnvoi = idEnvoiSiEtranger ? String.valueOf(valeurIdEnvoi) : null;
				zoneAffranchissement = idEnvoi != null ? STypeZoneAffranchissement.NA : STypeZoneAffranchissement.EU;
				break;
			case MONDE:
				idEnvoi = idEnvoiSiEtranger ? String.valueOf(valeurIdEnvoi) : null;
				zoneAffranchissement = idEnvoi != null ? STypeZoneAffranchissement.NA : STypeZoneAffranchissement.RM;
				break;
			default:
				throw new IllegalArgumentException("Type d'affranchissement non supporté : " + adresseEnvoi.getTypeAffranchissement());
			}
		}

		return Pair.of(zoneAffranchissement, idEnvoi);
	}

	protected AdresseEnvoiDetaillee getAdresseEnvoi(Tiers tiers) throws AdresseException {
		return adresseService.getAdresseEnvoi(tiers, null, TypeAdresseFiscale.COURRIER, false);
	}

	protected CTypeAdresse buildAdresse(CollectiviteAdministrative coll) throws AdresseException {
		final Tiers colAdm = tiersService.getCollectiviteAdministrative(coll.getNoColAdm());
		return buildAdresse(colAdm);
	}

	protected CTypeAdresse buildAdresse(Tiers tiers) throws AdresseException {
		final AdresseEnvoiDetaillee adresse = adresseService.getAdresseEnvoi(tiers, null, TypeAdresseFiscale.COURRIER, false);
		return buildAdresse(adresse);
	}

	@Nullable
	protected CTypeAdresse buildAdresse(AdresseEnvoi adresseEnvoi) {
		final List<String> lignes = new ArrayList<>(6);
		for (String ligne : adresseEnvoi.getLignes()) {
			if (StringUtils.isNotBlank(ligne)) {
				lignes.add(ligne);
			}
		}
		return lignes.isEmpty() ? null : new CTypeAdresse(lignes);
	}

	protected static CTypeInfoArchivage buildInfoArchivage(TypeDocumentEditique typeDocument, String cleArchivage, long noTiers, RegDate dateTraitement) {
		if (typeDocument.getCodeDocumentArchivage() == null) {
			throw new IllegalArgumentException("Archivage non-supporté pour le document de type " + typeDocument);
		}
		final CTypeInfoArchivage info = new CTypeInfoArchivage();
		info.setDatTravail(String.valueOf(dateTraitement.index()));
		info.setIdDocument(cleArchivage);
		info.setNomApplication(ConstantesEditique.APPLICATION_ARCHIVAGE);
		info.setNomDossier(FormatNumeroHelper.numeroCTBToDisplay(noTiers));
		info.setTypDocument(typeDocument.getCodeDocumentArchivage());
		info.setTypDossier(ConstantesEditique.TYPE_DOSSIER_ARCHIVAGE);
		return info;
	}

	protected CTypeInfoEnteteDocument buildInfoEnteteDocument(Tiers destinataire, RegDate dateExpedition,
	                                                          String traitePar, CollectiviteAdministrative expediteur) throws ServiceInfrastructureException, AdresseException {
		return buildInfoEnteteDocument(destinataire, dateExpedition, traitePar, expediteur, IMPOT_BENEFICE_CAPITAL);
	}

	protected CTypeInfoEnteteDocument buildInfoEnteteDocument(Tiers destinataire, RegDate dateExpedition,
	                                                          String traitePar, CollectiviteAdministrative expediteur,
	                                                          String libelleTitre) throws ServiceInfrastructureException, AdresseException {
		final CTypeInfoEnteteDocument entete = new CTypeInfoEnteteDocument();
		entete.setDestinataire(buildDestinataire(destinataire));
		entete.setExpediteur(buildExpediteur(expediteur, dateExpedition, traitePar));
		entete.setLigReference(null);
		entete.setPorteAdresse(null);
		entete.setLibelleTitre(libelleTitre);
		return entete;
	}

	private CTypeDestinataire buildDestinataire(Tiers tiers) throws AdresseException {
		final CTypeDestinataire destinataire = new CTypeDestinataire();
		destinataire.setAdresse(buildAdresse(tiers));
		destinataire.setNumContribuable(FormatNumeroHelper.numeroCTBToDisplay(tiers.getNumero()));
		if (tiers instanceof Entreprise) {
			final String ideBrut = tiersService.getNumeroIDE((Entreprise) tiers);
			if (NumeroIDEHelper.isValid(ideBrut)) {
				destinataire.getNumIDE().add(FormatNumeroHelper.formatNumIDE(ideBrut));
			}
		}
		return destinataire;
	}

	private CTypeExpediteur buildExpediteur(CollectiviteAdministrative ca, RegDate dateExpedition, String traitePar) throws ServiceInfrastructureException, AdresseException {
		final CTypeExpediteur expediteur = new CTypeExpediteur();
		final CTypeAdresse adresse = buildAdresse(ca);
		expediteur.setAdresse(adresse);
		expediteur.setAdrMes(ca.getAdresseEmail());
		expediteur.setDateExpedition(RegDateHelper.toIndexString(dateExpedition));
		expediteur.setLocaliteExpedition(ca.getAdresse().getLocalite());
		expediteur.setNumCCP(ca.getNoCCP());
		expediteur.setNumFax(ca.getNoFax());
		expediteur.setNumIBAN(null);
		expediteur.setNumTelephone(ca.getNoTelephone());
		expediteur.setTraitePar(traitePar);
		return expediteur;
	}

	@Override
	@Nullable
	public FichierImpression.Document buildCopieMandataire(FichierImpression.Document original, Contribuable destinataire, RegDate dateReference) throws EditiqueException {
		// y a-t-il un mandataire général à la date de référence ?
		final AdresseEnvoiDetaillee adresse;

		try {
			// 1. sous forme de lien ?
			final Tiers mandataireGeneral = findMandataireGeneral(destinataire, dateReference);
			if (mandataireGeneral != null) {
				adresse = adresseService.getAdresseEnvoi(mandataireGeneral, dateReference, TypeAdresseFiscale.REPRESENTATION, false);
			}
			else {
				// 2. ou sous forme d'adresse mandataire explicite ?
				final AdresseMandataire adresseMandataire = findAdresseMandataireGeneral(destinataire, dateReference);
				if (adresseMandataire != null) {
					final AdresseGenerique adresseGenerique = new AdresseMandataireAdapter(adresseMandataire, infraService);
					adresse = adresseService.buildAdresseEnvoi(adresseGenerique.getSource().getTiers(), adresseGenerique, dateReference);
				}
				else {
					// en fait, non, rien...
					adresse = null;
				}
			}
		}
		catch (AdresseException e) {
			throw new EditiqueException(e);
		}

		// pas d'adresse de mandataire trouvée -> pas de copie mandataire envoyée...
		if (adresse == null) {
			return null;
		}

		// données d'affranchissement pour l'adresse mandataire
		final Pair<STypeZoneAffranchissement, String> affranchissement = getInformationsAffranchissement(adresse, false, ServiceInfrastructureService.noOIPM);
		final CTypeAdresse adresseEditique = buildAdresse(adresse);

		final CTypeInfoEnteteDocument originalInfoEnteteDocument = original.getInfoEnteteDocument();
		final CTypeInfoDocument originalInfoDocument = original.getInfoDocument();

		// calcul des lignes de "copie à"
		final List<String> texteCopieAChezMandadaire = buildCopieA(originalInfoEnteteDocument.getDestinataire().getAdresse());
		final List<String> texteCopieAChezDestinataire = buildCopieA(adresseEditique);

		// il faut donc générer un double du document original pour le mandataire...
		final CTypeInfoDocument copieInfoDocument = new CTypeInfoDocument(originalInfoDocument.getVersionXSD(),
		                                                                  originalInfoDocument.getPrefixe(),
		                                                                  CODE_PORTE_ADRESSE_MANDATAIRE,
		                                                                  texteCopieAChezMandadaire,
		                                                                  affranchissement.getRight(),
		                                                                  originalInfoDocument.getTypDoc(),
		                                                                  originalInfoDocument.getCodDoc(),
		                                                                  originalInfoDocument.getCleRgp(),
		                                                                  originalInfoDocument.getPopulations(),
		                                                                  new CTypeAffranchissement(affranchissement.getLeft(), null),
		                                                                  originalInfoDocument.isBrouillon());

		// le "copie à" doit apparaître aussi dans le document original
		originalInfoDocument.getTxtCopieMandataire().addAll(texteCopieAChezDestinataire);

		final CTypeInfoEnteteDocument copieInfoEnteteDocument = new CTypeInfoEnteteDocument(originalInfoEnteteDocument.getExpediteur(),
		                                                                                    originalInfoEnteteDocument.getDestinataire(),
		                                                                                    new CTypePorteAdresse(adresseEditique),
		                                                                                    originalInfoEnteteDocument.getLigReference(),
		                                                                                    originalInfoEnteteDocument.getLibelleTitre());

		// on renvoie une copie légèrement modifiée de l'original
		return new FichierImpression.Document(copieInfoDocument,
		                                      original.getInfoArchivage(),
		                                      copieInfoEnteteDocument,
		                                      original.getDeclarationImpot(),
		                                      original.getDeclarationImpotAPM(),
		                                      original.getRefusDelai(),
		                                      original.getAccordDelai(),
		                                      original.getSommation(),
		                                      original.getAccordDelaiApresSommation(),
		                                      original.getLettreBienvenue(),
		                                      original.getLettreRappel(),
		                                      original.getInfoRoutage());
	}

	@Nullable
	private Tiers findMandataireGeneral(Tiers tiers, RegDate dateReference) {
		for (RapportEntreTiers ret : tiers.getRapportsSujet()) {
			if (ret.getType() == TypeRapportEntreTiers.MANDAT && ((Mandat) ret).getTypeMandat() == TypeMandat.GENERAL && ret.isValidAt(dateReference)) {
				return tiersService.getTiers(ret.getObjetId());
			}
		}
		return null;
	}

	@Nullable
	private AdresseMandataire findAdresseMandataireGeneral(Contribuable contribuable, RegDate dateReference) {
		for (AdresseMandataire adresse : contribuable.getAdressesMandataires()) {
			if (adresse.getTypeMandat() == TypeMandat.GENERAL && adresse.isValidAt(dateReference)) {
				return adresse;
			}
		}
		return null;
	}

	@NotNull
	private static List<String> buildCopieA(CTypeAdresse adresse) {
		final List<String> lignesBruttes = adresse.getAdresseLigne();
		final List<String> lignesCopiesA = new ArrayList<>(2);
		if (lignesBruttes.size() > 0) {
			final String ligne1 = StringUtils.trimToNull(CollectionsUtils.concat(lignesBruttes.subList(0, Math.min(3, lignesBruttes.size())), ", "));
			final String ligne2;
			if (lignesBruttes.size() > 3) {
				ligne2 = StringUtils.trimToNull(CollectionsUtils.concat(lignesBruttes.subList(3, lignesBruttes.size()), ", "));
			}
			else {
				ligne2 = null;
			}

			if (ligne1 != null && ligne2 != null) {
				lignesCopiesA.add(String.format("%s,", ligne1));
				lignesCopiesA.add(ligne2);
			}
			else if (ligne1 != null) {
				lignesCopiesA.add(ligne1);
			}
			else if (ligne2 != null) {
				lignesCopiesA.add(ligne2);
			}
		}
		return lignesCopiesA;
	}
}
