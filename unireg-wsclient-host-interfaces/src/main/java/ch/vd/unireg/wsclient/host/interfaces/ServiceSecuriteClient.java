package ch.vd.unireg.wsclient.host.interfaces;


import ch.vd.infrastructure.model.rest.ListeCollectiviteAdministrative;
import ch.vd.infrastructure.model.rest.TypeCollectivite;
import ch.vd.securite.model.rest.ListeOperateurs;
import ch.vd.securite.model.rest.Operateur;
import ch.vd.securite.model.rest.ProfilOperateur;

public interface ServiceSecuriteClient {
	ListeCollectiviteAdministrative getCollectivitesUtilisateur(String var1) throws SecuriteException;

	ListeCollectiviteAdministrative getCollectivitesUtilisateurCommunicationTier(String var1) throws SecuriteException;

	ProfilOperateur getProfileUtilisateur(String var1, int var2) throws SecuriteException;

	ListeOperateurs getOperateurs(TypeCollectivite[] var1) throws SecuriteException;

	Operateur getOperateur(long var1) throws SecuriteException;

	Operateur getOperateurTous(long var1) throws SecuriteException;

	Operateur getOperateur(String var1) throws SecuriteException;

	Operateur getOperateurTous(String var1) throws SecuriteException;

	String ping() throws SecuriteException;
}
