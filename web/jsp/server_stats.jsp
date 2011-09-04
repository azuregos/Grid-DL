<%@page import="ua.kpi.griddl.core.infrastructure.ServerStartupManager"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>

<h2>GRID-DL Server Status:</h2>
<table>
    <tr>
        <td>Server status:</td>
        <td><b><%= ServerStartupManager.getStatus()%></b></td>
    </tr>
    <tr>
        <td>Available processors:</td>
        <td><%= Runtime.getRuntime().availableProcessors()%></td>
    </tr>
    <tr>
        <td>Free memory (bytes):</td>
        <td><%= Runtime.getRuntime().freeMemory()%></td>
    </tr>
    <tr>
        <td>Maximum memory (bytes):</td>
        <td><%= Runtime.getRuntime().maxMemory() %></td>
    </tr>
    <tr>
        <td>Total memory (bytes)::</td>
        <td><%= Runtime.getRuntime().totalMemory() %></td>
    </tr>
</table>
<br/>

