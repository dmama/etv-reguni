package ch.vd.unireg.interfaces.entreprise.rcent.adapter.historizer.convertor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ch.ech.ech0097.v2.NamedOrganisationId;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

public class MultivalueListConverterTest {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void testToMapOfListsOfDateRangedValues() throws Exception {
		List<DateRangeHelper.Ranged<NamedOrganisationId>> rl = new ArrayList<>();
		rl.add(new DateRangeHelper.Ranged<>(RegDate.get(2015, 4, 1), RegDate.get(2015, 4, 2), new NamedOrganisationId("CHE", "DATA1")));
		rl.add(new DateRangeHelper.Ranged<>(RegDate.get(2015, 4, 3), null, new NamedOrganisationId("CHE", "DATA2")));
		rl.add(new DateRangeHelper.Ranged<>(RegDate.get(2015, 4, 2), RegDate.get(2015, 4, 6), new NamedOrganisationId("CHE_GUEVARA", "DATA1")));
		rl.add(new DateRangeHelper.Ranged<>(RegDate.get(2015, 4, 9), null, new NamedOrganisationId("CHE_GUEVARA", "DATA2")));

		Map<String, List<DateRangeHelper.Ranged<String>>> NamedOrganisationIdsMap = MultivalueListConverter.toMapOfListsOfDateRangedValues(rl, NamedOrganisationId::getOrganisationIdCategory, NamedOrganisationId::getOrganisationId);

		List<DateRangeHelper.Ranged<String>> che_list = NamedOrganisationIdsMap.get("CHE");
		assertThat(che_list.size(), equalTo(2));
		{
			assertThat(che_list.get(0).getDateDebut(), equalTo(RegDate.get(2015, 4, 1)));
			assertThat(che_list.get(0).getDateFin(), equalTo(RegDate.get(2015, 4, 2)));
			assertThat(che_list.get(0).getPayload(), equalTo("DATA1"));
		}
		{
			assertThat(che_list.get(1).getDateDebut(), equalTo(RegDate.get(2015, 4, 3)));
			assertThat(che_list.get(1).getDateFin(), nullValue());
			assertThat(che_list.get(1).getPayload(), equalTo("DATA2"));
		}

		List<DateRangeHelper.Ranged<String>> che_guevara_list = NamedOrganisationIdsMap.get("CHE_GUEVARA");
		assertThat(che_list.size(), equalTo(2));
		{
			assertThat(che_guevara_list.get(0).getDateDebut(), equalTo(RegDate.get(2015, 4, 2)));
			assertThat(che_guevara_list.get(0).getDateFin(), equalTo(RegDate.get(2015, 4, 6)));
			assertThat(che_guevara_list.get(0).getPayload(), equalTo("DATA1"));
		}
		{
			assertThat(che_guevara_list.get(1).getDateDebut(), equalTo(RegDate.get(2015, 4, 9)));
			assertThat(che_guevara_list.get(1).getDateFin(), nullValue());
			assertThat(che_guevara_list.get(1).getPayload(), equalTo("DATA2"));
		}
	}

	@Test
	public void testFailWithOverlapping() throws Exception {
		List<DateRangeHelper.Ranged<NamedOrganisationId>> rl = new ArrayList<>();
		rl.add(new DateRangeHelper.Ranged<>(RegDate.get(2015, 4, 1), RegDate.get(2015, 4, 2), new NamedOrganisationId("CHE", "DATA1")));
		rl.add(new DateRangeHelper.Ranged<>(RegDate.get(2015, 4, 2), null, new NamedOrganisationId("CHE", "DATA2")));

		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("Found overlapping range in list for key CHE:");
		Map<String, List<DateRangeHelper.Ranged<String>>> NamedOrganisationIdsMap = MultivalueListConverter.toMapOfListsOfDateRangedValues(rl, NamedOrganisationId::getOrganisationIdCategory, NamedOrganisationId::getOrganisationId);
	}

	@Test
	public void testFailWithOverlappingUnbounded() throws Exception {
		List<DateRangeHelper.Ranged<NamedOrganisationId>> rl = new ArrayList<>();
		rl.add(new DateRangeHelper.Ranged<>(RegDate.get(2015, 4, 1), null, new NamedOrganisationId("CHE_GUEVARA", "DATA1")));
		rl.add(new DateRangeHelper.Ranged<>(RegDate.get(2015, 5, 2), null, new NamedOrganisationId("CHE_GUEVARA", "DATA2")));

		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("Found overlapping range in list for key CHE_GUEVARA:");
		Map<String, List<DateRangeHelper.Ranged<String>>> NamedOrganisationIdsMap = MultivalueListConverter.toMapOfListsOfDateRangedValues(rl, NamedOrganisationId::getOrganisationIdCategory, NamedOrganisationId::getOrganisationId);
	}
}