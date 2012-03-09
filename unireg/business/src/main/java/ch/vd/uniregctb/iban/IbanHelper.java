package ch.vd.uniregctb.iban;

public class IbanHelper {
	public static String removeSpaceAndDoUpperCase(String iban){
		if (iban==null) {
			return null;
		}
		return iban.toUpperCase().replace(" ","");

	}
}
