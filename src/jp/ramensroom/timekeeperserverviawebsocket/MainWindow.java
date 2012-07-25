package jp.ramensroom.timekeeperserverviawebsocket;


import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.SpinnerListModel;

import java.awt.Font;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.Future;

import javax.swing.SwingConstants;
import javax.swing.JButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocket.Connection;
import org.eclipse.jetty.websocket.WebSocketClient;
import org.eclipse.jetty.websocket.WebSocketClientFactory;

public class MainWindow extends JFrame{
	
	private static final String TITLE = "TimeKeeperServer via WebSocket";
	private static final int WIDTH = 1024;
	private static final int HEIGHT = 512;
		
	private static final String MESSAGE_START_SERVER = "Start Server";
	private static final String MESSAGE_STOP_SERVER = "Stop Server";
	
	// http://鯖IPアドレス:8000 で稼働
	private static final int SERVER_PORT = 8000;
	private static final String SERVER_DOCROOT = "./html";
	
	// JSpinnerの中身
	private static final String[] SPINNER_NUMBERS = {	"00", "01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20",
														"21", "22", "23", "24", "25", "26", "27", "28", "29", "30", "31", "32", "33", "34", "35", "36", "37", "38", "39", "40",
														"41", "42", "43", "44", "45", "46", "47", "48", "49", "50", "51", "52", "53", "54", "55", "56", "57", "58", "59"};

	private static final int EXTERNAL_BUFFER_SIZE = 128000;	// 制限時間通知時の音源のバッファサイズ規定

	// 鯖周辺クラス
	private WebSocketServer server;
	private Thread serverThread; 
	private boolean isServerInitialized;

	// WebSocketクライアント周辺クラス
	private WebSocketClient webSocketClient;
	private static Connection connection;
	
	// 時間計測クラス
	private static TimeManager timeManager;

	//-- GUIパーツ --
	private JLabel serverStateLabel;		// 鯖起動状態 JLabel
	private JToggleButton bootServerToggleButton;	// 鯖 起動/停止トグルボタン
	
	private static JLabel hourLabel;	// 時 表示 JLabel
	private static JLabel minuteLabel;	// 分 表示 JLabel
	private static JLabel secondLabel;	// 秒 表示 JLabel
	
	private static JSpinner hourSpinner;	// 時 設定 JSpinner
	private static JSpinner minuteSpinner;	// 分 設定 JSpinner
	private static JSpinner secondSpinner;	// 秒 設定 JSpinner
	
	private static JButton startPauseButton;	// カウントダウン開始/一時停止 ボタン
	private JButton resetButton;				// リセットボタン
	
	private static boolean isCounting;	// カウントダウン状態フラグ
	
	private Thread countDownThread;	// カウントダウン処理Thread
	
	private static String currentHour;		// 設定 時 保持変数
	private static String currentMinute;	// 設定 分 保持変数
	private static String currentSecond;	// 設定 秒 保持変数
	
	public MainWindow() {

		isCounting = false;
		
		// TimeManagerのインスタンス生成、00:00:00 の初期値を入れとく
		timeManager = new TimeManager(0, 0, 0);

		currentHour = "00";
		currentMinute = "00";
		currentSecond = "00";
				
		// カウントダウン処理Thread
		countDownThread = new Thread(){
			@Override
			public void run() {
				while (true) {
					try {
						// カウントダウン開始状態？
						if(isCounting){
							System.out.println("CountDown.");

							// TimeManager内でカウントダウン処理、00時00分00秒になったときにtrueを返すメソッド
							if(timeManager.countDownLimitNotify()){
								// カウントダウン終了、停止
								System.out.println("Time Up !");
								isCounting = false;
								
								// 音を鳴らすThread
								new Thread(){
									public void run() {
										// Ωヾ(´ω｀　)カーンカーンカーン (GUIが固まらないようにスレッドで鳴らす)
										try {
											File soundFile = new File("./snd/second_gong.wav");	// classファイルまたはjarでこのアプリを実行するとき、sndというフォルダにsecond_gong.wavっていうファイルを置いといてください
											AudioInputStream audioInputStream;					// (でないと音が鳴らないです)
											audioInputStream = AudioSystem.getAudioInputStream(soundFile);
											AudioFormat format = audioInputStream.getFormat();

											DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
											SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
											line.open(format);
											line.start();

											int byteRead = 0;
											byte[] data = new byte[EXTERNAL_BUFFER_SIZE];
											while (byteRead != -1) {
												byteRead = audioInputStream.read(data, 0, data.length);
												if (byteRead >= 0) {
													int byteWritten = line.write(data, 0, byteRead);
												}
											}
											line.drain();
											line.close();
										} catch (UnsupportedAudioFileException e) {
											// TODO Auto-generated catch block
											e.printStackTrace();
										} catch (IOException e) {
											// TODO Auto-generated catch block
											e.printStackTrace();
										} catch (LineUnavailableException e) {
											// TODO Auto-generated catch block
											e.printStackTrace();
										}
									};
								}.start();
								
							// カウントダウンがまだ終わってない
							}else{
								// JLabelに現在の時分秒を書き込み
								hourLabel.setText(timeManager.getHour(TimeManager.FORMATTED_MODE));
								minuteLabel.setText(timeManager.getMinute(TimeManager.FORMATTED_MODE));
								secondLabel.setText(timeManager.getSecond(TimeManager.FORMATTED_MODE));

								System.out.println( timeManager.getHour(TimeManager.FORMATTED_MODE) + ":" +
													timeManager.getMinute(TimeManager.FORMATTED_MODE) + ":" +
													timeManager.getSecond(TimeManager.FORMATTED_MODE));
								try {
									// 鯖が稼働状態ならWebSocketで接続されているクライアントに現在の時分秒を投げる
									if(bootServerToggleButton.isSelected()){
										connection.sendMessage( timeManager.getHour(TimeManager.FORMATTED_MODE) + ":" +
																timeManager.getMinute(TimeManager.FORMATTED_MODE) + ":" +
																timeManager.getSecond(TimeManager.FORMATTED_MODE));
									}
								} catch (IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
						}
						// 1秒ごとに処理
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		};
		countDownThread.start();
		
		Container container = getContentPane();
		JPanel parentPanel = new JPanel();
		parentPanel.setLayout(new BorderLayout(0, 0));
		
		JPanel timeViewPanel = new JPanel();
		timeViewPanel.setBackground(Color.BLACK);
		parentPanel.add(timeViewPanel, BorderLayout.CENTER);
		
		// 現在の時表示 JLabel
		hourLabel = new JLabel("00");
		hourLabel.setHorizontalAlignment(SwingConstants.CENTER);
		hourLabel.setForeground(Color.GREEN);
		hourLabel.setFont(new Font("Arial", Font.PLAIN, 230));
		timeViewPanel.add(hourLabel);
		
		JLabel colon01Label = new JLabel(":");
		colon01Label.setHorizontalAlignment(SwingConstants.CENTER);
		colon01Label.setForeground(Color.GREEN);
		colon01Label.setFont(new Font("Arial", Font.PLAIN, 230));
		timeViewPanel.add(colon01Label);
		
		// 現在の分表示 JLabel
		minuteLabel = new JLabel("00");
		minuteLabel.setHorizontalAlignment(SwingConstants.CENTER);
		minuteLabel.setForeground(Color.GREEN);
		minuteLabel.setFont(new Font("Arial", Font.PLAIN, 230));
		timeViewPanel.add(minuteLabel);
		
		JLabel colon02Label = new JLabel(":");
		colon02Label.setHorizontalAlignment(SwingConstants.CENTER);
		colon02Label.setForeground(Color.GREEN);
		colon02Label.setFont(new Font("Arial", Font.PLAIN, 230));
		timeViewPanel.add(colon02Label);
		
		// 現在の秒表示 JLabel
		secondLabel = new JLabel("00");
		secondLabel.setHorizontalAlignment(SwingConstants.CENTER);
		secondLabel.setForeground(Color.GREEN);
		secondLabel.setFont(new Font("Arial", Font.PLAIN, 230));
		timeViewPanel.add(secondLabel);
		
		container.add(parentPanel);
		
		JPanel settingPanel = new JPanel();
		parentPanel.add(settingPanel, BorderLayout.SOUTH);
		
		serverStateLabel = new JLabel("●");
		serverStateLabel.setForeground(Color.RED);
		serverStateLabel.setFont(new Font("MS UI Gothic", Font.PLAIN, 30));
		settingPanel.add(serverStateLabel);
		
		bootServerToggleButton = new JToggleButton("Boot Server");
		bootServerToggleButton.setFont(new Font("MS UI Gothic", Font.PLAIN, 30));
		bootServerToggleButton.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent arg0) {
				if(bootServerToggleButton.isSelected()){
					// 起動ステータスを書き換え(緑色の●)
					serverStateLabel.setForeground(Color.GREEN);
				}else{
					// 起動ステータスを書き換え(赤色の●)
					serverStateLabel.setForeground(Color.RED);
				}
			}
		});
		settingPanel.add(bootServerToggleButton);

		isServerInitialized = false;
		
		serverThread = new Thread(){
			public void run() {
				// Thread自体はずっとWhileループで稼働
				while (true) {
					// Boot Serverボタンが押されてる状態？
					if(bootServerToggleButton.isSelected()){
						// Serverが初期化「されていない」状態？(起動処理してない状態？)
						if(!isServerInitialized){
							// してないなら起動(server.join()がブロッキングなソレなのでさらに新規Thread)
							new Thread(){
								@Override
								public void run() {
									try {
										System.out.println("Starting Server...");
										server = new WebSocketServer(SERVER_PORT, SERVER_DOCROOT);
										server.start();
										server.join();
									} catch (Exception e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
								}
							}.start();
							
							// 鯖初期化しますた
							isServerInitialized = true;
							
							// 鯖自身に接続するWebSocketクライアントの生成 (他のクライアントにメッセージ投げたりするのに使う)
							new Thread(){
								public void run() {
									try {
										WebSocketClientFactory factory = new WebSocketClientFactory();
										factory.start();
										webSocketClient = factory.newWebSocketClient();
										
										Future<Connection> futureConnection = webSocketClient.open(new URI("ws://localhost:" + SERVER_PORT) , new WebSocket.OnTextMessage() {
											@Override
											public void onOpen(Connection arg0) {
												// 他クライアントからへのレスポンスは無し
											}
											
											@Override
											public void onClose(int arg0, String arg1) {
												// 他クライアントからへのレスポンスは無し
											}
											
											@Override
											public void onMessage(String msg) {
												// 他クライアントからへのレスポンスは無し
											}
										});
										connection = futureConnection.get();
									} catch (Exception e1) {
										// TODO Auto-generated catch block
										e1.printStackTrace();
									}
								}
							}.start();
						}
						
					// Boot Serverボタンが押されてない状態で、
					}else{
						//鯖が動いてるなら鯖を止める処理
						if(isServerInitialized){
							try {
								System.out.println("Stopping Server...");
								server.stop();
								server.destroy();
								server = null;
								System.out.println("Server Stopped.");
							} catch (Exception e) {
								e.printStackTrace();
							}
							isServerInitialized = false;
						}
					}
				}
			}
		};
		serverThread.start();
		
		JLabel label = new JLabel("　　時:");
		label.setFont(new Font("MS UI Gothic", Font.PLAIN, 32));
		settingPanel.add(label);
		
		// 時指定のJSpinner
		SpinnerListModel hourModel = new SpinnerListModel(SPINNER_NUMBERS);
		hourSpinner = new JSpinner();
		hourSpinner.setModel(hourModel);
		hourSpinner.setFont(new Font("MS UI Gothic", Font.PLAIN, 32));
		hourSpinner.setPreferredSize(new Dimension(70, 50));
		hourSpinner.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				// JSpinnerの値を変えるたびにTimeManagerとJLabelの値を変更
				String hour = (String)hourSpinner.getValue();
				currentHour = hour;
				hourLabel.setText(hour);
				timeManager.setHour(Integer.parseInt(hour));
				System.out.println("Set Hour: " + Integer.parseInt(hour));
			}
		});
		settingPanel.add(hourSpinner);
		
		JLabel label_1 = new JLabel(" 分:");
		label_1.setFont(new Font("MS UI Gothic", Font.PLAIN, 32));
		settingPanel.add(label_1);
		
		// 分指定のJSpinner
		SpinnerListModel minuteModel = new SpinnerListModel(SPINNER_NUMBERS);
		minuteSpinner = new JSpinner();
		minuteSpinner.setModel(minuteModel);
		minuteSpinner.setFont(new Font("MS UI Gothic", Font.PLAIN, 32));
		minuteSpinner.setPreferredSize(new Dimension(70, 50));
		minuteSpinner.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				// JSpinnerの値を変えるたびにTimeManagerとJLabelの値を変更
				String minute = (String)minuteSpinner.getValue();
				currentMinute = minute;
				minuteLabel.setText(minute);
				timeManager.setMinute(Integer.parseInt(minute));

				System.out.println("Set Minute: " + Integer.parseInt(minute));
			}
		});
		settingPanel.add(minuteSpinner);
		
		JLabel label_2 = new JLabel(" 秒:");
		label_2.setFont(new Font("MS UI Gothic", Font.PLAIN, 32));
		settingPanel.add(label_2);
		
		// 秒指定のJSpinner
		SpinnerListModel secondModel = new SpinnerListModel(SPINNER_NUMBERS);
		secondSpinner = new JSpinner();
		secondSpinner.setModel(secondModel);
		secondSpinner.setFont(new Font("MS UI Gothic", Font.PLAIN, 32));
		secondSpinner.setPreferredSize(new Dimension(70, 50));
		secondSpinner.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent ce) {
				// JSpinnerの値を変えるたびにTimeManagerとJLabelの値を変更
				String second = (String)secondSpinner.getValue();
				currentSecond = second;
				secondLabel.setText(second);
				timeManager.setSecond(Integer.parseInt(second));

				System.out.println("Set Second: " + Integer.parseInt(second));
			}
		});
		settingPanel.add(secondSpinner);
		
		startPauseButton = new JButton("Start");
		startPauseButton.setFont(new Font("MS UI Gothic", Font.PLAIN, 32));
		startPauseButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				// カウントダウン中？
				if(isCounting){
					// カウントダウンしてたら一時停止
					isCounting = false;
					startPauseButton.setText("Start");
					System.out.println("Count Down Pause: " + timeManager.getHour(TimeManager.FORMATTED_MODE) + ":"
															+ timeManager.getMinute(TimeManager.FORMATTED_MODE) + ":"
															+ timeManager.getSecond(TimeManager.FORMATTED_MODE));
				}else{
					// カウントダウンしてなかったら開始
					isCounting = true;
					startPauseButton.setText("Pause");
					System.out.println("Count Down Start: " + timeManager.getHour(TimeManager.FORMATTED_MODE) + ":"
															+ timeManager.getMinute(TimeManager.FORMATTED_MODE) + ":"
															+ timeManager.getSecond(TimeManager.FORMATTED_MODE));
				}
			}
		});
		settingPanel.add(startPauseButton);
		
		resetButton = new JButton("Reset");
		resetButton.setFont(new Font("MS UI Gothic", Font.PLAIN, 32));
		resetButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				// カウントダウン停止
				isCounting = false;

				// さきほどセットした時間を再度セット
				timeManager.setHour(Integer.parseInt(currentHour));
				timeManager.setMinute(Integer.parseInt(currentMinute));
				timeManager.setSecond(Integer.parseInt(currentSecond));
				
				// JLabelもセット
				hourLabel.setText(currentHour);
				minuteLabel.setText(currentMinute);
				secondLabel.setText(currentSecond);
				
				startPauseButton.setText("Start");
			}
		});
		settingPanel.add(resetButton);
		
		setTitle(TITLE);
		setSize(WIDTH, HEIGHT);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setVisible(true);
	}
		
	public static void main(String[] args) {
		new MainWindow();
	}
	
	// クライアントからの命令判定メソッド
	// 主にMyWebSocketのonMessageから呼び出す想定
	public static void putInstruction(String cmd, Object obj){
		// 時間セット命令
		if(cmd.equals("set")){
			String[] time = (String[])obj;
			
			// 「現時点で指定された時間」として保持 (Reset時にこの値を使う)
			currentHour = time[0];
			currentMinute = time[1];
			currentSecond = time[2];
			
			// TimeManagerクラスに値をセット
			timeManager.setHour(Integer.parseInt(currentHour));
			timeManager.setMinute(Integer.parseInt(currentMinute));
			timeManager.setSecond(Integer.parseInt(currentSecond));
			
			// JLabel書き換え
			hourLabel.setText(currentHour);
			minuteLabel.setText(currentMinute);
			secondLabel.setText(currentSecond);
			
			// JSpinnerも書き換え
			hourSpinner.setValue(currentHour);
			minuteSpinner.setValue(currentMinute);
			secondSpinner.setValue(currentSecond);

			try {
				// クライアントに向けてWebSocketで現在時刻を投げる
				connection.sendMessage(currentHour + ":" + currentMinute + ":" + currentSecond);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		
		 // カウントダウン開始命令
		}else if(cmd.equals("start")){
			 // カウントダウンするか否かのフラグをセット
			isCounting = true;
			
			startPauseButton.setText("Pause");

			System.out.println("Count Down Start: " + timeManager.getHour(TimeManager.FORMATTED_MODE) + ":"
													+ timeManager.getMinute(TimeManager.FORMATTED_MODE) + ":"
													+ timeManager.getSecond(TimeManager.FORMATTED_MODE));
		 // カウントダウン一時停止命令
		}else if (cmd.equals("pause")) {
			isCounting = false;
			
			startPauseButton.setText("Start");

			System.out.println("Count Down Pause: " + timeManager.getHour(TimeManager.FORMATTED_MODE) + ":"
													+ timeManager.getMinute(TimeManager.FORMATTED_MODE) + ":"
													+ timeManager.getSecond(TimeManager.FORMATTED_MODE));
		 // リセット命令
		}else if (cmd.equals("reset")) {
			 // カウントダウン止める
			isCounting = false;
			
			startPauseButton.setText("Start");
			
			 // 先ほど指定された時間をもう一度セット
			timeManager.setHour(Integer.parseInt(currentHour));
			timeManager.setMinute(Integer.parseInt(currentMinute));
			timeManager.setSecond(Integer.parseInt(currentSecond));

			 // JLabelもセット
			hourLabel.setText(currentHour);
			minuteLabel.setText(currentMinute);
			secondLabel.setText(currentSecond);
			
			try {
				 // クライアントにWebSocketで投げる
				connection.sendMessage(currentHour + ":" + currentMinute + ":" + currentSecond);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}	
}
