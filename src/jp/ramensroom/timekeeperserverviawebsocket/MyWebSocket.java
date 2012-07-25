package jp.ramensroom.timekeeperserverviawebsocket;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.eclipse.jetty.websocket.WebSocket;

public class MyWebSocket implements WebSocket.OnTextMessage {

	// 鯖に対してコネクションが途切れないように定期的に送るメッセージ
	private static final String KEEP_ALIVE = "keep_alive";
	
	// コネクションID
	private int myID;
	private Connection connection;

	// すべてのコネクションセット
	static Set<MyWebSocket> wsConnections = new CopyOnWriteArraySet<MyWebSocket>();

	public MyWebSocket(int id) {
		myID = id;
	}

	@Override
	public void onOpen(Connection conn) {
		connection = conn;
		connection.setMaxIdleTime(Integer.MAX_VALUE);

		System.out.println("Connection Added:" + myID);
		System.out.println(" - MaxIdleTime:" + connection.getMaxIdleTime());
		System.out.println(" - MaxTextMessageSize:" + connection.getMaxTextMessageSize());

		synchronized (connection) {
			wsConnections.add(this);
		}
	}

	@Override
	public void onMessage(String msg) {
		// KEEP_ALIVEメッセージなら無視、ソレ以外なら処理
		if(!msg.equals(KEEP_ALIVE)){
			// 命令データを展開
			String[] instruction = msg.split(",");

			if(instruction.length > 1){
				String cmd = instruction[0];
				String[] time = instruction[1].split(":");
				
				// MainWindow内で処理をタノム('ω'人)
				MainWindow.putInstruction(cmd, time);
			}
		}

		for (MyWebSocket ws : wsConnections) {
			try {
				// 自分以外の接続クライアントに対してメッセージ送信
				if(ws.myID != this.myID){
						System.out.println("Get Message:" + msg);
						ws.connection.sendMessage(msg);
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	@Override
	public void onClose(int parameter, String msg) {
		System.out.println(parameter + " disconnect. -> " + msg);
		synchronized (connection) {
			wsConnections.remove(this);
		}
	}
}
