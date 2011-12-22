<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

  	<tiles:put name="title">Ajout d'un nouveau job</tiles:put>

  	<tiles:put name="body">

  		<h1>Ajout d'un nouveau job</h1>

		<form:form commandName="job" action="add.do" method="POST">
			<fieldset class="ui-widget-content">
				<label for="name">Nom :</label><form:input path="name" id="name"/><br/>
				<label for="name">Cron :</label><form:input path="cronExpression" id="cron"/> (*)<br/>
				<label for="env">Répertoire d''import :</label><form:select id="dir" path="dirId"><form:options items="${directories}" itemValue="id" itemLabel="name"/></form:select><br/>
				<hr/>
				<span>
					(*) cron format :
					<ul>
					  <li><code>0 0 12 * * ?</code> => Fire at 12pm (noon) every day</li>
					  <li><code>0 15 10 * * ?</code> => Fire at 10:15am every day</li>
					  <li><code>0 0/5 14 * * ?</code> => Fire every 5 minutes starting at 2pm and ending at 2:55pm, every day</li>
					  <li><code>0 0-5 14 * * ?</code> => Fire every minute starting at 2pm and ending at 2:05pm, every day</li>
					  <li><code>0 0 8-18 ? * MON-FRI *</code> => Fire every hour starting at 8am and ending at 6pm, monday to friday</li>
					</ul>
				</span>
			</fieldset><br/>
			<input type="submit" value="Ajouter"/> ou <a href="<c:url value="/job/list.do"/>">annuler</a>
		</form:form>

  	</tiles:put>

</tiles:insert>