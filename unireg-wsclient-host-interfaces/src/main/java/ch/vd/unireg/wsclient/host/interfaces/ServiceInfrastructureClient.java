package ch.vd.unireg.wsclient.host.interfaces;


import ch.vd.infrastructure.model.rest.CollectiviteAdministrative;
import ch.vd.infrastructure.model.rest.ListeCollectiviteAdministrative;
import ch.vd.infrastructure.model.rest.ListeTypesCollectivite;
import ch.vd.infrastructure.model.rest.Rue;
import ch.vd.infrastructure.model.rest.TypeCollectivite;
import ch.vd.infrastructure.model.rest.TypeCommunicationPourTier;
import ch.vd.infrastructure.registre.common.model.rest.InstitutionFinanciere;
import ch.vd.infrastructure.registre.common.model.rest.ListeInstitutionsFinancieres;

public interface ServiceInfrastructureClient {

	CollectiviteAdministrative getCollectivite(int var1) throws ServiceInfrastructureClientException;


	Rue getRueByNumero(Integer var1) throws ServiceInfrastructureClientException;

	CollectiviteAdministrative getCollectivite(String var1) throws ServiceInfrastructureClientException;

	ListeCollectiviteAdministrative getCollectivitesAdministratives(String var1) throws ServiceInfrastructureClientException;

	ListeCollectiviteAdministrative getCollectivitesAdministratives(TypeCollectivite[] var1) throws ServiceInfrastructureClientException;

	ListeCollectiviteAdministrative getCollectivitesAdministrativesPourTypeCommunication(TypeCommunicationPourTier var1) throws ServiceInfrastructureClientException;

	CollectiviteAdministrative getOidDeCommune(int var1) throws ServiceInfrastructureClientException;

	ListeTypesCollectivite getTypesCollectivites() throws ServiceInfrastructureClientException;

	InstitutionFinanciere getInstitutionFinanciere(int id) throws ServiceInfrastructureClientException;

	ListeInstitutionsFinancieres getInstitutionsFinancieres(String noClearing) throws ServiceInfrastructureClientException;

	String ping() throws ServiceInfrastructureClientException;
}
