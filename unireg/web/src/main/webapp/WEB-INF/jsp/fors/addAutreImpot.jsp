<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<%--@elvariable id="command" type="ch.vd.uniregctb.fors.AddForAutreImpotView"--%>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
  	<tiles:put name="title">
  		<fmt:message key="title.creation.for.autre.impot">
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
				background: url(../css/x/fors/autre_32.png) no-repeat;
			}
		</style>
	</tiles:put>
	<tiles:put name="body">

		<div style="float: right; margin: 0.5em;">
			<authz:authorize ifAnyGranted="ROLE_FOR_PRINC_ORDDEP_HAB,ROLE_FOR_PRINC_ORDDEP_HCHS,ROLE_FOR_PRINC_ORDDEP_GRIS,ROLE_FOR_PRINC_SOURC_HAB,ROLE_FOR_PRINC_SOURC_HCHS,ROLE_FOR_PRINC_SOURC_GRIS">
				<unireg:linkTo name="Créer un for principal" action="/fors/addPrincipal.do" params="{tiersId:${command.tiersId}}" link_class="createPrincipalLink"/>
			</authz:authorize>
			<authz:authorize ifAnyGranted="ROLE_FOR_SECOND_PP">
				<unireg:linkTo name="Créer un for secondaire" action="/fors/addSecondaire.do" params="{tiersId:${command.tiersId}}" link_class="createSecondaireLink"/>
			</authz:authorize>
			<authz:authorize ifAnyGranted="ROLE_FOR_AUTRE">
				<unireg:linkTo name="Créer un for autre élément imposable" action="/fors/addAutreElementImposable.do" params="{tiersId:${command.tiersId}}" link_class="createAutreElementLink"/>
			</authz:authorize>
		</div>
		<div style="clear: right;"/>

		<form:form commandName="command" action="addAutreImpot.do">
			<fieldset>
				<legend><span><fmt:message key="label.for.fiscal" /></span></legend>

				<form:hidden path="tiersId"/>

				<!-- Debut For -->
				<table border="0">
					<unireg:nextRowClass reset="0"/>
					<tr class="<unireg:nextRowClass/>" >
						<td width="20%"><fmt:message key="label.genre.impot"/>&nbsp;:</td>
						<td colspan="3">
							<%--@elvariable id="genresImpot" type="java.util.Map<GenreImpot, String>"--%>
							<form:select path="genreImpot" items="${genresImpot}"/>
							<form:errors path="genreImpot" cssClass="error"/>
						</td>
					</tr>
					<tr class="<unireg:nextRowClass/>" >
						<td><fmt:message key="label.date.evenement" />&nbsp;:</td>
						<td colspan="3">
							<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
								<jsp:param name="path" value="dateEvenement" />
								<jsp:param name="id" value="dateEvenement" />
							</jsp:include>
						</td>
					</tr>

					<tr class="<unireg:nextRowClass/>" >
						<td><fmt:message key="label.type.for.fiscal"/>&nbsp;:</td>
						<td><fmt:message key="option.type.for.fiscal.COMMUNE_OU_FRACTION_VD" /></td>
						<td><label for="autoriteFiscale"><fmt:message key="label.commune.fraction"/>&nbsp;:</label></td>
						<td>
							<input id="autoriteFiscale" size="25" />
							<form:errors path="noAutoriteFiscale" cssClass="error" />
							<form:hidden path="noAutoriteFiscale" />
						</td>
					</tr>
				</table>

				<script type="text/javascript">
					// on initialise l'auto-completion de l'autorité fiscale
					Fors.autoCompleteCommunesVD('#autoriteFiscale', '#noAutoriteFiscale');
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
