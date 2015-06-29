package ch.vd.uniregctb.migration.pm.engine.helpers;

import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.adresse.AdresseEtrangere;
import ch.vd.uniregctb.adresse.AdresseSuisse;
import ch.vd.uniregctb.adresse.AdresseSupplementaire;
import ch.vd.uniregctb.adresse.AdresseTiers;
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
	 * Construit une adresse tiers (= surcharge d'adresse civile) à partir des données fournie.<br/>
	 * Les champs {@link AdresseTiers#usage}, {@link AdresseTiers#tiers} et {@link AdresseTiers#id} ne sont pas remplis.<br/>
	 * L'entité retournée n'est rattachée à aucune session Hibernate.
	 * @param source source des données de l'adresse
	 * @param mr collecteurs de messages de suivi
	 * @param complement supplier pour la valeur du "complément"
	 * @param permanente <code>true</code> si l'adresse doit être flaggée comme "permanente"
	 * @return une adresse presque prête à persister
	 */
	public AdresseTiers buildAdresse(AdresseAvecRue source, MigrationResultContextManipulation mr, @Nullable Supplier<String> complement, boolean permanente) {
		if (source == null) {
			return null;
		}

		mr.pushContextValue(AdresseLoggedElement.class, new AdresseLoggedElement(source));
		try {
			final StreetData streetData = streetDataMigrator.migrate(source, mr);
			final AdresseSupplementaire dest;
			if (streetData != null) {
				// on ne migre pas une adresse qui ne contient ni rue ni localité postale...
				if (streetData instanceof StreetData.AucuneNomenclatureTrouvee) {
					return null;
				}

				// adresse suisse
				dest = buildAdresseSuisse(streetData);
			}
			else {
				// adresse étrangère
				dest = buildAdresseEtrangere(source);
			}
			dest.setDateDebut(source.getDateDebut());
			dest.setDateFin(source.getDateFin());
			dest.setPermanente(permanente);
			dest.setComplement(complement != null ? complement.get() : null);
			return dest;
		}
		finally {
			mr.popContexteValue(AdresseLoggedElement.class);
		}
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

}
