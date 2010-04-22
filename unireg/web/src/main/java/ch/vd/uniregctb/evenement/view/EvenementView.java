package ch.vd.uniregctb.evenement.view;

import java.util.List;

import ch.vd.uniregctb.adresse.AdresseEnvoi;
import ch.vd.uniregctb.evenement.EvenementCivilRegroupe;
import ch.vd.uniregctb.individu.IndividuView;

/**
 * Structure permettant l'affichage
 * de la page de detail de l'evenement
 *
 * @author xcifde
 *
 */
public class EvenementView {

		private EvenementCivilRegroupe evenement;

		private IndividuView individuPrincipal;

		private IndividuView individuConjoint;

		private List<TiersAssocieView> tiersAssocies;

		private AdresseEnvoi adressePrincipal;

		private AdresseEnvoi adresseConjoint;


		public EvenementCivilRegroupe getEvenement() {
			return evenement;
		}

		public void setEvenement(EvenementCivilRegroupe evenement) {
			this.evenement = evenement;
		}

		public IndividuView getIndividuPrincipal() {
			return individuPrincipal;
		}

		public void setIndividuPrincipal(IndividuView individuPrincipal) {
			this.individuPrincipal = individuPrincipal;
		}

		public IndividuView getIndividuConjoint() {
			return individuConjoint;
		}

		public void setIndividuConjoint(IndividuView individuConjoint) {
			this.individuConjoint = individuConjoint;
		}

		public List<TiersAssocieView> getTiersAssocies() {
			return tiersAssocies;
		}

		public void setTiersAssocies(List<TiersAssocieView> tiersAssocies) {
			this.tiersAssocies = tiersAssocies;
		}

		public AdresseEnvoi getAdressePrincipal() {
			return adressePrincipal;
		}

		public void setAdressePrincipal(AdresseEnvoi adressePrincipal) {
			this.adressePrincipal = adressePrincipal;
		}

		public AdresseEnvoi getAdresseConjoint() {
			return adresseConjoint;
		}

		public void setAdresseConjoint(AdresseEnvoi adresseConjoint) {
			this.adresseConjoint = adresseConjoint;
		}

}
