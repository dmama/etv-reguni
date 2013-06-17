package ch.vd.uniregctb.evenement.party.control;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public abstract class AbstractControlTaxliabilityTest extends BusinessTest{

	protected AssujettissementService assujettissementService;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		assujettissementService = getBean(AssujettissementService.class,"assujettissementService");
	}

	@Override
	public void onTearDown() throws Exception {
	}

	protected void assertListTiers(List<Long> expected,List<Long> actual){
		Collections.sort(expected);
		Collections.sort(actual);
		assertEquals(expected.size(),actual.size());
		for (int i = 0; i < expected.size() ; i++) {
			assertEquals(expected.get(i),actual.get(i));
		}
	}

	protected void assertControlNumeroKO(TaxLiabilityControlResult result) {
		final Long idTiersAssujetti = result.getIdTiersAssujetti();
		assertNull(idTiersAssujetti);
		assertNotNull(result.getEchec());
		assertEquals(TaxLiabilityControlEchec.EchecType.CONTROLE_NUMERO_KO, result.getEchec().getType());
	}
	protected void assertPasDeParent(TaxLiabilityControlResult result) {
		final Long idTiersAssujetti = result.getIdTiersAssujetti();
		assertNull(idTiersAssujetti);
		assertNotNull(result.getEchec());
		assertEquals(TaxLiabilityControlEchec.EchecType.CONTROLE_SUR_PARENTS_KO, result.getEchec().getType());
		assertNull(result.getEchec().getMenageCommunIds());
		assertNull(result.getEchec().getMenageCommunParentsIds());
		assertNull(result.getEchec().getParentsIds());
	}

	protected void assertUnParentNonAssujetti(Long idPere, TaxLiabilityControlResult result) {
		final Long idTiersAssujetti = result.getIdTiersAssujetti();
		assertNull(idTiersAssujetti);
		assertNotNull(result.getEchec());
		assertEquals(TaxLiabilityControlEchec.EchecType.CONTROLE_SUR_PARENTS_KO, result.getEchec().getType());
		assertEquals(idPere, result.getEchec().getParentsIds().get(0));
	}

	protected void assertTiersAssujetti(Long idTiers, TaxLiabilityControlResult result) {
		final Long idTiersAssujetti = result.getIdTiersAssujetti();
		assertNotNull(idTiersAssujetti);
		assertNull(result.getEchec());
		assertEquals(idTiers, idTiersAssujetti);
	}

	protected void assertUnParentWithMCNonAssujetti(Long idParent,Long idMenage, TaxLiabilityControlResult result) {
		final Long idTiersAssujetti = result.getIdTiersAssujetti();
		assertNull(idTiersAssujetti);
		assertNotNull(result.getEchec());
		assertEquals(TaxLiabilityControlEchec.EchecType.CONTROLE_SUR_PARENTS_KO, result.getEchec().getType());
		assertEquals(idMenage, result.getEchec().getMenageCommunParentsIds().get(0));
		assertEquals(idParent, result.getEchec().getParentsIds().get(0));
	}



	protected void assertDeuxParentsNonAssujettis(Long idPere,Long idMere, TaxLiabilityControlResult result) {
		final Long idTiersAssujetti = result.getIdTiersAssujetti();
		assertNull(idTiersAssujetti);
		assertNotNull(result.getEchec());
		assertEquals(TaxLiabilityControlEchec.EchecType.CONTROLE_SUR_PARENTS_KO, result.getEchec().getType());
		List<Long> expected = new ArrayList<Long>();
		expected.add(idPere);
		expected.add(idMere);
		final List<Long> parentsIds = result.getEchec().getParentsIds();
		assertEquals(2, parentsIds.size());
		assertListTiers(expected, parentsIds);
	}

	protected void assertDeuxParentUnMCNonAssujetti(Long idPere, Long idMere, Long idMenagePere, TaxLiabilityControlResult result) {
		final Long idTiersAssujetti = result.getIdTiersAssujetti();
		assertNull(idTiersAssujetti);
		assertNotNull(result.getEchec());
		assertEquals(TaxLiabilityControlEchec.EchecType.CONTROLE_SUR_PARENTS_KO, result.getEchec().getType());
		List<Long> expected = new ArrayList<Long>();
		expected.add(idPere);
		expected.add(idMere);
		final List<Long> parentsIds = result.getEchec().getParentsIds();
		assertEquals(2, parentsIds.size());
		assertListTiers(expected, parentsIds);
		final List<Long> menageCommunParentsIds = result.getEchec().getMenageCommunParentsIds();
		assertEquals(1, menageCommunParentsIds.size());
		assertEquals(idMenagePere, menageCommunParentsIds.get(0));
	}

	protected void assertDeuxParentsDeuxMCNonAssujetti(Long idPere,Long idMere, Long idMenagePere,Long idMenageMere, TaxLiabilityControlResult result) {
		assertDeuxPArentsWithDeuxMenagesFail(idPere, idMere, idMenagePere, idMenageMere, result);
	}

	protected void assertDeuxPArentsWithDeuxMenagesAssujetti(Long idPere,Long idMere, Long idMenagePere,Long idMenageMere, TaxLiabilityControlResult result) {
		assertDeuxPArentsWithDeuxMenagesFail(idPere, idMere, idMenagePere, idMenageMere, result);
	}

	protected void assertDeuxPArentsWithDeuxMenagesFail(Long idPere, Long idMere, Long idMenagePere, Long idMenageMere, TaxLiabilityControlResult result) {
		final Long idTiersAssujetti = result.getIdTiersAssujetti();
		assertNull(idTiersAssujetti);
		assertNotNull(result.getEchec());
		assertEquals(TaxLiabilityControlEchec.EchecType.CONTROLE_SUR_PARENTS_KO, result.getEchec().getType());
		List<Long> expectedParentIds = new ArrayList<Long>();
		expectedParentIds.add(idPere);
		expectedParentIds.add(idMere);
		final List<Long> parentsIds = result.getEchec().getParentsIds();
		assertEquals(2, parentsIds.size());
		assertListTiers(expectedParentIds, parentsIds);

		List<Long> expectedMenageParentIds = new ArrayList<Long>();
		expectedMenageParentIds.add(idMenagePere);
		expectedMenageParentIds.add(idMenageMere);
		final List<Long> menageParentsIds = result.getEchec().getMenageCommunParentsIds();
		assertEquals(2, menageParentsIds.size());
		assertListTiers(expectedMenageParentIds, menageParentsIds);
	}

	protected void assertDeuxParentsWithUnMCNonAssujetti(Long idPere,Long idMere,Long idMenage, TaxLiabilityControlResult result) {
		final Long idTiersAssujetti = result.getIdTiersAssujetti();
		assertNull(idTiersAssujetti);
		assertNotNull(result.getEchec());
		assertEquals(TaxLiabilityControlEchec.EchecType.CONTROLE_SUR_PARENTS_KO, result.getEchec().getType());
		List<Long> expected = new ArrayList<>();
		expected.add(idPere);
		expected.add(idMere);
		final List<Long> parentsIds = result.getEchec().getParentsIds();
		assertEquals(2, parentsIds.size());
		assertEquals(idMenage, result.getEchec().getMenageCommunParentsIds().get(0));
	}
}
