<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<%--@elvariable id="actionCommand" type="ch.vd.unireg.entreprise.complexe.FusionEntreprisesView"--%>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

	<tiles:put name="title">
		<fmt:message key="title.traitement.annulation.fusion.entreprises"/>
	</tiles:put>

	<tiles:put name="body">
		<c:set var="titre">
			<fmt:message key="label.caracteristiques.fusion.entreprise.absorbante"/>
		</c:set>
		<unireg:bandeauTiers numero="${actionCommand.idEntrepriseAbsorbante}" showAvatar="true" showValidation="false" showEvenementsCivils="false" showLinks="false" showComplements="true" titre="${titre}"/>
		<unireg:nextRowClass reset="0"/>

		<form:form method="post" id="recapDatesFusion" name="recapDatesFusion" action="choix-dates.do" commandName="actionCommand">
			<form:hidden path="idEntrepriseAbsorbante"/>
			<fieldset>
				<legend><span><fmt:message key="label.caracteristiques.fusion.entreprises" /></span></legend>
				<table>
					<tr class="<unireg:nextRowClass/>" >
						<td width="25%"><fmt:message key="label.date.bilan.fusion" />&nbsp;:</td>
						<td width="75%">
							<%--@elvariable id="datesBilan" type="java.util.Map<String, String>"--%>
							<form:select path="dateBilanFusion" items="${datesBilan}"
							             onchange="AnnulationFusion.selectDateBilanFusion(this.options[this.selectedIndex].value);" />
							<span class="mandatory">*</span>
						</td>
					</tr>
					<tr class="<unireg:nextRowClass/>">
						<td width="25%"><fmt:message key="label.date.contrat.fusion" />&nbsp;:</td>
						<td width="75%">
							<form:select id="selectDateContrat" path="dateContratFusion"/>
							<span class="mandatory">*</span>
						</td>
					</tr>
				</table>
			</fieldset>

			<!-- Debut Boutons -->
			<unireg:RetourButton link="list.do" message="Voulez-vous vraiment quitter cette page sans sauver ?"/>
			<input type="submit" value="<fmt:message key="label.bouton.sauver"/>" onClick="return confirm('Voulez-vous vraiment annuler la fusion d\'entreprise ?');" />
			<!-- Fin Boutons -->

			<script type="application/javascript">

				var AnnulationFusion = {
					selectDateBilanFusion: function(selectedDate) {
						// appels ajax pour mettre-à-jour les dates possibles pour les dates de contrat
						$.get(App.curl('/processuscomplexe/annulation/fusion/dates-contrat.do?idEntreprise=${actionCommand.idEntrepriseAbsorbante}&dateBilan=' + selectedDate + '&' + new Date().getTime()), function(dates) {
							var list = '';
							for(var i = 0; i < dates.length; ++i) {
								var d = dates[i];
								var str = RegDate.format(d);
								list += '<option value="' + str + '"' + (str === '<unireg:regdate regdate="${actionCommand.dateContratFusion}"/>' ? ' selected=true' : '') + '">' + str + '</option>';
							}
							$('#selectDateContrat').html(list);
						}, 'json').error(Ajax.popupErrorHandler);
					}
				};

				// au chargement de la page
				$(function() {
					AnnulationFusion.selectDateBilanFusion('<unireg:regdate regdate="${actionCommand.dateBilanFusion}"/>');
				});

			</script>

		</form:form>
	</tiles:put>

</tiles:insert>