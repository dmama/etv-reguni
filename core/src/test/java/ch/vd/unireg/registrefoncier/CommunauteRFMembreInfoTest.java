package ch.vd.unireg.registrefoncier;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.tiers.CommunauteHeritiers;
import ch.vd.unireg.tiers.Heritage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class CommunauteRFMembreInfoTest {

	/**
	 * [SIFISC-28067] Ce test vérifie que la méthode 'applyHeritages' met bien à jour les listes d'ids de CTB et l'historique des appartenances des membres de la communauté.
	 */
	@Test
	public void testApplyHeritages() {

		final long bluetteId = 2727272L;
		final long robertId = 473727L;
		final long heatherId = 717171L;
		final long bryanId = 8666939L;

		final PersonnePhysiqueRF arnoldRF = new PersonnePhysiqueRF();
		arnoldRF.setId(1L);
		arnoldRF.setDateNaissance(RegDate.get(1922, 3, 23));
		arnoldRF.setPrenom("Arnold");
		arnoldRF.setNom("Whitenegger");

		final PersonnePhysiqueRF jackRF = new PersonnePhysiqueRF();
		jackRF.setId(2L);
		jackRF.setDateNaissance(RegDate.get(1956, 9, 3));
		jackRF.setPrenom("Jack");
		jackRF.setNom("Nicole");

		final RegDate dateSuccession = RegDate.get(2000, 1, 1);
		final RegDate dateVente = RegDate.get(2005, 12, 31);
		final RegDate dateDeces = RegDate.get(2010, 1, 1);

		// le 01.01.2000, une communauté se créé avec Robert, Bluette et Arnold comme membres
		// le 31.12.2005, Bluette vend sa part à Jack
		CommunauteRFAppartenanceInfo appartenance1 = new CommunauteRFAppartenanceInfo(dateSuccession, null, null, null, robertId);
		CommunauteRFAppartenanceInfo appartenance2 = new CommunauteRFAppartenanceInfo(dateSuccession, dateVente.getOneDayBefore(), null, null, bluetteId);
		CommunauteRFAppartenanceInfo appartenance3 = new CommunauteRFAppartenanceInfo(dateSuccession, null, null, arnoldRF, null);
		CommunauteRFAppartenanceInfo appartenance4 = new CommunauteRFAppartenanceInfo(dateVente, null, null, jackRF, null);

		final CommunauteRFMembreInfo info = new CommunauteRFMembreInfo(Arrays.asList(bluetteId, robertId), // les membres rapprochés
		                                                               Arrays.asList(arnoldRF, jackRF), // les membres non-rapprochés
		                                                               Arrays.asList(appartenance1, appartenance2, appartenance3, appartenance4));

		// le 01.01.2010, Bluette et Robert décèdent dans un regrettable accident de jokari, leurs enfants Heather et Bryan sont leurs héritiers fiscaux
		final CommunauteHeritiers heritiersBluette = new CommunauteHeritiers(bluetteId, Arrays.asList(new Heritage(dateDeces, null, heatherId, bluetteId, true),
		                                                                                              new Heritage(dateDeces, null, bryanId, bluetteId, false)));
		final CommunauteHeritiers heritiersRobert = new CommunauteHeritiers(robertId, Arrays.asList(new Heritage(dateDeces, null, heatherId, robertId, true),
		                                                                                            new Heritage(dateDeces, null, bryanId, robertId, false)));
		final Map<Long, CommunauteHeritiers> communautesHeritiers = new HashMap<>();
		communautesHeritiers.put(bluetteId, heritiersBluette);
		communautesHeritiers.put(robertId, heritiersRobert);

		// on demande de calculer la nouvelle situation de la communauté
		final CommunauteRFMembreInfo infoPostHeritage = info.applyHeritages(communautesHeritiers);

		// Bluette et Robert ne devraient plus se trouver dans la liste des ids de contribuables (c'est discutable, à mon avis,
		// mais Carbo a demandé que ce soit fait comme ça), à la place, on devrait trouver leurs enfants
		final List<Long> ctbIds = infoPostHeritage.getCtbIds();
		ctbIds.sort(Comparator.naturalOrder());
		assertEquals(Arrays.asList(heatherId, bryanId), ctbIds);

		// Dans l'historique de l'appartenance des membres, on devrait avoir la situation suivante :
		final List<CommunauteRFAppartenanceInfo> membresHisto = infoPostHeritage.getMembresHisto();
		assertNotNull(membresHisto);
		assertEquals(6, membresHisto.size());
		// l'appartenance de Bluette n'est pas modifiée car elle avait vendu son droit avant son décès
		assertAppartenanceCtb(dateSuccession, dateVente.getOneDayBefore(), bluetteId, membresHisto.get(0));
		// l'appartenance de Robert se termine la veille de son décès
		assertAppartenanceCtb(dateSuccession, dateDeces.getOneDayBefore(), robertId, membresHisto.get(1));
		// l'appartenance d'Arnold n'est pas modifiée car il n'est pas décédé
		assertAppartenanceTiersRF(dateSuccession, null, arnoldRF, membresHisto.get(2));
		// l'appartenance de Jack n'est pas modifiée car il n'est pas décédé
		assertAppartenanceTiersRF(dateVente, null, jackRF, membresHisto.get(3));
		// les héritiers fiscaux de Robert apparaissent
		assertAppartenanceCtb(dateDeces, null, heatherId, membresHisto.get(4));
		assertAppartenanceCtb(dateDeces, null, bryanId, membresHisto.get(5));
	}

	private static void assertAppartenanceCtb(RegDate dateDebut, RegDate dateFin, Long ctbId, CommunauteRFAppartenanceInfo appartenance) {
		assertNotNull(appartenance);
		assertEquals(dateDebut, appartenance.getDateDebut());
		assertEquals(dateFin, appartenance.getDateFin());
		assertEquals(ctbId, appartenance.getCtbId());
		assertNull(appartenance.getAyantDroit());
	}

	private static void assertAppartenanceTiersRF(RegDate dateDebut, RegDate dateFin, TiersRF tiersRF, CommunauteRFAppartenanceInfo appartenance) {
		assertNotNull(appartenance);
		assertEquals(dateDebut, appartenance.getDateDebut());
		assertEquals(dateFin, appartenance.getDateFin());
		assertNull(appartenance.getCtbId());
		assertSame(tiersRF, appartenance.getAyantDroit());
	}
}