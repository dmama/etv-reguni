<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

  	<tiles:put name="title"><fmt:message key="title.info"/></tiles:put>

  	<tiles:put name="body">
  	
  		<a href="JavaScript:location.reload(true);">
			(<fmt:message key="label.bouton.refresh"/>)
		</a>

		<fieldset>
			<legend><span><fmt:message key="label.batch.actif"/></span></legend> 
			<div id="jobsActif"></div>
		</fieldset>

		<script type="text/javascript" language="Javascript1.3">
		
			var requestDone = true;
			var jobExecuter = new PeriodicalExecuter(function() {
				if (!requestDone)
					return;
				requestDone = false;
				XT.doAjaxAction('loadJobActif', E$("jobsActif"), {},
					{ 
						clearQueryString: true,
						errorHandler : function(ajaxRequest, exception) {
							requestDone = true;
						}
		   			});
			}, 3);
			
			jobExecuter.onTimerEvent();
			
			function onRecieved() {
				requestDone = true;
			}
		</script>

		<fieldset class="info">
			<legend><span><fmt:message key="title.info.data" /></span></legend>
			<table>
				<tr><td><fmt:message key="label.info.db.nombre.tiers"/></td><td><span class="value"><c:out value="${tiersCount}"/></span></td></tr>
				<tr><td><fmt:message key="label.info.indexer.nombre.docs"/></td><td><span class="value"><c:out value="${indexCount}"/></span></td></tr>
			</table>
		</fieldset>

		<fieldset class="info">
			<legend><span><fmt:message key="title.info.connection" /></span></legend>
			<table class="display_table">
				<thead>
					<tr>
						<th/><th>Etat</th>
					</tr>
				</thead>
				<tbody>
					<tr>
						<td><fmt:message key="label.info.service.civil"/></td>
						<td id="serviceCivilStatus"><c:out value="${serviceCivilStatus}" escapeXml="false"/></td>
					</tr>
					<tr>
						<td><fmt:message key="label.info.service.infra"/></td>
						<td id="serviceInfraStatus"><c:out value="${serviceInfraStatus}" escapeXml="false"/></td>
					</tr>
					<tr>
						<td><fmt:message key="label.info.service.securite"/></td>
						<td id="serviceSecuriteStatus"><c:out value="${serviceSecuriteStatus}" escapeXml="false"/></td>
					</tr>
					<tr>
						<td><fmt:message key="label.info.service.brvplus"/></td>
						<td id="bvrPlusStatus"><c:out value="${bvrPlusStatus}" escapeXml="false"/></td>
					</tr>
				</tbody>
			</table>
		</fieldset>

        <fieldset id="stats.fieldset" class="info">
            <legend><span><fmt:message key="title.info.stats" /></span></legend>
            <div id="stats.output" class="asciiart"><c:out value="${stats}" escapeXml="false"/></div>

			<%-- Ajuste la taille du div stats.output par rapport à celle du div stats.fieldset, ceci pour
			     contourner une limitation du style 'overflow:auto' qui ne supporte pas des tailles relatives --%>
			<script type="text/javascript" language="Javascript1.3">
				var fieldset = E$('stats.fieldset');
				var output = E$('stats.output');
				output.style.width = fieldset.offsetWidth - 20;
			</script>

        </fieldset>

		<fieldset class="info">
			<legend><span><fmt:message key="title.info.cache" /></span></legend>
			<c:out value="${cacheStatus}" escapeXml="false"/>
		</fieldset>
		
		<authz:authorize ifAnyGranted="ROLE_TESTER, ROLE_ADMIN">
			<fieldset id="extprop.fieldset" class="info">
				<legend><span><fmt:message key="title.info.extprop" /></span></legend>
				<div id="extprop.output" class="output"><c:out value="${extProps}" escapeXml="false"/></div>
			</fieldset>

			<%-- Ajuste la taille du div extprop.output par rapport à celle du div extprop.div, ceci pour
			     contourner une limitation du style 'overflow:auto' qui ne supporte pas des tailles relatives --%>
			<script type="text/javascript" language="Javascript1.3">
				var fieldset = E$('extprop.fieldset');
				var output = E$('extprop.output');
				output.style.width = fieldset.offsetWidth - 20;
			</script>

			<fieldset id="log4j.fieldset" class="info">
				<legend><span><fmt:message key="title.info.log4j" /></span></legend>
				<table>
					<tr><td><fmt:message key="label.info.log4j.config"/></td><td><span class="value"><c:out value="${log4jConfig}"/></span></td></tr>
					<tr><td><fmt:message key="label.info.log4j.logfile"/></td><td><span class="value"><c:out value="${logFilename}"/></span></td></tr>
					<tr><td colspan="2"><fmt:message key="label.info.log4j.taillog"/></td></tr>
					<tr><td colspan="2"><div id="log4j.output" class="console"><c:out value="${tailLog}" escapeXml="false"/></div></td></tr>
				</table>
			
			</fieldset>

			<script type="text/javascript" language="Javascript1.3">

				var fieldset = E$('log4j.fieldset');
				var output = E$('log4j.output');
				output.style.width = fieldset.offsetWidth - 20;
			
				var logExecuter = new PeriodicalExecuter(function() {
					XT.doAjaxAction('updateTailLog', E$("log4j.output"), {},
						{ 
							clearQueryString: true,
							errorHandler : function(ajaxRequest, exception) {
								// on ignore les éventuelles erreurs
							}
		    			});
				}, 3);
				
				logExecuter.onTimerEvent();
				
			</script>
			
		</authz:authorize>

  	</tiles:put>
</tiles:insert>
