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

		    <form:form id="formBean" method="post" action="upload.do" enctype="multipart/form-data"  modelAttribute="script" onsubmit="return App.confirm_trash_db()">
			    <table>
					<tr class="odd" >
						<td >
							<fmt:message key="label.admin.importScript"/>
						</td>
						<td >
							<input type="file" id="scriptData" name="scriptData" size="50" />
						</td>
						<td width="1%" rowspan="2"><input type="submit" id="charger" style="padding-left:10em;padding-right:10em;padding-top:1em;padding-bottom:1em;" value="<fmt:message key="label.bouton.charger"/>"/></td>
					</tr>
					<tr class="odd" >
						<td/>
						<td colspan="2">
							<form:radiobutton path="mode" value="CLEAN_INSERT"/>
							<fmt:message key="label.admin.modeDBUnit.CLEAN_INSERT"/>
							<br>
							<form:radiobutton path="mode" value="DELETE_ALL"/>
							<fmt:message key="label.admin.modeDBUnit.DELETE_ALL"/>
							<br>
							<form:radiobutton path="mode" value="INSERT_APPEND"/>
							<fmt:message key="label.admin.modeDBUnit.INSERT_APPEND"/>
						</td>
					</tr>
				</table>
			</form:form>
		</fieldset>
		<fieldset>
			<legend><span><fmt:message key="title.import.script.DBUnit" /></span></legend>
		    
		   	    <display:table 	name="listFilesName" id="descr" pagesize="20" defaultsort="2">
					<display:column titleKey="label.admin.dbunit.action" >
						<unireg:raccourciDemarrer id="loadFile-${descr.filename}" tooltip="Demarrer" onClick="if (App.confirm_trash_db()) {submit('import.do?fileName=${descr.filename}&action=launchUnit');}"/>
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
		    <input type="button" value="<fmt:message key="label.bouton.exporter"/>" onclick="javascript:submit('${contextPath}/admin/dbdump/dump.do');"/>
		    <span class="error"><fmt:message key="label.export.DBUnit.export.remark"/></span>
		    <br/>
			<br/>
		    <input type="button" value="<fmt:message key="label.bouton.exporter.filesystem"/>" onclick="javascript:submit('${contextPath}/admin/dbdump/dump2fs.do');"/>
		    <span class="error"><fmt:message key="label.export.DBUnit.exportfs.remark"/></span>
		    <br/>
			<br/>
		    
	   	    <display:table name="fileDumps" id="file" pagesize="20" defaultsort="2" sort="list">
				<display:column>
					<unireg:document doc="${file}" />
				</display:column>
				<display:column titleKey="label.export.DBUnit.filename" >
					<unireg:linkTo name="${file.nom}" action="/common/docs/download.do" params="{id:${file.id}}"/>
				</display:column>
				<display:column titleKey="label.export.DBUnit.nbTiers" >
					<c:out value="${file.nbTiers}" />
				</display:column>
				<display:column titleKey="label.export.DBUnit.creation.date" >
					<unireg:sdate sdate="${file.logCreationDate}"/>
				</display:column>
				<display:column titleKey="label.export.DBUnit.filesize" >
					<c:out value="${file.fileSize}" />
				</display:column>
				<display:column>
					<unireg:linkTo name="Réimporter" action="/admin/dbdump/fs2import.do" params="{file:${file.id}}" method="POST"
					               confirm="Attention ! Cette opération va détruire les données existantes de la base.\n\nVoulez-vous vraiment continuer ?"/>
				</display:column>
				<display:column>
					<unireg:linkTo name="Supprimer" action="/common/docs/delete.do" params="{id:${file.id}}" method="POST"
					               confirm="Voulez-vous vraiment supprimer ce fichier ?"/>
				</display:column>
				<display:setProperty name="paging.banner.no_items_found" value=""/>
				<display:setProperty name="paging.banner.one_item_found" value=""/>
				<display:setProperty name="paging.banner.some_items_found" value=""/>
				<display:setProperty name="paging.banner.all_items_found" value=""/>
			</display:table>
		</fieldset>
  	</tiles:put>
</tiles:insert>
