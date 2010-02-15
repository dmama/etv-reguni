package ch.vd.uniregctb.interfaces.utils;

import org.springframework.util.Assert;

import ch.vd.registre.fiscal.model.Contribuable;

import com.thoughtworks.xstream.XStream;

public class ContribuableComparator {

	XStream xstream = new XStream();;

	public  void compare(Contribuable contribuableHost, Contribuable contribuableUnireg) {
		String difference = null;
		String message = "";
		message = "le numéro du contribuable";
		Assert.isTrue(contribuableHost.getNoContribuable() == contribuableUnireg.getNoContribuable(), message);
		message = "le type de contribuable";
		Assert.isTrue(contribuableHost.getTypeContribuable().equals(contribuableUnireg.getTypeContribuable()), message);
		message = "la formule de politesse";
		Assert.isTrue(contribuableHost.getFormuleDePolitesse().equals(contribuableUnireg.getFormuleDePolitesse()), message);
		message = "le nom courrier 1";
		//Assert.isTrue(contribuableHost.getNomCourrier1().equals(contribuableUnireg.getNomCourrier1()), message);
		message = "le nom courrier 2";
		if (contribuableHost.getNomCourrier2() != null) {
		Assert.isTrue(contribuableHost.getNomCourrier2().equals(contribuableUnireg.getNomCourrier2()), message);
		}
		message = "le code de blocage du remboursement automatique";
		Assert.isTrue(contribuableHost.getCodeBlocageRmbtAuto() == contribuableUnireg.getCodeBlocageRmbtAuto(), message);
		message = "le numero de téléphone fixe";
		if (contribuableHost.getNumeroTelephoniqueFixe() != null) {
			Assert.isTrue(contribuableHost.getNumeroTelephoniqueFixe().equals(contribuableUnireg.getNumeroTelephoniqueFixe()), message);
		}

		message = "le numero de téléphone portable";
		if (contribuableHost.getNumeroTelephoniquePortable() != null) {
			Assert.isTrue(contribuableHost.getNumeroTelephoniquePortable().equals(contribuableUnireg.getNumeroTelephoniquePortable()),
					message);
		}
		message = "l'email";
		if (contribuableHost.getEmail() != null) {
			Assert.isTrue(contribuableHost.getEmail().equals(contribuableUnireg.getEmail()), message);
		}

		/** Adresse*/
		message = "l'adresse du contribuable";
		Assert.isTrue(compareContribuableObject(contribuableHost.getAdresse(),contribuableUnireg.getAdresse()), message);
	}

	private  boolean compareContribuableObject(Object host, Object unireg){

		return  xstream.toXML(host).toLowerCase().equals(xstream.toXML(unireg).toLowerCase());

	}
}
