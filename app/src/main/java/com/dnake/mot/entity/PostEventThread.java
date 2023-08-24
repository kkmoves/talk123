package com.dnake.mot.entity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Base64;
import android.util.Log;
import com.dnake.v700.utils;
import com.dnake.mot.PostEventAPI;
import com.dnake.viid.utils.ImageUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.awt.Paint;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

public class PostEventThread extends Thread {
    public static boolean _DEBUGGER_ = false;
    public static List<AccidentInform> accidentInforms;
    public static byte[] frame;
    public static String API;
    public static String VidiconNumber;
    public static String EventAddress;
    public static String EventDetector;
    public static String crossLine = "";
    // Paint
    public static Paint redPaint = new Paint();
    public static Paint greenPaint = new Paint();
    
    public PostEventThread(List<AccidentInform> _accidentInforms, byte[] _frame, String _API, String _mCode) {
        initPaint();
        accidentInforms = _accidentInforms;
        frame = _frame;
        API = _API.isEmpty() ? "http://192.168.2.30:8081/api/TIDA/AddTrafficEvent" : _API;
        VidiconNumber = _mCode.split(";")[0];
        EventAddress = _mCode.split(";")[1];
        EventDetector = _mCode.split(";")[2];
        crossLine = _mCode.split(";")[3];
    }

    public static JSONObject makeEventObject(String EventType, byte[] imageByte) throws JSONException {
        JSONObject event = new JSONObject();
        event.put("EventType", EventType);
        event.put("VidiconNumber", VidiconNumber);
        event.put("EventAddress", EventAddress);
        event.put("EventImg", "集美大桥进岛方向右侧1#灯杆");
        event.put("EventDetector", EventDetector);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        event.put("EventDate", simpleDateFormat.format(new Date()));
        String imageBase64 = Base64.encodeToString(imageByte, Base64.DEFAULT);
//        GZIPOutputStream gzip = null;
//        ByteArrayOutputStream out = new ByteArrayOutputStream();
//        try {
//            gzip = new GZIPOutputStream(out);
//            gzip.write(imageBase64.getBytes());
//            gzip.close();
//            Log.d("out str:",out.toString("utf-8"));
//            event.put("EventImg", out.toString("utf-8"));
//        } catch (IOException e) {
//            e.printStackTrace();
//
//        }
        event.put("EventImg", imageBase64);
        //event.put("EventImg", "iVBORw0KGgoAAAANSUhEUgAAAA4AAAAWCAYAAADwza0nAAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsMAAA7DAcdvqGQAAABzSURBVDhPYzA2Nv5PDh4OGjMzM/9PmjTpf2trK1Z5FI1+fn7/T548+R8ZgDQjq4FhFI0gW0Dg2rVrcAOI0ujo6AjGIDZIA9EakfGoxgHXGBMTA454EH7y5AlYI4iGiYHksWqEpRxcACSPVSMpePhrNP4PAHHrSA5YvlKFAAAAAElFTkSuQmCC");
        return event;

    }

    public static void initPaint(){
        redPaint.setStrokeWidth(2);
        redPaint.setColor(Color.RED);
        redPaint.setStyle(Paint.Style.STROKE);
        greenPaint.setStrokeWidth(2);
        greenPaint.setColor(Color.GREEN);
        greenPaint.setStyle(Paint.Style.STROKE);
    }
    
    public static void drawBox(Canvas canvas, DetectBox detectBox, String accident, int track_id){
        Rect rect = new Rect(detectBox.left, detectBox.top, detectBox.right, detectBox.bottom);
        if(_DEBUGGER_){   // Pass
            if(accident.equals("jam"))
                canvas.drawText(accident, 20, 20, redPaint);
            else if(accident.equals("no")) {
                canvas.drawRect(rect, greenPaint);
                canvas.drawText(String.valueOf(track_id), rect.left, rect.top, greenPaint);
            }else {
                canvas.drawRect(rect, redPaint);
                canvas.drawText(track_id + ":" + accident, rect.left, rect.top, redPaint);
            }
        }else{            // Look here
            if(accident.equals("jam"))
                canvas.drawText(accident, 20, 20, redPaint);
            else{
                canvas.drawRect(rect, redPaint);
                canvas.drawText(accident, rect.left, rect.top, redPaint);
            }
        }
    }

    public static String eventNum2String(int num){
        String res = "";
        switch (num){
            case 0:
                res = "no";
                break;
            case 1:
                res = "park";
                break;
            case 2:
                res = "retrograde";
                break;
            case 3:
                res = "person";
                break;
            case 4:
                res = "spill";
                break;
            case 5:
                res = "jam";
                break;
            case 6:
                res = "cross";
                break;
        }
        return res;
    }

    public static void drawCrossLine(Canvas canvas){
        List<List<Integer>> cross_dict = new ArrayList<List<Integer>>();
        if(crossLine.equals(""))
            return;
        String[] lines = crossLine.split("-");
        for(String line:lines){
            String[] points = line.split(",");
            List<Integer> tempList = new ArrayList<Integer>();
            for(String point:points)
                tempList.add(Integer.parseInt(point.trim()));
            cross_dict.add(tempList);
        }
        for(List<Integer> line:cross_dict)
            canvas.drawLine(line.get(0), line.get(1), line.get(2), line.get(3), redPaint);
    }

    @Override
    public void run(){
        Bitmap bmp = ImageUtils.bytes2Bitmap(frame).copy(Bitmap.Config.ARGB_8888, true);
        Random r = new Random();
        // --------------------------------------->mark
//        utils.save(bmp, "/var/img/"+r.nextInt(50)+".jpg");
        Canvas canvas = new Canvas(bmp);
        Set<Integer> accidentTypes = new HashSet<Integer>();
        for(AccidentInform accidentInform : accidentInforms){
            if (!accidentTypes.contains(accidentInform.accidentType) && accidentInform.accidentType != 0)
                accidentTypes.add(accidentInform.accidentType);
            drawBox(canvas, accidentInform.detectBox, eventNum2String(accidentInform.accidentType), accidentInform.trackID);
        }
        // Accident type: 1 park 2 retrograde 3 person 4 spill 5 jam 6 cross
        Log.d("---->Event Type",String.valueOf(accidentTypes)+" API:" + API);
        drawCrossLine(canvas);
        canvas.save(Canvas.ALL_SAVE_FLAG);
        canvas.restore();
        // Compress bitmap


//        Matrix matrix = new Matrix();
//        matrix.setScale(0.5f, 0.5f);
//        bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(),
//                bmp.getHeight(), matrix, true);
        frame = ImageUtils.bitmap2Bytes(bmp);
        for(Integer accidentType : accidentTypes){
            try {
                JSONObject eventData = makeEventObject(String.valueOf(accidentType), frame);
                PostEventAPI.doPost(eventData, API);
                Log.d("Post Data:", String.valueOf(eventData));
            } catch (JSONException e) {
                e.printStackTrace();
                Log.d("Error", "eorrr");
            }
        }
    }
}
