<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<%--@elvariable id="actionCommand" type="ch.vd.uniregctb.entreprise.complexe.RequisitionRadiationRCView"--%>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

	<tiles:put name="title">
		<fmt:message key="title.requisition.radiation.rc.entreprise">
			<fmt:param>
				<unireg:numCTB numero="${actionCommand.idEntreprise}"/>
			</fmt:param>
		</fmt:message>
	</tiles:put>

	<tiles:put name="body">
		<unireg:bandeauTiers numero="${actionCommand.idEntreprise}" showAvatar="true" showValidation="false" showEvenementsCivils="false" showLinks="false" showComplements="true"/>
		<unireg:nextRowClass reset="0"/>

		<form:form method="post" id="recapReqRadiationRC" name="recapReqRadiationRC" commandName="actionCommand">
			<form:hidden path="idEntreprise"/>
			<fieldset>
				<legend><span><fmt:message key="label.caracteristiques.requisition.rc" /></span></legend>
				<table>
					<tr class="<unireg:nextRowClass/>" >
						<td width="25%"><fmt:message key="label.date.requisition.rc" />&nbsp;:</td>
						<td width="75%" colspan="3">
							<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
								<jsp:param name="path" value="dateFinActivite" />
								<jsp:param name="id" value="dateFinActivite" />
							</jsp:include>
							<span style="color: red;">*</span>
						</td>
					</tr>
					<tr class="<unireg:nextRowClass/>" style="height: 2em;">
						<td width="25%"><fmt:message key="label.imprimer.demande.bilan.final" />&nbsp;:</td>
						<td width="25%"><form:checkbox path="imprimerDemandeBilanFinal" id="imprimerDemandeBilanFinal" onchange="ReqRadiationRC.avecOuSans(this.checked);"/></td>
						<td width="25%" style="display: none;" class="avecBilanFinal"><fmt:message key="label.periode.fiscale"/>&nbsp;:</td>
						<td width="25%" style="display: none;" class="avecBilanFinal">
							<form:input path="periodeFiscale"/>
							<form:errors cssClass="error" path="periodeFiscale"/>
							<span style="color: red;">*</span>
						</td>
						<td width="50%" colspan="2" class="sansBilanFinal">&nbsp;</td>
					</tr>
					<tr class="<unireg:nextRowClass/>">
						<td width="25%"><fmt:message key="label.commentaire" />&nbsp;:</td>
						<td width="75%" colspan="3">
							<form:textarea path="remarque" id="remarque" cols="80" rows="5"/>
						</td>
					</tr>
				</table>
			</fieldset>

			<script type="application/javascript">
				const ReqRadiationRC = {
					avecOuSans: function(checked) {
						$('.avecBilanFinal, .sansBilanFinal').hide();
						if (checked) {
							$('.avecBilanFinal').show();
						}
						else {
							$('.sansBilanFinal').show();
						}
					}
				};

				$(function() {
					ReqRadiationRC.avecOuSans($('#imprimerDemandeBilanFinal')[0].checked);
				});
			</script>

			<!-- Debut Boutons -->
			<unireg:RetourButton link="list.do" message="Voulez-vous vraiment quitter cette page sans sauver ?"/>
			<input type="submit" value="<fmt:message key="label.bouton.sauver"/>" onClick="return confirm('Voulez-vous vraiment traiter requisition de radiation du RC de cette entreprise ?');" />
			<!-- Fin Boutons -->

		</form:form>
	</tiles:put>

</tiles:insert>