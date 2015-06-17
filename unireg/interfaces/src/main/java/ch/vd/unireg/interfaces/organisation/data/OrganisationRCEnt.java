package ch.vd.unireg.interfaces.organisation.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.interfaces.organisation.rcent.RCEntHelper;
import ch.vd.uniregctb.adapter.rcent.model.OrganisationLocation;

public class OrganisationRCEnt implements Organisation {


	private final long cantonalId;

	@NotNull
	private final Map<String, List<DateRanged<String>>> identifiants;

	@NotNull
	private final List<DateRanged<String>> nom;
	private final List<DateRanged<String>> nomsAdditionels;
	private final List<DateRanged<LegalForm>> formeLegale;

	private final List<DateRanged<Long>> sites;
	private final List<SiteOrganisationRCEnt> donneesSites;

	private final List<DateRanged<Long>> transfereA;
	private final List<DateRanged<Long>> transferDe;
	private final List<DateRanged<Long>> remplacePar;
	private final List<DateRanged<Long>> enRemplacementDe;

	public OrganisationRCEnt(ch.vd.uniregctb.adapter.rcent.model.Organisation organisation) {

		cantonalId = organisation.getCantonalId();

		identifiants = RCEntHelper.convert(organisation.getOrganisationIdentifiers());

		nom = RCEntHelper.convert(organisation.getOrganisationName());
		nomsAdditionels = RCEntHelper.convert(organisation.getOrganisationAdditionalName());
		formeLegale = RCEntHelper.convertAndMap(organisation.getLegalForm(), new Function<ch.vd.evd0022.v1.LegalForm, LegalForm>() {
			@Override
			public LegalForm apply(ch.vd.evd0022.v1.LegalForm legalForm) {
				return LegalForm.valueOf(legalForm.toString());
			}
		});
		sites = RCEntHelper.convert(organisation.getLocations());
		donneesSites = convertLocations(organisation.getLocationData());
		transfereA = RCEntHelper.convert(organisation.getTransferTo());
		transferDe = RCEntHelper.convert(organisation.getTransferFrom());
		remplacePar = RCEntHelper.convert(organisation.getReplacedBy());
		enRemplacementDe = RCEntHelper.convert(organisation.getInReplacementOf());
	}

	private static List<SiteOrganisationRCEnt> convertLocations(List<OrganisationLocation> locations) {
		List<SiteOrganisationRCEnt> sites = new ArrayList<>();
		for (OrganisationLocation loc : locations) {
			sites.add(new SiteOrganisationRCEnt(loc));
		}
		return sites;
	}
}
