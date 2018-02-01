package ch.vd.unireg.etiquette;

import java.util.List;

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
	public List<Etiquette> getAllEtiquettes(boolean doNotAutoflush) {
		return etiquetteDAO.getAll(doNotAutoflush);
	}

	@Override
	public Etiquette getEtiquette(long id) {
		return etiquetteDAO.get(id);
	}

	@Override
	public Etiquette getEtiquette(String code) {
		return etiquetteDAO.getByCode(code);
	}

	@Override
	public EtiquetteTiers getEtiquetteTiers(long id) {
		return etiquetteTiersDAO.get(id);
	}
}
