package ch.vd.unireg.evenement.party;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.Query;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.shared.batchtemplate.BatchResults;
import ch.vd.shared.batchtemplate.BatchWithResultsCallback;
import ch.vd.shared.batchtemplate.Behavior;
import ch.vd.shared.batchtemplate.ParallelBatchTransactionTemplateWithResults;
import ch.vd.unireg.common.AuthenticationInterface;
import ch.vd.unireg.common.CollectionsUtils;
import ch.vd.unireg.common.CsvHelper;
import ch.vd.unireg.common.TemporaryFile;
import ch.vd.unireg.evenement.RequestHandlerResult;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.jms.EsbBusinessCode;
import ch.vd.unireg.jms.EsbBusinessException;
import ch.vd.unireg.metier.assujettissement.Assujettissement;
import ch.vd.unireg.metier.assujettissement.AssujettissementException;
import ch.vd.unireg.metier.assujettissement.AssujettissementService;
import ch.vd.unireg.metier.bouclement.ExerciceCommercial;
import ch.vd.unireg.security.Role;
import ch.vd.unireg.security.SecurityProviderInterface;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.ForFiscal;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.RegimeFiscal;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.type.GenreImpot;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.xml.DataHelper;
import ch.vd.unireg.xml.ServiceException;
import ch.vd.unireg.xml.event.party.advancepayment.corporation.v1.AdvancePaymentPopulationRequest;
import ch.vd.unireg.xml.event.party.advancepayment.corporation.v1.AdvancePaymentPopulationResponse;
import ch.vd.unireg.xml.event.party.advancepayment.corporation.v1.AttachmentDescriptor;
import ch.vd.unireg.xml.exception.v1.AccessDeniedExceptionInfo;

/**
 * Handler qui publie la population des personnes morales soumises aux acomptes
 */
public class AdvancePaymentCorporationsRequestHandler implements RequestHandlerV2<AdvancePaymentPopulationRequest> {

	private static final int BATCH_SIZE = 50;
	private static final int NB_THREADS = 4;

	private static final String ATTACHEMENT_EXTRAITES = "extracted";
	private static final String ATTACHEMENT_IGNOREES = "ignored";
	private static final String ATTACHEMENT_ERREURS = "errors";

	private static final Logger LOGGER = LoggerFactory.getLogger(AdvancePaymentCorporationsRequestHandler.class);

	private HibernateTemplate hibernateTemplate;
	private PlatformTransactionManager transactionManager;
	private TiersService tiersService;
	private AssujettissementService assujettissementService;
	private SecurityProviderInterface securityProvider;

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setAssujettissementService(AssujettissementService assujettissementService) {
		this.assujettissementService = assujettissementService;
	}

	public void setSecurityProvider(SecurityProviderInterface securityProvider) {
		this.securityProvider = securityProvider;
	}

	@Override
	public RequestHandlerResult<AdvancePaymentPopulationResponse> handle(AdvancePaymentPopulationRequest request) throws ServiceException, EsbBusinessException {

		// Vérification des droits d'accès
		final Pair<String, Integer> login = UserLoginHelper.parse(request.getLogin());
		if (!securityProvider.isGranted(Role.VISU_ALL, login.getLeft(), login.getRight())) {
			throw new ServiceException(
					new AccessDeniedExceptionInfo("L'utilisateur spécifié (" + login.getLeft() + '/' + login.getRight() + ") n'a pas les droits d'accès en lecture complète sur l'application.", null));
		}

		final RegDate dateReference = DataHelper.xmlToCore(request.getReferenceDate());
		if (dateReference == null) {
			throw new EsbBusinessException(EsbBusinessCode.XML_INVALIDE, "Date de référence non-reconnue!", null);
		}

		final ExtractionResult population = buildPopulation(dateReference, NB_THREADS);
		final AdvancePaymentPopulationResponse response = new AdvancePaymentPopulationResponse();
		response.setReferenceDate(request.getReferenceDate());

		final RequestHandlerResult<AdvancePaymentPopulationResponse> result = new RequestHandlerResult<>(response);
		try (TemporaryFile file = buildListeExtraction(population.extraits)) {
			final long size = file.getFullPath().length();
			if (size > Integer.MAX_VALUE) {
				throw new Exception("Taille du fichier des extraits trop grande (" + size + ")... abandonné!");
			}

			final byte[] contenu;
			try (ByteArrayOutputStream baos = new ByteArrayOutputStream((int) size);
			     InputStream is = file.openInputStream()) {

				IOUtils.copy(is, baos);
				contenu = baos.toByteArray();
			}
			result.addAttachment(ATTACHEMENT_EXTRAITES, contenu);
			response.setPopulation(new AttachmentDescriptor(ATTACHEMENT_EXTRAITES, population.extraits.size(), CsvHelper.CHARSET));
		}
		catch (Exception e) {
			LOGGER.error("Problème à la génération de la liste des contribuables extraits", e);
		}
		if (!population.ignores.isEmpty()) {
			try (TemporaryFile file = buildListeIgnores(population.ignores)) {
				final long size = file.getFullPath().length();
				if (size > Integer.MAX_VALUE) {
					throw new Exception("Taille du fichier des ignorés trop grande (" + size + ")... abandonné!");
				}

				final byte[] contenu;
				try (ByteArrayOutputStream baos = new ByteArrayOutputStream((int) size);
				     InputStream is = file.openInputStream()) {

					IOUtils.copy(is, baos);
					contenu = baos.toByteArray();
				}
				result.addAttachment(ATTACHEMENT_IGNOREES, contenu);
				response.setIgnored(new AttachmentDescriptor(ATTACHEMENT_IGNOREES, population.ignores.size(), CsvHelper.CHARSET));
			}
			catch (Exception e) {
				LOGGER.error("Problème à la génération de la liste des contribuables ignorés", e);
			}
		}
		if (!population.erreurs.isEmpty()) {
			try (TemporaryFile file = buildListeErreurs(population.erreurs)) {
				final long size = file.getFullPath().length();
				if (size > Integer.MAX_VALUE) {
					throw new Exception("Taille du fichier des erreurs trop grande (" + size + ")... abandonné!");
				}

				final byte[] contenu;
				try (ByteArrayOutputStream baos = new ByteArrayOutputStream((int) size);
				     InputStream is = file.openInputStream()) {

					IOUtils.copy(is, baos);
					contenu = baos.toByteArray();
				}
				result.addAttachment(ATTACHEMENT_ERREURS, contenu);
				response.setIgnored(new AttachmentDescriptor(ATTACHEMENT_ERREURS, population.erreurs.size(), CsvHelper.CHARSET));
			}
			catch (Exception e) {
				LOGGER.error("Problème à la génération de la liste des contribuables en erreur", e);
			}
		}

		return result;
	}

	private static TemporaryFile buildListeExtraction(List<ExtractionResult.Extrait> extraits) {
		return CsvHelper.asCsvTemporaryFile(extraits, null, null, new CsvHelper.FileFiller<ExtractionResult.Extrait>() {
			@Override
			public void fillHeader(CsvHelper.LineFiller b) {
				b.append("NO_CTB").append(CsvHelper.COMMA);
				b.append("RAISON_SOCIALE").append(CsvHelper.COMMA);
				b.append("DATE_BOUCLEMENT_FUTUR").append(CsvHelper.COMMA);
				b.append("DATE_BOUCLEMENT_PRECEDENT").append(CsvHelper.COMMA);
				b.append("DEBUT_ICC").append(CsvHelper.COMMA);
				b.append("FIN_ICC").append(CsvHelper.COMMA);
				b.append("DEBUT_IFD").append(CsvHelper.COMMA);
				b.append("FIN_IFD").append(CsvHelper.COMMA);
				b.append("RF_VD").append(CsvHelper.COMMA);
				b.append("RF_CH");
			}

			@Override
			public boolean fillLine(CsvHelper.LineFiller b, ExtractionResult.Extrait elt) {
				b.append(elt.noCtb).append(CsvHelper.COMMA);
				b.append(CsvHelper.escapeChars(elt.raisonSociale)).append(CsvHelper.COMMA);
				b.append(RegDateHelper.dateToDisplayString(elt.dateBouclementFutur)).append(CsvHelper.COMMA);
				b.append(RegDateHelper.dateToDisplayString(elt.dateDenierBouclement)).append(CsvHelper.COMMA);
				b.append(RegDateHelper.dateToDisplayString(elt.dateDebutIcc)).append(CsvHelper.COMMA);
				b.append(RegDateHelper.dateToDisplayString(elt.dateFinIcc)).append(CsvHelper.COMMA);
				b.append(RegDateHelper.dateToDisplayString(elt.dateDebutIfd)).append(CsvHelper.COMMA);
				b.append(RegDateHelper.dateToDisplayString(elt.dateFinIfd)).append(CsvHelper.COMMA);
				b.append(elt.codeRegimeFiscalVD).append(CsvHelper.COMMA);
				b.append(elt.codeRegimeFiscalCH);
				return true;
			}
		});
	}

	private static TemporaryFile buildListeIgnores(List<ExtractionResult.Ignore> ignores) {
		return CsvHelper.asCsvTemporaryFile(ignores, null, null, new CsvHelper.FileFiller<ExtractionResult.Ignore>() {
			@Override
			public void fillHeader(CsvHelper.LineFiller b) {
				b.append("NO_CTB").append(CsvHelper.COMMA).append("RAISON");
			}

			@Override
			public boolean fillLine(CsvHelper.LineFiller b, ExtractionResult.Ignore elt) {
				b.append(elt.noCtb).append(CsvHelper.COMMA);
				b.append(CsvHelper.escapeChars(elt.raison.getLibelle()));
				return true;
			}
		});
	}

	private static TemporaryFile buildListeErreurs(List<ExtractionResult.EnErreur> erreurs) {
		return CsvHelper.asCsvTemporaryFile(erreurs, null, null, new CsvHelper.FileFiller<ExtractionResult.EnErreur>() {
			@Override
			public void fillHeader(CsvHelper.LineFiller b) {
				b.append("NO_CTB").append(CsvHelper.COMMA).append("MESSAGE");
			}

			@Override
			public boolean fillLine(CsvHelper.LineFiller b, ExtractionResult.EnErreur elt) {
				b.append(elt.noCtb).append(CsvHelper.COMMA);
				b.append(CsvHelper.escapeChars(elt.message));
				return true;
			}
		});
	}

	/**
	 * @param dateReference date de référence du traitement
	 * @return la liste des données à exposer sur les personnes morales concernées par les acomptes à la date de traitement
	 */
	protected ExtractionResult buildPopulation(@NotNull final RegDate dateReference, int nbThreads) {

		// il faut d'abord aller chercher la liste des contribuables PM avec un for IBC non-annulé
		final List<Long> ids = fetchPmIds();

		// maintenant on va faire comme un petit job sur plusieurs threads histoire de s'en sortir relativement rapidement
		final ExtractionResult rapportFinal = new ExtractionResult();
		final ParallelBatchTransactionTemplateWithResults<Long, ExtractionResult> template = new ParallelBatchTransactionTemplateWithResults<>(ids, BATCH_SIZE, nbThreads, Behavior.REPRISE_AUTOMATIQUE,
		                                                                                                                                       transactionManager, null,
		                                                                                                                                       AuthenticationInterface.INSTANCE);
		template.execute(rapportFinal, new BatchWithResultsCallback<Long, ExtractionResult>() {
			@Override
			public boolean doInTransaction(List<Long> batch, ExtractionResult rapport) throws Exception {
				traiterBatch(batch, dateReference, rapport);
				return true;
			}

			@Override
			public ExtractionResult createSubRapport() {
				return new ExtractionResult();
			}
		}, null);

		// et on renvoie le résultat de l'extraction
		rapportFinal.sort();
		return rapportFinal;
	}


	protected static final class ExtractionResult implements BatchResults<Long, ExtractionResult> {

		public enum RaisonIgnore {
			NON_ASSUJETTI("Contribuable non-assujetti à la date de référence."),
			FAILLITE("Contribuable en faillite.");

			private final String libelle;

			RaisonIgnore(String libelle) {
				this.libelle = libelle;
			}

			public String getLibelle() {
				return libelle;
			}
		}

		public enum RaisonErreur {
			PAS_EXERCICE_COMMERCIAL("Pas d'exercice commercial à la date de référence."),
			PAS_REGIME_FISCAL("Régime fiscal VD et/ou CH inconnu à la date de référence.");

			private final String libelle;

			RaisonErreur(String libelle) {
				this.libelle = libelle;
			}

			public String getLibelle() {
				return libelle;
			}
		}

		public static class Ignore implements Comparable<Ignore> {
			public final long noCtb;
			public final RaisonIgnore raison;

			public Ignore(long noCtb, RaisonIgnore raison) {
				this.noCtb = noCtb;
				this.raison = raison;
			}

			@Override
			public int compareTo(@NotNull Ignore o) {
				return Long.compare(noCtb, o.noCtb);
			}
		}

		public static class EnErreur implements Comparable<EnErreur> {
			public final long noCtb;
			public final String message;

			public EnErreur(long noCtb, String message) {
				this.noCtb = noCtb;
				this.message = message;
			}

			public EnErreur(long noCtb, RaisonErreur raison) {
				this(noCtb, raison.getLibelle());
			}

			@Override
			public int compareTo(@NotNull EnErreur o) {
				return Long.compare(noCtb, o.noCtb);
			}
		}

		public static class Extrait  implements Comparable<Extrait> {
			public final long noCtb;
			public final String raisonSociale;
			public final RegDate dateBouclementFutur;
			public final RegDate dateDenierBouclement;
			public final RegDate dateDebutIcc;
			public final RegDate dateFinIcc;
			public final RegDate dateDebutIfd;
			public final RegDate dateFinIfd;
			public final String codeRegimeFiscalVD;
			public final String codeRegimeFiscalCH;

			public Extrait(long noCtb, String raisonSociale, RegDate dateBouclementFutur, RegDate dateDenierBouclement,
			               RegDate dateDebutIcc, RegDate dateFinIcc, RegDate dateDebutIfd, RegDate dateFinIfd, String codeRegimeFiscalVD, String codeRegimeFiscalCH) {
				this.noCtb = noCtb;
				this.raisonSociale = raisonSociale;
				this.dateBouclementFutur = dateBouclementFutur;
				this.dateDenierBouclement = dateDenierBouclement;
				this.dateDebutIcc = dateDebutIcc;
				this.dateFinIcc = dateFinIcc;
				this.dateDebutIfd = dateDebutIfd;
				this.dateFinIfd = dateFinIfd;
				this.codeRegimeFiscalVD = codeRegimeFiscalVD;
				this.codeRegimeFiscalCH = codeRegimeFiscalCH;
			}

			@Override
			public int compareTo(@NotNull Extrait o) {
				return Long.compare(noCtb, o.noCtb);
			}
		}

		public final List<Ignore> ignores = new LinkedList<>();
		public final List<EnErreur> erreurs = new LinkedList<>();
		public final List<Extrait> extraits = new LinkedList<>();

		@Override
		public void addErrorException(Long element, Exception e) {
			this.erreurs.add(new EnErreur(element, e.getMessage()));
		}

		public void addError(Long element, RaisonErreur raison) {
			this.erreurs.add(new EnErreur(element, raison));
		}

		public void addIgnore(Long element, RaisonIgnore raison) {
			this.ignores.add(new Ignore(element, raison));
		}

		public void addExtrait(long noCtb, String raisonSociale, RegDate dateBouclementFutur, RegDate dateDenierBouclement,
		                       RegDate dateDebutIcc, RegDate dateFinIcc, RegDate dateDebutIfd, RegDate dateFinIfd, String codeRegimeFiscalVD, String codeRegimeFiscalCH) {
			this.extraits.add(new Extrait(noCtb, raisonSociale, dateBouclementFutur, dateDenierBouclement, dateDebutIcc, dateFinIcc, dateDebutIfd, dateFinIfd, codeRegimeFiscalVD, codeRegimeFiscalCH));
		}

		@Override
		public void addAll(ExtractionResult right) {
			this.ignores.addAll(right.ignores);
			this.erreurs.addAll(right.erreurs);
			this.extraits.addAll(right.extraits);
		}

		public void sort() {
			Collections.sort(this.ignores);
			Collections.sort(this.erreurs);
			Collections.sort(this.extraits);
		}
	}

	private List<Long> fetchPmIds() {
		return hibernateTemplate.executeWithNewSession(session -> {
			final String hql = "select distinct tiers.id " +
					"from ForFiscal as ff " +
					"join ff.tiers tiers " +
					"where ff.annulationDate is null " +
					"and ff.typeAutoriteFiscale=:taf " +
					"and ff.genreImpot=:gi " +
					"and type(tiers)=Entreprise " +
					"order by tiers.id";
			final Query query = session.createQuery(hql);
			query.setParameter("taf", TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			query.setParameter("gi", GenreImpot.BENEFICE_CAPITAL);
			//noinspection unchecked
			return (List<Long>) query.list();
		});
	}

	private void traiterBatch(List<Long> idpms, RegDate dateReference, ExtractionResult results) throws AssujettissementException {
		for (Long id : idpms) {
			final Entreprise entreprise = (Entreprise) tiersService.getTiers(id);
			final List<Assujettissement> assujettissements = assujettissementService.determine(entreprise);
			if (assujettissements == null || DateRangeHelper.rangeAt(assujettissements, dateReference) == null) {
				results.addIgnore(id, ExtractionResult.RaisonIgnore.NON_ASSUJETTI);
				continue;
			}

			// cas spécial de la faillite : si on se place après la fin du for fermé pour motif faillite et qu'on est toujours assujetti
			final ForFiscalPrincipal dernierForPrincipal = entreprise.getDernierForFiscalPrincipal();
			if (dernierForPrincipal.getDateFin() != null
					&& dernierForPrincipal.getMotifFermeture() == MotifFor.FAILLITE
					&& dateReference.isAfter(dernierForPrincipal.getDateFin())) {
				results.addIgnore(id, ExtractionResult.RaisonIgnore.FAILLITE);
				continue;
			}

			// d'abord, on cherche "le dernier assujettissement continu" du tiers
			final List<DateRange> assujettissementsContinus = DateRangeHelper.merge(assujettissements);
			final DateRange dernierAssujettissementContinu = DateRangeHelper.rangeAt(assujettissementsContinus, dateReference);

			// la conuverture des fors non-annulés
			final List<ForFiscal> fors = entreprise.getForsFiscauxNonAnnules(true);

			// les dates ICC
			final DateRange icc = icc(fors, dernierAssujettissementContinu, dateReference);

			// les dates IFD
			final List<ExerciceCommercial> exercicesCommerciaux = tiersService.getExercicesCommerciaux(entreprise);
			final DateRange ifd = ifd(fors, exercicesCommerciaux, dernierAssujettissementContinu, dateReference);

			// aucun assujettissement ?
			if (icc == null && ifd == null) {
				results.addIgnore(id, ExtractionResult.RaisonIgnore.NON_ASSUJETTI);
			}
			else {
				final ExerciceCommercial exerciceCourant = DateRangeHelper.rangeAt(exercicesCommerciaux, dateReference);
				if (exerciceCourant == null) {
					results.addError(id, ExtractionResult.RaisonErreur.PAS_EXERCICE_COMMERCIAL);
				}
				else {
					String codeRegimeVD = null;
					String codeRegimeCH = null;
					final List<RegimeFiscal> regimes = entreprise.getRegimesFiscauxNonAnnulesTries();
					for (RegimeFiscal regime : regimes) {
						if (regime.isValidAt(dateReference)) {
							switch (regime.getPortee()) {
							case CH:
								codeRegimeCH = regime.getCode();
								break;
							case VD:
								codeRegimeVD = regime.getCode();
								break;
							default:
								break;
							}
							if (codeRegimeCH != null && codeRegimeVD != null) {
								break;
							}
						}
					}
					if (codeRegimeVD == null || codeRegimeCH == null) {
						results.addError(id, ExtractionResult.RaisonErreur.PAS_REGIME_FISCAL);
					}
					else {
						final RegDate veilleDebutExerciceCourant = exerciceCourant.getDateDebut().getOneDayBefore();
						results.addExtrait(id,
						                   tiersService.getDerniereRaisonSociale(entreprise),
						                   exerciceCourant.getDateFin(),
						                   DateRangeHelper.rangeAt(exercicesCommerciaux, veilleDebutExerciceCourant) != null ? veilleDebutExerciceCourant : null,
						                   icc != null ? icc.getDateDebut() : null,
						                   icc != null ? icc.getDateFin() : null,
						                   ifd != null ? ifd.getDateDebut() : null,
						                   ifd != null ? ifd.getDateFin() : null,
						                   codeRegimeVD,
						                   codeRegimeCH);
					}
				}
			}
		}
	}

	@Nullable
	private static DateRange icc(List<ForFiscal> fors, DateRange dernierAssujettissementContinu, RegDate dateReference) {

		// calcul de la couverture des fors vaudois...

		final List<ForFiscal> vaudois = new ArrayList<>(fors.size());
		final DateRange past = new DateRangeHelper.Range(null, dateReference);
		for (ForFiscal ff : fors) {
			if (ff.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD
					&& ff.getGenreImpot() == GenreImpot.BENEFICE_CAPITAL
					&& DateRangeHelper.intersect(past, ff)
					&& DateRangeHelper.intersect(dernierAssujettissementContinu, ff)) {
				vaudois.add(ff);
			}
		}
		final List<DateRange> couvertureVaudoise = DateRangeHelper.merge(vaudois);
		if (couvertureVaudoise == null || couvertureVaudoise.isEmpty()) {
			return null;
		}

		final DateRange courant = DateRangeHelper.rangeAt(couvertureVaudoise, dateReference);
		if (courant != null) {
			return new DateRangeHelper.Range(courant.getDateDebut(), null);
		}
		else {
			// TODO peut-être qu'en cas de faillite, il faudrait poursuivre l'assujettissement ICC jusqu'à la fin de l'exercice, non ?
			return CollectionsUtils.getLastElement(couvertureVaudoise);
		}
	}

	@Nullable
	private static DateRange ifd(List<ForFiscal> fors, List<ExerciceCommercial> exercices, DateRange dernierAssujettissementContinu, RegDate dateReference) throws AssujettissementException {

		// on ne prend que les fors VD/HS qui intersectent la dernière période d'assujettissement continu et qui sont au moins pour partie dans le passé de la date de référence
		final DateRange past = new DateRangeHelper.Range(null, dateReference);
		final List<ForFiscalPrincipal> forsPrincipauxVdHs = new ArrayList<>(fors.size());
		for (ForFiscal ff : fors) {
			if (DateRangeHelper.intersect(ff, dernierAssujettissementContinu)
					&& DateRangeHelper.intersect(ff, past)
					&& ff.getGenreImpot() == GenreImpot.BENEFICE_CAPITAL
					&& ff.isPrincipal()
					&& (ff.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD || ff.getTypeAutoriteFiscale() == TypeAutoriteFiscale.PAYS_HS)) {

				forsPrincipauxVdHs.add((ForFiscalPrincipal) ff);
			}
		}

		// pas de fors principaux VD ni HS -> pas d'IFD
		if (forsPrincipauxVdHs.isEmpty()) {
			return null;
		}

		// date de fin pour l'IFD (seulement valide si la "période intéressante" est non-nulle)
		RegDate dateFin = null;

		// période qui délimite la position de la date de début (si elle existe)
		DateRange periodeInteressante = null;

		// si le for principal est VD ou HS à la date de référence, la date de fin de l'IFD est "null"...
		if (DateRangeHelper.rangeAt(forsPrincipauxVdHs, dateReference) != null) {
			periodeInteressante = new DateRangeHelper.Range(null, dateReference);
		}
		else {
			// revenons en arrière sur le dernier exercice commercial dont la date de bouclement est sur un for principal VD ou HS
			for (ExerciceCommercial exercice : CollectionsUtils.revertedOrder(exercices)) {
				if (dateReference.isBefore(exercice.getDateDebut())) {
					// exercice commercial complètement postérieur à la date de référence, ignoré
					continue;
				}

				if (dateReference == exercice.getDateFin()) {
					// exercice commercial qui se termine à la date de référence
					if (DateRangeHelper.rangeAt(forsPrincipauxVdHs, dateReference) != null) {
						periodeInteressante = new DateRangeHelper.Range(null, dateReference);
						break;
					}
				}
				else if (!exercice.isValidAt(dateReference)) {
					// exercice commercial complètement antérieur à la date de référence
					if (DateRangeHelper.rangeAt(forsPrincipauxVdHs, exercice.getDateFin()) != null) {
						dateFin = exercice.getDateFin();
						periodeInteressante = new DateRangeHelper.Range(null, dateFin);
						break;
					}
				}
			}

			// pas de date de fin trouvée, pas d'assujettissement IFD
			if (periodeInteressante == null) {
				return null;
			}
		}

		// maintenant qu'on a une période intéressante, occupons-nous de la date de début
		final List<DateRange> couvertureVdHc = DateRangeHelper.merge(DateRangeHelper.intersections(periodeInteressante, forsPrincipauxVdHs));
		if (couvertureVdHc == null || couvertureVdHc.isEmpty()) {
			throw new IllegalArgumentException("Erreur d'algorithme...");
		}

		final RegDate dateDebutCouverture = CollectionsUtils.getLastElement(couvertureVdHc).getDateDebut();
		final ExerciceCommercial debutExercice = DateRangeHelper.rangeAt(exercices, dateDebutCouverture);
		return new DateRangeHelper.Range(debutExercice.getDateDebut(), dateFin);
	}

	@Override
	@NotNull
	public List<String> getRequestXSDs() {
		return Arrays.asList("unireg-common-2.xsd",
		                     "event/party/request-2.xsd",
		                     "event/party/advance-payment-corporations-request-1.xsd");
	}

	@Override
	@NotNull
	public List<String> getResponseXSDs() {
		return Arrays.asList("unireg-common-2.xsd",
		                     "unireg-exception-1.xsd",
		                     "event/party/response-2.xsd",
		                     "event/party/advance-payment-corporations-response-1.xsd");
	}
}
