package jp.ramensroom.timekeeperserverviawebsocket;

public class TimeManager{
	public static final int SIMPLE_MODE = 1;	// 単純にint型で示される値と同じ表示にするときのソレ → 0, 1, 20 等
	public static final int FORMATTED_MODE = 2;	// データが0～9のときに0を付加した表示にするときのソレ → 00, 01, 20等
	
	private int hour;
	private int minute;
	private int second;
	
	// コンストラクタ
	public TimeManager() {
		hour = 0;
		minute = 0;
		second = 0;
	}

	// すぐさま値を入れる場合のコンストラクタ
	public TimeManager(int hour, int minute, int second) {
		this.hour = hour;
		this.minute = minute;
		this.second = second;
	}
	
	public int getHour(){
		return hour;
	}
	public String getHour(int mode){
		if(mode == SIMPLE_MODE){
			return String.valueOf(hour);
		}else{
			return (hour <= 9 ? "0" + hour : String.valueOf(hour));	// hourが0～9のときに 00～09として返す処理
		}
	}

	public int getMinute(){
		return minute;
	}
	public String getMinute(int mode){
		if(mode == SIMPLE_MODE){
			return String.valueOf(minute);
		}else{
			return (minute <= 9 ? "0" + minute : String.valueOf(minute));
		}
	}

	public int getSecond(){
		return second;
	}
	public String getSecond(int mode){
		if(mode == SIMPLE_MODE){
			return String.valueOf(second);
		}else{
			return (second <= 9 ? "0" + second : String.valueOf(second));
		}
	}
	
	// (String) XX:XX:XX の形式で時間を返すメソッド
	public String getTime(){
		String h = (hour > 9 ? "" + hour : "0" + hour);
		String m = (minute > 9 ? "" + minute : "0" + minute);
		String s = (second > 9 ? "" + second : "0" + second);
		return (h + ":" + m + ":" + s);
	}
	
	// hourに値をセット(2桁までなので99以上の値はリセット)
	public void setHour(int h){
		if(h > 0){
			hour = h % 99;
		}else{
			hour = 0;
		}
	}
	// minuteに値をセット(0～59分の範囲で値を入れる)
	public void setMinute(int m){
		if(m > 0){
			minute = m % 60;
		}else{
			minute = 0;
		}
	}
	public void setSecond(int s){
		if(s > 0){
			second = s % 60;
		}else{
			second = 0;
		}
	}
	
	// カウントアップ
	public void countUp(){
		second++;

		if(second > 59){	//60秒に至ってたらsecondを00にしてminute++
			second = 0;
			minute++;
		}
		
		if(minute > 59){	//60分に至ってたらminuteを00にしてhour++
			minute = 0;
			hour++;
		}
	}

	// カウントダウン
	public void countDown(){		
		second--;
		
		if(second < 0){	// 00秒を下回っていたら59秒に戻してminute--
			second = 59;
			minute--;
		}
		
		if(minute < 0){	// おんなじようなことをする
			minute = 59;
			hour--;
		}
		
		if(hour < 0){	// 上記2点を実行したうえでhourすらも0未満ならそれは時が満ちたということだ…
			second = 0;
			minute = 0;
			hour = 0;
		}
	}

	// ↑のcountDown()に、00:00:00になったときだけtrueを返し、それ以外ならfalseを返す処理をつけたもの
	public boolean countDownLimitNotify(){
		second--;
		
		if(second < 0){
			second = 59;
			minute--;
		}
		
		if(minute < 0){
			minute = 59;
			hour--;
		}
		
		if(hour < 0){
			second = 0;
			minute = 0;
			hour = 0;
			
			return true;
		}

		return false;
	}
}
