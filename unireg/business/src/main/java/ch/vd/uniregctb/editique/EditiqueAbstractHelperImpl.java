package ch.vd.uniregctb.editique;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.stream.Stream;

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
import ch.vd.unireg.interfaces.common.Adresse;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrative;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.data.Pays;
import ch.vd.uniregctb.adresse.AdresseEnvoi;
import ch.vd.uniregctb.adresse.AdresseEnvoiDetaillee;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.AdresseGenerique;
import ch.vd.uniregctb.adresse.AdresseMandataire;
import ch.vd.uniregctb.adresse.AdresseMandataireAdapter;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.adresse.LigneAdresse;
import ch.vd.uniregctb.adresse.TypeAdresseFiscale;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.common.NumeroIDEHelper;
import ch.vd.uniregctb.declaration.DeclarationAvecNumeroSequence;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ContribuableImpositionPersonnesMorales;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.LocalisationDatee;
import ch.vd.uniregctb.tiers.Mandat;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeMandat;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

public abstract class EditiqueAbstractHelperImpl implements EditiqueAbstractHelper {

	public static final Logger LOGGER = LoggerFactory.getLogger(EditiqueAbstractHelperImpl.class);

	public static final String IMPOT_BENEFICE_CAPITAL = "IMPÔT SUR LE BÉNÉFICE ET LE CAPITAL";
	public static final String IMPOT_COMPLEMENTAIRE_IMMEUBLES = "IMPÔT COMPLÉMENTAIRE SUR LES IMMEUBLES";
	public static final String CODE_PORTE_ADRESSE_MANDATAIRE = "M";

	public static final String VERSION_XSD = "16.9";

	public static final String TYPE_DOCUMENT_CO = "CO";     // pour "courrier", apparemment
	public static final String TYPE_DOCUMENT_DI = "DI";

	public static final String TRAITE_PAR = "CAT";
	public static final String NOM_SERVICE_EXPEDITEUR = "Centre d'appels téléphoniques";

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

	/**
	 * @param contribuable un contribuable entreprise
	 * @return si le dernier for fiscal principal de l'entreprise est fermé pour motif FAILLITE
	 */
	private static boolean isEnFaillite(Contribuable contribuable) {
		final ForFiscalPrincipal dernierForPrincipal = contribuable.getDernierForFiscalPrincipal();
		return dernierForPrincipal != null && dernierForPrincipal.getDateFin() != null && dernierForPrincipal.getMotifFermeture() == MotifFor.FAILLITE;
	}

	/**
	 * Construit un code-à-barres compatible avec les nouveaux documents éditiques
	 * @param declaration une déclaration
	 * @param modele le modèle de document choisi
	 * @param noCollectivite le numéro de la collectivité administrative qui doit faire partie du code-à-barres
	 * @return le code à barres
	 */
	protected static String buildCodeBarre(DeclarationAvecNumeroSequence declaration, ModeleFeuilleDocumentEditique modele, int noCollectivite) {
		return String.format("%04d%05d%04d%09d%02d%02d",
		                     modele.getNoCADEV(),
		                     modele.getNoFormulaireACI() != null ? modele.getNoFormulaireACI() : 0,
		                     declaration.getPeriode().getAnnee(),
		                     declaration.getTiers().getNumero(),
		                     declaration.getNumero() % 100,
		                     noCollectivite);
	}

	/**
	 * Assigne la valeur du champ idEnvoi dans la structure {@link CTypeInfoDocument} passée en paramètre selon les critères suivants :
	 * <ul>
	 *     <li>si l'IdEnvoi dans les informations d'affranchissement est renseigné, c'est lui</li>
	 *     <li>sinon, si le contribuable a son dernier for principal fermé pour motif "FAILLITE", alors on y met la valeur de l'OIPM (21)</li>
	 * </ul>
	 * @param infoDocument structure qui va recevoir la valeur de l'IdEnvoi
	 * @param contribuable contribuable concerné par le document envoyé
	 * @param infoAffranchissement information calculée préalablement par un appel à {@link #getInformationsAffranchissement(AdresseEnvoiDetaillee, boolean, int)}
	 */
	protected static void assigneIdEnvoi(CTypeInfoDocument infoDocument, ContribuableImpositionPersonnesMorales contribuable, Pair<STypeZoneAffranchissement, String> infoAffranchissement) {
		final String idEnvoi;
		if (StringUtils.isNotBlank(infoAffranchissement.getRight())) {
			idEnvoi = infoAffranchissement.getRight();
		}
		else if (isEnFaillite(contribuable)) {
			idEnvoi = String.valueOf(ServiceInfrastructureService.noOIPM);
		}
		else {
			idEnvoi = null;
		}
		infoDocument.setIdEnvoi(idEnvoi);
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
			lignes.add(StringUtils.trimToEmpty(ligne));
		}

		// suppression des dernières lignes si elles sont vides
		final ListIterator<String> iterator = lignes.listIterator(lignes.size());
		while (iterator.hasPrevious()) {
			if (StringUtils.isBlank(iterator.previous())) {
				iterator.remove();
			}
			else {
				break;
			}
		}

		return lignes.isEmpty() ? null : new CTypeAdresse(lignes);
	}

	/**
	 * @param noColAdministrative numéro à placer dans "CEDI XX" (c'est le XX) sur la troisième ligne
	 * @return l'adresse du CEDI à utiliser comme adresse de retour
	 */
	protected final CTypeAdresse buildAdresseCEDI(int noColAdministrative) {
		final CollectiviteAdministrative cedi = infraService.getCEDI();
		final List<String> lignes = new ArrayList<>(4);
		final Adresse adresse = cedi.getAdresse();
		lignes.add(cedi.getNomComplet1());
		lignes.add(cedi.getNomComplet2());
		lignes.add(cedi.getNomCourt() + ' ' + noColAdministrative);
		lignes.add(adresse.getNumeroPostal() + ' ' + adresse.getLocalite());
		return new CTypeAdresse(lignes);
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
	                                                          String traitePar, String nomServiceExpediteur,
	                                                          CollectiviteAdministrative expediteurPourAdresse, CollectiviteAdministrative expediteurPourTelFaxMail) throws ServiceInfrastructureException, AdresseException {
		return buildInfoEnteteDocument(destinataire, dateExpedition, traitePar, nomServiceExpediteur, expediteurPourAdresse, expediteurPourTelFaxMail, IMPOT_BENEFICE_CAPITAL);
	}

	protected CTypeInfoEnteteDocument buildInfoEnteteDocument(Tiers destinataire, RegDate dateExpedition,
	                                                          String traitePar, String nomServiceExpediteur,
	                                                          CollectiviteAdministrative expediteurPourAdresse, CollectiviteAdministrative expediteurPourTelFaxMail,
	                                                          String libelleTitre) throws ServiceInfrastructureException, AdresseException {
		final CTypeInfoEnteteDocument entete = new CTypeInfoEnteteDocument();
		entete.setDestinataire(buildDestinataire(destinataire));
		entete.setExpediteur(buildExpediteur(expediteurPourAdresse, expediteurPourTelFaxMail, dateExpedition, traitePar, nomServiceExpediteur));
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

	private CTypeExpediteur buildExpediteur(CollectiviteAdministrative expediteurPourAdresse, CollectiviteAdministrative expediteurPourTelFaxMail,
	                                        RegDate dateExpedition, String traitePar, String nomServiceExpediteur) throws ServiceInfrastructureException, AdresseException {
		final CTypeExpediteur expediteur = new CTypeExpediteur();

		final Adresse adresse = expediteurPourAdresse.getAdresse();
		final AdresseEnvoi adresseEnvoi = new AdresseEnvoi();
		adresseEnvoi.addLine(expediteurPourAdresse.getNomComplet1());
		adresseEnvoi.addLine(expediteurPourAdresse.getNomComplet2());
		adresseEnvoi.addLine(expediteurPourAdresse.getNomComplet3());
		adresseEnvoi.addLine(adresse.getRue());
		adresseEnvoi.addLine(adresse.getNumeroPostal() + ' ' + adresse.getLocalite());

		expediteur.setAdresse(buildAdresse(adresseEnvoi));
		expediteur.setAdrMes(expediteurPourTelFaxMail.getAdresseEmail());
		expediteur.setDateExpedition(RegDateHelper.toIndexString(dateExpedition));
		expediteur.setLocaliteExpedition(expediteurPourAdresse.getAdresse().getLocalite());
		expediteur.setNumCCP(expediteurPourAdresse.getNoCCP());
		expediteur.setNumFax(expediteurPourTelFaxMail.getNoFax());
		expediteur.setNumIBAN(null);
		expediteur.setNumTelephone(expediteurPourTelFaxMail.getNoTelephone());
		expediteur.setTraitePar(traitePar);
		expediteur.setSrvExp(nomServiceExpediteur);
		return expediteur;
	}

	@Override
	@Nullable
	public FichierImpression.Document buildCopieMandataire(FichierImpression.Document original, Contribuable destinataire, RegDate dateReference) throws EditiqueException {

		// [SIFISC-18705] si le contribuable est en faillite, on ne fait jamais de copie mandataire
		if (isEnFaillite(destinataire)) {
			return null;
		}

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
		final List<String> texteCopieA = buildCopieA(adresse);

		// il faut donc générer un double du document original pour le mandataire...
		final CTypeInfoDocument copieInfoDocument = new CTypeInfoDocument(originalInfoDocument.getVersionXSD(),
		                                                                  originalInfoDocument.getPrefixe(),
		                                                                  CODE_PORTE_ADRESSE_MANDATAIRE,
		                                                                  texteCopieA,
		                                                                  affranchissement.getRight(),
		                                                                  originalInfoDocument.getTypDoc(),
		                                                                  originalInfoDocument.getCodDoc(),
		                                                                  originalInfoDocument.getCleRgp(),
		                                                                  originalInfoDocument.getPopulations(),
		                                                                  new CTypeAffranchissement(affranchissement.getLeft(), null),
		                                                                  originalInfoDocument.isBrouillon());

		// le "copie à" doit apparaître aussi dans le document original
		originalInfoDocument.getTxtCopieMandataire().addAll(texteCopieA);

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
		                                      original.getQuestSNC(),
		                                      original.getQuestSNCRappel(),
		                                      original.getDemandeBilanFinal(),
		                                      original.getAutorisationRadiation(),
		                                      original.getLettreLiquidation(),
		                                      original.getLettreDegrevementImm(),
		                                      original.getLettreDegrevementImmRappel(),
											  original.getFourreNeutre(),
		                                      original.getInfoRoutage());
	}

	@Nullable
	private Tiers findMandataireGeneral(Tiers tiers, RegDate dateReference) {
		for (RapportEntreTiers ret : tiers.getRapportsSujet()) {
			if (ret.getType() == TypeRapportEntreTiers.MANDAT && ret.isValidAt(dateReference)) {
				final Mandat mandat = (Mandat) ret;
				if (mandat.getTypeMandat() == TypeMandat.GENERAL && mandat.getWithCopy() != null && mandat.getWithCopy()) {
					return tiersService.getTiers(ret.getObjetId());
				}
			}
		}
		return null;
	}

	@Nullable
	private AdresseMandataire findAdresseMandataireGeneral(Contribuable contribuable, RegDate dateReference) {
		for (AdresseMandataire adresse : contribuable.getAdressesMandataires()) {
			if (adresse.getTypeMandat() == TypeMandat.GENERAL && adresse.isValidAt(dateReference) && adresse.isWithCopy()) {
				return adresse;
			}
		}
		return null;
	}

	private static String concat(LigneAdresse[] source, int inclusiveFrom, int exclusiveTo) {
		final StringBuilder b = new StringBuilder();
		for (int i = inclusiveFrom ; i < exclusiveTo ; ++ i) {
			final LigneAdresse ligne = source[i];
			if (ligne != null) {
				if (b.length() > 0) {
					b.append(ligne.isWrapping() ? " " : ", ");
				}
				b.append(StringUtils.trimToEmpty(ligne.getTexte()));
			}
		}
		return StringUtils.trimToNull(b.toString());
	}

	@NotNull
	private static List<String> buildCopieA(AdresseEnvoiDetaillee adresse) {
		final LigneAdresse[] lignesBruttes = adresse.getLignesAdresse();
		final List<String> lignesCopiesA = new ArrayList<>(2);
		if (lignesBruttes.length > 0) {
			final String ligne1 = concat(lignesBruttes, 0, Math.min(3, lignesBruttes.length));
			final String ligne2;
			if (lignesBruttes.length > 3) {
				ligne2 = concat(lignesBruttes, 3, lignesBruttes.length);
			}
			else {
				ligne2 = null;
			}

			Stream.of(ligne1, ligne2)
					.filter(Objects::nonNull)
					.forEach(lignesCopiesA::add);
		}
		return lignesCopiesA;
	}

	@NotNull
	protected final String getNomCommuneOuPays(LocalisationDatee localisationDatee) {
		if (localisationDatee != null && localisationDatee.getTypeAutoriteFiscale() == TypeAutoriteFiscale.PAYS_HS) {
			final Pays pays = infraService.getPays(localisationDatee.getNumeroOfsAutoriteFiscale(), localisationDatee.getDateFin());
			return String.format("Etranger (%s)", pays != null ? pays.getNomCourt() : "?");
		}
		else if (localisationDatee != null && localisationDatee.getTypeAutoriteFiscale() != null) {
			final Commune commune = infraService.getCommuneByNumeroOfs(localisationDatee.getNumeroOfsAutoriteFiscale(), localisationDatee.getDateFin());
			return commune != null ? commune.getNomOfficielAvecCanton() : "Suisse (?)";
		}
		else {
			return "-";
		}
	}

	protected final String getNomRaisonSociale(Tiers tiers) {
		return tiersService.getNomRaisonSociale(tiers);
	}
}
