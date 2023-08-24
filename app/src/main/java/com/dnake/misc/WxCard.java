package com.dnake.misc;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.dnake.v700.utils;

public class WxCard {
	private static String url = "/dnake/data/wx/card.json";
	private static JSONObject json = null;

	public static void load() {
		json = null;
		byte[] data = utils.readFile(url);
		if (data == null) {
			return;
		}
		try {
			json = new JSONObject(new String(data));
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	public static SysCard.Data verify(String card) {
		if (json == null)
			return null;
		try {
			JSONArray rows = json.getJSONArray("rows");
			for(int i=0; i<rows.length(); i++) {
				JSONObject obj = rows.getJSONObject(i);
				String c = obj.getString("c");
				if (c != null && c.equalsIgnoreCase(card)) {
					SysCard.Data d = new SysCard.Data();
					d.card = c;
					d.build = obj.getInt("b");
					d.unit = obj.getInt("u");
					int r = obj.getInt("r");
					d.floor = r / 100;
					d.family = r % 100;
					for(int k=1; k<10; k++) {
						String ss = "r"+String.valueOf(k);
						if (obj.has(ss)) { //扩展房号
							Integer rr = obj.getInt(ss);
							d.ext.add(rr);
						} else
							break;
					}
					return d;
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return null;
	}
}
