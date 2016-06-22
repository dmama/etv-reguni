package ch.vd.unireg.wsclient.host.interfaces;


import ch.vd.infrastructure.model.rest.Canton;
import ch.vd.infrastructure.model.rest.CollectiviteAdministrative;
import ch.vd.infrastructure.model.rest.Commune;
import ch.vd.infrastructure.model.rest.ListeCantons;
import ch.vd.infrastructure.model.rest.ListeCollectiviteAdministrative;
import ch.vd.infrastructure.model.rest.ListeCommunes;
import ch.vd.infrastructure.model.rest.ListeLocalites;
import ch.vd.infrastructure.model.rest.ListePays;
import ch.vd.infrastructure.model.rest.ListeRues;
import ch.vd.infrastructure.model.rest.ListeTypesCollectivite;
import ch.vd.infrastructure.model.rest.ListeTypesCommunicationPourTier;
import ch.vd.infrastructure.model.rest.Localite;
import ch.vd.infrastructure.model.rest.Pays;
import ch.vd.infrastructure.model.rest.Rue;
import ch.vd.infrastructure.model.rest.TypeCollectivite;
import ch.vd.infrastructure.model.rest.TypeCommunicationPourTier;

public interface ServiceInfrastructureClient {
	ListeCantons getCantons(Pays var1) throws ServiceInfrastructureClientException;

	ListeCantons getCantons() throws ServiceInfrastructureClientException;

	CollectiviteAdministrative getCollectivite(int var1) throws ServiceInfrastructureClientException;

	Localite getLocalite(int var1) throws ServiceInfrastructureClientException;

	ListeLocalites getLocalites(Canton var1) throws ServiceInfrastructureClientException;

	ListeLocalites getLocalites(String var1) throws ServiceInfrastructureClientException;

	Rue getRueByNumero(Integer var1) throws ServiceInfrastructureClientException;

	ListeRues getRues(String var1, int var2) throws ServiceInfrastructureClientException;

	ListeRues getRues(int var1) throws ServiceInfrastructureClientException;

	ListeRues getRues(Canton var1) throws ServiceInfrastructureClientException;

	ListeRues getRues(String var1) throws ServiceInfrastructureClientException;

	CollectiviteAdministrative getCollectivite(String var1) throws ServiceInfrastructureClientException;

	ListeCollectiviteAdministrative getCollectivitesAdministratives(String var1) throws ServiceInfrastructureClientException;

	ListeCollectiviteAdministrative getCollectivitesAdministratives(TypeCollectivite[] var1) throws ServiceInfrastructureClientException;

	ListeCollectiviteAdministrative getCollectivitesAdministrativesPourTypeCommunication(TypeCommunicationPourTier var1) throws ServiceInfrastructureClientException;

	Commune getCommune(int var1) throws ServiceInfrastructureClientException;

	Commune getCommuneById(String var1) throws ServiceInfrastructureClientException;

	ListeCommunes getCommunes(Canton var1) throws ServiceInfrastructureClientException;

	ListeCommunes getCommunes(String var1) throws ServiceInfrastructureClientException;

	ListePays getListePays() throws ServiceInfrastructureClientException;

	CollectiviteAdministrative getOidDeCommune(int var1) throws ServiceInfrastructureClientException;

	ListeTypesCollectivite getTypesCollectivites() throws ServiceInfrastructureClientException;

	ListeTypesCommunicationPourTier getTypesCommunicationPourTier() throws ServiceInfrastructureClientException;

	String ping() throws ServiceInfrastructureClientException;
}
