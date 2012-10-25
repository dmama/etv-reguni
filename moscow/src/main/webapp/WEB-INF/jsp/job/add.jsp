<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/jsp/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

	<tiles:put name="title">Ajout d'un nouveau job</tiles:put>

	<tiles:put name="body">

		<form:form commandName="job" action="add.do" method="POST" cssClass="form-horizontal">
			<legend>Ajout d'un nouveau job</legend>

			<div class="control-group">
				<label class="control-label" for="name">Nom :</label>

				<div class="controls">
					<form:input path="name" id="name"/><br/>
				</div>
			</div>
			<div class="control-group">
				<label class="control-label" for="name">Cron :</label>

				<div class="controls">
					<form:input path="cronExpression" id="cron"/> (*)<br/>
				</div>
			</div>
			<div class="control-group">
				<label class="control-label" for="dir">Répertoire d''import :</label>

				<div class="controls">
					<form:select id="dir" path="dirId"><form:options items="${directories}" itemValue="id" itemLabel="name"/></form:select><br/>
				</div>
			</div>

			<div class="form-actions">
				<input type="submit" value="Ajouter" class="btn btn-primary"/> ou <a href="<c:url value="/job/list.do"/>">annuler</a>
			</div>
		</form:form>

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

	</tiles:put>

</tiles:insert>