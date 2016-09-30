package ch.vd.unireg.interfaces.infra.data;

public interface CollectiviteAdministrativeUtilisateur extends CollectiviteAdministrative {

	/**
	 * @return <code>true</code> si la collectivité administrative est la collectivité par défaut pour l'utilisateur pour laquelle elle a été demandée
	 */
	boolean isCollectiviteParDefaut();

}
