package ch.vd.unireg.evenement.retourdi.pm;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.tiers.LocalisationFiscale;
import ch.vd.unireg.type.TypeAutoriteFiscale;

public class LocalisationTest extends BusinessTest {

	@Test
	public void testTranscriptionSaisieLibreVide() {
		final Localisation localisation = new Localisation.SaisieLibre("   ");
		Assert.assertNull(localisation.transcriptionFiscale(serviceInfra, RegDate.get()));
	}

	@Test
	public void testTranscriptionSaisieLibreValideVaudoise() {
		final Localisation localisation = new Localisation.SaisieLibre("BEX");
		final LocalisationFiscale lf = localisation.transcriptionFiscale(serviceInfra, RegDate.get());
		Assert.assertNotNull(lf);
		Assert.assertEquals((Integer) MockCommune.Bex.getNoOFS(), lf.getNumeroOfsAutoriteFiscale());
		Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, lf.getTypeAutoriteFiscale());
	}

	@Test
	public void testTranscriptionSaisieLibreValideHorsCanton() {
		final Localisation localisation = new Localisation.SaisieLibre("BerN");
		final LocalisationFiscale lf = localisation.transcriptionFiscale(serviceInfra, RegDate.get());
		Assert.assertNotNull(lf);
		Assert.assertEquals((Integer) MockCommune.Bern.getNoOFS(), lf.getNumeroOfsAutoriteFiscale());
		Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_HC, lf.getTypeAutoriteFiscale());
	}

	@Test
	public void testTranscriptionSaisieLibreValidePays() {
		final Localisation localisation = new Localisation.SaisieLibre("FrAnCe");
		final LocalisationFiscale lf = localisation.transcriptionFiscale(serviceInfra, RegDate.get());
		Assert.assertNull(lf);      // on ne cherche que dans les communes suisses, désolé...
	}

	@Test
	public void testTranscriptionSaisieLibreInconnu() {
		final Localisation localisation = new Localisation.SaisieLibre("Lozane");
		final LocalisationFiscale lf = localisation.transcriptionFiscale(serviceInfra, RegDate.get());
		Assert.assertNull(lf);      // commune inconnue en Suisse...
	}

	@Test
	public void testTranscriptionCommuneVaudoise() {
		final Localisation localisation = new Localisation.CommuneSuisse(MockCommune.Aigle.getNoOFS());
		final LocalisationFiscale lf = localisation.transcriptionFiscale(serviceInfra, RegDate.get());
		Assert.assertNotNull(lf);
		Assert.assertEquals((Integer) MockCommune.Aigle.getNoOFS(), lf.getNumeroOfsAutoriteFiscale());
		Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, lf.getTypeAutoriteFiscale());
	}

	@Test
	public void testTranscriptionCommuneHorsCanton() {
		final Localisation localisation = new Localisation.CommuneSuisse(MockCommune.Neuchatel.getNoOFS());
		final LocalisationFiscale lf = localisation.transcriptionFiscale(serviceInfra, RegDate.get());
		Assert.assertNotNull(lf);
		Assert.assertEquals((Integer) MockCommune.Neuchatel.getNoOFS(), lf.getNumeroOfsAutoriteFiscale());
		Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_HC, lf.getTypeAutoriteFiscale());
	}

	@Test
	public void testTranscriptionCommuneInconnue() {
		final Localisation localisation = new Localisation.CommuneSuisse(-1);
		final LocalisationFiscale lf = localisation.transcriptionFiscale(serviceInfra, RegDate.get());
		Assert.assertNull(lf);
	}

	@Test
	public void testTranscriptionPaysValide() {
		final Localisation localisation = new Localisation.Etranger(MockPays.Kosovo.getNoOFS(), "Priština");
		final LocalisationFiscale lf = localisation.transcriptionFiscale(serviceInfra, RegDate.get());
		Assert.assertNotNull(lf);
		Assert.assertEquals((Integer) MockPays.Kosovo.getNoOFS(), lf.getNumeroOfsAutoriteFiscale());
		Assert.assertEquals(TypeAutoriteFiscale.PAYS_HS, lf.getTypeAutoriteFiscale());
	}

	@Test
	public void testTranscriptionPaysInconnu() {
		final Localisation localisation = new Localisation.Etranger(8000, "Où ça?");
		final LocalisationFiscale lf = localisation.transcriptionFiscale(serviceInfra, RegDate.get());
		Assert.assertNull(lf);
	}

	@Test
	public void testDisplayStringLibreCommuneVaudoise() {
		final Localisation localisation = new Localisation.SaisieLibre("EchaLLeNs");
		Assert.assertEquals("Echallens (VD)", localisation.toDisplayString(serviceInfra, RegDate.get()));
	}

	@Test
	public void testDisplayStringLibreCommuneHorsCanton() {
		final Localisation localisation = new Localisation.SaisieLibre("Neuchatel");
		Assert.assertEquals("Neuchâtel (NE)", localisation.toDisplayString(serviceInfra, RegDate.get()));
	}
}
