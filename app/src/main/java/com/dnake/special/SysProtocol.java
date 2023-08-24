package com.dnake.special;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import com.dnake.misc.HttpConnect;
import com.dnake.panel.FaceCompare;
import com.dnake.v700.dxml;

@SuppressLint("DefaultLocale")
public final class SysProtocol {
	public static int mEnable = 0;
	public static String mHost = "";
	public static String mCode = "";
	public static String mData = "";
	public static int mProtocol = 0;

	public static class FaceGlobal { // 全景抓拍
		public byte[] jpeg;
		public int width;
		public int height;
		public int f_x;
		public int f_y;
		public int f_w;
		public int f_h;
	}

	public static class FaceData {
		public int id;
		public String name;
		public Bitmap bmp;
		public int sim;
		public int channel;
		public long ts;
		public String identity; // 身份证号码
		public boolean black;

		public FaceGlobal global;

		public int from; //0: 本地  1:CMS 2:微信
		public int type;
		public String body;

		public int mask; //口罩

		public byte[] data; //图片文件
		public byte[] mData;

		public long mTs;
	}

	public static class PlateResult {
		public int channel;
		public String text;
		public Bitmap bmp;
		public long ts;
		public byte[] data; //图片文件
	}

	public static class ObjectPosition {
		public int label;
		public int x;
		public int y;
		public int w;
		public int h;
	}

	public static class ObjectData {
		public int mChannel;
		public Queue<ObjectPosition> mObject = new LinkedList<ObjectPosition>();
		public byte[] mData;
		public long mTs;
	}

	public static void load() {
		dxml p = new dxml();
		p.load("/dnake/cfg/sdt.xml");
		mEnable = p.getInt("/sys/enable", 0);
		mHost = p.getText("/sys/host", "");
		mCode = p.getText("/sys/code", "");
		mData = p.getText("/sys/data", "");
		mProtocol = p.getInt("/sys/protocol", 0);

		if (mThread == null) {
			mThread = new Thread(new ProcessThread());
			mThread.start();
		}
	}

	public static void save() {
		dxml p = new dxml();
		p.setInt("/sys/enable", mEnable);
		p.setText("/sys/host", mHost);
		p.setText("/sys/code", mCode);
		p.setText("/sys/data", mData);
		p.setInt("/sys/protocol", mProtocol);
		p.save("/dnake/cfg/sdt.xml");
	}

	public static void sdtSwipe(FaceCompare.Data d) {
	}

	public static void sdtVerify(FaceCompare.Data d, boolean result, Bitmap bmp) {
	}

	// 南强智视
	private static void doXmuProtocol(ObjectData d) {
		if (mHost == null || mHost.length() == 0)
			return;

		String url = mHost+"/upload";
		try {
			JSONObject json = new JSONObject();
			json.put("deviceId", mCode); //设备ID
			json.put("channel", d.mChannel);  //通道

			String b64 = Base64.encodeToString(d.mData, Base64.DEFAULT);
			json.put("image", b64);  // 图片

			Date dt = new Date(d.mTs);  // 识别记录
			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			json.put("ts", formatter.format(dt));

			JSONArray data = new JSONArray();

			System.out.println("+++ mCode +++:"+mCode+"+++ 通道 +++ :"+d.mChannel);
			while (true) {
				ObjectPosition obj = d.mObject.poll();
				if (obj == null)
					break;
				JSONObject d2 = new JSONObject();
				d2.put("class", obj.label);
				d2.put("x", obj.x);
				d2.put("y", obj.y);
				d2.put("w", obj.w);
				d2.put("h", obj.h);
				data.put(d2);
			}
			json.put("data", data);
			System.out.println("doXmuProtocol: " + url);
			HttpConnect c = new HttpConnect() {
				@Override
				public void onBody(int result, String body) {
				}
			};
			c.mContentType = "application/json; charset=utf-8";
			c.post(url, json.toString());
		} catch (JSONException e) {
		}
	}

	// 惟尔拓协议
	private static void doWerTopProtocol(FaceData d ) {  // 惟尔拓协议

		if (mHost == null || mHost.length() == 0)
			return;
		String url = mHost + "/control/boxRecognition/record"; // 上传服务器地址

		Date dt = new Date(d.mTs);  // 识别记录
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");



		JSONObject data = new JSONObject();

		try {
			JSONObject json = new JSONObject();
			json.put("ts", formatter.format(dt));
			json.put("deviceNo", mCode);
			json.put("channel",d.channel);
			json.put("createTime",d.ts);
			json.put("ts", formatter.format(dt));
			json.put("name",d.name);
			json.put("faceCapture",d.bmp);
			json.put("identity",d.identity);
			json.put("personnelId",d.id); // 人员添加时自动生成
			json.put("similarity",d.sim); // 相似度

			System.out.println("------------------------------------------------------------------------");
			System.out.println("deviceNo:"+mCode +"\n" +  "通道:"+d.channel +"\n" + "抓拍时间:" +d.ts+"\n" + "相似度:" +d.sim +"\n"
					+ "人员ID：" +d.id +"\n" + "姓名:" + d.name + "\n" + "证件号:" +d.identity +"\n");

			System.out.println("------------------------------------------------------------------------");
			// TODO:可设置阈值
			if (d.black = true && d.sim > 70) {   // black: 0:正常 1:黑名单
				System.out.println("厦门大学！厦门大学！");
				// TODO:发送短信
			}else {
				System.out.println("非黑名单人员");

			}

			// 人脸小图
			if (d.bmp != null) {
				ByteArrayOutputStream os = new ByteArrayOutputStream();  // 图片转换成字节(流)数组
				d.bmp.compress(Bitmap.CompressFormat.JPEG, 80, os);
				String b64 = Base64.encodeToString(os.toByteArray(), Base64.NO_WRAP|Base64.NO_PADDING); // os.toByteArray() 图片字节流转换成Base64(数据流)
				b64.replace("[\\t\\n\\r]","");

//				System.out.println("--------:"+ b64);

				data.put("faceCapture", b64);
				data.put("w", d.bmp.getWidth());
				data.put("h", d.bmp.getHeight());
			}

			if (d.global != null && d.global.jpeg != null) {
				String b64 = Base64.encodeToString(d.global.jpeg, Base64.NO_WRAP|Base64.NO_PADDING);

				data.put("faceCapture",b64);
				data.put("x", (double) d.global.f_x);
				data.put("y", (double)d.global.f_y);
				data.put("w",d.global.width);
				data.put("h",d.global.height);

			}
			HttpConnect c = new HttpConnect() {
				@Override
				public void onBody(int result, String body) {
				}
			};

			c.mContentType = "application/json; charset=utf-8";
//            c.post(url, json.toString()); // 上传到服务器

		} catch (Exception e) {
		}

		try {
			ByteArrayOutputStream os = new ByteArrayOutputStream();  // 图片转换成字节(流)数组
			d.bmp.compress(Bitmap.CompressFormat.JPEG, 80, os);

			// Base64图片处理
			String b64 = Base64.encodeToString(os.toByteArray(), Base64.NO_WRAP|Base64.NO_PADDING); //param.getBytes()  os.toByteArray()
			b64.replace("[\\t\\n\\r]","");

			JSONObject json = new JSONObject();
			data.put("deviceNo", mCode);
			data.put("channel", d.channel);
			data.put("name", d.name==null?null:" "+d.name);
			data.put("code",d.id);
//			data.put("createTime", DateTimeUtils.getDateFormat003(new Date()));
//            data.put("recordImage",d.bmp); // 记录图片
			data.put("similarity", d.sim);
			data.put("faceCapture", b64);  // 人脸捕捉
//			json.put("messageId", UUID.randomUUID().toString().replace("-", "")); // 自动生成
			json.put("messageType", "RECOGNITION_RECORD");
			json.put("data", data);

		} catch (Exception ex){

		}
	}

	private static Queue<FaceData> mFaceData = new LinkedList<FaceData>();
	private static Queue<ObjectData> mObjectData = new LinkedList<ObjectData>();
	private static Queue<PlateResult> mPlateData = new LinkedList<PlateResult>();
	private static Thread mThread = null;


	private static class ProcessThread implements Runnable {
		@Override
		public void run() {
			while (true) {
				// 注意FaceData
				if (mFaceData.size() > 0) {
					FaceData d = mFaceData.poll();
					if (d == null)
						continue;

					if (d.bmp == null && d.data != null && d.data.length > 0) {
						d.bmp = BitmapFactory.decodeByteArray(d.data, 0, d.data.length);
						d.data = null;
					}
					if (mProtocol == 2) {        //  惟尔拓协议
						doWerTopProtocol(d);
					}


					if (YmsProtocol.m_enabled != 0) {
						YmsProtocol.doLogger(d);
					}
				}
				if (mObjectData.size() > 0) {
					ObjectData d = mObjectData.poll();
					mObjectData.poll();
					if (mProtocol==1){          //   南强智视协议
						doXmuProtocol(d);
					}
				}
				if (mPlateData.size() > 0) {
					//PlateResult d = mPlateData.poll();
					mPlateData.poll();
				}

				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
				}
			}
		}
	}

	public static void face(FaceData d) {
		mFaceData.add(d);
	}

	public static void object(ObjectData d) {
		if (mEnable != 0) {
			mObjectData.add(d);
		}
	}

	public static void plate(PlateResult d) {
		if (mEnable != 0) {
			mPlateData.add(d);
		}
	}

	public static String displayName(int id) {
		String name = null;
		JSONObject obj = YmsProtocol.queryFaceId(id);
		if (obj != null) {
			try {
				name = obj.getString("nickName");
			} catch (JSONException e) {
			}
		}
		return name;
	}
}
