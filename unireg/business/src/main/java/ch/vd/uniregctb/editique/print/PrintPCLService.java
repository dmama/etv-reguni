package ch.vd.uniregctb.editique.print;

import java.io.InputStream;

import ch.vd.securite.model.ProfilOperateur;

public interface PrintPCLService {

	 void printLocalStream(ProfilOperateur profilOperateur, InputStream inputStream) throws PrintPCLException ;

}
