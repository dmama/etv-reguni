<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
  	<tiles:put name="title">
  		<fmt:message key="title.recapitulatif.annulation" />
  	</tiles:put>
  	<tiles:put name="body">

	    <unireg:bandeauTiers numero="${deactivationCommand.numeroTiers}" titre="Tiers à annuler" showValidation="false" showEvenementsCivils="false" showLinks="false" showAvatar="true"/>

	  	<form:form method="post" id="formRecapAnnulation"  name="formRecapAnnulation" commandName="deactivationCommand" action="deactivate.do?population=${population}">
		    <form:hidden path="numeroTiers"/>
			<fieldset>
				<legend><span><fmt:message key="label.caracteristiques.annulation" /></span></legend>
				<table>
					<tr class="<unireg:nextRowClass/>" >
						<td width="25%"><fmt:message key="label.tiers.a.annuler" />&nbsp;:</td>
						<td width="75%"><unireg:numCTB numero="${deactivationCommand.numeroTiers}" /></td>
					</tr>
					<tr class="<unireg:nextRowClass/>" >
						<td width="25%"><fmt:message key="label.date.annulation" />&nbsp;:</td>
						<td width="75%">
							<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
								<jsp:param name="path" value="dateAnnulation" />
								<jsp:param name="id" value="dateAnnulation" />
							</jsp:include>
							<span style="color: red;">*</span>
						</td>
					</tr>
				</table>
			</fieldset>

			<!-- Debut Boutons -->
			<unireg:RetourButton link="list.do?mode=DESACTIVATION&population=${population}" message="Voulez-vous vraiment quitter cette page sans sauver ?"/>
			<input type="submit" value="<fmt:message key="label.bouton.sauver"/>" onClick="return Page_SauverAnnulation(event || window.event);" />
			<!-- Fin Boutons -->
		</form:form>

		<script type="text/javascript" language="Javascript">
			function Page_SauverAnnulation(event) {
				if(!confirm('Voulez-vous vraiment confirmer cette annulation ?')) {
					return Event.stop(event);
			 	}
			 	return true ;
			}
		</script>

	</tiles:put>
</tiles:insert>