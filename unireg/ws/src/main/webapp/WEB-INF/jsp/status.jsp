<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

  	<tiles:put name="title"><fmt:message key="title.info"/></tiles:put>

  	<tiles:put name="body">
  	
  		<a href="JavaScript:location.reload(true);">
			(<fmt:message key="label.bouton.refresh"/>)
		</a>

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

  	</tiles:put>
</tiles:insert>
