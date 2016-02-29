package ch.vd.uniregctb.migration.pm.engine.helpers;

import java.util.function.Function;

import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.adresse.AdresseEtrangere;
import ch.vd.uniregctb.adresse.AdresseFiscale;
import ch.vd.uniregctb.adresse.AdresseMandataire;
import ch.vd.uniregctb.adresse.AdresseMandataireEtrangere;
import ch.vd.uniregctb.adresse.AdresseMandataireSuisse;
import ch.vd.uniregctb.adresse.AdresseSuisse;
import ch.vd.uniregctb.adresse.AdresseSupplementaire;
import ch.vd.uniregctb.adresse.AdresseTiers;
import ch.vd.uniregctb.common.HibernateDateRangeEntity;
import ch.vd.uniregctb.migration.pm.MigrationResultContextManipulation;
import ch.vd.uniregctb.migration.pm.adresse.StreetData;
import ch.vd.uniregctb.migration.pm.adresse.StreetDataMigrator;
import ch.vd.uniregctb.migration.pm.log.AdresseLoggedElement;
import ch.vd.uniregctb.migration.pm.regpm.AdresseAvecRue;

public class AdresseHelper {

	protected StreetDataMigrator streetDataMigrator;

	public AdresseHelper(StreetDataMigrator streetDataMigrator) {
		this.streetDataMigrator = streetDataMigrator;
	}

	/**
	 * Méthode centrale de ce helper, construction d'une adresse à partir d'une adresse de RegPM
	 */
	private <T extends HibernateDateRangeEntity & AdresseFiscale> T buildAdresse(AdresseAvecRue source,
	                                                                             MigrationResultContextManipulation mr,
	                                                                             Function<StreetData, ? extends T> adresseSuisseBuilder,
	                                                                             Function<AdresseAvecRue, ? extends T> adresseEtrangereBuilder) {

		if (source == null) {
			return null;
		}

		mr.pushContextValue(AdresseLoggedElement.class, new AdresseLoggedElement(source));
		try {
			final StreetData streetData = streetDataMigrator.migrate(source, mr);
			final T dest;
			if (streetData != null) {
				// on ne migre pas une adresse qui ne contient ni rue ni localité postale...
				if (streetData instanceof StreetData.AucuneNomenclatureTrouvee) {
					return null;
				}

				// adresse suisse
				dest = adresseSuisseBuilder.apply(streetData);
			}
			else {
				// adresse étrangère
				dest = adresseEtrangereBuilder.apply(source);
			}
			dest.setDateDebut(source.getDateDebut());
			dest.setDateFin(source.getDateFin());
			return dest;
		}
		finally {
			mr.popContexteValue(AdresseLoggedElement.class);
		}
	}

	/**
	 * Construit une adresse mandataire à partir des données fournie.<br/>
	 * Les champs {@link AdresseMandataire#typeMandat}, {@link AdresseMandataire#mandant} et {@link AdresseMandataire#id} ne sont pas remplis.<br/>
	 * L'entité retournée n'est rattachée à aucune session Hibernate.
	 * @param source source des données de l'adresse
	 * @param mr collecteurs de messages de suivi
	 * @param complement valeur du "complément"
	 * @return une adresse presque prête à persister
	 */
	@Nullable
	public AdresseMandataire buildAdresseMandataire(AdresseAvecRue source, MigrationResultContextManipulation mr, String complement) {
		final AdresseMandataire dest = buildAdresse(source, mr, AdresseHelper::buildAdresseMandataireSuisse, AdresseHelper::buildAdresseMandataireEtrangere);
		if (dest != null) {
			dest.setComplement(complement);
		}
		return dest;
	}

	/**
	 * Construit une adresse tiers (= surcharge d'adresse civile) à partir des données fournie.<br/>
	 * Les champs {@link AdresseTiers#usage}, {@link AdresseTiers#tiers} et {@link AdresseTiers#id} ne sont pas remplis.<br/>
	 * L'entité retournée n'est rattachée à aucune session Hibernate.
	 * @param source source des données de l'adresse
	 * @param mr collecteurs de messages de suivi
	 * @param complement valeur du "complément"
	 * @param permanente <code>true</code> si l'adresse doit être flaggée comme "permanente"
	 * @return une adresse presque prête à persister
	 */
	@Nullable
	public AdresseSupplementaire buildAdresseSupplementaire(AdresseAvecRue source, MigrationResultContextManipulation mr, String complement, boolean permanente) {
		final AdresseSupplementaire dest = buildAdresse(source, mr, AdresseHelper::buildAdresseSuisse, AdresseHelper::buildAdresseEtrangere);
		if (dest != null) {
			dest.setPermanente(permanente);
			dest.setComplement(complement);
		}
		return dest;
	}

	private static AdresseSuisse buildAdresseSuisse(StreetData streetData) {
		final AdresseSuisse a = new AdresseSuisse();
		a.setNpaCasePostale(null);
		a.setNumeroAppartement(null);
		a.setNumeroCasePostale(null);
		a.setNumeroMaison(streetData.getNoPolice());
		a.setNumeroOrdrePoste(streetData.getNoOrdreP());
		a.setNumeroRue(streetData.getEstrid());
		a.setRue(streetData.getNomRue());
		a.setTexteCasePostale(null);
		return a;
	}

	private static AdresseMandataireSuisse buildAdresseMandataireSuisse(StreetData streetData) {
		final AdresseMandataireSuisse a = new AdresseMandataireSuisse();
		a.setNpaCasePostale(null);
		a.setNumeroCasePostale(null);
		a.setNumeroMaison(streetData.getNoPolice());
		a.setNumeroOrdrePoste(streetData.getNoOrdreP());
		a.setNumeroRue(streetData.getEstrid());
		a.setRue(streetData.getNomRue());
		a.setTexteCasePostale(null);
		return a;
	}

	private static AdresseEtrangere buildAdresseEtrangere(AdresseAvecRue source) {
		final AdresseEtrangere a = new AdresseEtrangere();
		a.setComplementLocalite(null);
		a.setNumeroAppartement(null);
		a.setNumeroCasePostale(null);
		a.setNumeroMaison(source.getNoPolice());
		a.setNumeroOfsPays(source.getOfsPays());
		a.setNumeroPostalLocalite(source.getLieu());
		a.setRue(source.getNomRue());
		a.setTexteCasePostale(null);
		return a;
	}

	private static AdresseMandataireEtrangere buildAdresseMandataireEtrangere(AdresseAvecRue source) {
		final AdresseMandataireEtrangere a = new AdresseMandataireEtrangere();
		a.setComplementLocalite(null);
		a.setNumeroCasePostale(null);
		a.setNumeroMaison(source.getNoPolice());
		a.setNumeroOfsPays(source.getOfsPays());
		a.setNumeroPostalLocalite(source.getLieu());
		a.setRue(source.getNomRue());
		a.setTexteCasePostale(null);
		return a;
	}

}
