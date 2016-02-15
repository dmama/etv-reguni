package ch.vd.unireg.interfaces.organisation.rcent;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.evd0022.v3.Capital;
import ch.vd.evd0022.v3.CommercialRegisterStatus;
import ch.vd.evd0022.v3.LegalForm;
import ch.vd.evd0022.v3.TypeOfLocation;
import ch.vd.evd0022.v3.UidRegisterStatus;
import ch.vd.registre.base.date.DateRangeHelper.Ranged;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockServiceInfrastructureService;
import ch.vd.unireg.interfaces.organisation.data.SiteOrganisation;
import ch.vd.unireg.interfaces.organisation.data.StatusInscriptionRC;
import ch.vd.unireg.interfaces.organisation.data.StatusRegistreIDE;
import ch.vd.unireg.interfaces.organisation.data.TypeDeSite;
import ch.vd.uniregctb.adapter.rcent.model.OrganisationFunction;
import ch.vd.uniregctb.adapter.rcent.model.OrganisationLocation;

/**
 * @author Raphaël Marmier, 2015-11-05
 */
public class RCEntSiteOrganisationHelperTest {

	private final MockServiceInfrastructureService serviceInfra = new MockServiceInfrastructureService() {
		@Override
		protected void init() {
			add(MockCommune.Lausanne);
		}
	};

	@Test
	public void conversionSiteSimple() throws Exception {

		final RegDate refDate = RegDate.get(2015, 6, 1);

		final List<Ranged<String>> nom = new ArrayList<>(1);
		nom.add(new Ranged<>(refDate, null, "Ma boîte"));

		final Map<String, List<Ranged<String>>> identifiers = new HashMap<>();
		final List<Ranged<String>> additionalName = new ArrayList<>();
		final List<Ranged<TypeOfLocation>> typeOfLocation = new ArrayList<>();
		typeOfLocation.add(new Ranged<>(refDate, null, TypeOfLocation.ETABLISSEMENT_PRINCIPAL));
		final List<Ranged<Integer>> municipality = new ArrayList<>();
		municipality.add(new Ranged<>(refDate, null, MockCommune.Lausanne.getNoOFS()));
		final List<Ranged<LegalForm>> legalForm = new ArrayList<>();
		legalForm.add(new Ranged<>(refDate, null, LegalForm.N_0106_SOCIETE_ANONYME));
		final Map<String, List<Ranged<OrganisationFunction>>> function = new HashMap<>();
		final List<Ranged<Long>> replacedBy = new ArrayList<>();
		final List<Ranged<Long>> inReplacementOf = new ArrayList<>();

		final List<Ranged<CommercialRegisterStatus>> statusInscription = new ArrayList<>(1);
		statusInscription.add(new Ranged<>(refDate, null, CommercialRegisterStatus.ACTIF));
		final List<Ranged<Capital>> capital = new ArrayList<>(1);

		// Capital libéré correctement réglés, les autres champs sont optionels jusqu'à preuve du contraire.
		capital.add(new Ranged<>(refDate, null, new Capital(null, null, null, BigDecimal.valueOf(100000), null)));

		final OrganisationLocation.RCEntRCData rc = new OrganisationLocation.RCEntRCData(statusInscription, capital, null, null, null, null, null);

		final List<Ranged<UidRegisterStatus>> statusIde = new ArrayList<>();
		statusIde.add(new Ranged<>(refDate, null, UidRegisterStatus.DEFINITIF));
		final OrganisationLocation.RCEntUIDData uid = new OrganisationLocation.RCEntUIDData(null, statusIde, null, null, null);

		final OrganisationLocation loc = new OrganisationLocation(4567, nom, rc, uid, identifiers, additionalName, typeOfLocation, legalForm, municipality, function, null, null, replacedBy, inReplacementOf);

		// Conversion
		final SiteOrganisation site = RCEntSiteOrganisationHelper.get(loc, serviceInfra);

		Assert.assertEquals(4567, site.getNumeroSite());
		Assert.assertEquals("Ma boîte", site.getNom().get(0).getPayload());
		Assert.assertEquals(TypeDeSite.ETABLISSEMENT_PRINCIPAL, site.getTypeDeSite().get(0).getPayload());
		Assert.assertEquals(MockCommune.Lausanne.getNoOFS(), site.getDomiciles().get(0).getNoOfs());
		Assert.assertEquals(StatusInscriptionRC.ACTIF, site.getDonneesRC().getStatusInscription().get(0).getPayload());
		Assert.assertEquals(StatusRegistreIDE.DEFINITIF, site.getDonneesRegistreIDE().getStatus().get(0).getPayload());

		Assert.assertEquals(100000L, site.getDonneesRC().getCapital().get(0).getCapitalLibere().longValue());
	}

	@Test
	public void conversionSiteCapitalSansMontantLibere() throws Exception {

		final RegDate refDate = RegDate.get(2015, 6, 1);

		final List<Ranged<String>> nom = new ArrayList<>(1);
		nom.add(new Ranged<>(refDate, null, "Ma boîte"));

		final Map<String, List<Ranged<String>>> identifiers = new HashMap<>();
		final List<Ranged<String>> additionalName = new ArrayList<>();
		final List<Ranged<TypeOfLocation>> typeOfLocation = new ArrayList<>();
		typeOfLocation.add(new Ranged<>(refDate, null, TypeOfLocation.ETABLISSEMENT_PRINCIPAL));
		final List<Ranged<Integer>> municipality = new ArrayList<>();
		municipality.add(new Ranged<>(refDate, null, MockCommune.Lausanne.getNoOFS()));
		final List<Ranged<LegalForm>> legalForm = new ArrayList<>();
		legalForm.add(new Ranged<>(refDate, null, LegalForm.N_0106_SOCIETE_ANONYME));
		final Map<String, List<Ranged<OrganisationFunction>>> function = new HashMap<>();
		final List<Ranged<Long>> replacedBy = new ArrayList<>();
		final List<Ranged<Long>> inReplacementOf = new ArrayList<>();

		final List<Ranged<CommercialRegisterStatus>> statusInscription = new ArrayList<>(1);
		statusInscription.add(new Ranged<>(refDate, null, CommercialRegisterStatus.ACTIF));
		final List<Ranged<Capital>> capital = new ArrayList<>(1);

		// Capital libéré et devise correctement réglés
		capital.add(new Ranged<>(refDate, null, new Capital(null, null, BigDecimal.valueOf(100000), null, null)));

		final OrganisationLocation.RCEntRCData rc = new OrganisationLocation.RCEntRCData(statusInscription, capital, null, null, null, null, null);

		final List<Ranged<UidRegisterStatus>> statusIde = new ArrayList<>();
		statusIde.add(new Ranged<>(refDate, null, UidRegisterStatus.DEFINITIF));
		final OrganisationLocation.RCEntUIDData uid = new OrganisationLocation.RCEntUIDData(null, statusIde, null, null, null);

		final OrganisationLocation loc = new OrganisationLocation(4567, nom, rc, uid, identifiers, additionalName, typeOfLocation, legalForm, municipality, function, null, null, replacedBy, inReplacementOf);

		// Conversion
		final SiteOrganisation site = RCEntSiteOrganisationHelper.get(loc, serviceInfra);

		Assert.assertEquals(4567, site.getNumeroSite());
		Assert.assertEquals("Ma boîte", site.getNom().get(0).getPayload());
		Assert.assertEquals(TypeDeSite.ETABLISSEMENT_PRINCIPAL, site.getTypeDeSite().get(0).getPayload());
		Assert.assertEquals(MockCommune.Lausanne.getNoOFS(), site.getDomiciles().get(0).getNoOfs());
		Assert.assertEquals(StatusInscriptionRC.ACTIF, site.getDonneesRC().getStatusInscription().get(0).getPayload());
		Assert.assertEquals(StatusRegistreIDE.DEFINITIF, site.getDonneesRegistreIDE().getStatus().get(0).getPayload());

		Assert.assertTrue(site.getDonneesRC().getCapital().isEmpty());
	}
}