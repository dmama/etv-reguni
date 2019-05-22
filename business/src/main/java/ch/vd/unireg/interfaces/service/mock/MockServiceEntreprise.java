package ch.vd.unireg.interfaces.service.mock;

import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.entreprise.EntrepriseConnector;
import ch.vd.unireg.interfaces.entreprise.EntrepriseConnectorException;
import ch.vd.unireg.interfaces.entreprise.data.AnnonceIDE;
import ch.vd.unireg.interfaces.entreprise.data.AnnonceIDEQuery;
import ch.vd.unireg.interfaces.entreprise.data.BaseAnnonceIDE;
import ch.vd.unireg.interfaces.entreprise.data.EntrepriseCivile;
import ch.vd.unireg.interfaces.entreprise.data.EntrepriseCivileEvent;
import ch.vd.unireg.interfaces.entreprise.data.EtablissementCivil;
import ch.vd.unireg.interfaces.model.AdressesCivilesHisto;
import ch.vd.unireg.interfaces.service.ServiceEntreprise;

/**
 * Mock du service entreprise civile qui par défaut ne fait rien et retourne toujours null.
 */
public class MockServiceEntreprise implements ServiceEntreprise {
	@Override
	public EntrepriseCivile getEntrepriseHistory(long noEntreprise) throws EntrepriseConnectorException {
		return null;
	}

	@Override
	public Map<Long, EntrepriseCivileEvent> getEntrepriseEvent(long noEvenement) throws EntrepriseConnectorException {
		return null;
	}

	@Override
	public EntrepriseConnector.Identifiers getEntrepriseIdsByNoIde(String noide) throws EntrepriseConnectorException {
		return null;
	}

	@Override
	public Long getNoEntrepriseCivileFromNoEtablissementCivil(Long noEtablissement) throws EntrepriseConnectorException {
		return null;
	}

	@Override
	public AdressesCivilesHisto getAdressesEntrepriseHisto(long noEntrepriseCivile) throws EntrepriseConnectorException {
		return null;
	}

	@Nullable
	@Override
	public AdressesCivilesHisto getAdressesEtablissementCivilHisto(long noEtablissementCivil) throws EntrepriseConnectorException {
		return null;
	}

	@Nullable
	@Override
	public AnnonceIDE getAnnonceIDE(long numero, @NotNull String userId) {
		return null;
	}

	@NotNull
	@Override
	public Page<AnnonceIDE> findAnnoncesIDE(@NotNull AnnonceIDEQuery query, @Nullable Sort.Order order, int pageNumber, int resultsPerPage) throws
			EntrepriseConnectorException {
		return null;
	}

	@Override
	public BaseAnnonceIDE.Statut validerAnnonceIDE(BaseAnnonceIDE annonceIDE) {
		return null;
	}

	@NotNull
	@Override
	public String createEntrepriseDescription(EntrepriseCivile entrepriseCivile, RegDate date) {
		return null;
	}

	@Override
	public String afficheAttributsEtablissement(@Nullable EtablissementCivil etablissement, @Nullable RegDate date) {
		return null;
	}
}
