package ch.vd.uniregctb.tiers.view;

import java.util.Set;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.tiers.IdentificationPersonne;
import ch.vd.uniregctb.tiers.PersonnePhysique;

/**
 * Structure model pour l'ecran d'edition de IdentificationPersonne
 *
 * @author xcifde
 *
 */
public class IdentificationPersonneView {

	private String ancienNumAVS;
	private String numRegistreEtranger;

	public IdentificationPersonneView() {
	}

	public IdentificationPersonneView(PersonnePhysique nh) {
		final Set<IdentificationPersonne> ips = nh.getIdentificationsPersonnes();
		for (IdentificationPersonne ip : ips) {
			switch (ip.getCategorieIdentifiant()) {
			case CH_AHV_AVS:
				ancienNumAVS = ip.getIdentifiant();
				break;
			case CH_ZAR_RCE:
				numRegistreEtranger = ip.getIdentifiant();
				break;
			default:
				Assert.fail("Catégorie d'identifiant inconnu :" + ip.getCategorieIdentifiant());
			}
		}
	}

	public String getNumRegistreEtranger() {
		return numRegistreEtranger;
	}

	public String getAncienNumAVS() {
		return ancienNumAVS;
	}

	public void setAncienNumAVS(String ancienNumAVS) {
		this.ancienNumAVS = ancienNumAVS;
	}

	public void setNumRegistreEtranger(String numRegistreEtranger) {
		this.numRegistreEtranger = numRegistreEtranger;
	}
}
