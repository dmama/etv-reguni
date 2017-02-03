package ch.vd.uniregctb.registrefoncier.dataimport.elements;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import ch.vd.capitastra.rechteregister.BerechtigtePerson;
import ch.vd.capitastra.rechteregister.Dienstbarkeit;
import ch.vd.capitastra.rechteregister.DienstbarkeitExtended;
import ch.vd.capitastra.rechteregister.JuristischePersonGb;
import ch.vd.capitastra.rechteregister.LastRechtGruppe;
import ch.vd.capitastra.rechteregister.NatuerlichePersonGb;
import ch.vd.capitastra.rechteregister.PersonGb;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "DienstbarkeitExtended", namespace = "http://bedag.ch/capitastra/schemas/A51/v20101231/Datenexport/Rechteregister")
public class DienstbarkeitExtendedElement extends DienstbarkeitExtended {

	public DienstbarkeitExtendedElement() {
	}

	public DienstbarkeitExtendedElement(Dienstbarkeit dienstbarkeit) {
		super(dienstbarkeit, null);
	}

	/**
	 * Ajoute un groupe de bénéficiaires, c'est-à-dire le ou les immeubles concernés et le ou les bénéficiaires concernés. Cette méthode regroupe les immeubles par bénéficiaire.
	 *
	 * @param gruppe une groupe de bénéficiaires
	 */
	public void addLastRechtGruppe(@NotNull LastRechtGruppe gruppe) {
		if (lastRechtGruppe == null) {
			lastRechtGruppe = gruppe;
		}
		else {
			final BeneficiaireKeys existants = new BeneficiaireKeys(lastRechtGruppe.getBerechtigtePerson());
			final BeneficiaireKeys nouveaux = new BeneficiaireKeys(gruppe.getBerechtigtePerson());
			// on part du principe que les bénéficiaires sont constants sur une servitude.
			if (!nouveaux.equals(existants)) {
				throw new IllegalArgumentException("Les bénénificaires de la servitude standardRechtID=[" + dienstbarkeit.getStandardRechtID() + "] ne sont pas constants.");
			}
			// on ajoute les immeubles
			lastRechtGruppe.getBelastetesGrundstueck().addAll(gruppe.getBelastetesGrundstueck());
		}
	}

	private static class BeneficiaireKeys {
		private final Set<String> keys;

		public BeneficiaireKeys(@NotNull List<BerechtigtePerson> people) {
			keys = people.stream()
					.map(DienstbarkeitExtendedElement::getPerson)
					.map(DienstbarkeitExtendedElement::getPersonIDRef)
					.collect(Collectors.toSet());
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			final BeneficiaireKeys that = (BeneficiaireKeys) o;
			return Objects.equals(keys, that.keys);
		}

		@Override
		public int hashCode() {
			return Objects.hash(keys);
		}
	}

	@NotNull
	public static String getPersonIDRef(PersonGb person) {

		final String idRef;
		if (person instanceof NatuerlichePersonGb) {
			idRef = ((NatuerlichePersonGb) person).getPersonstammIDREF();
		}
		else if (person instanceof JuristischePersonGb) {
			idRef = ((JuristischePersonGb) person).getPersonstammIDREF();
		}
		else {
			throw new IllegalArgumentException("Type de personne inconnue = [" + person.getClass().getName() + "]");
		}

		if (idRef == null) {
			throw new IllegalArgumentException("La personne masterId=[" + person.getMasterID() + "] ne possède pas d'idRef.");
		}

		return idRef;
	}

	@NotNull
	public static PersonGb getPerson(BerechtigtePerson p) {
		final NatuerlichePersonGb pp = p.getNatuerlichePersonGb();
		final JuristischePersonGb pm = p.getJuristischePersonGb();
		if (pp == null && pm == null) {
			throw new IllegalArgumentException("Le bénéficiaire versonID=[" + p.getVersionID() + "] n'est ni une personne physique ni une personne morale");
		}
		return pp == null ? pm : pp;
	}
}
