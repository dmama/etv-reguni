<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<%--@elvariable id="command" type="ch.vd.uniregctb.entreprise.AddSiegeView"--%>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
  	<tiles:put name="title">
  		<fmt:message key="title.creation.civil.siege">
  			<fmt:param><unireg:numCTB numero="${command.entrepriseId}"/></fmt:param>
  		</fmt:message>
  	</tiles:put>

	<tiles:put name="body">
		<unireg:bandeauTiers numero="${command.entrepriseId}" showLinks="false" showComplements="false" showEvenementsCivils="false" showValidation="false" showAvatar="false" titre="Caractéristiques de l'entreprise"/>

		<form:form id="addDomicileForm" commandName="command" action="add.do">
			<fieldset>
				<legend><span><fmt:message key="label.entreprise.siege" /></span></legend>

				<form:hidden path="tiersId"/>
				<form:hidden path="entrepriseId" value="${command.entrepriseId}"/>

				<script type="text/javascript">

					function selectAutoriteFiscale(name) {
						if (name == 'COMMUNE_OU_FRACTION_VD') {
							$('#domicile_commune_vd_label').show();
							$('#domicile_commune_hc_label').hide();
							$('#domicile_pays_label').hide();
							$('#autoriteFiscale').val(null);
							$('#noAutoriteFiscale').val(null);
							Fors.autoCompleteCommunesVD('#autoriteFiscale', '#noAutoriteFiscale');
						}
						else if (name == 'COMMUNE_HC') {
							$('#domicile_commune_vd_label').hide();
							$('#domicile_commune_hc_label').show();
							$('#domicile_pays_label').hide();
							$('#autoriteFiscale').val(null);
							$('#noAutoriteFiscale').val(null);
							Fors.autoCompleteCommunesHC('#autoriteFiscale', '#noAutoriteFiscale');
						}
						else if (name == 'PAYS_HS') {
							$('#domicile_commune_vd_label').hide();
							$('#domicile_commune_hc_label').hide();
							$('#domicile_pays_label').show();
							$('#autoriteFiscale').val(null);
							$('#noAutoriteFiscale').val(null);
							Fors.autoCompletePaysHS('#autoriteFiscale', '#noAutoriteFiscale');
						}
					}
				</script>

				<!-- Debut Domicile -->
				<table border="0">
					<unireg:nextRowClass reset="0"/>
					<tr class="<unireg:nextRowClass/>" >
						<td><fmt:message key="label.date.ouverture" />&nbsp;:</td>
						<td>
							<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
								<jsp:param name="path" value="dateDebut" />
								<jsp:param name="id" value="dateDebut" />
							</jsp:include>
						</td>
						<td><fmt:message key="label.date.fermeture" />&nbsp;:</td>
						<td>
							<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
								<jsp:param name="path" value="dateFin" />
								<jsp:param name="id" value="dateFin" />
							</jsp:include>
						</td>
					</tr>

					<tr class="<unireg:nextRowClass/>" >
						<td><fmt:message key="label.type.domicile"/>&nbsp;:</td>
						<td>
							<div id="select_type_for">
								<%--@elvariable id="typesDomicileFiscal" type="java.util.Map<TypeAutoriteFiscale, String>"--%>
								<form:select path="typeAutoriteFiscale" items="${typesDomicileFiscal}" id="optionTypeAutoriteFiscale"
								             onchange="selectAutoriteFiscale(this.options[this.selectedIndex].value);" />
							</div>
						</td>
						<td>
							<label for="autoriteFiscale">
								<span id="domicile_commune_vd_label"><fmt:message key="label.commune.fraction"/></span>
								<span id="domicile_commune_hc_label"><fmt:message key="label.commune"/></span>
								<span id="domicile_pays_label"><fmt:message key="label.pays"/></span>
								&nbsp;:
							</label>
						</td>
						<td>
							<input id="autoriteFiscale" size="25" />
							<form:errors path="noAutoriteFiscale" cssClass="error" />
							<form:hidden path="noAutoriteFiscale" />
						</td>
					</tr>

				</table>

				<script type="text/javascript">
					// on initialise l'auto-completion de l'autorité fiscale
					selectAutoriteFiscale('${command.typeAutoriteFiscale}');
				</script>
			</fieldset>

			<table border="0">
				<tr>
					<td width="25%">&nbsp;</td>
					<td width="25%"><input type="submit" value="<fmt:message key="label.bouton.ajouter" />"></td>
					<td width="25%"><unireg:buttonTo name="Retour" action="/civil/entreprise/edit.do" params="{id:${command.entrepriseId}}" method="GET"/> </td>
					<td width="25%">&nbsp;</td>
				</tr>
			</table>
		</form:form>

	</tiles:put>
</tiles:insert>
