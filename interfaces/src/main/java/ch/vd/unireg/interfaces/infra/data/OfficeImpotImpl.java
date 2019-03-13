package ch.vd.unireg.interfaces.infra.data;

import java.io.Serializable;

import org.jetbrains.annotations.NotNull;

import ch.vd.infrastructure.model.rest.CollectiviteAdministrative;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureRaw;

public class OfficeImpotImpl extends CollectiviteAdministrativeImpl implements OfficeImpot, Serializable {

	private static final long serialVersionUID = -6974849369927648008L;

	public OfficeImpotImpl(CollectiviteAdministrative target) {
		super(target);
	}

	public OfficeImpotImpl(@NotNull ch.vd.fidor.xml.colladm.v1.CollectiviteAdministrative right, @NotNull ServiceInfrastructureRaw service) {
		super(right, service);
		if (!SIGLE_OID.equals(right.getCodeType())) {
			throw new IllegalArgumentException("La collectivité n'est pas un office d'impôt : type = [" + right.getCodeType() + "]");
		}
	}
}
