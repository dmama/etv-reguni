<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<c:set var="contextPath" scope="request" value="${pageContext.request.contextPath}" />

<script>

function submit(url, methodType, delegateName){
	var aForm = document.getElementById('formBean');
	
	aForm.action = url;
	aForm.submit();
}
</script>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
  	<tiles:put name="title"><fmt:message key="title.import.script.DBUnit" /></tiles:put>
  	
  	<tiles:put name="body">
		<fieldset>
			<legend><span><fmt:message key="label.importer.tiers" /></span></legend>

		    <form:form method="post" enctype="multipart/form-data" id="formBean" onsubmit="return confirm_trash_db()">
			    <table>
					<tr class="odd" >
						<td>
							<fmt:message key="label.admin.importScript"/>
						</td>
						<td><input type="file" id="scriptData" name="scriptData" size="100" />
						</td>
					</tr>
					<tr class="even" >
						<td><fmt:message key="label.admin.modeDBUnit"/></td>
						<td>
							<form:radiobutton path="mode" id="mode-clean-insert" value="CLEAN_INSERT"/>
							<fmt:message key="label.admin.modeDBUnit.CLEAN_INSERT"/>
							<br>
							<form:radiobutton path="mode" id="mode-delete-all" value="DELETE_ALL"/>
							<fmt:message key="label.admin.modeDBUnit.DELETE_ALL"/>
						</td>
					</tr>
					<tr class="odd" >
						<td><input type="submit" id="charger" value="<fmt:message key="label.bouton.charger"/>"/></td>
						<td><input type="button" value="<fmt:message key="label.bouton.annuler"/>" onclick="document.location='<c:url value="/index.do"/>';"/></td>
					</tr>
				</table>
			</form:form>
		</fieldset>
		<fieldset>
			<legend><span><fmt:message key="title.import.script.DBUnit" /></span></legend>
		    
		   	    <display:table 	name="listFilesName" id="descr" pagesize="20" defaultsort="2">
					<display:column titleKey="label.admin.dbunit.action" >
						<unireg:raccourciDemarrer id="loadFile-${descr.filename}" tooltip="Demarrer" onClick="if (confirm_trash_db()) {submit('tiersImport.do?fileName=${descr.filename}&action=launchUnit');}"/>
					</display:column>
					<display:column titleKey="label.admin.dbunit.name" >
						<c:out value="${descr.description}"  escapeXml="false"/>
					</display:column> 

					<display:setProperty name="paging.banner.all_items_found" value=""/>
				</display:table>
		</fieldset>
		<fieldset>
			<legend><span><fmt:message key="title.export.DBUnit" /></span></legend>
			<fmt:message key="label.export.DBUnit.message1"/>&nbsp;<b><c:out value="${tiersCount}"/></b>&nbsp;<fmt:message key="label.export.DBUnit.message2"/>
			<a href="JavaScript:location.reload(true);">
				<fmt:message key="label.bouton.refresh"/>
			</a>
			<br/>
			<br/>
		    <input type="button" value="<fmt:message key="label.bouton.exporter"/>" onclick="javascript:submit('dbdump.do?action=dump');"/>
		    <span class="error"><fmt:message key="label.export.DBUnit.export.remark"/></span>
		    <br/>
			<br/>
		    <input type="button" value="<fmt:message key="label.bouton.exporter.filesystem"/>" onclick="javascript:submit('dbdump.do?action=dump2fs');"/>
		    <span class="error"><fmt:message key="label.export.DBUnit.exportfs.remark"/></span>
		    <br/>
			<br/>
		    
	   	    <display:table name="fileDumps" id="file" pagesize="20" defaultsort="2" sort="list">
				<display:column>
					<unireg:document doc="${file}" />
				</display:column>
				<display:column titleKey="label.export.DBUnit.filename" >
					<a href="<c:out value='${contextPath}/common/docs.do?action=download&id=${file.id}'/>" />
						<c:out value="${file.nom}" />
					</a>
				</display:column>
				<display:column titleKey="label.export.DBUnit.nbTiers" >
					<c:out value="${file.nbTiers}" />
				</display:column>
				<display:column titleKey="label.export.DBUnit.creation.date" >
					<unireg:sdate sdate="${file.logCreationDate}"></unireg:sdate>
				</display:column>
				<display:column titleKey="label.export.DBUnit.filesize" >
					<c:out value="${file.fileSize}" />
				</display:column>
				<display:column>
					<a href="#" onclick="if (confirm_trash_db()) {javascript:submit('dbdump.do?action=fs2import&file=<c:out value="${file.id}" />');}">
						<fmt:message key="label.bouton.reimport"/>
					</a>
				</display:column>
				<display:column>
					<a href="#" onclick="javascript:submit('<c:out value='${contextPath}/common/docs.do?action=delete&id=${file.id}'/>');">
						<fmt:message key="label.bouton.delete"/>
					</a>
				</display:column>
				<display:setProperty name="paging.banner.no_items_found" value=""/>
				<display:setProperty name="paging.banner.one_item_found" value=""/>
				<display:setProperty name="paging.banner.some_items_found" value=""/>
				<display:setProperty name="paging.banner.all_items_found" value=""/>
			</display:table>
		</fieldset>
  	</tiles:put>
</tiles:insert>
