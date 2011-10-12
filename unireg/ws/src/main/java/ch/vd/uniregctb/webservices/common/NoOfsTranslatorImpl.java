package ch.vd.uniregctb.webservices.common;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.InitializingBean;

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
	@Override
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

	@Override
	public void afterPropertiesSet() throws Exception {

		if (exposeNosTechniques) {
			// selon résultat de la requête "select NO_OFS, NO_TECHNIQUE from CIIV1.COMMUNE where NO_TECHNIQUE != int(NO_OFS) and int(NO_OFS) > 0 ORDER by int(NO_OFS);"
			communes.put(252, 8356);
			communes.put(253, 8355);
			communes.put(461, 8354);
			communes.put(462, 8353);
			communes.put(463, 8352);
			communes.put(464, 8351);
			communes.put(465, 8350);
			communes.put(466, 8349);
			communes.put(467, 8348);
			communes.put(468, 8347);
			communes.put(469, 8346);
			communes.put(470, 8345);
			communes.put(471, 8344);
			communes.put(472, 8343);
			communes.put(473, 8342);
			communes.put(474, 8341);
			communes.put(475, 8340);
			communes.put(476, 8339);
			communes.put(477, 8338);
			communes.put(478, 8337);
			communes.put(479, 8336);
			communes.put(480, 8335);
			communes.put(481, 8334);
			communes.put(482, 8333);
			communes.put(483, 8332);
			communes.put(484, 8331);
			communes.put(485, 8330);
			communes.put(486, 8329);
			communes.put(487, 8328);
			communes.put(488, 8327);
			communes.put(489, 8326);
			communes.put(511, 8325);
			communes.put(512, 8324);
			communes.put(513, 8323);
			communes.put(514, 8322);
			communes.put(515, 8321);
			communes.put(516, 8320);
			communes.put(517, 8319);
			communes.put(518, 8318);
			communes.put(519, 8317);
			communes.put(520, 8316);
			communes.put(521, 8315);
			communes.put(522, 8314);
			communes.put(523, 8313);
			communes.put(524, 8312);
			communes.put(525, 8311);
			communes.put(526, 8310);
			communes.put(527, 8309);
			communes.put(528, 8308);
			communes.put(529, 8307);
			communes.put(583, 8306);
			communes.put(632, 8060);
			communes.put(641, 8305);
			communes.put(642, 8304);
			communes.put(643, 8303);
			communes.put(644, 8302);
			communes.put(645, 8301);
			communes.put(646, 8300);
			communes.put(647, 8299);
			communes.put(648, 8298);
			communes.put(649, 8297);
			communes.put(650, 8296);
			communes.put(651, 8295);
			communes.put(652, 8294);
			communes.put(653, 8293);
			communes.put(685, 8292);
			communes.put(686, 8291);
			communes.put(688, 8290);
			communes.put(689, 8289);
			communes.put(693, 8288);
			communes.put(695, 8287);
			communes.put(698, 8286);
			communes.put(705, 8285);
			communes.put(714, 8284);
			communes.put(756, 8283);
			communes.put(801, 8282);
			communes.put(802, 8281);
			communes.put(803, 8280);
			communes.put(804, 8279);
			communes.put(805, 8278);
			communes.put(806, 8277);
			communes.put(807, 8276);
			communes.put(808, 8275);
			communes.put(809, 8274);
			communes.put(810, 8273);
			communes.put(811, 8272);
			communes.put(812, 8271);
			communes.put(813, 8270);
			communes.put(814, 8269);
			communes.put(815, 8268);
			communes.put(816, 8267);
			communes.put(817, 8266);
			communes.put(818, 8265);
			communes.put(819, 8264);
			communes.put(820, 8263);
			communes.put(821, 8262);
			communes.put(822, 8261);
			communes.put(823, 8260);
			communes.put(824, 8259);
			communes.put(825, 8258);
			communes.put(826, 8257);
			communes.put(827, 8256);
			communes.put(828, 8255);
			communes.put(829, 8254);
			communes.put(830, 8253);
			communes.put(831, 8252);
			communes.put(832, 8251);
			communes.put(833, 8250);
			communes.put(834, 8249);
			communes.put(835, 8248);
			communes.put(836, 8247);
			communes.put(855, 8106);
			communes.put(888, 8065);
			communes.put(948, 8079);
			communes.put(1151, 8076);
			communes.put(1630, 8108);
			communes.put(1631, 8107);
			communes.put(1632, 8110);
			communes.put(2006, 8246);
			communes.put(2019, 8245);
			communes.put(2021, 8244);
			communes.put(2029, 8048);
			communes.put(2030, 8243);
			communes.put(2048, 8242);
			communes.put(2050, 8062);
			communes.put(2051, 8066);
			communes.put(2052, 8077);
			communes.put(2063, 8039);
			communes.put(2065, 8241);
			communes.put(2073, 8240);
			communes.put(2076, 8239);
			communes.put(2078, 8238);
			communes.put(2084, 8237);
			communes.put(2090, 8236);
			communes.put(2098, 8235);
			communes.put(2104, 8234);
			communes.put(2106, 8233);
			communes.put(2114, 8052);
			communes.put(2115, 8061);
			communes.put(2116, 8067);
			communes.put(2121, 8026);
			communes.put(2122, 8054);
			communes.put(2157, 8232);
			communes.put(2162, 8063);
			communes.put(2174, 8023);
			communes.put(2178, 8231);
			communes.put(2184, 8047);
			communes.put(2187, 8230);
			communes.put(2192, 8036);
			communes.put(2195, 8229);
			communes.put(2201, 8228);
			communes.put(2207, 8227);
			communes.put(2209, 8226);
			communes.put(2212, 8225);
			communes.put(2220, 8055);
			communes.put(2223, 8056);
			communes.put(2224, 8224);
			communes.put(2233, 8053);
			communes.put(2234, 8051);
			communes.put(2235, 8059);
			communes.put(2242, 8223);
			communes.put(2245, 8222);
			communes.put(2246, 8221);
			communes.put(2256, 8220);
			communes.put(2268, 8219);
			communes.put(2272, 8038);
			communes.put(2273, 8218);
			communes.put(2297, 8217);
			communes.put(2334, 8216);
			communes.put(2337, 8058);
			communes.put(2338, 8064);
			communes.put(2451, 8215);
			communes.put(2456, 8031);
			communes.put(2503, 8081);
			communes.put(2512, 8214);
			communes.put(2515, 8213);
			communes.put(2533, 8212);
			communes.put(2577, 8211);
			communes.put(2801, 8210);
			communes.put(2802, 8209);
			communes.put(2803, 8208);
			communes.put(2804, 8207);
			communes.put(2805, 8206);
			communes.put(2805, 8205);
			communes.put(2806, 8204);
			communes.put(2807, 8203);
			communes.put(2808, 8202);
			communes.put(2809, 8201);
			communes.put(2810, 8200);
			communes.put(2811, 8199);
			communes.put(2812, 8198);
			communes.put(2813, 8197);
			communes.put(2814, 8196);
			communes.put(2815, 8195);
			communes.put(2816, 8194);
			communes.put(2935, 8193);
			communes.put(3340, 8080);
			communes.put(3353, 8192);
			communes.put(3358, 8070);
			communes.put(3359, 8191);
			communes.put(3378, 8086);
			communes.put(3404, 8190);
			communes.put(3535, 8189);
			communes.put(3541, 8043);
			communes.put(3585, 8188);
			communes.put(3597, 8187);
			communes.put(3599, 8027);
			communes.put(3617, 8087);
			communes.put(3671, 8088);
			communes.put(3713, 8082);
			communes.put(3772, 8186);
			communes.put(3792, 8185);
			communes.put(3802, 8184);
			communes.put(3807, 8183);
			communes.put(3809, 8182);
			communes.put(3847, 8089);
			communes.put(3894, 8181);
			communes.put(3931, 8083);
			communes.put(3932, 8090);
			communes.put(4025, 8180);
			communes.put(4049, 8071);
			communes.put(4102, 8179);
			communes.put(4184, 8178);
			communes.put(4447, 8177);
			communes.put(4462, 8176);
			communes.put(4463, 8175);
			communes.put(4464, 8174);
			communes.put(4476, 4479);
			communes.put(4479, 8173);
			communes.put(4486, 8034);
			communes.put(4492, 8172);
			communes.put(4495, 4492);
			communes.put(4506, 4510);
			communes.put(4510, 8171);
			communes.put(4511, 8037);
			communes.put(4511, 4912);
			communes.put(4512, 4913);
			communes.put(4536, 8045);
			communes.put(4541, 8170);
			communes.put(4545, 4541);
			communes.put(4546, 8044);
			communes.put(4562, 8169);
			communes.put(4583, 8168);
			communes.put(4590, 4583);
			communes.put(4601, 8035);
			communes.put(4611, 4612);
			communes.put(4612, 8167);
			communes.put(4616, 8029);
			communes.put(4621, 8028);
			communes.put(4633, 8166);
			communes.put(4643, 4686);
			communes.put(4647, 8165);
			communes.put(4666, 8033);
			communes.put(4678, 8164);
			communes.put(4681, 4678);
			communes.put(4683, 8040);
			communes.put(4686, 8163);
			communes.put(4687, 8162);
			communes.put(4688, 8161);
			communes.put(4701, 4704);
			communes.put(4704, 8160);
			communes.put(4716, 4736);
			communes.put(4721, 8032);
			communes.put(4723, 4771);
			communes.put(4724, 4762);
			communes.put(4727, 8159);
			communes.put(4728, 8158);
			communes.put(4729, 8157);
			communes.put(4730, 8156);
			communes.put(4736, 8155);
			communes.put(4738, 8154);
			communes.put(4741, 4738);
			communes.put(4751, 8042);
			communes.put(4757, 8153);
			communes.put(4761, 4764);
			communes.put(4762, 8152);
			communes.put(4764, 8151);
			communes.put(4771, 8150);
			communes.put(4776, 8046);
			communes.put(4782, 8149);
			communes.put(4783, 8148);
			communes.put(4784, 8147);
			communes.put(4786, 8041);
			communes.put(4792, 8146);
			communes.put(4793, 8145);
			communes.put(4832, 8144);
			communes.put(4837, 8143);
			communes.put(4841, 4837);
			communes.put(4852, 8142);
			communes.put(4853, 8141);
			communes.put(4862, 8140);
			communes.put(4871, 4873);
			communes.put(4873, 8139);
			communes.put(4881, 8030);
			communes.put(4891, 4892);
			communes.put(4892, 8138);
			communes.put(4901, 4902);
			communes.put(4902, 8137);
			communes.put(4912, 8136);
			communes.put(4913, 8135);
			communes.put(4942, 8134);
			communes.put(4951, 4954);
			communes.put(4954, 8133);
			communes.put(5048, 8072);
			communes.put(5049, 8078);
			communes.put(5124, 8132);
			communes.put(5137, 8024);
			communes.put(5138, 8084);
			communes.put(5152, 8131);
			communes.put(5157, 8130);
			communes.put(5166, 8129);
			communes.put(5172, 8128);
			communes.put(5226, 8025);
			communes.put(5236, 8074);
			communes.put(5237, 8075);
			communes.put(5238, 8105);
			communes.put(5261, 8127);
			communes.put(5269, 8103);
			communes.put(5323, 8073);
			communes.put(5324, 8085);
			communes.put(5397, 8102);
			communes.put(5398, 8104);
			communes.put(5487, 5510);
			communes.put(5626, 8121);
			communes.put(5753, 8120);
			communes.put(5887, 8117);
			communes.put(6003, 8116);
			communes.put(6005, 8115);
			communes.put(6073, 8050);
			communes.put(6074, 8068);
			communes.put(6075, 8069);
			communes.put(6076, 8091);
			communes.put(6090, 8109);
			communes.put(6117, 8049);
			communes.put(6118, 8092);
			communes.put(6138, 8114);
			communes.put(6181, 8057);
			communes.put(6203, 8093);
			communes.put(6204, 8094);
			communes.put(6236, 8113);
			communes.put(6252, 8095);
			communes.put(6262, 8112);
			communes.put(6284, 8111);
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
