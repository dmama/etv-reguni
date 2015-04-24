package ch.vd.uniregctb.migration.pm.adresse;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import ch.vd.fidor.xml.common.v1.Date;
import ch.vd.fidor.xml.post.v1.PostalLocality;
import ch.vd.fidor.xml.post.v1.Street;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.migration.pm.MigrationResultMessage;
import ch.vd.uniregctb.migration.pm.regpm.AdresseAvecRue;
import ch.vd.uniregctb.migration.pm.regpm.RegpmLocalitePostale;
import ch.vd.uniregctb.migration.pm.regpm.RegpmRue;
import ch.vd.uniregctb.webservice.fidor.v5.FidorClient;

public class StreetDataMigratorImpl implements StreetDataMigrator {

	private static final Pattern NUMERO_MAISON_EXTRACTOR_PATTERN = Pattern.compile("(.*) +(\\d.*)");

	private FidorClient fidorClient;
	private Map<Integer, List<Integer>> mappingNosOrdrePoste;

	private static String canonize(String rue) {
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

	private static RegDate fromDate(Date date) {
		if (date == null) {
			return null;
		}
		return RegDate.get(date.getYear(), date.getMonth(), date.getDay());
	}

	private static RegDate getDateReference(AdresseAvecRue adresse) {
		return adresse.getDateFin() == null ? adresse.getDateDebut() : adresse.getDateFin();
	}

	public void setFidorClient(FidorClient fidorClient) {
		this.fidorClient = fidorClient;
	}

	public void setMappingNosOrdrePoste(Map<Integer, List<Integer>> mappingNosOrdrePoste) {
		this.mappingNosOrdrePoste = mappingNosOrdrePoste;
	}

	@Override
	public StreetData migrate(AdresseAvecRue adresse) {

		// on écarte de suite les adresses hors-Suisse
		if (adresse.getOfsPays() != null && ServiceInfrastructureService.noOfsSuisse != adresse.getOfsPays()) {
			return null;
		}

		// 1. on recherche le numéro d'ordre postal de la localité postale et le libellé de la rue
		final RegpmLocalitePostale localitePostale;
		final String libelleRue;
		final String libelleCanoniqueRue;

		final RegpmRue rue = adresse.getRue();
		if (rue != null) {
			localitePostale = rue.getLocalitePostale();
			libelleRue = StringUtils.trimToNull(rue.getDesignationCourrier());
			libelleCanoniqueRue = canonize(libelleRue);
		}
		else if (adresse.getLocalitePostale() != null) {
			localitePostale = adresse.getLocalitePostale();
			libelleRue = StringUtils.trimToNull(adresse.getNomRue());
			libelleCanoniqueRue = canonize(libelleRue);
		}
		else {
			// ni rue ni localité de la nomenclature officielle (étranger sans indication de pays?)
			return new StreetData.AucuneNomenclatureTrouvee(adresse.getNomRue(), adresse.getNoPolice(), adresse.getLieu());
		}

		final int noOrdreP = localitePostale.getNoOrdreP().intValue();

		// 2. on vérifie que la localité postale existe toujours avec ce numéro d'ordre postal à la date de fin de l'adresse
		// (integer = noOrdrePoste, RegDate = date de début de validité de la commune)
		final List<Pair<Integer, RegDate>> localitesATester = new LinkedList<>();
		final PostalLocality pl = findPostalLocalityForDate(noOrdreP, getDateReference(adresse));
		if (pl == null) {
			// pas de localité postale ?

			// il faut donc chercher dans le mapping "en dur", au cas où...
			if (mappingNosOrdrePoste.containsKey(noOrdreP)) {
				final RegDate dateBidonDebut = RegDate.get(1291, 8, 1);     // on n'a pas d'adresse avec une date de fin aussi vieille..,
				for (int remplacant : mappingNosOrdrePoste.get(noOrdreP)) {
					localitesATester.add(Pair.of(remplacant, dateBidonDebut));
				}
			}
			else {
				// voyons voir quel NPA le mainframe associait à ce numéro d'ordre poste... on peut peut-être essayer de passer par là...
				if (localitePostale.getNpa() != null) {
					final List<PostalLocality> candidates = fidorClient.getLocalitesPostales(getDateReference(adresse), localitePostale.getNpa(), null, null, null);
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
				return new StreetData.LocaliteAbsenteRefinf(libelleRue, adresse.getNoPolice(), noOrdreP, localitePostale.getNpa(), localitePostale.getNpaChiffreComplementaire(), localitePostale.getNomLong());
			}
		}
		else {
			localitesATester.add(Pair.of(pl.getSwissZipCodeId(), fromDate(pl.getValidity().getDateFrom())));
		}

		// 3. on appelle FiDoR pour voir s'il connait une rue avec ce nom dans l'une de ces localités
		for (Pair<Integer, RegDate> aTester : localitesATester) {
			// on recalcule une date de référence pour prendre en compte le cas où Ref-Inf ne connait pas de localité postale avec un numéro donnée
			// à la date de fin de l'adresse connue, mais seulement plus tard (problématique d'historique tronqué dans le passé...)
			final RegDate refDate = RegDateHelper.maximum(aTester.getRight(), adresse.getDateFin(), NullDateBehavior.LATEST);
			final FidorInfo info = askFidor(aTester.getLeft(), refDate, libelleCanoniqueRue);
			if (info != null && info.street != null) {
				return new StreetData.AvecEstrid(info.street, info.numeroMaison, info.noOrdrePostal);
			}
		}

		// 4. constat d'échec... pas de rue avec de nom là... on prend la première localité postale (qui peut être celle fournie en entrée, ou une meilleure - plus récente - approximation de celle-ci...)
		final int mostProbableSwissZipCodeId = localitesATester.get(0).getLeft();
		final StreetData.RueInconnue result = new StreetData.RueInconnue(libelleRue, adresse.getNoPolice(), mostProbableSwissZipCodeId);
		if (mostProbableSwissZipCodeId != noOrdreP) {
			final String noRue = adresse.getRue() != null ? String.format("%d (%s)", adresse.getRue().getId(), adresse.getRue().getDesignationCourrier()) : null;
			final String nomRue = StringUtils.isNotBlank(adresse.getNomRue()) ? String.format("'%s'", adresse.getNomRue()) : null;
			final PostalLocality locality = findPostalLocalityForDate(mostProbableSwissZipCodeId, adresse.getDateFin());
			final String npaLocalitePrise = locality != null ? String.format("%d %s", locality.getSwissZipCode(), locality.getLongName()) : "???";

			final String npaLocaliteMainframe = String.format("%d (%d %s)", localitePostale.getNoOrdreP(), localitePostale.getNpa(), localitePostale.getNomLong());
			final String detailsFin = adresse.getDateFin() != null
					? String.format("fermée le %s", RegDateHelper.dateToDisplayString(adresse.getDateFin()))
					: "active";

			final String msg = String.format("Adresse %s: données du mainframe {onrp=%s, rue=%s, noRue=%s}, onrp pris par défaut %d (%s)",
			                                  detailsFin,
			                                  npaLocaliteMainframe, nomRue, noRue,
			                                  mostProbableSwissZipCodeId, npaLocalitePrise);
			result.addMessage(MigrationResultMessage.CategorieListe.ADRESSES, MigrationResultMessage.Niveau.INFO, msg);
		}
		return result;
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

	@Nullable
	private FidorInfo askFidor(int noOrdrePostal, RegDate refDate, String libelleCanoniqueRue) {
		Street street = null;
		String numeroMaison = null;
		final List<Street> ruesCandidates = fidorClient.getRuesParNumeroOrdrePosteEtDate(noOrdrePostal, refDate);
		if (ruesCandidates != null) {
			for (Street candidate : ruesCandidates) {
				if (candidate.getLongName().equalsIgnoreCase(libelleCanoniqueRue) || candidate.getShortName().equalsIgnoreCase(libelleCanoniqueRue)) {
					street = candidate;
					break;
				}
			}

			// si le nom de la rue contient un numéro de maison, on peut essayer de l'enlever
			final Matcher noMaisonMatcher = NUMERO_MAISON_EXTRACTOR_PATTERN.matcher(StringUtils.trimToEmpty(libelleCanoniqueRue));

			// on n'a pas trouvé comme ça, mais peut-être qu'en enlevant le numéro de maison ?
			if (street == null && noMaisonMatcher.matches()) {
				final String sansNumero = noMaisonMatcher.group(1);
				if (StringUtils.isNotBlank(sansNumero)) {
					for (Street candidate : ruesCandidates) {
						if (candidate.getLongName().equalsIgnoreCase(sansNumero) || candidate.getShortName().equalsIgnoreCase(sansNumero)) {
							street = candidate;
							numeroMaison = noMaisonMatcher.group(2);
							break;
						}
					}
				}
			}

			return new FidorInfo(street, numeroMaison, noOrdrePostal);
		}
		return null;
	}

	private static final class FidorInfo {
		final Street street;
		final String numeroMaison;
		final int noOrdrePostal;

		private FidorInfo(Street street, String numeroMaison, int noOrdrePostal) {
			this.street = street;
			this.numeroMaison = numeroMaison;
			this.noOrdrePostal = noOrdrePostal;
		}
	}
}
