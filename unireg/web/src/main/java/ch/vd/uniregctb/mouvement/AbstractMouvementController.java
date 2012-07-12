package ch.vd.uniregctb.mouvement;

import java.util.Map;

import org.springframework.web.bind.annotation.ModelAttribute;

import ch.vd.uniregctb.mouvement.manager.MouvementEditManager;
import ch.vd.uniregctb.type.Localisation;
import ch.vd.uniregctb.type.TypeMouvement;

public class AbstractMouvementController {

	protected MouvementEditManager mouvementEditManager;

	@SuppressWarnings("UnusedDeclaration")
	public void setMouvementEditManager(MouvementEditManager mouvementEditManager) {
		this.mouvementEditManager = mouvementEditManager;
	}

	private MouvementMapHelper mouvementMapHelper;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setMouvementMapHelper(MouvementMapHelper mouvementMapHelper) {
		this.mouvementMapHelper = mouvementMapHelper;
	}

	@ModelAttribute("typesMouvement")
	private Map<TypeMouvement, String> typesMouvement() throws Exception {
		return mouvementMapHelper.getMapTypesMouvement();
	}

	@ModelAttribute("etatsMouvement")
	private  Map<EtatMouvementDossier, String> etatsMouvement() throws Exception {
		return mouvementMapHelper.getMapEtatsMouvement();
	}

	@ModelAttribute("localisations")
	private Map<Localisation, String> localisations() throws Exception {
		return mouvementMapHelper.getMapLocalisations();
	}
}
