<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
  	<tiles:put name="title">
  		<fmt:message key="title.recapitulatif.reactivation" />
  	</tiles:put>
  	<tiles:put name="body">
	  	<form:form method="post" id="formRecapReactivation"  name="formRecapReactivation" commandName="reactivationCommand" action="reactivate.do?population=${population}">
		    <form:hidden path="tiers.numero"/>
			<jsp:include page="../../general/tiers.jsp">
				<jsp:param name="commandName" value="reactivationCommand"/>
				<jsp:param name="page" value="activation" />
				<jsp:param name="path" value="tiers" />
			</jsp:include>
			<jsp:include page="rapport.jsp" />
			<!-- Debut Boutons -->
			<unireg:RetourButton link="list.do?mode=REACTIVATION&population=${population}" message="Voulez-vous vraiment quitter cette page sans sauver ?"/>
			<input type="submit" value="<fmt:message key="label.bouton.sauver"/>" onClick="return Page_SauverReactivation(event || window.event);" />
			<!-- Fin Boutons -->
		</form:form>
		<script type="text/javascript" language="Javascript">
			function Page_SauverReactivation(event) {
				if(!confirm('Voulez-vous vraiment confirmer cette réactivation ?')) {
					return Event.stop(event);
			 	}
			 	return true ;
			}
		</script>
	</tiles:put>
</tiles:insert>