package com.darkshare.tclog.util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

public class FileSplit {
	
	private static Map<String, Integer> numMap = new TreeMap<String, Integer>();
	private static Map<String, List<Integer>> durationMap = new TreeMap<String, List<Integer>>();
	
	private static void readLargeTextWithNIO(String url, String tempUrl) throws IOException{
		long time = System.currentTimeMillis();
		FileInputStream fileInputStream = new FileInputStream(url);
		FileChannel fileChannel = fileInputStream.getChannel();
		ByteBuffer byteBuffer = ByteBuffer.allocate(128*1024*1024);//分割成大小为128M的TXT文件
		String startName = getFileNameNoEx(url);
		new File(startName).mkdir();
		String endName = getExtensionName(url);
		int i = 1;
		while (true) {
			byteBuffer.clear();
			int flag = fileChannel.read(byteBuffer);
			if(flag == -1){
				break;
			}
			byteBuffer.flip();
			FileOutputStream fileOutputStream = new FileOutputStream(startName+"\\"+i+"."+endName);
			FileChannel channel = fileOutputStream.getChannel();
			channel.write(byteBuffer);
			i+=1;
		}
		time = System.currentTimeMillis() - time;
		System.out.println(time);
		
		readFiles(startName);
	}
	
	private static void readFiles(String url) throws IOException {
		long startTime = System.currentTimeMillis();  //读取开始时的时间
		File file = new File(url);
		if(file.exists() && file.isDirectory()){
			String[] fileList = file.list();
			for(int i = 0; i < fileList.length; i++){
				readFile(url+"\\"+fileList[i]);
			}
		}else{
			System.out.println("文件错了...");
		}
		long endTime = System.currentTimeMillis();
	    System.out.println(url+"读取文件夹内容的时间是："+(endTime - startTime));
	}
	
	private static void readFile(String url) throws IOException {
		long startTime = System.currentTimeMillis();  //读取开始时的时间
	    /*File file = new File(url); //找到目标文件
	    FileInputStream fileInputStream = new FileInputStream(file); //建立数据通道
	    int length = 0 ;
	    byte[] buf = new byte[1024];  //建立缓存数组，缓存数组的大小一般都是1024的整数倍，理论上越大效率越好
	    while((length = fileInputStream.read(buf))!=-1){
	    	System.out.print(new String(buf,0 ,length));
	    }
	    fileInputStream.close(); //关闭资源
*/	    
		BufferedInputStream bis = new BufferedInputStream(new FileInputStream(new File(url)));
		BufferedReader in = new BufferedReader(new InputStreamReader(bis, "utf-8"), 10 * 1024 * 1024);// 10M缓存
		while (in.ready()) {
			String line = in.readLine();
			
			handle(line);
		}
		in.close();
		
	    long endTime = System.currentTimeMillis();
	    System.out.println(url+"读取的时间是："+(endTime - startTime));
	}
	
	public static void handle(String data) {
		String ip = StringUtils.substringBefore(data, "- -").trim();
		String time = StringUtils.substringBetween(data, "[", "+").trim();
		String[] req = StringUtils.substringBetween(data, "\"", "\"").split(" ");
		String[] resp = getSubStr(data, " ", 2).split(" ");
		Integer code = Integer.valueOf(resp[0]);
		if(code == 200){
			String url = StringUtils.substringBefore(req[1], "?").trim();
			if(isNumeric(resp[1])){
				Integer duration = Integer.valueOf(resp[1]);
				if(duration > 2000 && !isStatic(url)){
					if(durationMap.containsKey(url)){
						durationMap.get(url).add(duration);
					}else{
						List<Integer> list = new ArrayList<Integer>();
						list.add(duration);
						durationMap.put(url, list);
					}
				}
			}else{
				System.out.println(data);
			}
			int num = 1;
			if(numMap.containsKey(ip))
				num += numMap.get(ip);
			numMap.put(ip, num);
		}
		
	}
	
	

	public static void main(String[] args) {
		try {
			//readFiles("D:\\var\\test_log");
			
			readFile("D:\\var\\localhost_access_log.2018-09-25.txt");
			
			List<Map.Entry<String,Integer>> list = new ArrayList<Map.Entry<String,Integer>>(numMap.entrySet());
			Collections.sort(list,new Comparator<Map.Entry<String,Integer>>() {
	            public int compare(Entry<String, Integer> o1, Entry<String, Integer> o2) {
	                return o2.getValue().compareTo(o1.getValue());
	            }

	        });
			
			for(Map.Entry<String,Integer> mapping : list){ 
	               System.out.println(mapping.getKey()+":"+mapping.getValue()); 
	        }
			
			Iterator<Map.Entry<String, List<Integer>>> entries = durationMap.entrySet().iterator(); 
			while (entries.hasNext()) { 
			  Map.Entry<String, List<Integer>> entry = entries.next();
			  Integer sum = 0;
			  for(Integer integer : entry.getValue()){
				  sum += integer;
			  }
			  System.out.println("url = " + entry.getKey() + ", size = " + entry.getValue().size() + ", sum = " + sum / entry.getValue().size()); 
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 获取文件扩展名
	 * @param filename
	 * @return
	 */
    public static String getExtensionName(String filename) { 
        if ((filename != null) && (filename.length() > 0)) { 
            int dot = filename.lastIndexOf('.'); 
            if ((dot >-1) && (dot < (filename.length() - 1))) { 
                return filename.substring(dot + 1); 
            } 
        } 
        return filename; 
    } 

    /**
     * 获取不带扩展名的文件名
     * @param filename
     * @return
     */
    public static String getFileNameNoEx(String filename) { 
        if ((filename != null) && (filename.length() > 0)) { 
            int dot = filename.lastIndexOf('.'); 
            if ((dot >-1) && (dot < (filename.length()))) { 
                return filename.substring(0, dot); 
            }
        }
        return filename; 
    } 
    
    /**
     * 取得倒数第“num”个“indexOf”后面的内容
     */
    private static String getSubStr(String str, String indexOf, int num) {
		String result = "";
		int i = 0;
		while (i < num) {
			int lastFirst = str.lastIndexOf(indexOf);
			result = str.substring(lastFirst) + result;
			str = str.substring(0, lastFirst);
			i++;
		}
		return result.substring(1);
	}
    
    /**
     * 判断是否为整数
     */
	public static boolean isNumeric(String str) {
		Pattern pattern = Pattern.compile("[0-9]*"); 
		return pattern.matcher(str).matches();
	}
	
	/**
	 * 判断是否为静态资源
	 */
	public static boolean isStatic(String str) {
		Pattern r = Pattern.compile(".*\\.(css|js)$");
		Matcher m = r.matcher(str);
		return m.matches();
	}

}
