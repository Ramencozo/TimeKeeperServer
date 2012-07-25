package jp.ramensroom.timekeeperserverviawebsocket;


import javax.servlet.http.HttpServletRequest;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocketHandler;

public class WebSocketServer extends Server{
	
	private SelectChannelConnector connector;
	private WebSocketHandler wsHandler;
	private ResourceHandler resHandler;

	private int idCounter;		// クライアントに付与するコネクションIDのカウンター
	
	public WebSocketServer(int port, String rootDirPath) {
		idCounter = 0;
		
		connector = new SelectChannelConnector();
		connector.setPort(port);
		addConnector(connector);
		
		wsHandler = new WebSocketHandler() {
			@Override
			public WebSocket doWebSocketConnect(HttpServletRequest request, String protocol) {
				idCounter++;
				System.out.printf("Protocol:%s\n", protocol);
				return new MyWebSocket(idCounter);	// WebSocketコネクションを返す
			}
		};
		
		resHandler = new ResourceHandler();
		resHandler.setDirectoriesListed(true);
		resHandler.setResourceBase(rootDirPath);
		
		wsHandler.setHandler(resHandler);
		setHandler(wsHandler);
	}	
}
