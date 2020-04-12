<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
<xsl:output method="xml" encoding="utf-8" omit-xml-declaration="yes" />

<xsl:param name="home"/>

<xsl:template match="/Status">
<html>
	<head>
		<title>Queue Status</title>
		<link rel="Stylesheet" type="text/css" media="all" href="/BaseStyles.css"></link>
		<style>
			td {font-family: Arial, Helvetica, sans-serif;}
			td.left { text-align: left; }
			td.right { text-align: right; }
			td.name { text-align: left; font-weight: bold }
			td.destination { font-family: monospace; }
			td.center { text-align: center; }
			td {font-family: Arial, Helvetica, sans-serif; font-size: 14pt; padding-left: 5pt; padding-right: 5pt;}
			th.left { text-align: left; }
			th.right { text-align: center; }
			th.center { text-align: center; }
			th {font-family: Arial, Helvetica, sans-serif; font-size: 16pt; padding-left: 5pt; padding-right: 5pt;}
		</style>
			
	</head>
	<body>
	<center>
	<h1>Queue Status</h1>
		<table border="0">
			<tr>
				<td class="right">SCP IP Address:</td>
				<td><xsl:value-of select="@ip"/></td>
			</tr>
			<tr>
				<td class="right">SCP Port:</td>
				<td><xsl:value-of select="@port"/></td>
			</tr>
			<tr>
				<td class="right">SCP AE Title:</td>
				<td><xsl:value-of select="@ae"/></td>
			</tr>
		</table>
		<br/>
		<table border="0">
			<tr>
				<th class="left">Name</th>
				<th>Priority</th>
				<th class="left">Destination</th>
				<th>Size</th>
				<th>Count</th>
			</tr>
			<xsl:for-each select="Queue">
				<tr>
					<td class="name"><xsl:value-of select="@name"/></td>
					<td class="center"><xsl:value-of select="@priority"/></td>
					<td class="destination"><xsl:value-of select="@destination"/></td>
					<td class="right"><xsl:value-of select="@size"/></td>
					<td class="right"><xsl:value-of select="@count"/></td>
				</tr>
			</xsl:for-each>
		</table>
	</center>
	</body>
</html>
</xsl:template>

</xsl:stylesheet>