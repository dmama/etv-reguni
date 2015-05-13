package ch.vd.uniregctb.migration.pm.historizer.extractor.organization;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import ch.vd.evd0022.v1.Identifier;
import ch.vd.evd0022.v1.OrganisationLocation;
import ch.vd.evd0022.v1.SwissMunicipality;

public class Etablissement {

	private static final Function<List<Identifier>, String> NO_IDE_EXTRACTOR = new NumeroIdeExtractor();

	private final long id;
	private final String noIde;
	private final String nom;
	private final Integer noOfsCommune;
	private final String noga;

	public Etablissement(long id, String noIde, String nom, Integer noOfsCommune, String noga) {
		this.id = id;
		this.noIde = noIde;
		this.nom = nom;
		this.noOfsCommune = noOfsCommune;
		this.noga = noga;
	}

	public Etablissement(OrganisationLocation location) {
		this(location.getCantonalId().longValue(),
		     NO_IDE_EXTRACTOR.apply(location.getIdentifier()),
		     location.getName(),
		     Optional.ofNullable(location.getSeat()).map(SwissMunicipality::getMunicipalityId).orElse(null),
		     location.getNogaCode());
	}

	public long getId() {
		return id;
	}

	public String getNoIde() {
		return noIde;
	}

	public String getNom() {
		return nom;
	}

	public Integer getNoOfsCommune() {
		return noOfsCommune;
	}

	public String getNoga() {
		return noga;
	}
}
