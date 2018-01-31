<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<%--@elvariable id="command" type="ch.vd.uniregctb.fors.AddForPrincipalView"--%>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
  	<tiles:put name="title">
  		<fmt:message key="title.creation.decision.aci">
  			<fmt:param><unireg:numCTB numero="${command.tiersId}"/></fmt:param>
  		</fmt:message>
  	</tiles:put>
	<tiles:put name="head">
		<style type="text/css">
			h1 {
				margin-left: 10px;
				padding-left: 40px;
				padding-top: 4px;
				height: 32px;
				background: url(../css/x/fors/principal_32.png) no-repeat;
			}
		</style>
	</tiles:put>
	<tiles:put name="body">

		<div style="clear: right;"></div>

		<form:form id="addDecisionForm" commandName="command" action="add.do">
			<fieldset>
				<legend><span><fmt:message key="label.decision.aci" /></span></legend>

				<form:hidden path="tiersId"/>

				<script type="text/javascript">
                    function selectAutoriteFiscale(name) {
                        selectAutoriteFiscale(name, null, null)
                    }

					function selectAutoriteFiscale(name, autNo, autNom) {
						if (name == 'COMMUNE_OU_FRACTION_VD') {
							$('#for_commune_vd_label').show();
							$('#for_commune_hc_label').hide();
							$('#for_pays_label').hide();
							$('#autoriteFiscale').val(autNom);
							$('#numeroAutoriteFiscale').val(autNo);
							Fors.autoCompleteCommunesVD('#autoriteFiscale', '#numeroAutoriteFiscale');
						}
						else if (name == 'COMMUNE_HC') {
							$('#for_commune_vd_label').hide();
							$('#for_commune_hc_label').show();
							$('#for_pays_label').hide();
							$('#autoriteFiscale').val(autNom);
							$('#numeroAutoriteFiscale').val(autNo);
							Fors.autoCompleteCommunesHC('#autoriteFiscale', '#numeroAutoriteFiscale');
						}
						else if (name == 'PAYS_HS') {
							$('#for_commune_vd_label').hide();
							$('#for_commune_hc_label').hide();
							$('#for_pays_label').show();
							$('#autoriteFiscale').val(autNom);
							$('#numeroAutoriteFiscale').val(autNo);
							Fors.autoCompletePaysHS('#autoriteFiscale', '#numeroAutoriteFiscale');
						}
					}

				</script>

				<table border="0">
					<unireg:nextRowClass reset="0"/>
                    <tr class="<unireg:nextRowClass/>" >
                        <td><fmt:message key="label.type.for.fiscal"/>&nbsp;:</td>
                        <td>
                            <div id="select_type_for">
                                    <%--@elvariable id="typesForFiscal" type="java.util.Map<TypeAutoriteFiscale, String>"--%>
                                <form:select path="typeAutoriteFiscale" items="${typesForFiscal}" id="optionTypeAutoriteFiscale"
                                             onchange="selectAutoriteFiscale(this.options[this.selectedIndex].value);" />
                            </div>
                            <div id="mandatory_type_for" style="display: none;"></div>
                        </td>
                    </tr>
					<tr class="<unireg:nextRowClass/>" >
                        <td>
                            <label for="autoriteFiscale">
                                <span id="for_commune_vd_label"><fmt:message key="label.commune.fraction"/></span>
                                <span id="for_commune_hc_label"><fmt:message key="label.commune"/></span>
                                <span id="for_pays_label"><fmt:message key="label.pays"/></span>
                                &nbsp;:
                            </label>
                        </td>
                        <td>
                            <input id="autoriteFiscale" size="25"/>
	                        <span class="mandatory">*</span>
                            <form:errors path="numeroAutoriteFiscale" cssClass="error"/>
                            <form:hidden path="numeroAutoriteFiscale"/>
                        </td>
					</tr>
                    <tr class="<unireg:nextRowClass/>" >
                        <td><fmt:message key="label.date.debut" />&nbsp;:</td>
                        <td>
                            <jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
                                <jsp:param name="path" value="dateDebut" />
                                <jsp:param name="id" value="dateDebut" />
	                            <jsp:param name="mandatory" value="true" />
                            </jsp:include>
                        </td>
                    </tr>
                    <tr class="<unireg:nextRowClass/>" >
                        <td><fmt:message key="label.date.fin" />&nbsp;:</td>
                        <td>
                            <jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
                                <jsp:param name="path" value="dateFin" />
                                <jsp:param name="id" value="dateFin" />
                            </jsp:include>
                        </td>
                    </tr>
                    <tr class="<unireg:nextRowClass/>" >
                        <td><fmt:message key="label.decision.aci.remarque" />&nbsp;:</td>
                        <td>
                            <div id="newRemarque" class="new_remarque">
                                <form:textarea path="remarque" cols="80" rows="3"/><br>
                            </div>
                        </td>
                    </tr>
				</table>

				<script type="text/javascript">
					// on initialise l'auto-completion de l'autorité fiscale
					selectAutoriteFiscale('${command.typeAutoriteFiscale}', '${command.numeroAutoriteFiscale}', '${command.autoriteFiscaleNom}');

				</script>
			</fieldset>

			<table border="0">
				<tr>
					<td width="25%">&nbsp;</td>
					<td width="25%"><input type="submit" value="<fmt:message key="label.bouton.ajouter" />"></td>
					<td width="25%"><unireg:buttonTo name="Retour" action="/fiscal/edit.do" params="{id:${command.tiersId}}" method="GET"/> </td>
					<td width="25%">&nbsp;</td>
				</tr>
			</table>
		</form:form>

	</tiles:put>
</tiles:insert>
