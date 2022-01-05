package com.onearth.server;

import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;


import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.sql.Array;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;

import javax.imageio.ImageIO;
import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
//import javax.validation.Path;

import org.apache.commons.io.FileUtils;
import org.hibernate.validator.internal.engine.path.PathImpl;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.resource.PathResourceResolver;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.web.multipart.MultipartFile;




@RestController
@EnableAutoConfiguration //자동 구성 annotation
						// 여러 번 구성 할 수 없음 -> 기본 구성 클래스 부여 권장 
public class server implements ApplicationRunner, WebMvcConfigurer{
	
	static JSONArray databaseMembers = new JSONArray();
	static HashMap<Integer, JSONObject> memberInfo = new HashMap();
	
	
	static JSONArray membersName = new JSONArray();
	
	static JSONArray todayPostings = new JSONArray(); 
	
	static String realPath = "/onearth/public/static/image";
//	static String realPath = "/Users/jungbyungjun/onearth/online_earth/src/main/resources/static/image";
	@Autowired
	private DataSource dataSource;
	
	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	@Autowired
    private HttpServletRequest request;
	
	public static void main(String[] args) throws Exception {
		
		SpringApplication.run(server.class, args);
	}
	
	@Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/test/**").addResourceLocations("file:///onearth/public/static/");
        System.out.println("addResourceHandler!!");
    }

	@Override
    public void run(ApplicationArguments args) throws Exception {
		
		System.out.println("PgSQLRunner is running");
		try(Connection connection = dataSource.getConnection()) {
			System.out.println(dataSource.getClass());
			System.out.println(connection.getMetaData().getURL());
			System.out.println(connection.getMetaData().getUserName());
			Statement statement = connection.createStatement();
			Statement statement2 = connection.createStatement();
			Statement statement3 = connection.createStatement();
			ResultSet rs = statement.executeQuery("SELECT * FROM user_information");
			ResultSet rs3 = statement3.executeQuery("SELECT * FROM today_posting");
			JSONObject member = new JSONObject();
			for(int i = 0; rs.next(); i++) {
//				int result = statement2.executeUpdate("ALTER TABLE \""+rs.getString("name")+"_image_information\" ADD COLUMN tripgeocodeshort varchar(20);");
//				int result = statement2.executeUpdate("ALTER TABLE \""+rs.getString("name")+"_image_information\" ALTER COLUMN smiles TYPE VARCHAR(100)[];");
//				if(result == 1) {
//					System.out.println(rs.getString("name") + "image information is altered!");
//				}
				member = new JSONObject();
				member.put("name", rs.getString("name"));
				member.put("password", rs.getString("password"));
				System.out.println(rs.getString("name"));
				membersName.add(rs.getString("name"));
				databaseMembers.add(member);
			}
			for(int l=0; rs3.next(); l++) {
				JSONObject imageObjForTodayPostings = new JSONObject();
				imageObjForTodayPostings.put("userName", rs3.getString("username"));
				imageObjForTodayPostings.put("imageName", rs3.getString("imagename"));
				imageObjForTodayPostings.put("latitude", rs3.getDouble("latitude"));
				imageObjForTodayPostings.put("longitude", rs3.getDouble("longitude"));
				JSONParser parser = new JSONParser();
				Object obj = parser.parse( rs3.getObject("createdat").toString() );
				JSONArray jsonArray = (JSONArray) obj;
				JSONObject createdAtNew = new JSONObject();
			    createdAtNew.put("year", (int) (long) (((JSONObject) jsonArray.get(0)).get("year")));
			    createdAtNew.put("month", (int) (long) (((JSONObject) jsonArray.get(0)).get("month")));
			    createdAtNew.put("date", (int) (long) (((JSONObject) jsonArray.get(0)).get("date")));
			    createdAtNew.put("hour", (int) (long) (((JSONObject) jsonArray.get(0)).get("hour")));
			    createdAtNew.put("minute", (int) (long) (((JSONObject) jsonArray.get(0)).get("minute")));
				imageObjForTodayPostings.put("createdAt", createdAtNew);
				todayPostings.add(imageObjForTodayPostings);
			}
			System.out.println(todayPostings.size());
		}
		
	}
	
	public static Image getImage(final String pathAndFileName) {
	    final URL url = Thread.currentThread().getContextClassLoader().getResource(pathAndFileName);
	    return Toolkit.getDefaultToolkit().getImage(url);
	}
	
	void refreshDatabase() throws Exception {
		try(Connection connection = dataSource.getConnection()) {
			Statement statement = connection.createStatement();
			ResultSet rs = statement.executeQuery("SELECT * FROM user_information");
			JSONObject member = new JSONObject();
			databaseMembers = new JSONArray();
			membersName = new JSONArray();
			for(int i = 0; rs.next(); i++) {
				member = new JSONObject();
				member.put("name", rs.getString("name"));
				member.put("password", rs.getString("password"));
				databaseMembers.add(member);
				membersName.add(rs.getString("name"));
			}
		}
		System.out.println("database refreshed!");
	}
	
	@CrossOrigin(origins="http://localhost:8100")
	@RequestMapping("/")
    String login(@RequestParam(value = "id", required = false)String id, @RequestParam(value = "password", required = false)String password) throws SQLException, IOException{

		System.out.println("requesting : id="+id+", password="+password);
		for(int i =0; i < databaseMembers.size(); i++) {
			this.memberInfo.put(i, (JSONObject) databaseMembers.get(i));
		}
		if(id != null && password != null) {
			for(int i = 0; i < databaseMembers.size(); i++) {
				if(this.memberInfo.get(i).get("name").equals(id) && this.memberInfo.get(i).get("password").equals(password)) {
					System.out.println("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^login complete!");
					String profileImgPath = realPath+"/profileImages/"+id+"/profileImg.png";
					String encodedString;
					if(new File(profileImgPath).exists()) {
			        	 File f = new File(profileImgPath);			  
			        	 byte[] fileContent = FileUtils.readFileToByteArray(new File(profileImgPath));
			        	 encodedString = Base64.getEncoder().encodeToString(fileContent);
			        }else {
			        	 encodedString = "";
			        }
					System.out.println("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^profileImage complete!");
					JSONObject jsonObject = new JSONObject();
			        jsonObject.put("profileImg", encodedString);
			        String realPathtoUploads = realPath+"/user_images/"+id+"/";
			        if(new File(realPathtoUploads).exists()) {
			        	 File f = new File(realPathtoUploads);
						 File[] files = f.listFiles();
						 JSONObject[] images = new JSONObject[files.length];
						 for(int l=0; l<files.length; l++) {
							 JSONObject imageObject = new JSONObject();
							 String imageName = files[l].getName();
							 imageObject.put("imageName", imageName);
							 try(Connection connection = dataSource.getConnection()) {
									Statement statement = connection.createStatement();
									Statement statement2 = connection.createStatement();
									ResultSet rs = statement.executeQuery("select latitude, longitude, targetimage, triptitle, postingcategory, postingdate, smiles, tripgeocode, tripgeocodeshort, enddate, startdate from  \""+id+"_image_information\" where image='"+imageName+"'");
									while (rs.next()) {
										imageObject.put("latitude", rs.getDouble("latitude"));
										imageObject.put("longitude", rs.getDouble("longitude"));
										imageObject.put("targetimage", rs.getString("targetimage"));
										imageObject.put("triptitle", rs.getString("triptitle"));
										imageObject.put("postingcategory", rs.getString("postingcategory"));
										imageObject.put("postingdate", rs.getString("postingdate"));
										imageObject.put("tripgeocode", rs.getString("tripgeocode"));
										imageObject.put("tripgeocodeshort", rs.getString("tripgeocodeshort"));
										imageObject.put("enddate", rs.getString("enddate"));
										imageObject.put("startdate", rs.getString("startdate"));
										if(rs.getArray("smiles") != null) {
											String[] smilesArr = (String[]) rs.getArray("smiles").getArray();
											imageObject.put("smiles", smilesArr);
										}
									}
							 }
							 images[l] = imageObject;
						 }
						 System.out.println(images);
				        jsonObject.put("images", images);
			        }else {
			        	jsonObject.put("images", null);
			        }
			        jsonObject.put("membersName", membersName);
			        try(Connection connection = dataSource.getConnection()) {
						Statement statement = connection.createStatement();
						Statement statement2 = connection.createStatement();
						Statement statement3 = connection.createStatement();
						ResultSet rs = statement.executeQuery("SELECT * FROM user_relationship where name='"+id+"';");
						while (rs.next()) {
							String[] Arr = (String[]) rs.getArray("following").getArray();
							jsonObject.put("following", Arr);
						}
						ResultSet rs2 = statement2.executeQuery("select name from user_relationship where '"+id+"'=ANY(following);");
						LinkedList<String> list = new LinkedList();
						for(int l=0; rs2.next(); l++) {
							list.add(rs2.getString("name"));
						}
						String[] Arr = new String[list.size()];
						for(int l=0; l<list.size(); l++) {
							Arr[l] = list.get(l);
						}
						jsonObject.put("followers", Arr);
						ResultSet rs3 = statement3.executeQuery("select * from user_information where name = '"+id+"'");
						while (rs3.next()) {
							System.out.println(rs3.getString("tutorialstate"));
					        jsonObject.put("tutorialState", rs3.getString("tutorialstate"));
					        jsonObject.put("postingTutorialState", rs3.getString("postingtutorialstate"));
						}
					}
			        ObjectMapper mapper = new ObjectMapper();
			    	String stringJson = mapper.writeValueAsString(jsonObject);
					return stringJson;
				}
			}
			System.out.println("login failed!");
			return "false";
		}else {
			System.out.println("login failed!");
			return "false";
		}
	}
	
	@CrossOrigin(origins="http://localhost:8100")
    @RequestMapping("/register")
    boolean register(@RequestParam(value = "id", required = false)String id, @RequestParam(value = "password", required = false)String password) throws Exception{
    	System.out.println(id);
    	System.out.println(password);
    	int result = 0;
		try(Connection connection = dataSource.getConnection()) {
					Statement statement = connection.createStatement();
					Statement statement2 = connection.createStatement();
					Statement statement3 = connection.createStatement();
					Statement statement4 = connection.createStatement();
					Statement statement5 = connection.createStatement();
					Statement statement6 = connection.createStatement();
					Statement statement7 = connection.createStatement();
					ResultSet rss = statement.executeQuery("select exists(select * from user_information where name = '"+id+"');");
					while(rss.next()) {
						if(rss.getString("exists").equals("f")) {
							result = statement2.executeUpdate("insert into user_information(name, password, tutorialstate, postingtutorialstate) values('"+id+"', '"+password+"', 'false', 'false');");
							result = statement3.executeUpdate("insert into user_relationship(name, following) values('"+id+"', '{}')");
							result = statement7.executeUpdate("create table \""+id+"_image_information\"(image varchar(50) PRIMARY KEY, applies JSONB, postingtext varchar(50), latitude double precision, longitude double precision, targetimage varchar(50), triptitle varchar(50), postingcategory varchar(20), postingdate date, smiles varchar(50)[], startdate date, enddate date, createdat jsonb, tripgeocode varchar(50), tripgeocodeshort varchar(50));");
							refreshDatabase();
				        	 String realPathtoUploads = realPath+"/profileImages/"+id+"/";
						 	 if(! new File(realPathtoUploads).exists()) {
								 new File(realPathtoUploads).mkdir();
								 System.out.println("mkdir ! ");
							 }
						 	 File file = new File(realPath+"/profileImages/icon-avatar-default.PNG");
					        BufferedImage image = null;
					        try{
					        	File f = new File(realPathtoUploads+"profileImg.png");
					        	image = ImageIO.read(file);
					            ImageIO.write(image, "png", f);
					        }catch (IOException e){
					            e.printStackTrace();
					        }
					        result = 1;
						}else {
							result = 0;
						}
					}
		}
		if(result == 1) {
			System.out.println("user register complete!");
			this.refreshDatabase();
			return true;
		}else {
			return false;
		}
		
    }
	
	@CrossOrigin(origins="http://localhost:8100")
    @RequestMapping("/uploadImageIOS")
    boolean uploadImageIOS(@RequestBody HashMap<String, Object> map, @RequestParam(value = "id", required = true)String id) throws Exception{
		
		String tripTitle = (String) map.get("tripTitle");
		ArrayList imageInformation = (ArrayList) map.get("imageInfos");
		String postingCategory = (String) map.get("postingCategory");
		String dailyDate = (String) map.get("dailyDate");
		String startDate = (String) map.get("startDate");
		String endDate = (String) map.get("endDate");
		String tripGeocode = (String) map.get("tripGeocode");
		String tripGeocodeShort = (String) map.get("tripGeocodeShort");
		this.saveImgIOS(imageInformation, id, tripTitle, postingCategory, dailyDate, startDate, endDate, tripGeocode, tripGeocodeShort);
    	return true;
    	
    }
	
	private void saveImgIOS(ArrayList imageInformations, String id, String tripTitle, String postingCategory, String dailyDate, String startDate, String endDate, String tripGeocode, String tripGeocodeShort) throws IOException, SQLException {
	
		for(int l=0; l<imageInformations.size(); l++) {
			HashMap imgInfo = (HashMap) imageInformations.get(l);
			String img = (String) imgInfo.get("imageWebPath");
			String postingText = (String) imgInfo.get("postingText");
			double latitude = (double) imgInfo.get("latitude");
			double longitude = (double) imgInfo.get("longitude");
			int thisImageIndex = l;
			String realPathtoUploads = realPath+"/user_images/"+id+"/";
		 	 if(! new File(realPathtoUploads).exists()) {
				 new File(realPathtoUploads).mkdir();
				 System.out.println("mkdir ! ");
			 }
			 String base64Image = img.split(",")[1];
			 byte[] dataBytes=Base64.getDecoder().decode(base64Image);
			 File f = new File(realPathtoUploads);
			 File[] files = f.listFiles();
			 int lastImageNum = -1;
			 for(int i=0; i<files.length; i++) {
				 String fileName = files[i].getName();
				 int imageNum = Integer.parseInt(fileName.substring(fileName.lastIndexOf("e")+1, fileName.lastIndexOf(".")));
				 if(imageNum > lastImageNum) {
					 lastImageNum = imageNum;
				 }
			 }
			 System.out.println(lastImageNum);
			 int newImageNum = lastImageNum+1;
			 String orgName = "image" + newImageNum + ".jpg";
			 String targetImageName = "null";
			 System.out.println(imgInfo.get("targetImageIndex"));
			 if(!imgInfo.get("targetImageIndex").equals("null")) {
				 int targetImageIndex = (int) imgInfo.get("targetImageIndex");
				 int targetImageNum = newImageNum + targetImageIndex - thisImageIndex;
				  targetImageName = "image" + targetImageNum + ".jpg";
			}
		    String directory=realPathtoUploads + orgName;
		    File file = new File(directory);
		    FileOutputStream fos = new FileOutputStream(file);
		    fos.write(dataBytes);
		    boolean todayState = true;
		    if(todayPostings.size() != 0) {
		    		boolean yearState = ((int) (((Map) imgInfo.get("createdAt")).get("year")) == (int) ((Map) ((JSONObject) todayPostings.get(0)).get("createdAt")).get("year"));
		    		boolean monthState =  ((int) (((Map) imgInfo.get("createdAt")).get("month")) == (int) (((Map) ((JSONObject) todayPostings.get(0)).get("createdAt"))).get("month"));
		    		boolean dateState = ((int) (((Map) imgInfo.get("createdAt")).get("date")) == (int) (((Map) ((JSONObject) todayPostings.get(0)).get("createdAt")).get("date")));
		    		System.out.println(yearState);
		    		System.out.println(monthState);
		    		System.out.println(dateState);
		    		if(yearState && monthState && dateState) {
				    	
				    }else {
				    	todayState = false;
				    }
		    }
		    System.out.println(todayState);
		    if(!todayState) {
		    	todayPostings = new JSONArray();
		    }
		    JSONObject createdAtNew = new JSONObject();
		    createdAtNew.put("year", (int) ((Map) imgInfo.get("createdAt")).get("year"));
		    createdAtNew.put("month", (int) ((Map) imgInfo.get("createdAt")).get("month"));
		    createdAtNew.put("date", (int) ((Map) imgInfo.get("createdAt")).get("date"));
		    createdAtNew.put("hour", (int) ((Map) imgInfo.get("createdAt")).get("hour"));
		    createdAtNew.put("minute", (int) ((Map) imgInfo.get("createdAt")).get("minute"));
			JSONObject imageObjForTodayPostings = new JSONObject();
			imageObjForTodayPostings.put("userName", id);
			imageObjForTodayPostings.put("imageName", orgName);
			imageObjForTodayPostings.put("latitude", latitude);
			imageObjForTodayPostings.put("longitude", longitude);
			imageObjForTodayPostings.put("createdAt", createdAtNew);
			todayPostings.add(imageObjForTodayPostings);
			System.out.println(todayPostings);
		    int result = 0;
			 try(Connection connection = dataSource.getConnection()) {
				JSONArray cretedAtArr = new JSONArray();
				cretedAtArr.add(createdAtNew);
				Statement statement = connection.createStatement();
				Statement statement2 = connection.createStatement();
				Statement statement3 = connection.createStatement();
				if(postingCategory.equals("trip")) {
					result = statement.executeUpdate("insert into \""+id+"_image_information\"(image, postingtext, latitude, longitude, targetimage, triptitle, postingcategory, startdate, enddate, createdat, tripgeocode, tripgeocodeshort) values('"+orgName+"', '"+postingText+"', "+latitude+", "+longitude+", '"+targetImageName+"', '"+tripTitle+"', '"+postingCategory+"', '"+startDate+"', '"+endDate+"', '"+cretedAtArr+"', '"+tripGeocode+"', '"+tripGeocodeShort+"' ); ");
				}else {
					result = statement.executeUpdate("insert into \""+id+"_image_information\"(image, postingtext, latitude, longitude, targetimage, triptitle, postingcategory, postingdate, createdat, tripgeocode, tripgeocodeshort) values('"+orgName+"', '"+postingText+"', "+latitude+", "+longitude+", '"+targetImageName+"', '"+tripTitle+"', '"+postingCategory+"', '"+dailyDate+"', '"+cretedAtArr+"', '"+tripGeocode+"', '"+tripGeocodeShort+"' ); ");
				}
				if(!todayState) {
					int result2 = statement2.executeUpdate("delete from today_posting");
				}
				int result3 = statement3.executeUpdate("insert into today_posting(username, imagename, latitude, longitude, createdat) values('"+id+"', '"+orgName+"', '"+latitude+"', '"+longitude+"', '"+cretedAtArr+"')"); 
			 }
			 if(result == 1) {
				 System.out.println("image save completely !!!");
			 }else {
				 System.out.println("image save failed !!!");
			 }
		}
		
	}
	
	@CrossOrigin(origins="http://localhost:8100")
    @RequestMapping("/deletePost")
    boolean deletePost(@RequestParam(value = "userName", required = false)String userName, @RequestParam(value = "imageName", required = false)String imageName) throws Exception{
    	
		System.out.println(userName + imageName);
    	String realPathtoDelete = realPath+"/user_images/"+userName+"/"+imageName;
    	File file = new File(realPathtoDelete);
    	if(file.exists()) {
    		boolean didSuccess = file.delete();
    		if(didSuccess) {
    			int result = 0;
    			int result2 = 0;
				try(Connection connection = dataSource.getConnection()) {
					Statement statement = connection.createStatement();
					Statement statement2 = connection.createStatement();
					Statement statement3 = connection.createStatement();
					result = statement.executeUpdate("DELETE FROM \""+userName+"_image_information\" WHERE image = '"+imageName+"';");
					result2 = statement2.executeUpdate("UPDATE \""+userName+"_image_information\" SET targetimage = 'null' WHERE targetimage = '"+imageName+"';");
					int result3 = 0;
					ResultSet rs = statement.executeQuery("select * from user_notification where name='"+userName+"';");
					boolean isThereSavedNtf = false;
					JSONParser parser = new JSONParser();
					for(int l=0; rs.next(); l++) {
						isThereSavedNtf = true;
			        	JSONArray jsonArray = new JSONArray();
						Object object = parser.parse( rs.getObject("notification").toString() );
						jsonArray = (JSONArray) object;
						JSONArray newArray = new JSONArray();
						System.out.println(jsonArray.size());
						if(jsonArray.size() == 30) {
							for(int i=1; i<jsonArray.size(); i++) {
								if(!((JSONObject) jsonArray.get(i)).get("imageName").equals(imageName)) {
									newArray.add(jsonArray.get(i));
								}
							}
						}else {
							newArray = jsonArray;
						}
						result3 = statement2.executeUpdate(" update user_notification set notification='"+newArray+"' where name='"+userName+"';");	
					};
				}				
				if((result == 1) && (result2 == 1)) {
					System.out.println("delete post complete!");
					return false;
				}else if(result2 != 1) {
					System.out.println(result2);
					System.out.println("no targetimage!");
					return false;
				}else {
					System.out.println("delete post failed (database) or there isnt database");
					return false;
				}
    		}else {
    			System.out.println("delete post failed (image)");
    			return false;
    		}
    	}
    	System.out.println("image not exist");
		return false;
		
	}
	
	@CrossOrigin(origins="http://localhost:8100")
    @RequestMapping("/changeProfileImg")
    boolean changeProfileImg(@RequestParam("file") MultipartFile profileImg, @RequestParam(value = "id", required = false)String id ) throws Exception{
		
		this.saveProfileImg(profileImg, id);
		System.out.println("who changed profile image : " + id);
    	return true;
    	
    }
	
	@CrossOrigin(origins="http://localhost:8100")
    @RequestMapping("/changeProfileImgIOS")
    boolean changeProfileImgIOS(@RequestBody HashMap<String, Object> map, @RequestParam(value = "id", required = true)String id) throws Exception{
	
		System.out.println("success");
		System.out.println(map.get("image"));
		this.saveProfileImgIOS((String) map.get("image"), id);
    	return true;
    	
    }

	private void saveProfileImg(MultipartFile profileImg, String id) throws IllegalStateException, IOException, SQLException {
		 if(!profileImg.isEmpty()) {
			 	System.out.println("profileImage not empty");
			 	String realPathtoUploads = realPath+"/profileImages/"+id+"/";
			 	 if(! new File(realPathtoUploads).exists()) {
					 new File(realPathtoUploads).mkdir();
					 System.out.println("mkdir ! ");
				 }
				 String orgName = profileImg.getOriginalFilename();
				 orgName = orgName.substring(0, 10);
				 orgName += ".png";
				 String filePath = realPathtoUploads + orgName;
				 File dest = new File(filePath);
				 profileImg.transferTo(dest);
				 System.out.println("profileImage save completely !!!");
		 }else{
			 System.out.println("profileImage empty");
		 }
		 
	}
	

	private void saveProfileImgIOS(String img, String id) throws IOException {
		
		 String realPathtoUploads = realPath+"/profileImages/"+id+"/";
	 	 if(! new File(realPathtoUploads).exists()) {
			 new File(realPathtoUploads).mkdir();
			 System.out.println("mkdir ! ");
		 }
		 System.out.println(img);	 
		 String base64Image = img.split(",")[1];
		 byte[] dataBytes=Base64.getDecoder().decode(base64Image);
		 String directory=realPathtoUploads + "profileImg.png";
		 File file = new File(directory);
		 FileOutputStream fos = new FileOutputStream(file);
		 fos.write(dataBytes);
		 
	}
	
	@CrossOrigin(origins="http://localhost:8100")
	@RequestMapping("/getOtherUserInfo")
    String getOtherUserInfo(@RequestParam(value = "id", required = true)String id) throws SQLException, IOException, ParseException{

		JSONObject jsonObject = new JSONObject();
        String realPathtoUploads = realPath+"/user_images/"+id+"/";
        if(new File(realPathtoUploads).exists()) {
        	 File f = new File(realPathtoUploads);
			 File[] files = f.listFiles();
			 String[] imageNames = new String[files.length];
			 for(int l=0; l<files.length; l++) {
				 String imageName = files[l].getName();
				 imageNames[l] = imageName;
			 }
	        jsonObject.put("imageNames", imageNames);
        }else {
        	jsonObject.put("NumOfImgs", 0);
        }
        try(Connection connection = dataSource.getConnection()) {
			Statement statement = connection.createStatement();
			Statement statement2 = connection.createStatement();
			Statement statement3 = connection.createStatement();
			ResultSet rs = statement.executeQuery("SELECT * FROM user_relationship where name='"+id+"';");
			while (rs.next()) {
				String[] Arr = (String[]) rs.getArray("following").getArray();
				jsonObject.put("following", Arr);
			}
			ResultSet rs2 = statement2.executeQuery("select name from user_relationship where '"+id+"'=ANY(following);");
			LinkedList<String> list = new LinkedList();
			for(int i=0; rs2.next(); i++) {
				list.add(rs2.getString("name"));
			}
			String[] Arr = new String[list.size()];
			for(int i=0; i<list.size(); i++) {
				Arr[i] = list.get(i);
			}
			jsonObject.put("followers", Arr);
			JSONArray imageArr = new JSONArray();
			ResultSet rs3 = statement3.executeQuery("select * from  \""+id+"_image_information\";");
			while (rs3.next()) {
				JSONObject imageObj = new JSONObject();
				System.out.println(rs3.getString("image"));
				imageObj.put("imageName", rs3.getString("image"));
				imageObj.put("latitude", rs3.getDouble("latitude"));
				imageObj.put("longitude", rs3.getDouble("longitude"));
				imageObj.put("targetimage", rs3.getString("targetimage"));
				imageObj.put("triptitle", rs3.getString("triptitle"));
				imageObj.put("postingcategory", rs3.getString("postingcategory"));
				imageObj.put("postingdate", rs3.getString("postingdate"));
				imageObj.put("startdate", rs3.getString("startdate"));
				imageObj.put("enddate", rs3.getString("enddate"));
				imageObj.put("tripgeocode", rs3.getString("tripgeocode"));
				imageObj.put("tripgeocodeshort", rs3.getString("tripgeocodeshort"));
				if(rs3.getArray("smiles") != null) {
					String[] smilesArr = (String[]) rs3.getArray("smiles").getArray();
					imageObj.put("smiles", smilesArr);
				}
				JSONParser parser = new JSONParser();

				if(rs3.getObject("applies") != null) {
					Object obj2 = parser.parse( rs3.getObject("applies").toString() );
					JSONArray jsonArray = (JSONArray) obj2;
					imageObj.put("applies", jsonArray);
				}
				imageArr.add(imageObj);
			}
			jsonObject.put("images", imageArr);
        }
        ObjectMapper mapper = new ObjectMapper();
    	String stringJson = mapper.writeValueAsString(jsonObject);
		return stringJson;			
		
	}
	
	@CrossOrigin(origins="http://localhost:8100")
    @RequestMapping("/following")
    int following(@RequestParam(value = "userName", required = true)String userName, @RequestParam(value = "OtherUsername", required = true)String OtherUsername) throws Exception{
		
		int result = 0;
    	if(!OtherUsername.equals("")&&!OtherUsername.equals("null")&&!OtherUsername.equals("undefined")){
			try(Connection connection = dataSource.getConnection()) {
				Statement statement = connection.createStatement();
				Statement statement2 = connection.createStatement();
				ResultSet rs = statement.executeQuery("SELECT * FROM user_relationship where name='"+userName+"';");
				while (rs.next()) {
					String[] Arr = (String[]) rs.getArray("following").getArray();
					String newArr = "{";
					for(int i = 0; i<Arr.length; i++) {
						if(!Arr[i].equals(OtherUsername)) {
							newArr += Arr[i] + ",";
						}
					}
					newArr += OtherUsername + "}";
					result = statement2.executeUpdate("UPDATE user_relationship SET following='"+newArr+"' WHERE name = '"+userName+"'");	
				}
			}
    	}
		if(result == 0) {
			System.out.println("failed to write following");
		}else {
			System.out.println("success to write following!");
		}
    	return result;
    	
    }
	
	@CrossOrigin(origins="http://localhost:8101")
    @RequestMapping("/unFollowing")
    boolean unFollowing(@RequestParam(value = "userName", required = true)String userName, @RequestParam(value = "OtherUsername", required = true)String OtherUsername) throws Exception{
		
		int result = 1;
		try(Connection connection = dataSource.getConnection()) {
			Statement statement = connection.createStatement();
			Statement statement2 = connection.createStatement();
			ResultSet rs = statement.executeQuery("SELECT * FROM user_relationship where name='"+userName+"';");
			while (rs.next()) {
				String[] Arr = (String[]) rs.getArray("following").getArray();
				String newArr = "{";
				for(int i = 0; i<Arr.length; i++) {
					if(!Arr[i].equals(OtherUsername)) {
						newArr += Arr[i] + ",";						
					}
				}
				if(newArr.length()>1) {
					newArr=newArr.substring(0, newArr.length()-1);
				}
				newArr += "}";
				result = statement2.executeUpdate("UPDATE user_relationship SET following='"+newArr+"' WHERE name = '"+userName+"'");
			}
		}
    	return true;
    	
    }		
	
	@CrossOrigin(origins="http://localhost:8100")
	@RequestMapping("/getFollowers")
    String getFollowers(@RequestParam(value = "userName", required = true)String userName) throws SQLException, IOException{

		JSONObject jsonObject = new JSONObject();
		System.out.println(userName);        
        try(Connection connection = dataSource.getConnection()) {
			Statement statement = connection.createStatement();
			ResultSet rs = statement.executeQuery("select name from user_relationship where '"+userName+"'=ANY(following);");
			LinkedList<String> list = new LinkedList();
			for(int i=0; rs.next(); i++) {
				list.add(rs.getString("name"));
			}
			String[] Arr = new String[list.size()];
			for(int i=0; i<list.size(); i++) {
				Arr[i] = list.get(i);
			}
			jsonObject.put("followers", Arr);
        }       
        ObjectMapper mapper = new ObjectMapper();
    	String stringJson = mapper.writeValueAsString(jsonObject);
    	System.out.println(stringJson);
		return stringJson;
		
	}
	
	@CrossOrigin(origins="http://localhost:8100")
	@RequestMapping("/liking")
    int liking(
    		@RequestParam(value = "likingUserName", required = true)String likingUserName, 
    		@RequestParam(value = "postedUserName", required = true)String postedUserName,
    @RequestParam(value = "imageName", required = true)String imageName)
    throws SQLException, IOException{
		
		System.out.println(likingUserName + postedUserName + imageName);
		int result = 1;        
        try(Connection connection = dataSource.getConnection()) {
			Statement statement = connection.createStatement();
			Statement statement2 = connection.createStatement();
			ResultSet rs = statement.executeQuery("select smiles from \""+postedUserName+"_image_information\" where image='"+imageName+"';");
			String newArr = "{";
			while (rs.next()) {
				if(rs.getArray("smiles") != null) {
					String[] Arr = (String[]) rs.getArray("smiles").getArray();
					for(int i = 0; i<Arr.length; i++) {
						if(!Arr[i].equals(likingUserName)) {
							newArr += Arr[i] + ",";
						}
					}	
				}
			}
			newArr += likingUserName + "}";
			System.out.println("new array : " + newArr);
			result = statement2.executeUpdate("UPDATE \""+postedUserName+"_image_information\" SET smiles='"+newArr+"' WHERE image = '"+imageName+"';");
        }
		return result;
		
	}
	
	@CrossOrigin(origins="http://localhost:8100")
	@RequestMapping("/unLiking")
    int unLiking(
    		@RequestParam(value = "unLikingUserName", required = true)String unLikingUserName, 
    		@RequestParam(value = "postedUserName", required = true)String postedUserName,
    @RequestParam(value = "imageName", required = true)String imageName)
    throws SQLException, IOException{
		
		System.out.println(unLikingUserName + postedUserName + imageName);
		int result = 1;       
        try(Connection connection = dataSource.getConnection()) {
			Statement statement = connection.createStatement();
			Statement statement2 = connection.createStatement();
			ResultSet rs = statement.executeQuery("select smiles from \""+postedUserName+"_image_information\" where image='"+imageName+"';");
			String newArr = "{";
			while (rs.next()) {
				if(rs.getArray("smiles") != null) {
					String[] Arr = (String[]) rs.getArray("smiles").getArray();
					for(int i = 0; i<Arr.length; i++) {
						if(!Arr[i].equals(unLikingUserName)) {
							newArr += Arr[i] + ",";
						}
					}	
					if(Arr.length != 1) {
						newArr=newArr.substring(0, newArr.length()-1);
					}
				}
			}
			newArr += "}";
			System.out.println("new array : " + newArr);
			result = statement2.executeUpdate("UPDATE \""+postedUserName+"_image_information\" SET smiles='"+newArr+"' WHERE image = '"+imageName+"';");
        }
		return result;
		
	}
	
	
	@CrossOrigin(origins="http://localhost:8100")
	@RequestMapping("/getImageInfo")
    String getImageInfo(@RequestParam(value = "userName", required = true)String userName, @RequestParam(value = "imageName", required = true)String imageName) throws SQLException, IOException, ParseException{

		System.out.println("requesting image : id="+userName+", imageName="+imageName);		
		if(userName != null && imageName != null) {		
			JSONObject jsonObject = new JSONObject();			       
	        try(Connection connection = dataSource.getConnection()) {
				Statement statement = connection.createStatement();
				Statement statement2 = connection.createStatement();
				ResultSet rs2 = statement2.executeQuery("select * from \""+userName+"_image_information\" where image='"+imageName+"';");				
				LinkedList<String> list = new LinkedList();				
				JSONArray jsonArray = new JSONArray();
				for(int l=0; rs2.next(); l++) {
					if(rs2.getArray("smiles") != null) {
						String[] Arr = (String[]) rs2.getArray("smiles").getArray();
						for(int i = 0; i<Arr.length; i++) {
								list.add(Arr[i]);
						}	
 					}					
					JSONParser parser = new JSONParser();
					if(rs2.getObject("applies") != null) {
						Object obj = parser.parse( rs2.getObject("applies").toString() );
						jsonArray = (JSONArray) obj;
						jsonObject.put("applies", jsonArray);
					}
					if(rs2.getString("postingText") != null) {
						String postingText = rs2.getString("postingText");
						jsonObject.put("postingText", postingText);
					}
					if(rs2.getString("triptitle") != null) {
						String tripTitle = rs2.getString("triptitle");
						jsonObject.put("tripTitle", tripTitle);
					}
				}
				String[] Arr = new String[list.size()];
				for(int i=0; i<list.size(); i++) {
					Arr[i] = list.get(i);
				}
				jsonObject.put("whoLiked", Arr);
			}	       
	        ObjectMapper mapper = new ObjectMapper();
	    	String stringJson = mapper.writeValueAsString(jsonObject);
			return stringJson;			
		}else {
			System.out.println("getInfo failed!");
			return "false";
		}
		
	}
	
    @CrossOrigin(origins="http://localhost:8100")
	@RequestMapping("/applying")
    int applying(
    		@RequestParam(value = "userName", required = true)String userName, 
    		@RequestParam(value = "userNameOfImage", required = true)String userNameOfImage,
    		@RequestParam(value = "imageName", required = true)String imageName,
    		@RequestParam(value = "applyText", required = true)String applyText
    		)
    throws SQLException, IOException, ParseException{
    	
		System.out.println(userName);
		System.out.println(userNameOfImage);
		System.out.println(imageName);
		System.out.println(applyText);
		int result = 0;
        try(Connection connection = dataSource.getConnection()) {
        	JSONObject jsonObj = new JSONObject();
			Statement statement = connection.createStatement();
			Statement statement2 = connection.createStatement();
			ResultSet rs = statement.executeQuery("select * from \""+userNameOfImage+"_image_information\" where image='"+imageName+"';");
			JSONArray jsonArray = new JSONArray();
			for(int l=0; rs.next(); l++) {
				JSONParser parser = new JSONParser();
				if(rs.getObject("applies") != null) {
					Object obj = parser.parse( rs.getObject("applies").toString() );
					jsonArray = (JSONArray) obj;
				}
				jsonObj.put("name", userName);
				jsonObj.put("apply", applyText);
				if(rs.getString("image") == null) {
					jsonArray.add(jsonObj);
					result = statement2.executeUpdate("insert into \""+userNameOfImage+"_image_information\"(image, applies) values('"+imageName+"', '"+jsonArray+"');");	
				}else {
					jsonArray.add(jsonObj);
					result = statement2.executeUpdate(" update \""+userNameOfImage+"_image_information\" set applies='"+jsonArray+"' where image='"+imageName+"';");	
				}
			};
        }
        if(result == 1) {
			System.out.println("applying success!");
	    	return result;
		}else {
			System.out.println("applying failed!");
			return result;
		}
		
	}
    
    @CrossOrigin(origins="http://localhost:8100")
    @RequestMapping("/applyingIOS")
    int applyingIOS(
    		@RequestBody HashMap<String, Object> map, 
    		@RequestParam(value = "userName", required = true)String userName, 
    		@RequestParam(value = "userNameOfImage", required = true)String userNameOfImage,
    		@RequestParam(value = "imageName", required = true)String imageName
    		) 
    throws Exception{
    	
		int result = 0;
		System.out.println(userName);
		System.out.println(userNameOfImage);
		System.out.println(imageName);
		System.out.println(map.get("applyText"));
        try(Connection connection = dataSource.getConnection()) {
        	JSONObject jsonObj = new JSONObject();
			Statement statement = connection.createStatement();
			Statement statement2 = connection.createStatement();
			ResultSet rs = statement.executeQuery("select * from \""+userNameOfImage+"_image_information\" where image='"+imageName+"';");
			JSONArray jsonArray = new JSONArray();
			for(int l=0; rs.next(); l++) {
				JSONParser parser = new JSONParser();
				if(rs.getObject("applies") != null) {
					Object obj = parser.parse( rs.getObject("applies").toString() );
					jsonArray = (JSONArray) obj;
				}
				jsonObj.put("name", userName);
				jsonObj.put("apply", map.get("applyText"));
				if(rs.getString("image") == null) {
					jsonArray.add(jsonObj);
					result = statement2.executeUpdate("insert into \""+userNameOfImage+"_image_information\"(image, applies) values('"+imageName+"', '"+jsonArray+"');");	
				}else {
					jsonArray.add(jsonObj);
					result = statement2.executeUpdate(" update \""+userNameOfImage+"_image_information\" set applies='"+jsonArray+"' where image='"+imageName+"';");	
				}
			};
        }
        if(result == 1) {
			System.out.println("applying success!");
	    	return result;
		}else {
			System.out.println("applying failed!");
			return result;
		}
		
    }
    
    @CrossOrigin(origins="http://localhost:8100")
    @RequestMapping("/unApplying")
    boolean unApplying(
    		@RequestParam("applies") String applies, 
    		@RequestParam(value = "userNameOfImage", required = false)String userNameOfImage, 
    		@RequestParam(value = "imageName", required = false)String imageName 
    		) 
    throws Exception{
    	
    	int result = 0;
    	if(applies == "") {
    		return false;
    	}else {
    		String newString = "[" + applies + "]";
        	try(Connection connection = dataSource.getConnection()) {
            	JSONObject jsonObj = new JSONObject();
    			Statement statement = connection.createStatement();
    			result = statement.executeUpdate(" update \""+userNameOfImage+"_image_information\" set applies='"+newString+"' where image='"+imageName+"';");	
            }
        	if(result == 1) {
    			System.out.println("unapplying success!");
    	    	return true;
    		}else {
    			System.out.println("unapplying failed!");
    			return false;
    		}
    	}
    	
    }
    
    @CrossOrigin(origins="http://localhost:8100")
    @RequestMapping("/unApplyingIOS")
    boolean unApplyingIOS(
    		@RequestBody HashMap<String, Object> map, 
    		@RequestParam(value = "userNameOfImage", required = false)String userNameOfImage, 
    		@RequestParam(value = "imageName", required = false)String imageName
    		) 
    throws Exception{
    	
		int result = 0;
		if(map.get("applies") == "") {
		}else {
			String applies = (String) map.get("applies");
			String newString = "[" + applies + "]";
        	try(Connection connection = dataSource.getConnection()) {
            	JSONObject jsonObj = new JSONObject();
    			Statement statement = connection.createStatement();
    			result = statement.executeUpdate(" update \""+userNameOfImage+"_image_information\" set applies='"+newString+"' where image='"+imageName+"';");	
            }
		}
		if(result == 1) {
			System.out.println("unapplying in ios success!");
	    	return true;
		}else {
			System.out.println("unapplying in ios failed!");
			return false;
		}
		
    }
    
    @CrossOrigin(origins="http://localhost:8100")
    @RequestMapping("/saveNtf")
    boolean saveNtf(@RequestParam("notificationObj") String notificationObj ) throws Exception{   
    	
		System.out.println(notificationObj);
		JSONParser parser = new JSONParser();
		Object obj = parser.parse( notificationObj );
		JSONObject jsonObj = (JSONObject) obj;
		String otherUserName = (String) jsonObj.get("otherUserName");
		Boolean isThereSavedNtf = false; 
        try(Connection connection = dataSource.getConnection()) {
			Statement statement = connection.createStatement();
			Statement statement2 = connection.createStatement();
			Statement statement3 = connection.createStatement();
			ResultSet rs = statement.executeQuery("select * from user_notification where name='"+otherUserName+"';");
			for(int l=0; rs.next(); l++) {
				isThereSavedNtf = true;
				int result = 0;
	        	JSONArray jsonArray = new JSONArray();
				Object object = parser.parse( rs.getObject("notification").toString() );
				jsonArray = (JSONArray) object;
				JSONArray newArray = new JSONArray();
				System.out.println(jsonArray.size());
				if(jsonArray.size() == 30) {
					for(int i=1; i<jsonArray.size(); i++) {
						newArray.add(jsonArray.get(i));
					}
				}else {
					newArray = jsonArray;
				}
				newArray.add(jsonObj);
				result = statement2.executeUpdate(" update user_notification set notification='"+newArray+"' where name='"+otherUserName+"';");	
			};
			if(!isThereSavedNtf) {
	        	int result = 0;
	        	JSONArray jsonArray = new JSONArray();
	        	jsonArray.add(jsonObj);
				result = statement3.executeUpdate("insert into user_notification(name, notification) values('"+otherUserName+"', '"+jsonArray+"');");	
	        }
        }
    	return true;
    	
    }
	
	@CrossOrigin(origins="http://localhost:8100")
    @RequestMapping("/saveNtfIOS")
    boolean saveNtfIOS(@RequestBody HashMap<String, String> map) throws Exception{
		
		JSONParser parser = new JSONParser();
		Object obj = parser.parse( map.get("notificationObj") );
		JSONObject jsonObj = (JSONObject) obj;
		String otherUserName = (String) jsonObj.get("otherUserName");
		Boolean isThereSavedNtf = false; 
        try(Connection connection = dataSource.getConnection()) {
			Statement statement = connection.createStatement();
			Statement statement2 = connection.createStatement();
			Statement statement3 = connection.createStatement();
			ResultSet rs = statement.executeQuery("select * from user_notification where name='"+otherUserName+"';");
			for(int l=0; rs.next(); l++) {
				isThereSavedNtf = true;
				int result = 0;
	        	JSONArray jsonArray = new JSONArray();
				Object object = parser.parse( rs.getObject("notification").toString() );
				jsonArray = (JSONArray) object;
				JSONArray newArray = new JSONArray();
				System.out.println(jsonArray.size());
				if(jsonArray.size() == 30) {
					for(int i=1; i<jsonArray.size(); i++) {
						newArray.add(jsonArray.get(i));
					}
				}else {
					newArray = jsonArray;
				}
				newArray.add(jsonObj);
				result = statement2.executeUpdate(" update user_notification set notification='"+newArray+"' where name='"+otherUserName+"';");	
			};
			if(!isThereSavedNtf) {
	        	int result = 0;
	        	JSONArray jsonArray = new JSONArray();
	        	jsonArray.add(jsonObj);
				result = statement3.executeUpdate("insert into user_notification(name, notification) values('"+otherUserName+"', '"+jsonArray+"');");	
	        }
        }
    	return true;
    	
    }
	
	@CrossOrigin(origins="http://localhost:8100")
	@RequestMapping("/getNotification")
    String getNotification(@RequestParam(value = "userName", required = true)String userName) throws SQLException, IOException, ParseException{
		
		JSONParser parser = new JSONParser();
		JSONArray jsonArray = new JSONArray();
		if(userName != null) {
			JSONObject jsonObject = new JSONObject();
	        try(Connection connection = dataSource.getConnection()) {
				Statement statement = connection.createStatement();
				ResultSet rs = statement.executeQuery("select * from user_notification where name='"+userName+"';");
				for(int i=0; rs.next(); i++) {
					Object object = parser.parse( rs.getObject("notification").toString() );
					jsonArray = (JSONArray) object;
				}
	        }		
	        ObjectMapper mapper = new ObjectMapper();
	    	String stringJson = mapper.writeValueAsString(jsonArray);
			return stringJson;
		}else {
			System.out.println("getNotification failed!");
			return "false";
		}
		
	}
	
	@CrossOrigin(origins="http://localhost:8100")
    @RequestMapping("/feedBackIOS")
    int feedBackIOS(
    		@RequestBody HashMap<String, Object> map, 
    		@RequestParam(value = "userName", required = true)String userName
    		) 
    throws Exception{
		
		int result = 0;
		System.out.println(userName);
		System.out.println(map.get("feedBackText"));
		System.out.println(map.get("createdAt"));
        try(Connection connection = dataSource.getConnection()) {
        	JSONObject jsonObj = new JSONObject();
			Statement statement = connection.createStatement();
			Statement statement2 = connection.createStatement();
			ResultSet rs = statement.executeQuery("select feedback from user_information where name='"+userName+"';");
			JSONArray jsonArray = new JSONArray();
			for(int l=0; rs.next(); l++) {
				JSONParser parser = new JSONParser();
				if(rs.getObject("feedback") != null) {
					Object obj = parser.parse( rs.getObject("feedback").toString() );
					jsonArray = (JSONArray) obj;
				}
				jsonObj.put("feedback", map.get("feedBackText"));
				jsonObj.put("createdAt", map.get("createdAt"));
				jsonArray.add(jsonObj);
				result = statement2.executeUpdate(" update user_information set feedback='"+jsonArray+"' where name='"+userName+"';");	
			};
        }
        if(result == 1) {
			System.out.println("applying success!");
	    	return result;
		}else {
			System.out.println("applying failed!");
			return result;
		}
		
    }
	
	@CrossOrigin(origins="http://localhost:8100")
	@RequestMapping("/getUserFeedBack")
    String getUserFeedBack(@RequestParam(value = "userName", required = true)String userName) throws SQLException, IOException, ParseException{

		JSONObject jsonObject = new JSONObject();
		System.out.println(userName);
		JSONArray jsonArray = new JSONArray();
        try(Connection connection = dataSource.getConnection()) {
			Statement statement = connection.createStatement();
			Statement statement2 = connection.createStatement();
			ResultSet rs = statement.executeQuery("select feedback from user_information where name = '"+userName+"';");
			JSONParser parser = new JSONParser();
			for(int i=0; rs.next(); i++) {
				if(rs.getObject("feedback") != null) {
					Object obj = parser.parse( rs.getObject("feedback").toString() );
					JSONArray beforeJson = (JSONArray) obj;
					for(int m=0; m<beforeJson.size(); m++) {
						JSONObject feedObj = (JSONObject) beforeJson.get(m);
						feedObj.put("name", userName);
						jsonArray.add(feedObj);
					}
					if(userName.equals("onearth_official")){
						System.out.println("official!");
						ResultSet rs2 = statement2.executeQuery("select name, feedback from user_information;");
						for(int j=0; rs2.next(); j++) {
							if( rs2.getObject("feedback") != null) {
								Object obj2 = parser.parse( rs2.getObject("feedback").toString() );
								String usersName = rs2.getString("name");
								JSONArray usersFeed = (JSONArray) obj2;
								for(int p=0; p<usersFeed.size(); p++) {
									JSONObject feedObj = (JSONObject) usersFeed.get(p);
									feedObj.put("name", usersName);
									jsonArray.add(feedObj);
								}
							}
						}
					}
					jsonObject.put("feedBack", jsonArray);
				}
				System.out.println(jsonObject.get("feedBack"));
			}
        }
        ObjectMapper mapper = new ObjectMapper();
    	String stringJson = mapper.writeValueAsString(jsonObject);
    	System.out.println(stringJson);
		return stringJson;
		
	}
	
	@CrossOrigin(origins="http://localhost:8100")
    @RequestMapping("/getMainFeed")
    String getMainFeed(
    		@RequestBody HashMap<String, Object> map, 
    		@RequestParam(value = "userName", required = true)String userName
    		) 
    throws Exception{
		
		int result = 0;
		int feedState = (int) map.get("feedState");
		System.out.println(userName);
		System.out.println(map.get("following"));
		JSONArray tripTitles = new JSONArray();
		ArrayList following = (ArrayList) map.get("following");
		for(int i=0; i<following.size(); i++) {
			try(Connection connection = dataSource.getConnection()) {
				Statement statement = connection.createStatement();
				ResultSet rs = statement.executeQuery("select distinct triptitle, createdat from \""+following.get(i)+"_image_information\";");
				JSONArray jsonArray = new JSONArray();
				for(int l=0; rs.next(); l++) {
					if(rs.getObject("createdat") != null) {
						JSONParser parser = new JSONParser();
						Object obj = parser.parse( rs.getObject("createdat").toString() );
						jsonArray = (JSONArray) obj;
						
						JSONObject tripTitle = new JSONObject();
						tripTitle.put("followUserName", following.get(i));
						tripTitle.put("tripTitle", rs.getString("triptitle"));
						tripTitle.put("createdAt", jsonArray.get(0));
						tripTitles.add(tripTitle);
					}
				};
	        }
		}
		try(Connection connection = dataSource.getConnection()) {
			Statement statement2 = connection.createStatement();	
			ResultSet rs = statement2.executeQuery("select distinct triptitle, createdat from \""+userName+"_image_information\";");
			JSONArray jsonArray = new JSONArray();
			for(int l=0; rs.next(); l++) {
				if(rs.getObject("createdat") != null) {
					JSONParser parser = new JSONParser();
					Object obj = parser.parse( rs.getObject("createdat").toString() );
					jsonArray = (JSONArray) obj;
					JSONObject tripTitle = new JSONObject();
					tripTitle.put("followUserName", userName);
					tripTitle.put("tripTitle", rs.getString("triptitle"));
					tripTitle.put("createdAt", jsonArray.get(0));
					tripTitles.add(tripTitle);
				}
			};
		}
		JSONArray sortedTripTitles = new JSONArray();
	    List<JSONObject> jsonValues = new ArrayList<JSONObject>();
	    for (int i = 0; i < ((JSONArray) tripTitles).size(); i++) {
	        jsonValues.add((JSONObject) tripTitles.get(i));
	    }
	    Collections.sort( jsonValues, new Comparator<JSONObject>() {
	        private static final String KEY_NAME = "createdAt";
	        @Override
	        public int compare(JSONObject a, JSONObject b) {
	            String valA = new String();
	            String valB = new String();
	            Long yearA = (Long) ((JSONObject) a.get(KEY_NAME)).get("year");
	            Long monthA = (Long) ((JSONObject) a.get(KEY_NAME)).get("month");
	            Long dateA = (Long) ((JSONObject) a.get(KEY_NAME)).get("date");
	            Long hourA = (Long) ((JSONObject) a.get(KEY_NAME)).get("hour");
	            Long minuteA = (Long) ((JSONObject) a.get(KEY_NAME)).get("minute");
	            
	            Long yearB = (Long) ((JSONObject) b.get(KEY_NAME)).get("year");
	            Long monthB = (Long) ((JSONObject) b.get(KEY_NAME)).get("month");
	            Long dateB = (Long) ((JSONObject) b.get(KEY_NAME)).get("date");
	            Long hourB = (Long) ((JSONObject) b.get(KEY_NAME)).get("hour");
	            Long minuteB = (Long) ((JSONObject) b.get(KEY_NAME)).get("minute");
	            
	            if(!yearA.equals(yearB)) {
	            	return -yearA.compareTo(yearB);
	            }else if(!monthA.equals(monthB)) {
	            	return -monthA.compareTo(monthB);
	            }else if(!dateA.equals(dateB)) {
	            	return -dateA.compareTo(dateB);
	            }else if(!hourA.equals(hourB)) {
	            	return -hourA.compareTo(hourB);
	            }else if(!minuteA.equals(minuteB)) {
	            	return -minuteA.compareTo(minuteB);
	            }else {
	            	return 0;
	            }	            
	        }
	    });
	    for (int i = 0; i < ((JSONArray) tripTitles).size(); i++) {
	    	sortedTripTitles.add(jsonValues.get(i));
	    }
	    JSONArray mainFeedArr = new JSONArray();
	    ArrayList resolved = (ArrayList) map.get("feedResolves");
	    int startIndex = 0;
	    int endIndex = 0;
	    if((feedState == 0) && !resolved.isEmpty()) {
	    	JSONObject newJson = new JSONObject((Map) resolved.get(0));
	    	JSONObject newJsonPosting = new JSONObject((Map) ((ArrayList) newJson.get("posting")).get(0));
	    	String postingUserName = (String) newJsonPosting.get("userName");
	    	String tripTitle = (String) newJsonPosting.get("triptitle");
	    	JSONObject createdAt = new JSONObject((Map) newJsonPosting.get("createdAt"));
	    	for(int l=0; l<sortedTripTitles.size(); l++) {
	    		if(((JSONObject) sortedTripTitles.get(l)).get("followUserName").equals(postingUserName)&&((JSONObject) sortedTripTitles.get(l)).get("tripTitle").equals(tripTitle)&&((JSONObject) sortedTripTitles.get(l)).get("createdAt").toString().equals(createdAt.toString())) {
	    			endIndex = l;
	    			break;
	    		}
	    	}
	    } else if((feedState == 0) && resolved.isEmpty()) {
	    	if(sortedTripTitles.size() > 10) {
		    	endIndex = 10;
	    	}else {
	    		endIndex = sortedTripTitles.size();
	    	}
	    } else if((feedState != 0) && resolved.isEmpty()) {
	    	startIndex = 0;
	    	endIndex = 0;
	    }else {
	    	JSONObject newJson = new JSONObject((Map) resolved.get(resolved.size()-1));
	    	JSONObject newJsonPosting = new JSONObject((Map) ((ArrayList) newJson.get("posting")).get(0));
	    	String postingUserName = (String) newJsonPosting.get("userName");
	    	String tripTitle = (String) newJsonPosting.get("triptitle");
	    	System.out.println("tripTitle : " + tripTitle);
	    	JSONObject createdAt = new JSONObject((Map) newJsonPosting.get("createdAt"));
	    	for(int l=0; l<sortedTripTitles.size(); l++) {
	    		if(((JSONObject) sortedTripTitles.get(l)).get("followUserName").equals(postingUserName)&&((JSONObject) sortedTripTitles.get(l)).get("tripTitle").equals(tripTitle)&&((JSONObject) sortedTripTitles.get(l)).get("createdAt").toString().equals(createdAt.toString())) {
	    			startIndex = l+1;
	    			if(sortedTripTitles.size()<l+7) {
	    				endIndex = sortedTripTitles.size();
	    			}else {
		    			endIndex = l+6;
	    			}
	    			break;
	    		}
	    	}
	    }
	    System.out.println("sortedTripTitles : " + sortedTripTitles.size());
    	System.out.println("start index : "+startIndex);
    	System.out.println("end index : "+endIndex);
	    if(sortedTripTitles.size() != 0) {
	    	 for(int i=startIndex; i<endIndex; i++) {
	 		    JSONObject mainPosting = new JSONObject();
	 	    	JSONArray images = new JSONArray();
	 	    	try(Connection connection = dataSource.getConnection()) {
	 				Statement statement = connection.createStatement();
	 				ResultSet rs = statement.executeQuery("select * from \""+((JSONObject)sortedTripTitles.get(i)).get("followUserName")+"_image_information\" where triptitle = '"+((JSONObject)sortedTripTitles.get(i)).get("tripTitle")+"';");
	 				for(int l=0; rs.next(); l++) {
	 					JSONObject imageObject = new JSONObject();
	 					imageObject.put("userName", ((JSONObject)sortedTripTitles.get(i)).get("followUserName"));
	 					imageObject.put("imageName", rs.getString("image"));
	 					imageObject.put("latitude", rs.getDouble("latitude"));
	 					imageObject.put("longitude", rs.getDouble("longitude"));
	 					imageObject.put("targetimage", rs.getString("targetimage"));
	 					imageObject.put("triptitle", rs.getString("triptitle"));
	 					imageObject.put("postingcategory", rs.getString("postingcategory"));
	 					imageObject.put("postingdate", rs.getString("postingdate"));
	 					imageObject.put("postingText", rs.getString("postingText"));
	 					imageObject.put("startdate", rs.getString("startdate"));
	 					imageObject.put("enddate", rs.getString("enddate"));
	 					imageObject.put("tripgeocode", rs.getString("tripgeocode"));
	 					JSONParser parser = new JSONParser();
	 					Object obj = parser.parse( rs.getObject("createdat").toString() );
	 					JSONArray jsonArray = (JSONArray) obj;
	 					imageObject.put("createdAt", jsonArray.get(0));
	 					if(rs.getArray("smiles") != null) {
	 						String[] smilesArr = (String[]) rs.getArray("smiles").getArray();
	 						imageObject.put("smiles", smilesArr.length);
	 					}else {
	 						imageObject.put("smiles", 0);
	 					}
						if(rs.getObject("applies") != null) {
							Object obj2 = parser.parse( rs.getObject("applies").toString() );
							jsonArray = (JSONArray) obj2;
							imageObject.put("applies", jsonArray);
						}
	 					images.add(imageObject);
	 				}
	 				mainPosting.put("posting", images);
	 				mainFeedArr.add(mainPosting);
	 			};
	         }
	    }
	    ObjectMapper mapper = new ObjectMapper();
    	String stringJson = mapper.writeValueAsString(mainFeedArr);
    	return stringJson;
		
    }
	
	@CrossOrigin(origins="http://localhost:8100")
	@RequestMapping("/getTodayPostings")
    String getTodayPostings() throws SQLException, IOException{
    
		ObjectMapper mapper = new ObjectMapper();
		String stringJson = mapper.writeValueAsString(todayPostings);
		return stringJson;
			
	}
	
	@CrossOrigin(origins="http://localhost:8100")
	@RequestMapping("/getUsersList")
    String getUsersList() throws SQLException, IOException{

		JSONObject jsonObject = new JSONObject();
		String[] Arr = new String[membersName.size()];
		for(int i=0; i<membersName.size(); i++) {
			Arr[i] = (String) membersName.get(i);
		}
		jsonObject.put("usersList", Arr);
        ObjectMapper mapper = new ObjectMapper();
    	String stringJson = mapper.writeValueAsString(jsonObject);
    	System.out.println(stringJson);
		return stringJson;
		
	}
	
	@CrossOrigin(origins="http://localhost:8100")
	@RequestMapping("/setTutorialState")
	int	setTutorialState(@RequestParam(value = "userName", required = true)String userName, @RequestParam(value = "state", required = true)String state) throws SQLException, IOException, ParseException{
		
		if(userName != null) {
	        try(Connection connection = dataSource.getConnection()) {
				Statement statement = connection.createStatement();
				int rs = 0;
				if(state.equals("posting")) {
					rs = statement.executeUpdate("update user_information set postingtutorialstate = 'true' where name='"+userName+"';");
				}else {
					rs = statement.executeUpdate("update user_information set tutorialstate = 'true' where name='"+userName+"';");
				}
				return rs;
	        }				
		}else {
			System.out.println("set tutorial state failed!");
			return 0;
		}
	}

}
