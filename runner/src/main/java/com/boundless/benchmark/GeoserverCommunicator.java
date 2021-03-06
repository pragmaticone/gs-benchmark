/**
 * 
 */
package com.boundless.benchmark;

import java.io.File;
import java.io.IOException;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Soumya Sengupta
 * 
 */
public abstract class GeoserverCommunicator extends AbstractBenchmarkComponent {
	final static Logger logger = LoggerFactory
			.getLogger(GeoserverCommunicator.class);

	private String geoserverHostAndPort, geoserverUsername, geoserverPassword;

	/**
	 * @return the geoserverHostAndPort
	 */
	public String getGeoserverHostAndPort() {
		return geoserverHostAndPort;
	}

	/**
	 * @param geoserverHostAndPort
	 *            the geoserverHostAndPort to set
	 */
	public void setGeoserverHostAndPort(String geoserverHostAndPort) {
		this.geoserverHostAndPort = geoserverHostAndPort;
	}

	/**
	 * @return the geoserverUsername
	 */
	public String getGeoserverUsername() {
		return geoserverUsername;
	}

	/**
	 * @param geoserverUsername
	 *            the geoserverUsername to set
	 */
	public void setGeoserverUsername(String geoserverUsername) {
		this.geoserverUsername = geoserverUsername;
	}

	/**
	 * @return the geoserverPassword
	 */
	public String getGeoserverPassword() {
		return geoserverPassword;
	}

	/**
	 * @param geoserverPassword
	 *            the geoserverPassword to set
	 */
	public void setGeoserverPassword(String password) {
		this.geoserverPassword = password;
	}

	protected Object process(HttpUriRequest request, int expectedResponseCode)
			throws Exception {
		String[] geoserverHostAndPort = this.getGeoserverHostAndPort().split(
				":");
		String geoserverHost = geoserverHostAndPort[0];
		int geoserverPort = Integer.parseInt(geoserverHostAndPort[1]);
		String username = this.getGeoserverUsername();
		String password = this.getGeoserverPassword();

		CredentialsProvider credsProvider = new BasicCredentialsProvider();
		credsProvider.setCredentials(
				new AuthScope(geoserverHost, geoserverPort),
				new UsernamePasswordCredentials(username, password));

		CloseableHttpClient httpClient = null;
		CloseableHttpResponse response = null;

		try {
			logger.debug("Executing request: " + request.getRequestLine());

			// Do basic authentication first.
			httpClient = HttpClients.custom()
					.setDefaultCredentialsProvider(credsProvider).build();

			// Execute the request.
			response = httpClient.execute(request);
			logger.debug("Response status line: " + response.getStatusLine());
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				response.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return response.getStatusLine().getStatusCode() == expectedResponseCode;
	}

	public boolean checkIfWorkspaceExists(String workspaceName)
			throws Exception {
		String geoserverHostAndPort = this.getGeoserverHostAndPort();

		HttpGet request = new HttpGet("http://" + geoserverHostAndPort
				+ "/geoserver/rest/workspaces/" + workspaceName);
		request.addHeader("Accept", ContentType.APPLICATION_JSON.toString());

		return (Boolean) process(request, 404);
	}

	public boolean deleteWorkspace(String workspaceName) throws Exception {
		String geoserverHostAndPort = this.getGeoserverHostAndPort();

		HttpDelete request = new HttpDelete("http://" + geoserverHostAndPort
				+ "/geoserver/rest/workspaces/" + workspaceName
				+ "?recurse=true");
		request.addHeader("Accept", ContentType.APPLICATION_JSON.toString());

		return (Boolean) process(request, 200);
	}

	public boolean createWorkspace(String workspaceName) throws Exception {
		String geoserverHostAndPort = this.getGeoserverHostAndPort();

		logger.debug("About to create workspace: " + workspaceName);
		HttpPost request = new HttpPost("http://" + geoserverHostAndPort
				+ "/geoserver/rest/workspaces");
		request.addHeader("Accept", ContentType.APPLICATION_JSON.toString());
		request.setEntity(EntityBuilder.create()
				.setText("{'workspace': {'name': '" + workspaceName + "'}}")
				.setContentType(ContentType.APPLICATION_JSON).build());

		return (Boolean) process(request, 201);
	}

	public boolean loadDataStore(String workspaceName, String dataStoreName,
			String filePath) throws Exception {
		String geoserverHostAndPort = this.getGeoserverHostAndPort();

		logger.debug("About to load data from [" + filePath
				+ "] into data store [" + dataStoreName + "] in workspace ["
				+ workspaceName + "]");
		HttpPut request = new HttpPut("http://" + geoserverHostAndPort
				+ "/geoserver/rest/workspaces/" + workspaceName
				+ "/datastores/" + dataStoreName
				+ "/file.shp?configure=all&target=shp");
		request.addHeader("Content-type", "application/zip");
		request.setEntity(EntityBuilder.create().setFile(new File(filePath))
				.build());

		return (Boolean) process(request, 201);
	}

	public boolean createDatastore(String workspaceName, String datastoreName,
			String dbHost, String dbPort, String username, String password,
			String dbName) throws Exception {
		String geoserverHostAndPort = this.getGeoserverHostAndPort();
		String entity = "{" + "'dataStore': {" + "'name': '" + datastoreName
				+ "'," + "'connectionParameters': {" + "'host': '" + dbHost
				+ "'," + "'port': '" + dbPort + "'," + "'database': '" + dbName
				+ "'," + "'user': '" + username + "'," + "'geoserverPassword': '"
				+ password + "'," + "'dbtype': 'postgis'" + "	}" + "}" + "}";

		logger.debug("About to create datastore [ " + datastoreName
				+ "] in workspace [" + workspaceName + "]");
		HttpPost request = new HttpPost("http://" + geoserverHostAndPort
				+ "/geoserver/rest/workspaces/" + workspaceName + "/datastores");
		request.addHeader("Accept", ContentType.APPLICATION_JSON.toString());
		request.setEntity(EntityBuilder.create().setText(entity)
				.setContentType(ContentType.APPLICATION_JSON).build());

		return (Boolean) process(request, 201);
	}
}
