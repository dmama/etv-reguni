<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="title">A propos de Unireg</tiles:put>
	<tiles:put name="head">
		<style type="text/css">
			div.about {
				margin-top: 0.5em;
			}
			.about h3 {
				margin-top: 1em;
				margin-bottom: 0.5em;
			}
			.about h4 {
				margin-top: 0.5em;
				margin-left: 2em;
			}
			.about ul {
				margin-left: 4.5em;
				display:inline-block;
			}
			.about li {
				color: #000000;
				font-size: 12px;
				transition:color 1s, font-size 1s;
				-moz-transition:color 1s, font-size 1s; /* Firefox 4 */
				-webkit-transition:color 1s, font-size 1s; /* Safari and Chrome */
				-o-transition:color 1s, font-size 1s; /* Opera */
			}
			.about li:hover {
				color: #336CA6;
				font-size: 24px;
				transition:color 1s, font-size 1s;
				-moz-transition:color 1s, font-size 1s; /* Firefox 4 */
				-webkit-transition:color 1s, font-size 1s; /* Safari and Chrome */
				-o-transition:color 1s, font-size 1s; /* Opera */
			}
			.about fieldset {
				padding: 1em;
				width: 50em;
			}
		</style>
	</tiles:put>


	<tiles:put name="body">
		<div class="about">
			<div>Unireg est le registre des contribuables de l'Etat de Vaud. Copyright (c) 2009-2017, Etat-de-Vaud.</div>
			<div>
				<fieldset>
					<table>
						<tr><td>Version :</td><td><strong><fmt:message key="version" /></strong></td></tr>
						<tr><td>Build :</td><td><strong><fmt:message key="buildNumber" /></strong></td></tr>
						<tr><td>Bug tracker :</td><td><strong><a href="http://issuetracker.etat-de-vaud.ch/jira/browse/SIFISC/component/11239">http://issuetracker.etat-de-vaud.ch/jira/browse/SIFISC/component/11239</a></strong></td></tr>
						<tr><td>Wiki :</td><td><strong><a href="https://portail.etat-de-vaud.ch/outils/dsiwiki/display/FiscaliteUNIREG/Unireg">https://portail.etat-de-vaud.ch/outils/dsiwiki/display/FiscaliteUNIREG/Unireg</a></strong></td></tr>
						<tr><td>Mega :</td><td><strong><a href="http://intranet.etat-de-vaud.ch/carto_mega/acv/pages/57cb69d947b50b2f.htm">http://intranet.etat-de-vaud.ch/carto_mega/acv/pages/57cb69d947b50b2f.htm</a></strong></td></tr>
					</table>
				</fieldset>
			</div>
			<div class="team">
				<h3>L'équipe Unireg actuelle</h3>

				<h4>Cellule projet :</h4>
				<ul>
					<li>David Radelfinger</li>
					<li>Raphaël Carbo</li>
				</ul>

				<h4>Cellule registre :</h4>
				<ul>
					<li>Antonio Rollo</li>
				</ul>

				<h4>OIPM :</h4>
				<ul>
					<li>Dominique Viquerat</li>
				</ul>

				<h4>Chef de projet :</h4>
				<ul>
					<li>Sandro Zinzeri</li>
				</ul>

				<h4>Spécifications :</h4>
				<ul>
					<li>Gabrielle Servoz</li>
				</ul>

				<h4>Développement :</h4>
				<ul>
					<li>Baba Issa Ngom</li>
					<li>Euan Mateo</li>
					<li>Manuel Siggen</li>
					<li>Raphaël Marmier</li>
				</ul>

				<h4>Assurance qualité :</h4>
				<ul>
					<li>John Gibson</li>
				</ul>

				<h3>Personnes ayant contribué à Unireg par le passé</h3>
				<ul>
					<li>Akram Ben Aissi</li>
					<li>Andréa Osmani</li>
					<li>Annie Ourliac</li>
					<li>Boris Mayencourt</li>
					<li>Christophe Friederich</li>
					<li>Christophe Ollivier</li>
					<li>Claudio Parnenzini</li>
					<li>Céline Grand</li>
					<li>David Dublancher</li>
					<li>François Dardare</li>
					<li>Frédéric Noguier</li>
					<li>Gilles Dubey</li>
					<li>Hakan Kaynar</li>
					<li>Jacob Deker</li>
					<li>Jean-Eric Cuendet</li>
					<li>Jérôme Jacques</li>
					<li>Kamel Chabane</li>
					<li>Laurent Bourgeat</li>
					<li>Laurent Perret</li>
					<li>Ludovic Bertin</li>
					<li>Martin Croteau</li>
					<li>Mickaël Zanoli</li>
					<li>Pavel Blanco</li>
					<li>Smita Roy</li>
					<li>Thierry Declercq</li>
				</ul>
			</div>
		</div>
	</tiles:put>
</tiles:insert>
