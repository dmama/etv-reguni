package ch.vd.uniregctb.webservices.common;

import org.springframework.beans.factory.InitializingBean;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class NoOfsTranslatorImpl implements NoOfsTranslator, InitializingBean {

	/**
	 * Mapping nos Ofs -> nos techniques pour les communes suisses dont les deux numéros sont différents.
	 */
	private final Map<Integer, Integer> communes = new HashMap<Integer, Integer>();

	private boolean exposeNosTechniques;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setExposeNosTechniques(boolean exposeNosTechniques) {
		this.exposeNosTechniques = exposeNosTechniques;
	}

	/**
	 * {@inheritDoc}
	 */
	public int translateCommune(int noOfs) {
		if (exposeNosTechniques) {
			final Integer noTechnique = communes.get(noOfs);
			if (noTechnique != null) {
				// on a trouvé un numéro technique différent du numéro Ofs, on le retourne
				return noTechnique;
			}
			else {
				return noOfs;
			}
		}
		else {
			// on expose simplement le numéro Ofs
			return noOfs;
		}
	}

	public void afterPropertiesSet() throws Exception {

		if (exposeNosTechniques) {
			// selon résultat de la requête "select NO_OFS, NO_TECHNIQUE from CIIV1.COMMUNE where NO_TECHNIQUE != int(NO_OFS) and int(NO_OFS) > 0 ORDER by int(NO_OFS);"
			communes.put(632, 8060);
			communes.put(888, 8065);
			communes.put(948, 8079);
			communes.put(1151, 8076);
			communes.put(1151, 8076);
			communes.put(2029, 8048);
			communes.put(2050, 8062);
			communes.put(2051, 8066);
			communes.put(2052, 8077);
			communes.put(2063, 8039);
			communes.put(2114, 8052);
			communes.put(2115, 8061);
			communes.put(2116, 8067);
			communes.put(2121, 8026);
			communes.put(2122, 8054);
			communes.put(2162, 8063);
			communes.put(2174, 8023);
			communes.put(2184, 8047);
			communes.put(2192, 8036);
			communes.put(2220, 8055);
			communes.put(2223, 8056);
			communes.put(2233, 8053);
			communes.put(2234, 8051);
			communes.put(2235, 8059);
			communes.put(2272, 8038);
			communes.put(2337, 8058);
			communes.put(2338, 8064);
			communes.put(2456, 8031);
			communes.put(2503, 8081);
			communes.put(3340, 8080);
			communes.put(3358, 8070);
			communes.put(3378, 8086);
			communes.put(3541, 8043);
			communes.put(3599, 8027);
			communes.put(3617, 8087);
			communes.put(3671, 8088);
			communes.put(3713, 8082);
			communes.put(3847, 8089);
			communes.put(3931, 8083);
			communes.put(3932, 8090);
			communes.put(4049, 8071);
			communes.put(4476, 4479);
			communes.put(4486, 8034);
			communes.put(4495, 4492);
			communes.put(4506, 4510);
			communes.put(4511, 4912);
			communes.put(4511, 8037);
			communes.put(4512, 4913);
			communes.put(4536, 8045);
			communes.put(4545, 4541);
			communes.put(4546, 8044);
			communes.put(4590, 4583);
			communes.put(4601, 8035);
			communes.put(4611, 4612);
			communes.put(4616, 8029);
			communes.put(4621, 8028);
			communes.put(4643, 4686);
			communes.put(4666, 8033);
			communes.put(4681, 4678);
			communes.put(4683, 8040);
			communes.put(4701, 4704);
			communes.put(4716, 4736);
			communes.put(4721, 8032);
			communes.put(4723, 4771);
			communes.put(4724, 4762);
			communes.put(4741, 4738);
			communes.put(4751, 8042);
			communes.put(4761, 4764);
			communes.put(4776, 8046);
			communes.put(4786, 8041);
			communes.put(4841, 4837);
			communes.put(4871, 4873);
			communes.put(4881, 8030);
			communes.put(4891, 4892);
			communes.put(4901, 4902);
			communes.put(4951, 4954);
			communes.put(5048, 8072);
			communes.put(5049, 8078);
			communes.put(5137, 8024);
			communes.put(5138, 8084);
			communes.put(5226, 8025);
			communes.put(5236, 8074);
			communes.put(5237, 8075);
			communes.put(5323, 8073);
			communes.put(5324, 8085);
			communes.put(5487, 5510);
			communes.put(6073, 8050);
			communes.put(6074, 8068);
			communes.put(6075, 8069);
			communes.put(6076, 8091);
			communes.put(6117, 8049);
			communes.put(6118, 8092);
			communes.put(6181, 8057);
			communes.put(6203, 8093);
			communes.put(6204, 8094);
			communes.put(6252, 8095);
			communes.put(6461, 8096);
			communes.put(6512, 8097);
			communes.put(6728, 714);
			communes.put(6807, 8100);
			communes.put(6808, 8098);
			communes.put(6809, 8101);
			communes.put(6810, 8099);
		}
	}
}
