package ch.vd.uniregctb.migration.adresses;

import java.rmi.RemoteException;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.fidor.xml.common.v1.Date;
import ch.vd.fidor.xml.post.v1.PostalLocality;
import ch.vd.fidor.xml.post.v1.Street;
import ch.vd.infrastructure.model.Localite;
import ch.vd.infrastructure.model.Rue;
import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.infrastructure.service.ServiceInfrastructure;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.webservice.fidor.v5.FidorClient;

final class MigrationTask implements Callable<MigrationResult> {

	private static final Logger DUBIOUS_LOGGER = LoggerFactory.getLogger("ch.vd.uniregctb.dubious");

	private static final Pattern NUMERO_MAISON_EXTRACTOR_PATTERN = Pattern.compile("(.*) +(\\d.*)");

	private final FidorClient fidorClient;
	private final ServiceInfrastructure ifoiiClient;
	private final Map<Integer, List<Integer>> mappingNoOrdrePoste;
	private final DataAdresse adresse;

	MigrationTask(FidorClient fidorClient, ServiceInfrastructure ifoiiClient, Map<Integer, List<Integer>> mappingNoOrdrePoste, DataAdresse adresse) {
		this.fidorClient = fidorClient;
		this.ifoiiClient = ifoiiClient;
		this.mappingNoOrdrePoste = mappingNoOrdrePoste;
		this.adresse = adresse;
	}

	static String canonize(String rue) {
		rue = StringUtils.trimToEmpty(rue);
		rue = rue.replaceAll("^Av\\. *", "Avenue ");
		rue = rue.replaceAll("^Bvd\\. *", "Boulevard ");
		rue = rue.replaceAll("^Ch\\. *", "Chemin ");
		rue = rue.replaceAll("^Pl\\. *", "Place ");
		rue = rue.replaceAll("^Q\\. *", "Quai ");
		rue = rue.replaceAll("^R\\. *", "Rue ");
		rue = rue.replaceAll("^Rte\\b\\.? *", "Route ");
		rue = rue.replaceAll("^Sent\\. *", "Sentier ");
		rue = rue.replaceAll("\\b' +", "'");
		rue = rue.replaceAll("\\b- +", "-");
		rue = rue.replaceAll("\\b([a-zA-ZäöüÄÖÜ]+-?)[Ss][Tt][Rr]\\. *", "$1strasse ");
		return rue;
	}

	@Override
	public MigrationResult call() {

		// 1. on recherche le numéro d'ordre postal de la localité postale et le libellé de la rue
		final int noOrdreP;
		final String libelleRue;
		final String libelleCanoniqueRue;
		try {
			if (adresse.noRue != null) {
				final Rue rue = ifoiiClient.getRueByNumero(adresse.noRue);
				noOrdreP = rue.getNoLocalite();
				libelleRue = StringUtils.trimToNull(rue.getDesignationCourrier());
				libelleCanoniqueRue = canonize(libelleRue);
			}
			else if (adresse.noOrdrePoste != null) {
				noOrdreP = adresse.noOrdrePoste;
				libelleRue = StringUtils.trimToNull(adresse.rue);
				libelleCanoniqueRue = canonize(libelleRue);
			}
			else {
				// ni numéro de rue ni numéro de localité... que peut-on bien faire de ça ?
				return new MigrationResult.NotFound(adresse, null, false, StringUtils.trimToNull(adresse.rue));
			}
		}
		catch (RemoteException | InfrastructureException e) {
			return new MigrationResult.Erreur(adresse, null, false, null, e);
		}

		try {
			// 2. on vérifie que la localité postale existe toujours avec ce numéro d'ordre postal à la date de fin de l'adresse
			// (integer = noOrdrePoste, RegDate = date de début de validité de la commune)
			final List<Pair<Integer, RegDate>> localitesATester = new LinkedList<>();
			final PostalLocality pl = findPostalLocalityForDate(noOrdreP, adresse.dateFin);
			if (pl == null) {
				// pas de localité postale ?

				// il faut donc chercher dans le mapping "en dur", au cas où...
				if (mappingNoOrdrePoste.containsKey(noOrdreP)) {
					final RegDate dateBidonDebut = RegDate.get(1900, 1, 1);     // on n'a pas d'adresse avec une date de fin aussi vieille..,
					for (int remplacant : mappingNoOrdrePoste.get(noOrdreP)) {
						localitesATester.add(Pair.of(remplacant, dateBidonDebut));
					}
				}
				else {
					// voyons voir quel NPA le mainframe associait à ce numéro d'ordre poste... on peut peut-être essayer de passer par là...
					final Localite localiteMainframe = ifoiiClient.getLocalite(noOrdreP);
					if (localiteMainframe != null && localiteMainframe.getNPA() != null) {
						final List<PostalLocality> candidates = fidorClient.getLocalitesPostales(adresse.dateFin, localiteMainframe.getNPA(), null, null, null);
						if (candidates != null && !candidates.isEmpty()) {
							for (PostalLocality candidate : candidates) {
								localitesATester.add(Pair.of(candidate.getSwissZipCodeId(), fromDate(candidate.getValidity().getDateFrom())));
							}
						}
					}
				}

				// vraiment rien trouvé... (une source pour la prochaine itération, dans le mappingNoOrdrePoste...)
				if (localitesATester.isEmpty()) {
					// constat d'échec, aucune localité retrouvée...
					return new MigrationResult.LocalityNotFound(adresse, noOrdreP, false, libelleRue);
				}
			}
			else {
				localitesATester.add(Pair.of(pl.getSwissZipCodeId(), fromDate(pl.getValidity().getDateFrom())));
			}

			// 3. on appelle FiDoR pour voir s'il connait une rue avec ce nom dans l'une de ces localités
			for (Pair<Integer, RegDate> aTester : localitesATester) {
				// on recalcule une date de référence pour prendre en compte le cas où Ref-Inf ne connait pas de localité postale avec un numéro donnée
				// à la date de fin de l'adresse connue, mais seulement plus tard (problématique d'historique tronqué dans le passé...)
				final RegDate refDate = RegDateHelper.maximum(aTester.getRight(), adresse.dateFin, NullDateBehavior.LATEST);
				final FidorInfo info = askFidor(aTester.getLeft(), refDate, libelleCanoniqueRue);
				if (info != null && info.estrid != null) {
					return new MigrationResult.Ok(adresse, info.estrid, info.noOrdrePostal, info.numeroMaison);
				}
			}

			// 4. constat d'échec... pas de rue avec de nom là... on prend la première localité postale (qui peut être celle fournie en entrée, ou une meilleure - plus récente - approximation de celle-ci...)
			final int mostProbableSwissZipCodeId = localitesATester.get(0).getLeft();
			if (mostProbableSwissZipCodeId != noOrdreP && DUBIOUS_LOGGER.isWarnEnabled()) {
				final String noRue = adresse.noRue != null ? String.format("%d (%s)", adresse.noRue, libelleRue) : null;
				final String nomRue = StringUtils.isNotBlank(adresse.rue) ? String.format("'%s'", StringUtils.trim(adresse.rue)) : null;
				final PostalLocality locality = findPostalLocalityForDate(mostProbableSwissZipCodeId, adresse.dateFin);
				final String npaLocalitePrise = locality != null ? String.format("%d %s", locality.getSwissZipCode(), locality.getLongName()) : "???";
				final Localite localiteMainframe = ifoiiClient.getLocalite(noOrdreP);
				final String npaLocaliteMainframe = localiteMainframe != null ? String.format("%d (%d %s)", noOrdreP, localiteMainframe.getNPA(), localiteMainframe.getNomCompletMinuscule()) : Integer.toString(noOrdreP);
				final String detailsFinOuAnnulation;
				if (adresse.dateFin != null || adresse.dateAnnulation != null) {
					final StringBuilder b = new StringBuilder();
					if (adresse.dateFin != null) {
						b.append("fermée le ").append(RegDateHelper.dateToDisplayString(adresse.dateFin));
					}
					if (adresse.dateAnnulation != null) {
						if (b.length() > 0) {
							b.append(", ");
						}
						b.append("annulée");
					}
					detailsFinOuAnnulation = b.toString();
				}
				else {
					detailsFinOuAnnulation = "active";
				}
				DUBIOUS_LOGGER.warn(String.format("Adresse %d du tiers %d (%s): données du mainframe {onrp=%s, rue=%s, noRue=%s}, onrp pris par défaut %d (%s)",
				                                  adresse.id, adresse.tiersId, detailsFinOuAnnulation,
				                                  npaLocaliteMainframe, nomRue, noRue,
				                                  mostProbableSwissZipCodeId, npaLocalitePrise));
			}
			return new MigrationResult.NotFound(adresse, mostProbableSwissZipCodeId, mostProbableSwissZipCodeId != noOrdreP, libelleRue);
		}
		catch (Exception e) {
			return new MigrationResult.Erreur(adresse, noOrdreP, false, libelleRue, e);
		}
	}

	private static final class FidorInfo {
		final Integer estrid;
		final String numeroMaison;
		final int noOrdrePostal;

		private FidorInfo(Integer estrid, String numeroMaison, int noOrdrePostal) {
			this.estrid = estrid;
			this.numeroMaison = numeroMaison;
			this.noOrdrePostal = noOrdrePostal;
		}
	}

	private FidorInfo askFidor(int noOrdrePostal, RegDate refDate, String libelleCanoniqueRue) {
		Integer estrid = null;
		String numeroMaison = null;
		final List<Street> ruesCandidates = fidorClient.getRuesParNumeroOrdrePosteEtDate(noOrdrePostal, refDate);
		if (ruesCandidates != null) {
			for (Street candidate : ruesCandidates) {
				if (candidate.getLongName().equalsIgnoreCase(libelleCanoniqueRue) || candidate.getShortName().equalsIgnoreCase(libelleCanoniqueRue)) {
					estrid = candidate.getEstrid();
					break;
				}
			}

			// si le nom de la rue contient un numéro de maison, on peut essayer de l'enlever
			final Matcher noMaisonMatcher = NUMERO_MAISON_EXTRACTOR_PATTERN.matcher(StringUtils.trimToEmpty(libelleCanoniqueRue));

			// on n'a pas trouvé comme ça, mais peut-être qu'en enlevant le numéro de maison ?
			if (estrid == null && noMaisonMatcher.matches()) {
				final String sansNumero = noMaisonMatcher.group(1);
				if (StringUtils.isNotBlank(sansNumero)) {
					for (Street candidate : ruesCandidates) {
						if (candidate.getLongName().equalsIgnoreCase(sansNumero) || candidate.getShortName().equalsIgnoreCase(sansNumero)) {
							estrid = candidate.getEstrid();
							numeroMaison = noMaisonMatcher.group(2);
							break;
						}
					}
				}
			}

			return new FidorInfo(estrid, numeroMaison, noOrdrePostal);
		}
		return null;
	}

	/**
	 * Il est possible que lu numéro d'ordre postal donné ne soit plus valable à la date demandé, auquel cas il faut aller chercher le successeur, s'il existe
	 * @param noOrdreP numéro d'ordre poste connu
	 * @param dateReference date de référence
	 * @return si le numéro d'ordre poste est valide à la date de référence, la localité postale qui le porte, sinon la localité postale successeur
	 */
	private PostalLocality findPostalLocalityForDate(int noOrdreP, RegDate dateReference) {
		final List<PostalLocality> histo = fidorClient.getLocalitesPostalesHisto(noOrdreP);
		if (histo == null || histo.isEmpty()) {
			return null;
		}

		// depuis la fin de la liste, on prend la première localité dont la date de début est antérieure ou égale à la date de référence donnée
		final ListIterator<PostalLocality> iterator = histo.listIterator(histo.size());
		PostalLocality found = null;
		while (iterator.hasPrevious()) {
			final PostalLocality pl = iterator.previous();
			if (RegDateHelper.isBeforeOrEqual(fromDate(pl.getValidity().getDateFrom()), dateReference, NullDateBehavior.LATEST)) {
				found = pl;
				break;
			}
		}

		// si on n'a rien trouvé, c'est que la date de référence donnée est avant TOUTES les occurrences des localités avec ce numéro dans Ref-Inf
		// (cas connu, apparemment, l'historique des données ne remonte pas assez loin pour nous...)
		if (found == null) {
			// on prend la première occurrence connue dans Ref-Inf
			found = histo.get(0);
		}
		// la localité est-elle valide à la date de référence ?
		else if (RegDateHelper.isBefore(fromDate(found.getValidity().getDateTo()), dateReference, NullDateBehavior.LATEST)) {
			// non, pas valide... il faut trouver la suivante si elle existe
			if (found.getSuccessorSwissZipCodeId() != null) {
				if (noOrdreP == found.getSuccessorSwissZipCodeId()) {
					// blindage contre une localité qui dirait être son propre successeur (= stoppe la récursivité infinie)
					throw new IllegalArgumentException("Localité " + noOrdreP + " déclare être son propre successeur...");
				}

				// appel récursif avec le numéro du successeur
				found = findPostalLocalityForDate(found.getSuccessorSwissZipCodeId(), dateReference);
			}
		}

		return found;
	}

	private static RegDate fromDate(Date date) {
		if (date == null) {
			return null;
		}
		return RegDate.get(date.getYear(), date.getMonth(), date.getDay());
	}
}
