package ch.vd.uniregctb.etiquette;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.tiers.CollectiviteAdministrative;

public class EtiquetteServiceImpl implements EtiquetteService {

	private EtiquetteDAO etiquetteDAO;
	private EtiquetteTiersDAO etiquetteTiersDAO;

	public void setEtiquetteDAO(EtiquetteDAO etiquetteDAO) {
		this.etiquetteDAO = etiquetteDAO;
	}

	public void setEtiquetteTiersDAO(EtiquetteTiersDAO etiquetteTiersDAO) {
		this.etiquetteTiersDAO = etiquetteTiersDAO;
	}

	@Override
	public List<Etiquette> getAllEtiquettes() {
		return etiquetteDAO.getAll();
	}

	@Override
	public Etiquette getEtiquette(long id) {
		return etiquetteDAO.get(id);
	}

	@Override
	public Etiquette newEtiquette(String code, String libelle, @Nullable CollectiviteAdministrative collectiviteAdministrative) {
		final Etiquette etiquette = new Etiquette(code, libelle, collectiviteAdministrative);
		return etiquetteDAO.save(etiquette);
	}
}
