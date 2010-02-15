package ch.vd.uniregctb.mouvement.manager;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.mouvement.MouvementDossier;

public class AntiChronologiqueMouvementComparatorTest {

	private AntiChronologiqueMouvementComparator comparator;

	private final RegDate aujourdhui = RegDate.get();
	private final RegDate hier = aujourdhui.getOneDayBefore();
	private final RegDate demain = aujourdhui.getOneDayAfter();

	@Before
	public void setUp() throws Exception {
		comparator = new AntiChronologiqueMouvementComparator();
	}

	/**
	 * Crée un mouvement de dossier bidon qui contient les
	 * valeurs passées en paramètres
 	 */
	private static MouvementDossier createMouvement(final boolean annule, final RegDate dateMouvement, final Timestamp modifDate) {

		return new MouvementDossier() {
			@Override
			public boolean isAnnule() {
				return annule;
			}

			@Override
			public RegDate getDateMouvement() {
				return dateMouvement;
			}

			@Override
			public Timestamp getLogModifDate() {
				return modifDate;
			}
		};
	}

	private static Timestamp getTimestamp(RegDate date, int hours, int minutes, int seconds) {
		final Date javaDate = date.asJavaDate();
		return new Timestamp(javaDate.getTime() + (seconds + 60 * (minutes + 60 * hours)) * 1000L);
	}

	@Test
	public void testTriDateMvts() {
		doTestTriDateMvts(false);
	}

	@Test
	public void testTriDateMvtsTousAnnules() {
		doTestTriDateMvts(true);
	}

	private void doTestTriDateMvts(boolean annule) {
		final Timestamp modifTimestamp = getTimestamp(hier, 0, 0, 0);
		final MouvementDossier debut = createMouvement(annule, hier, modifTimestamp);
		final MouvementDossier milieu = createMouvement(annule, aujourdhui, modifTimestamp);
		final MouvementDossier fin = createMouvement(annule, demain, modifTimestamp);

		final List<MouvementDossier> liste = new ArrayList<MouvementDossier>(3);
		liste.add(milieu);
		liste.add(fin);
		liste.add(debut);
		Collections.sort(liste, comparator);
		Assert.assertEquals(3, liste.size());
		Assert.assertTrue(fin == liste.get(0));
		Assert.assertTrue(milieu == liste.get(1));
		Assert.assertTrue(debut == liste.get(2));
	}

	@Test
	public void testTriDateModification() {
		doTestTriDateModification(false);
	}

	@Test
	public void testTriDateModificationTousAnnules() {
		doTestTriDateModification(true);
	}

	private void doTestTriDateModification(boolean annule) {
		final MouvementDossier debut = createMouvement(annule, aujourdhui, getTimestamp(hier, 0, 0, 0));
		final MouvementDossier milieu = createMouvement(annule, aujourdhui, getTimestamp(aujourdhui, 0, 0, 0));
		final MouvementDossier fin = createMouvement(annule, aujourdhui, getTimestamp(demain, 0, 0, 0));

		final List<MouvementDossier> liste = new ArrayList<MouvementDossier>(3);
		liste.add(milieu);
		liste.add(fin);
		liste.add(debut);
		Collections.sort(liste, comparator);
		Assert.assertEquals(3, liste.size());
		Assert.assertTrue(fin == liste.get(0));
		Assert.assertTrue(milieu == liste.get(1));
		Assert.assertTrue(debut == liste.get(2));
	}

	@Test
	public void testTriAnnuleVsNonAnnule() {
		final MouvementDossier nonAnnule = createMouvement(false, aujourdhui, getTimestamp(aujourdhui, 0, 0, 0));
		final MouvementDossier annule = createMouvement(true, demain, getTimestamp(aujourdhui, 0, 0, 0));

		final List<MouvementDossier> liste = new ArrayList<MouvementDossier>(2);
		liste.add(annule);
		liste.add(nonAnnule);
		Collections.sort(liste, comparator);
		Assert.assertEquals(2, liste.size());
		Assert.assertTrue(nonAnnule == liste.get(0));
		Assert.assertTrue(annule == liste.get(1));
	}
}
