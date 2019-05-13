<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<%--@elvariable id="command" type="ch.vd.unireg.decision.aci.EditDecisionAciView"--%>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
  	<tiles:put name="title">
  		<fmt:message key="title.edition.decision.aci">
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

		<table border="0"><tr valign="top">
		<td>
			<form:form id="editForForm" modelAttribute="command" action="edit.do">
				<fieldset>
					<legend><span><fmt:message key="label.decision.aci" /></span></legend>

					<form:hidden path="id"/>
                    <script type="text/javascript">

                        function selectAutoriteFiscale(name) {
                            if (name == 'COMMUNE_OU_FRACTION_VD') {
                                $('#for_commune_vd_label').show();
                                $('#for_commune_hc_label').hide();
                                $('#for_pays_label').hide();
                                $('#autoriteFiscale').val('<unireg:commune ofs="${command.numeroAutoriteFiscale}" displayProperty="nomOfficiel" date="${command.dateDebut}" escapeMode="javascript"/>');
                                Fors.autoCompleteCommunesVD('#autoriteFiscale', '#numeroAutoriteFiscale');
                            }
                            else if (name == 'COMMUNE_HC') {
                                $('#for_commune_vd_label').hide();
                                $('#for_commune_hc_label').show();
                                $('#for_pays_label').hide();
                                $('#autoriteFiscale').val('<unireg:commune ofs="${command.numeroAutoriteFiscale}" displayProperty="nomOfficiel" date="${command.dateDebut}" escapeMode="javascript"/>');
                                Fors.autoCompleteCommunesHC('#autoriteFiscale', '#numeroAutoriteFiscale');
                            }
                            else if (name == 'PAYS_HS') {
                                $('#for_commune_vd_label').hide();
                                $('#for_commune_hc_label').hide();
                                $('#for_pays_label').show();
                                $('#autoriteFiscale').val('<unireg:pays ofs="${command.numeroAutoriteFiscale}" displayProperty="nomCourt" date="${command.dateDebut}" escapeMode="javascript"/>');
                                Fors.autoCompletePaysHS('#autoriteFiscale', '#numeroAutoriteFiscale');
                            }

                        }


                    </script>
                    <table border="0">
                        <unireg:nextRowClass reset="0"/>
                        <tr class="<unireg:nextRowClass/>" >
                            <td><fmt:message key="label.type.for.fiscal"/>&nbsp;:</td>
                            <td><fmt:message key="option.type.for.fiscal.${command.typeAutoriteFiscale}" /></td>
                        </tr>
                        <tr class="<unireg:nextRowClass/>" >
                            <td>
                                <label for="autoriteFiscale">
                                    <c:if test="${command.typeAutoriteFiscale == 'COMMUNE_OU_FRACTION_VD'}">
                                        <fmt:message key="label.commune.fraction"/>
                                    </c:if>
                                    <c:if test="${command.typeAutoriteFiscale == 'COMMUNE_HC'}">
                                        <fmt:message key="label.commune"/>
                                    </c:if>
                                    <c:if test="${command.typeAutoriteFiscale == 'PAYS_HS'}">
                                        <fmt:message key="label.pays"/>
                                    </c:if>
                                    &nbsp;:
                                </label>
                            </td>
                            <td>
                                <input id="autoriteFiscale" size="25"/>
                                <form:errors path="numeroAutoriteFiscale" cssClass="error"/>
                                <form:hidden path="numeroAutoriteFiscale"/>
                            </td>
                        </tr>
                        <tr class="<unireg:nextRowClass/>" >
                            <td><fmt:message key="label.date.debut" />&nbsp;:</td>
                            <td><unireg:regdate regdate="${command.dateDebut}"/></td>
                        </tr>
                        <tr class="<unireg:nextRowClass/>" >
                            <td><fmt:message key="label.date.fin" />&nbsp;:</td>
                            <td>
                                <c:if test="${command.dateFinEditable}">
                                    <jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
                                        <jsp:param name="path" value="dateFin"/>
                                        <jsp:param name="id" value="dateFin"/>
                                    </jsp:include>
                                </c:if>
                                <c:if test="${!command.dateFinEditable}">
                                    <unireg:regdate regdate="${command.dateFin}"/>
                                </c:if>
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
                        selectAutoriteFiscale('${command.typeAutoriteFiscale}');
                    </script>
				</fieldset>

				<table border="0">
					<tr>
						<td width="25%">&nbsp;</td>
						<td width="25%"><input type="submit" value="<fmt:message key="label.bouton.mettre.a.jour" />"></td>
						<td width="25%"><unireg:buttonTo name="Retour" action="/fiscal/edit.do" params="{id:${command.tiersId}}" method="GET"/> </td>
						<td width="25%">&nbsp;</td>
					</tr>
				</table>
			</form:form>

		</td>
		<td id="actions_column" style="display:none">
			<div id="actions_list"></div>
		</td>
		</tr></table>



	</tiles:put>
</tiles:insert>
