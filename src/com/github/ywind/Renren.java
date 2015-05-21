package com.github.ywind;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * @author Ywind E-mail:guoshukang@vip.qq.com
 * @version 创建时间：2015年5月20日 下午7:46:51
 * 类说明
 * 
 */
public class Renren {

	private String url="http://www.renren.com/386949378/profile";
	private static final String PIC_DIR = "/home/shukang/ren";
	BasicCookieStore cookieStore;
	Stack<String> urls;
	Set<String> haveFound;
	BlockingQueue<String> pics;

	public void init() {
		pics=new LinkedBlockingQueue<String>(100);
		urls=new Stack<String>();
		haveFound = Collections.synchronizedSet(new HashSet<String>());
		urls.push(url);
		try {
			login();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void login() throws Exception, IOException {
        BasicCookieStore cookieStore = new BasicCookieStore();
        CloseableHttpClient httpclient = HttpClients.custom()
                .setDefaultCookieStore(cookieStore)
                .build();
            HttpUriRequest login = RequestBuilder.post()
                    .setUri(new URI("http://www.renren.com/PLogin.do"))
                    .addParameter("email", "guoshukang@vip.qq.com")
                    .addParameter("password", "*********")
                    .build();
            CloseableHttpResponse response2 = httpclient.execute(login);
            try {
                HttpEntity entity = response2.getEntity();

                System.out.println("Login form get: " + response2.getStatusLine());
                EntityUtils.consume(entity);
                System.out.println("Post logon cookies:");
                List<Cookie> cookies = cookieStore.getCookies();
                if (cookies.isEmpty()) {
                    System.out.println("None");
                } else {
                    for (int i = 0; i < cookies.size(); i++) {
                        System.out.println("- " + cookies.get(i).toString());
                    }
                }
                
            } finally {
                response2.close();
                httpclient.close();
            }
			this.cookieStore=cookieStore;
			
    }
	
	public void Pa() throws Exception, IOException {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        try {
            CookieStore cookieStore = this.cookieStore;

            HttpClientContext localContext = HttpClientContext.create();
            localContext.setCookieStore(cookieStore);

            HttpGet httpget = new HttpGet(urls.pop());
            System.out.println("Executing request " + httpget.getRequestLine());

            // Pass local context as a parameter
            CloseableHttpResponse response = httpclient.execute(httpget, localContext);
            try {
                System.out.println("----------------------------------------");
                String entityString = EntityUtils.toString(response.getEntity());
                System.out.println(entityString);
                Document doc = Jsoup.parse(entityString);
                Element uls = doc.select("#specialfriend-box > div > div.has-friend > ul").first();
                Element urls2 = doc.select("#specialfriend-box > div > div.share-friend > ul").first();
                Element urls3 = doc.select("#footprint-box > ul").first();
                Element pic = doc.select("#userpic").first();
                Element school = doc.select("#operate_area > div.tl-information > ul > li.school > span").first();
                Element sex = doc.select("#operate_area > div.tl-information > ul > li.birthday > span:nth-child(1)").first();
                if (school!=null)
                	if (school.ownText().equalsIgnoreCase("就读于山东大学威海分校")||school.ownText().equalsIgnoreCase("就读于山东大学（威海）")||school.ownText().equalsIgnoreCase("就读于山东大学")) {
                		if (sex!=null&&sex.ownText().equalsIgnoreCase("女生")) {
        					pics.put(pic.attr("src"));
        				}
                		if(uls!=null)
    	                {
    	                	Elements links =uls.children();
    	                    for (Element element : links) {
    	                    	Element element2 = element.child(0); 
    	    					String linkHref = element2.attr("href");
    	    					if (!haveFound.contains(linkHref)) {
    		    						urls.push(linkHref);	
    								}
    							}
    	                }
                		if(urls2!=null)
    	                {
    	                	Elements links =urls2.children();
    	                    for (Element element : links) {
    	                    	Element element2 = element.child(0); 
    	    					String linkHref = element2.attr("href");
    	    					if (!haveFound.contains(linkHref)) {
    		    						urls.push(linkHref);	
    								}
    							}
    	                }
                		
                		if(urls3!=null)
    	                {
    	                	Elements links =urls3.children();
    	                    for (Element element : links) {
    	                    	Element element2 = element.child(0); 
    	    					String linkHref = element2.attr("href");
    	    					if (!haveFound.contains(linkHref)) {
    		    						urls.push(linkHref);	
    								}
    							}
    	                }
                		
					}
                
                EntityUtils.consume(response.getEntity());
            } finally {
                response.close();
            }
        } finally {
            httpclient.close();
        }
        
	}
	/*
	 * 获取pic地址
	 */
	private class PutPics implements Runnable{

		@Override
		public void run() {
			while (true) {
				try {
					Pa();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
		}
		
	}
	
	/*
	 * 消耗pic地址
	 */
	
	private class GetPics implements Runnable{

		@Override
		public void run() {
			while (true) {
				try {
					save();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
		}
		
	}
	
    /** 
     * 保存图片 
     * @param url 
     * @param i 
     * @throws Exception 
     */  
    public void save() throws Exception {

    	String url = pics.take();
    	haveFound.add(url);
        String fileName = url.substring(url.lastIndexOf("/"));  
        String filePath = PIC_DIR + "/" + fileName;  
        BufferedOutputStream out = null;  
        byte[] bit = getByte(url);  
        if (bit.length > 0) {  
            try {  
                out = new BufferedOutputStream(new FileOutputStream(filePath));  
                out.write(bit);  
                out.flush();  
                System.out.println("图片下载成功！");  
            } finally {  
                if (out != null)  
                    out.close();  
            }  
        }  
    }  
      
    /** 
     * 获取图片字节流 
     * @param uri 
     * @return 
     * @throws Exception 
     */  
    private byte[] getByte(String uri) throws Exception {  
    	CloseableHttpClient client = HttpClients.createDefault(); 
//      client.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, TIME_OUT);  
        HttpGet get = new HttpGet(uri);  
//      get.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, TIME_OUT);  
        try {  
            HttpResponse resonse = client.execute(get);  
            if (resonse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {  
                HttpEntity entity = resonse.getEntity();  
                if (entity != null) {  
                    return EntityUtils.toByteArray(entity);  
                }  
            }  
        } catch (Exception e) {  
            e.printStackTrace();  
        } finally {  
            client.close(); 
        }  
        System.out.println("获取失败!");  
        return new byte[0];  
    }  
	
	public static void main(String[] args) {
		Renren rr= new Renren();
		try {
			rr.init();
			Thread getThread= new Thread(rr.new GetPics());
			Thread getThread2= new Thread(rr.new GetPics());
			Thread putThread= new Thread(rr.new PutPics());
			getThread.start();
			getThread2.start();
			putThread.start();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
