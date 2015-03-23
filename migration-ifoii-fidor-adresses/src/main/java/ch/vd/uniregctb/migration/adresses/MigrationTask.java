package ch.vd.uniregctb.migration.adresses;

import java.rmi.RemoteException;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import ch.vd.fidor.xml.common.v1.Date;
import ch.vd.fidor.xml.post.v1.PostalLocality;
import ch.vd.fidor.xml.post.v1.Street;
import ch.vd.infrastructure.model.Rue;
import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.infrastructure.service.ServiceInfrastructure;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.webservice.fidor.v5.FidorClient;

final class MigrationTask implements Callable<MigrationResult> {

	private static final Pattern NUMERO_MAISON_EXTRACTOR_PATTERN = Pattern.compile("(.*) +(\\d.*)");

	private final FidorClient fidorClient;
	private final ServiceInfrastructure ifoiiClient;
	private final DataAdresse adresse;

	MigrationTask(FidorClient fidorClient, ServiceInfrastructure ifoiiClient, DataAdresse adresse) {
		this.fidorClient = fidorClient;
		this.ifoiiClient = ifoiiClient;
		this.adresse = adresse;
	}

	private static String canonize(String rue) {
		rue = StringUtils.trimToEmpty(rue);
		rue = rue.replaceAll("^Av\\. *", "Avenue ");
		rue = rue.replaceAll("^Bvd\\. *", "Boulevard ");
		rue = rue.replaceAll("^Ch\\. *", "Chemin ");
		rue = rue.replaceAll("^Pl\\. *", "Place ");
		rue = rue.replaceAll("^Q\\. *", "Quai ");
		rue = rue.replaceAll("^R\\. *", "Rue ");
		rue = rue.replaceAll("^Rte *", "Route ");
		rue = rue.replaceAll("\\b' +", "'");
		rue = rue.replaceAll("\\b- +", "-");
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
				return new MigrationResult.NotFound(adresse, null, StringUtils.trimToNull(adresse.rue));
			}
		}
		catch (RemoteException | InfrastructureException e) {
			return new MigrationResult.Erreur(adresse, null, null, e);
		}

		// si le nom de la rue contient un numéro de maison, on peut essayer de l'enlever
		final Matcher noMaisonMatcher = NUMERO_MAISON_EXTRACTOR_PATTERN.matcher(StringUtils.trimToEmpty(libelleCanoniqueRue));

		try {
			// 2. on vérifie que la localité postale existe toujours avec ce numéro d'ordre postal à la date de fin de l'adresse
			final PostalLocality pl = findPostalLocalityForDate(noOrdreP, adresse.dateFin);
			if (pl == null) {
				// pas de localité postale ?
				return new MigrationResult.NotFound(adresse, noOrdreP, libelleRue);
			}

			// 3. on appelle FiDoR pour voir s'il connait une rue avec ce nom dans cette localité
			Integer estrid = null;
			String numeroMaison = null;
			final List<Street> ruesCandidates = fidorClient.getRuesParNumeroOrdrePosteEtDate(pl.getSwissZipCodeId(), adresse.dateFin);
			if (ruesCandidates != null) {
				for (Street candidate : ruesCandidates) {
					if (candidate.getLongName().equalsIgnoreCase(libelleCanoniqueRue) || candidate.getShortName().equalsIgnoreCase(libelleCanoniqueRue)) {
						estrid = candidate.getEstrid();
						break;
					}
				}

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
			}

			// c'est fini!
			if (estrid == null) {
				return new MigrationResult.NotFound(adresse, pl.getSwissZipCodeId(), libelleRue);
			}
			else {
				return new MigrationResult.Ok(adresse, estrid, pl.getSwissZipCodeId(), numeroMaison);
			}
		}
		catch (Exception e) {
			return new MigrationResult.Erreur(adresse, noOrdreP, libelleRue, e);
		}
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

		// la localité est-elle valide à la date de référence ?
		if (found != null && RegDateHelper.isBefore(fromDate(found.getValidity().getDateTo()), dateReference, NullDateBehavior.LATEST)) {
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
