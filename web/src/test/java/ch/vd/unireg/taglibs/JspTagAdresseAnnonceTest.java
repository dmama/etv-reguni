package ch.vd.unireg.taglibs;

import org.junit.Test;

import ch.vd.unireg.interfaces.organisation.data.AdresseAnnonceIDERCEnt;
import ch.vd.unireg.annonceIDE.AdresseAnnonceIDEView;

import static org.junit.Assert.assertEquals;

public class JspTagAdresseAnnonceTest {

	@Test
	public void testBuildHtmlAdresseNull() throws Exception {

		JspTagAdresseAnnonce tag = new JspTagAdresseAnnonce();
		tag.setAdresse(null);
		assertEquals("", tag.buildHtml());
	}

	@Test
	public void testBuildHtmlAdresseVide() throws Exception {

		JspTagAdresseAnnonce tag = new JspTagAdresseAnnonce();
		tag.setAdresse(new AdresseAnnonceIDEView(null, null, null, null));
		assertEquals("", tag.buildHtml());
	}

	@Test
	public void testBuildHtmlAdressePartiellementRemplie() throws Exception {

		JspTagAdresseAnnonce tag = new JspTagAdresseAnnonce();
		tag.setAdresse(new AdresseAnnonceIDEView("rue de la spécification complète", "12.3 revision 130", 1000, "Lausanne"));
		assertEquals("rue de la sp&eacute;cification compl&egrave;te 12.3 revision 130<br>" +
				             "1000 Lausanne", tag.buildHtml());
	}

	@Test
	public void testBuildHtmlAdresseComplete() throws Exception {

		JspTagAdresseAnnonce tag = new JspTagAdresseAnnonce();
		final AdresseAnnonceIDEView adresse = new AdresseAnnonceIDEView("rue", "12bis", 1000, "Lausanne");
		adresse.setNumeroAppartement("404");
		adresse.setTexteCasePostale("Case postale");
		adresse.setNumeroCasePostale(33);
		adresse.setEgid(1234567);
		adresse.setPays(new AdresseAnnonceIDERCEnt.PaysRCEnt(8100, "CH", "Suisse"));

		tag.setAdresse(adresse);
		assertEquals("rue 12bis<br>" +
				             "App: 404<br>" +
				             "Case postale 33<br>" +
				             "1000 Lausanne<br>Suisse", tag.buildHtml());
	}
}