package ch.vd.unireg.interfaces.organisation.data.builder;

import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.interfaces.organisation.data.EtablissementCivil;
import ch.vd.unireg.interfaces.organisation.data.EtablissementCivilRCEnt;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.OrganisationRCEnt;

/**
 * Classe de builder pour la construction facilité d'entités d'organisation. Les autres builders de
 * ce package fonctionnent selon les mêmes principes.
 *
 * Le principe est d'assembler les informations une à une, au fil de l'eau. Chaque méthode renvoie la référence
 * vers le builder, ce qui permet de chaîner les appels et de réduire la gène visuelle.
 *
 *  En dernier, l'appel à build() crée l'entité définitive en appelant son constructeur, validant ainsi les
 *  éventuelles contraintes.
 *
 * Il y a deux types de méthodes:1
 * - les méthodes commençant par "add":   Ajouter des éléments un par un à une propriété collection de l'entité.
 * - les méthodes commençant par "with":  Spécifier d'un coup la valeur définitive de la propriété.
 *
 * Ainsi, on peut facilement créer des entités avec une notation naturelle et lisible.
 */
public class OrganisationBuilder implements DataBuilder<Organisation> {

	private final long numeroOrganisation;
	private Map<String, List<DateRanged<String>>> autresIdentifiants;

	private Map<Long, List<DateRanged<Long>>> numerosEtablissements;
	private Map<Long, EtablissementCivil> etablissements;

	public OrganisationBuilder(long numeroOrganisation) {
		this.numeroOrganisation = numeroOrganisation;
	}

	@Override
	public OrganisationRCEnt build() {
		return new OrganisationRCEnt(numeroOrganisation, numerosEtablissements, etablissements);
	}

	public OrganisationBuilder addSite(@NotNull Long cle, @NotNull RegDate dateDebut, RegDate dateDeFin, @NotNull Long valeur) {
		numerosEtablissements = BuilderHelper.addValueToMapOfList(numerosEtablissements, cle, new DateRanged<>(dateDebut, dateDeFin, valeur));
		return this;
	}

	public OrganisationBuilder addDonneesSite(@NotNull EtablissementCivilRCEnt etablissement) {
		etablissements = BuilderHelper.addValueToMap(etablissements, etablissement.getNumeroEtablissement(), etablissement);
		return this;
	}

	public OrganisationBuilder withSites(Map<Long, List<DateRanged<Long>>> numerosEtablissements) {
		this.numerosEtablissements = numerosEtablissements;
		return this;
	}

	public OrganisationBuilder withDonneesSites(Map<Long, EtablissementCivil> donneesSites) {
		this.etablissements = donneesSites;
		return this;
	}
}
