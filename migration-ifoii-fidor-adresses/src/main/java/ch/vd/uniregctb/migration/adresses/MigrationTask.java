package ch.vd.uniregctb.migration.adresses;

import java.rmi.RemoteException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import ch.vd.fidor.xml.post.v1.Street;
import ch.vd.infrastructure.model.Rue;
import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.infrastructure.service.ServiceInfrastructure;
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

		// 2. on appelle FiDoR pour voir s'il connait une rue avec ce nom dans cette localité
		try {
			Integer estrid = null;
			String numeroMaison = null;
			final List<Street> ruesCandidates = fidorClient.getRuesParNumeroOrdrePosteEtDate(noOrdreP, adresse.dateFin);
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
				return new MigrationResult.NotFound(adresse, noOrdreP, libelleRue);
			}
			else {
				return new MigrationResult.Ok(adresse, estrid, noOrdreP, numeroMaison);
			}
		}
		catch (Exception e) {
			return new MigrationResult.Erreur(adresse, noOrdreP, libelleRue, e);
		}
	}
}
